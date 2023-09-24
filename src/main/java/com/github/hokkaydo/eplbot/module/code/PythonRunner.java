package com.github.hokkaydo.eplbot.module.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.github.hokkaydo.eplbot.Strings;
public class PythonRunner {
    public static String run(String input) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'exec' or 'eval' are used";
        }
        try {
            Process process = new ProcessBuilder("python", "-c", input.replace("\"", "'")).redirectErrorStream(true).start();
            Thread timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(1000 * Integer.parseInt(Strings.getString("COMMAND_CODE_TIMELIMIT")));
                    System.out.println("Thread cancel");
                    process.destroy();
                } catch (InterruptedException e) {
                }
            });
            timeoutThread.start();
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine()) != null) {
                output.append(line).append("\n");
            }
            if (process.waitFor() == 0) {
                timeoutThread.interrupt();
                return output.toString();
            } else {
                timeoutThread.interrupt();
                String outProcess = output.toString();
                if (outProcess.isEmpty()) {
                    return "Run failed: Timelimit exceeded " + Strings.getString("COMMAND_CODE_TIMELIMIT") + " s";
                } else {
                    return "Run failed:\n" + outProcess;
                }
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
    
