package com.github.hokkaydo.eplbot.module.code;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class RustCompiler {
    private static final Path current_dir = Paths.get("\\temp");

    public static String run(String input) {
        System.out.println(current_dir.toString());
        if (containsUnsafeKeywords(input)) {
            return "Compilation failed:\nCheck if 'std' or 'use' are used";
        }

        try {
            Files.createDirectories(current_dir);
            Path sourceFile = current_dir.resolve("temp.rs");
            Files.write(sourceFile, input.getBytes(), StandardOpenOption.CREATE);
            ProcessBuilder compileProcess = new ProcessBuilder("rustc", sourceFile.toString());
            compileProcess.directory(current_dir.toFile()); // Set the working directory
            compileProcess.redirectErrorStream(true);
            Process compile = compileProcess.start();
            InputStream inputStream = compile.getInputStream();
            List<String> compileOutput = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileOutput.add(line);
                }
            }

            int compileExitCode = compile.waitFor();

            if (compileExitCode == 0) {
                String executableFileName = sourceFile.getFileName().toString().replace(".rs", "");
                ProcessBuilder runProcess = new ProcessBuilder(executableFileName);
                runProcess.directory(current_dir.toFile());
                runProcess.redirectErrorStream(true);
                Process run = runProcess.start();
                inputStream = run.getInputStream();
                List<String> runOutput = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        runOutput.add(line);
                    }
                }

                int runExitCode = run.waitFor();

                if (runExitCode == 0) {
                    return String.join("\n", runOutput);
                } else {
                    Files.deleteIfExists(sourceFile);
                    return "Run failed:\n" + String.join("\n", runOutput);
                }
            } else {
                Files.deleteIfExists(sourceFile);
                return "Compilation failed:\n" + String.join("\n", compileOutput);
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
