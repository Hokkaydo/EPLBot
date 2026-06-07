package com.github.hokkaydo.eplbot.module.code.c;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.code.GlobalRunner;
import com.github.hokkaydo.eplbot.module.code.Runner;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.regex.Pattern;

public class CRunner implements Runner {
    String processId;
    GlobalRunner runner;
    public CRunner(String processId){
        this.processId = processId;
        this.runner = new GlobalRunner("c-runner", processId, 16);
    }
    private static final String WRAPPER_TEMPLATE = """
        #include <stdlib.h>
        #include <string.h>
        #include <stdio.h>
        int main(int argc, char *argv[]){
            %s
            return 0;
        }""";
    /**
     * Wraps the code if necessary and run it
     * @param code the code to be run
     * @param timeout a timeout to avoid permanent loops
     * @return a Pair of the result and the exit code
     */
    @Override
    public Pair<String, Integer> run(String code, Integer timeout) {
        if (!containsAnyFunction(code)){
            code = WRAPPER_TEMPLATE.formatted(code);
        }
        if (!containsMainFunction(code)){
            return Pair.of(Strings.getString("command.code.no_main_method_found"),1);
        }
        return this.runner.run(code, timeout);
    }
    /**
     * @param code the submitted code
     * @return true if the code contains an int main method false otherwise
     */
    private static boolean containsMainFunction(String code) {

        return Pattern.compile("\\bint\\s+main\\s*\\([^)]*\\)\\s*\\{").matcher(code).find();
    }
    /**
     * @param code the submitted code
     * @return true if the code contains any method false otherwise
     */
    private static boolean containsAnyFunction(String code){
        return Pattern.compile("\\b\\w+\\s+\\w+\\s*\\([^;{=]*\\)\\s*\\{").matcher(code).find();

    }
}
