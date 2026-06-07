package com.github.hokkaydo.eplbot.module.code.java;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.code.GlobalRunner;
import com.github.hokkaydo.eplbot.module.code.Runner;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.regex.Pattern;


public class JavaRunner implements Runner {

    String processId;
    GlobalRunner runner;
    public JavaRunner(String processId){
        this.processId = processId;
        this.runner = new GlobalRunner("java-runner", processId, 64); // JVM spawns many threads; 64 prevents fork bombs while allowing normal execution
    }


    private static final String WRAPPER_TEMPLATE = """
        import java.util.*;
        import java.lang.Math;

        public class Main {
            public static void main(String[] args){
                %s
            }
        }""";

    /**
     * Wraps the code if necessary and run it
     * @param code the code to be run
     * @param timeout a timeout to avoid permanent loops
     * @return a Pair of the result and the exit code
     */
    @Override
    public Pair<String,Integer> run(String code, Integer timeout) {
        if (requiresWrapper(code)){
            code = WRAPPER_TEMPLATE.formatted(code);
        }
        if (!containsMainClass(code)){
            return Pair.of(Strings.getString("command.code.no_main_class_found"),1);
        }
        if (!hasMainMethod(code)){
            return Pair.of(Strings.getString(".code.no_main_method_found"),1);
        }
        return this.runner.run(code, timeout);
    }

    /**
     * @param code the submitted code
     * @return true if the code contains a public class {class} false otherwise
     */
    private static boolean requiresWrapper(String code) {
        boolean hasClass = Pattern.compile("\\bpublic\\s+class\\s+[A-Z][a-zA-Z0-9]*").matcher(code).find();
        return !hasClass;
    }
    /**
     * @param code the submitted code
     * @return true if the code contains a public class Main with a main method false otherwise
     */
    public static boolean containsMainClass(String code) {
        return Pattern.compile(
                "(?s)\\bpublic\\s+class\\s+Main\\b.*?\\{.*?\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*]\\s*\\w*\\s*\\).*?\\{.*?}\\s*}"
        ).matcher(code).find();
    }
    /**
     * @param code the submitted code
     * @return true if the code contains a public static void main() method false otherwise
     */
    public static boolean hasMainMethod(String code){
        return Pattern.compile(
                "\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String\\s*\\[\\s*]\\s*\\w*\\s*\\)"
        ).matcher(code).find();    }
}
