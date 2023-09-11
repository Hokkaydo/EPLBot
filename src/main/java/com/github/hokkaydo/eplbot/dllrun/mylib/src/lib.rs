use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use pyo3::prelude::*;
use pyo3::types::PyTuple;
#[no_mangle]
pub extern "system" fn Java_PythonRunner_run<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    input: JString<'local>)-> jstring {
    let input: String = env
    .get_string(&input)
    .expect("Error parsing input string")
    .into();
    let mut result_str = String::new();
    pyo3::prepare_freethreaded_python();
    Python::with_gil(|py| {
        let func: Py<PyAny> = PyModule::from_code(
            py,
            r#"def wrapper(*args):
                import traceback as tr
                import numpy as np
                import scipy as sci
                code = args[0]
                result = []
                def pprint(*args):
                    result.append(" ".join([str(x) for x in args]))
                env = globals().copy()
                env["pprint"] = pprint
                code = code.replace("print","pprint").replace("import","").replace("open","").replace("eval","").replace("exec","")
                try:
                    exec(code, env)
                except Exception:
                    result = [tr.format_exc()]
                return ("\n").join(result)"#,
            "",
            "",
        ).unwrap()
        .getattr("wrapper")
        .unwrap()
        .into();

        let args = PyTuple::new(py, &[input]);
        if let Ok(py_result) = func.call1(py,args) {
            if let Ok(result) = py_result.extract::<String>(py) {
                result_str = result;
            }
        }

    });
    let output = env
        .new_string(result_str)
        .expect("Couldn't create Java string!");

    output.into_raw()
}