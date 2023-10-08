package com.github.hokkaydo.eplbot.module.code;
import java.io.File;
import java.io.IOException;
import javax.tools.ToolProvider;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
public class JavaRunner implements Runner{
    private static final String OUTPUT_PATH = System.getProperty("user.dir")+"\\src\\temp\\";
    @Override
    public String run(String input, Integer runTimeout) {
        if (!input.equals(safeImports(input))){return "Invalid imports";};
        String className = regexClassName(input);
        writeFile(input,Path.of(OUTPUT_PATH+"\\"+className+".java"));
        String filePath = OUTPUT_PATH + File.pathSeparator + className + ".java";
        try {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            if (ToolProvider.getSystemJavaCompiler().run(null, null, new PrintStream(errorStream), filePath) == 0) {
                try {
                    Process process = new ProcessBuilder(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java", "-cp", System.getProperty("java.class.path") + File.pathSeparator + OUTPUT_PATH,regexClassName(input) ).redirectErrorStream(true).start();
                    Thread timeoutThread = new Thread(() -> {
                        try {
                            Thread.sleep(1000L*runTimeout);
                            process.destroy();
                        } catch (InterruptedException ignored) {}
                    });
                    timeoutThread.start();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getInputStream().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    if (process.waitFor() == 0) {
                        timeoutThread.interrupt();
                        deleteFiles( className);
                       return outputStream.toString();
                    } else {
                        timeoutThread.interrupt();
                        deleteFiles( className);
                        String output = outputStream.toString().trim();
                        if (output.isEmpty()) {
                            return "Run failed: Timelimit exceeded "+ runTimeout +" s";
                        } else {
                            return "Run failed:\n" + output;
                        }
                    }
                } catch (IOException e) {
                    deleteFiles( className);
                    return "Run failed:\n" + e.toString();
                }
            } else {
                deleteFiles( className);
                return "Compilation failed:\n" + errorStream;
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }
    public static void deleteFiles(String className){
        File outputDirectory = new File(OUTPUT_PATH);
        File[] files = outputDirectory.listFiles();
        if (files == null){
            System.out.println("NPE trying to delete created files");
        } else {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(className)) {
                    file.delete();
                }                            
            }
        }
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
        String regex = "(?i)import\\s+" + String.join("|", dangerousImports).replaceAll("\\.", "\\\\.") + "\\s*;";
        return input.replace(regex, "");
    }
}