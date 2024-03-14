package com.github.hokkaydo.eplbot.module.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PythonRunner implements Runner {
    private static final String CURRENT_DIR = STR."\{System.getProperty("user.dir")}\{File.separator}src\{File.separator}temp\{File.separator}";
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);

    @Override
    public String run(String input, Integer runTimeout) {
        if (containsUnsafeKeywords(input)){
            return "Compilation failed:\nCheck if 'exec' or 'eval' are used";
        }

        File sourceFile = new File(CURRENT_DIR,"temp.py");
        sourceFile.deleteOnExit();

        ScheduledFuture<?> timeOut = SCHEDULER.schedule(() -> {}, runTimeout, TimeUnit.SECONDS);
        StringBuilder output = new StringBuilder();
        String line;
        try (FileWriter writer = new FileWriter(sourceFile)){
            writer.write(input);
            Process process = new ProcessBuilder("python", sourceFile.getAbsolutePath()).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return STR."Server side error code P01 \{e.getMessage()}";
        }
        if (timeOut.isDone()) {
            deleteFiles();
            String outProcess = output.toString();
            if (outProcess.isEmpty()) {
                return STR."Run failed: Timelimit exceeded \{runTimeout} s";
            } else {
                return STR."Run succeeded:\n\{outProcess}";
            }
        }
        deleteFiles();
        return output.toString();
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
        Optional.ofNullable(new File(CURRENT_DIR).listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f.isFile() && f.getName().equals("temp.py"))
                .forEach(File::delete);
    }
}
    
