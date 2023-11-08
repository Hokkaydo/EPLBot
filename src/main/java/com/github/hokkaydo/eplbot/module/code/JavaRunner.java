package com.github.hokkaydo.eplbot.module.code;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class JavaRunner implements Runner{
    private static final String OUTPUT_PATH = System.getProperty("user.dir")+"\\src\\temp\\";
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);

    @Override
    public String run(String input, Integer runTimeout) {
        if (!input.equals(safeImports(input))){
            return "Invalid imports";
        if (requiresWrapper(input)){
            input = addWrapper(input);
        }
        String className = regexClassName(input);
        writeFile(input,Path.of(OUTPUT_PATH+"\\"+className+".java"));
        String filePath = OUTPUT_PATH + "\\" + className + ".java";
        try {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            if (ToolProvider.getSystemJavaCompiler().run(null, null, new PrintStream(errorStream), filePath) != 0) {
                deleteFiles( className);
                return "Compilation failed:\n" + errorStream;
            }

            Process process = new ProcessBuilder(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java", "-cp", System.getProperty("java.class.path") + File.pathSeparator + OUTPUT_PATH,regexClassName(input) ).redirectErrorStream(true).start();

            ScheduledFuture<?> timeOut = SCHEDULER.schedule(() -> {}, runTimeout, TimeUnit.SECONDS);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            if (timeOut.isDone()) {
                deleteFiles( className);
                String output = outputStream.toString().trim();
                if (output.isEmpty()) {
                    return "Run failed: Timelimit exceeded "+ runTimeout +" s";
                } else {
                    return "Run failed:\n" + output;
                }
            } else {
                timeOut.cancel(true);
                deleteFiles( className);
                return outputStream.toString();
            }


        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
    public static void deleteFiles(String className) {
        Optional.ofNullable(new File(OUTPUT_PATH).listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(f -> f.isFile() && f.getName().startsWith(className))
                .forEach(File::delete);
    }
    public static boolean requiresWrapper(String javaCode) {
        boolean hasClass = Pattern.compile("\\bpublic\\s+class\\s+[A-Z][a-zA-Z0-9]*").matcher(javaCode).find();
        boolean hasMainMethod = Pattern.compile("\\bpublic\\s+static\\s+void\\s+main\\s*\\(\\s*String\\[\\]\\s+[a-zA-Z0-9]*\\s*\\)").matcher(javaCode).find();
        boolean hasImports = Pattern.compile("\\bimport\\s+[a-zA-Z0-9.]+").matcher(javaCode).find();
        if (hasClass && hasMainMethod) {
            return false;
        } else if (hasImports) {
            return false;
        } else {
            return true;
        }
    }
    public static String addWrapper(String input){
        return """
                import java.util.*;
                import java.lang.Math;

                public class Wrapper {
                    public static void main(String[] args){""" + 
                        input +
                    """ 
                    }
                }""";
    }
    public static void writeFile(String input, Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, input.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String regexClassName(String input){
        Matcher matcher = Pattern.compile("class\\s+(\\w+)\\s*\\{").matcher(input);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "");
        } else {
            throw new RuntimeException("No class definition found.");}
    }
    public static String safeImports(String input){
        List<String> dangerousImports = Arrays.asList(
                "java.lang.reflect",
                "java.io",
                "java.util.zip",
                "java.net",
                "java.nio.file",
                "java.security",
                "java.awt",
                "javax.swing",
                "javax.script",
                "java.util.logging",
                "java.lang.ProcessBuilder",
                "java.lang.Runtime",
                "com",
                "net",
                "org"
        );
        String regex = "(?i)import\\s+" + String.join("|", dangerousImports).replace("\\.", "\\\\.") + "\\s*;";
        return input.replace(regex, "");
    }
}