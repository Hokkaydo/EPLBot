package com.github.hokkaydo.eplbot.module.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
public class PythonRunner implements Runner {
    private static final String CURRENT_DIR = System.getProperty("user.dir") + "\\src\\temp\\";
    @Override
    public String run(String input, Integer runTimeout) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'exec' or 'eval' are used";
        }
        try {
            File sourceFile = new File(CURRENT_DIR,"temp.py");
            sourceFile.deleteOnExit();
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(input);
            writer.close();
            Process process = new ProcessBuilder("python",  sourceFile.getAbsolutePath()).redirectErrorStream(true).start();
            Thread timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(1000L * runTimeout);
                    process.destroy();
                } catch (InterruptedException ignored) {
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
                deleteFiles();
                return output.toString();
            } else {
                timeoutThread.interrupt();
                deleteFiles();
                String outProcess = output.toString();
                if (outProcess.isEmpty()) {
                    return "Run failed: Timelimit exceeded " + runTimeout + " s";
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
        String[] unsafeKeywords = {"exec", "eval", "subprocess", "os.system", "open", "__import__", "pickle"};
        for (String keyword : unsafeKeywords) {
            if (code.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    private static void deleteFiles(){
        File outputDirectory = new File(CURRENT_DIR);
        File[] files = outputDirectory.listFiles();
        if (files == null){
            System.out.println("NPE trying to delete created files");
        } else {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("temp")) {
                    String[] validExtensions = {".py"};
                    boolean shouldDelete = false;
                    for (String extension : validExtensions) {
                        if (file.getName().equals("temp"+extension)) {
                            shouldDelete = true;
                            break;
                        }
                    }
                    if (shouldDelete) {
                        file.delete();
                    }
                }
            }
        }
    }
}
    