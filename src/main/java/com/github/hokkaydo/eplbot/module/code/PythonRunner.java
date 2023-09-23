package com.github.hokkaydo.eplbot.module.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
public class PythonRunner {
    public static String run(String input) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'exec' or 'eval' are used";
        }
        try {
            Process process = new ProcessBuilder("python", "-c", input).redirectErrorStream(true).start();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine()) != null) {
                output.append(line).append("\n");
            }

            if (process.waitFor() == 0) {
                return output.toString();
            } else {
                return "Python execution failed with exit code " + process.toString();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "An error occurred: " + e.getMessage();
        }
    }
    private static boolean containsUnsafeKeywords(String code) {
        String[] unsafeKeywords = {"std", "use", "exec", "eval", "subprocess", "os.system", "open", "__import__", "pickle"};
        for (String keyword : unsafeKeywords) {
            if (code.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

}
    
