package com.github.hokkaydo.eplbot.module.code.RustLoader;
import com.github.hokkaydo.eplbot.Strings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.util.concurrent.TimeUnit;

public class RustCompiler {

    public static String run(String input) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'std' or 'use' are used";
        }
        try {

            String currentDir = System.getProperty("user.dir") + "/src/main/java/com/github/hokkaydo/eplbot/module/code/RustLoader/temp/";
            File currentDirFile = new File(currentDir);
            File sourceFile = new File(currentDir, "temp.rs");
            sourceFile.deleteOnExit();
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(input);
            writer.close();
        
            ProcessBuilder compileProcess = new ProcessBuilder("rustc", sourceFile.getAbsolutePath());
            compileProcess.directory(new File(currentDir)); // Set the working directory
            compileProcess.redirectErrorStream(true);
            Process compile = compileProcess.start();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            compileProcess.redirectErrorStream(true);
 
            int compileExitCode = compile.waitFor();
            InputStream inputStream = compile.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            if (compileExitCode == 0) {
                String executableFileName = sourceFile.getName().replace(".rs", "");
                File executableFile = new File(currentDir, executableFileName);
                executableFile.setExecutable(true);
        
                ProcessBuilder runProcess = new ProcessBuilder(new File(currentDir, executableFileName).getAbsolutePath());
                runProcess.directory(new File(currentDir));
                runProcess.redirectErrorStream(true);
                Process run = runProcess.start();
                outputStream.reset();

                inputStream = run.getInputStream();
                buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                int runExitCode = run.waitFor();

                if (runExitCode == 0) {
                    for(File file: currentDirFile.listFiles()) 
                        if (!file.isDirectory()) 
                            file.delete();  

                    return outputStream.toString();
                } else {
                    Files.deleteIfExists(sourceFile.toPath());
                    return "Run failed:\n" + outputStream.toString();
                }
            } else {
                Files.deleteIfExists(sourceFile.toPath());
                return "Compilation failed:\n" + outputStream.toString();
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

}