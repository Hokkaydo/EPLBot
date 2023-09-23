package com.github.hokkaydo.eplbot.module.code;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class JavaRunner {
    private static final Path output_path = Path.of("\\temp");
    private static final String java_path = "com.github.hokkaydo.eplbot.temp";
    public static String run(String input) {
        String safe = safeImports(input);
        if (!input.equals(safe)){return "Unvalid imports";};
        String className = regexClassName(input);
        writeFile(input,Path.of(output_path+"\\"+className+".java"));
        String filePath = output_path + "\\" + className + ".java";
        try {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            System.out.println("here1");
            int compilationResult = ToolProvider.getSystemJavaCompiler().run(null, null, new PrintStream(errorStream), filePath);
            System.out.println("here2");
            if (compilationResult == 0) {
                System.out.println("here3");
                URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(output_path.toString()).toURI().toURL()});
                String classpath = System.getProperty("java.class.path");
                String[] classpathEntries = classpath.split(File.pathSeparator);
                System.out.println("here4");
                for (String classpathEntry : classpathEntries) {
                    File entry = new File(classpathEntry);
                    if (entry.isFile() && entry.getName().toLowerCase().endsWith(".jar")) {
                        try (JarFile jarFile = new JarFile(entry)) {
                            Enumeration<JarEntry> jarEntries = jarFile.entries();
                            while (jarEntries.hasMoreElements()) {
                                JarEntry jarEntry = jarEntries.nextElement();
                                String entryName = jarEntry.getName();
                                if (entryName.startsWith("com/github")) {
                                    System.out.println("Classpath entry: " + entryName);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    System.out.println("here5");
                    Class<?> cls = Class.forName(java_path + "." + className, true, classLoader);
                    System.out.println("here6");
                    Method mainMethod = cls.getMethod("main", String[].class);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    PrintStream customOut = new PrintStream(outputStream);
                    PrintStream originalOut = System.out;
                    System.setOut(customOut);
                    mainMethod.invoke(null, (Object) new String[0]);
                    System.setOut(originalOut);
                    String capturedOutput = outputStream.toString();

                    return capturedOutput;
                } catch (ClassNotFoundException e){
                    Thread.sleep(500);
                    Class<?> cls = Class.forName(java_path + "." + className, true, classLoader);
                    Method mainMethod = cls.getMethod("main", String[].class);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    PrintStream customOut = new PrintStream(outputStream);
                    PrintStream originalOut = System.out;
                    System.setOut(customOut);
                    mainMethod.invoke(null, (Object) new String[0]);
                    System.setOut(originalOut);
                    String capturedOutput = outputStream.toString();

                    return capturedOutput;
                    }
            } else {

                return "Compilation failed:\n" + errorStream.toString();
            }
    
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public static void writeFile(String input, Path path) {
        try {
            Files.createDirectories(path.getParent()); // Create parent directories if they don't exist
            Files.write(path, input.getBytes(), StandardOpenOption.CREATE);
            System.out.println("File written: " + path);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the IOException
        }
    }
    public static String packageBuilder(String input){
        return "package "+java_path+";\n" + input;

    }
    public static String regexDetector(String input){
        Pattern pattern = Pattern.compile("import\\s+[^;]+;", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String importStatement = matcher.group(0);
            result.append(importStatement).append("\n");
        }
        return result.toString();
    }
    public static String regexClassName(String input){
        Pattern pattern = Pattern.compile("class\\s+(\\w+)\\s*\\{");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "");
        } else {
            throw new RuntimeException("No class definition found.");}

    }
    public static void deleteFiles(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean success = file.delete();
        }
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
        return input.replaceAll(regex, "");
    }

}