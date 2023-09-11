package com.github.hokkaydo.eplbot.dllrun;
public class PythonRunner {
    private static native String run(String input);

    static {
        System.load(System.getProperty("user.dir") + "/src/main/java/com/github/hokkaydo/eplbot/dllrun/mylib/target/debug/mylib.dll");
    }
    public static String runPython(String code) {
        String output = PythonRunner.run(code);
        return output;
    }
}
