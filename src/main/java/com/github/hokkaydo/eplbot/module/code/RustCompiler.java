package com.github.hokkaydo.eplbot.module.code;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RustCompiler implements Runner{
    private static final String CURRENT_DIR = System.getProperty("user.dir") + "\\src\\temp\\";
    private static final String[] VALID_EXTENSIONS = {".exe", ".pdb", ".rs"};

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
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

                if (executableFile.setExecutable(true)) {
                    return "Couldn't make file executable";
                }
                Process run = new ProcessBuilder(new File(CURRENT_DIR, executableFileName).getAbsolutePath()).directory(new File(CURRENT_DIR)).redirectErrorStream(true).start();

                ScheduledFuture<?> timeOut = SCHEDULER.schedule(() -> {}, runTimeout, TimeUnit.SECONDS);

                outputStream.reset();
                inputStream = run.getInputStream();
                buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                if (timeOut.isDone()) {
                    deleteFiles();
                    String output = outputStream.toString().trim();
                    if (output.isEmpty()) {
                        return "Run failed: Timelimit exceeded "+ runTimeout +" s";
                    } else {
                        return "Run failed:\n" + output;
                    }
                } else {
                    deleteFiles();
                    return outputStream.toString();
                }
            } else {
                deleteFiles();
                return "Compilation failed:\n" + outputStream;
            }
        } catch (IOException | InterruptedException e) {
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

    private static boolean isValidExtension(String ext) {
        for (String validExtension : VALID_EXTENSIONS) {
            if(ext.equals(validExtension)) return true;
        }
        return false;
    }

    private static void deleteFiles(){
        Optional.ofNullable(new File(CURRENT_DIR).listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f.isFile() && isValidExtension(f.getName().replace("temp", "")))
                .forEach(File::delete);
    }
}