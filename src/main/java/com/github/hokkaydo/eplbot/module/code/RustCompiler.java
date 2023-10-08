package com.github.hokkaydo.eplbot.module.code;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

public class RustCompiler implements Runner{
    private static final String CURRENT_DIR = System.getProperty("user.dir") + "\\src\\temp\\";
    @Override
    public String run(String input, Integer runTimeout) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'std' or 'use' are used";
        }
        try {

            File sourceFile = new File(CURRENT_DIR, "temp.rs");
            sourceFile.deleteOnExit();
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(input);
            writer.close();

            Process compile = new ProcessBuilder("rustc", sourceFile.getAbsolutePath()).directory(new File(CURRENT_DIR)).redirectErrorStream(true).start();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
 
            int compileExitCode = compile.waitFor();
            InputStream inputStream = compile.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            if (compileExitCode == 0) {
                String executableFileName = sourceFile.getName().replace(".rs", "");
                File executableFile = new File(CURRENT_DIR, executableFileName);
                executableFile.setExecutable(true);
                Process run = new ProcessBuilder(new File(CURRENT_DIR, executableFileName).getAbsolutePath()).directory(new File(CURRENT_DIR)).redirectErrorStream(true).start();
                Thread timeoutThread = new Thread(() -> {
                    try {
                        Thread.sleep(1000L*runTimeout);
                        run.destroy();
                    } catch (InterruptedException ignored) {}
                });
                timeoutThread.start();
                outputStream.reset();
                inputStream = run.getInputStream();
                buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                if (run.waitFor() == 0) {
                    timeoutThread.interrupt();
                    deleteFiles();
                    return outputStream.toString();
                } else {
                    timeoutThread.interrupt();
                    deleteFiles();
                    String output = outputStream.toString().trim();
                    if (output.isEmpty()) {
                        return "Run failed: Timelimit exceeded "+ runTimeout +" s";
                    } else {
                        return "Run failed:\n" + output;
                    }
                }
            } else {
                deleteFiles();
                return "Compilation failed:\n" + outputStream;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
    private static boolean containsUnsafeKeywords(String code) {
        String[] unsafeKeywords = {"std", "use"};
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
            for (File file : new File(CURRENT_DIR).listFiles()) {
                if (file.isFile() && file.getName().startsWith("temp")) {
                    String[] validExtensions = {".exe", ".pdb", ".rs"};
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