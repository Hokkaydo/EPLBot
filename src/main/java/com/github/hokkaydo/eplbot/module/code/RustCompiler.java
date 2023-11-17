package com.github.hokkaydo.eplbot.module.code;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RustCompiler implements Runner{
    private static final String CURRENT_DIR = System.getProperty("user.dir") + File.separator+"src"+File.separator+"temp"+File.separator;
    private static final String[] VALID_EXTENSIONS = {".exe", ".pdb", ".rs"};

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
    @Override
    public String run(String input, Integer runTimeout) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'std' or 'use' are used";
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File sourceFile;
        Process compile;
        try {

            sourceFile = new File(CURRENT_DIR, "temp.rs");
            sourceFile.deleteOnExit();
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(input);
            writer.close();
            compile = new ProcessBuilder("rustc", sourceFile.getAbsolutePath()).directory(new File(CURRENT_DIR)).redirectErrorStream(true).start();

        } catch (IOException e) {
            e.printStackTrace();
            return "Server error code R01" + e;
        }
        int compileExitCode;
        try{
            compileExitCode = compile.waitFor();
        }catch (InterruptedException e){
            e.printStackTrace();
            return "Server error code R02" + e;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(compile.getInputStream()));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                outputStream.write((line + "\n").getBytes());
            }
        } catch (IOException e){
            e.printStackTrace();
            return "Server error code R03" + e;
        }

        if (compileExitCode != 0) {
            deleteFiles();
            return "Compilation failed:\n" + outputStream;
        }
        String executableFileName = sourceFile.getName().replace(".rs", "");
        File executableFile = new File(CURRENT_DIR, executableFileName);

        if (!executableFile.setExecutable(true)) {
            return "Couldn't make file executable";
        }
        Process run;
        try {
            run = new ProcessBuilder(new File(CURRENT_DIR, executableFileName).getAbsolutePath()).directory(new File(CURRENT_DIR)).redirectErrorStream(true).start();
        } catch (IOException e){
            e.printStackTrace();
            return "Server error code R04" + e;
        }

        BufferedReader reader2 = new BufferedReader(new InputStreamReader(run.getInputStream()));
        ScheduledFuture<?> timeOut = SCHEDULER.schedule(() -> {}, runTimeout, TimeUnit.SECONDS);
        String line2;
        try {
            while ((line2 = reader2.readLine()) != null) {
                outputStream.write((line2 + "\n").getBytes());
            }
        } catch (IOException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return "Server error code R05" + e;
        }
        System.out.println(timeOut.isDone());
        System.out.println(runTimeout);
        System.out.println(timeOut.toString());
        if (!timeOut.isDone()) {
            deleteFiles();
            Thread.currentThread().interrupt();
            return outputStream.toString();
        }
        deleteFiles();
        String output = outputStream.toString().trim();
        Thread.currentThread().interrupt();
        System.out.println(output);
        if (output.isEmpty()) {
            return "Run failed: Timelimit exceeded "+ runTimeout +" s";
        } else {
            return "Run failed:\n" + output;
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