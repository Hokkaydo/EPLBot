package com.github.hokkaydo.eplbot.module.code.JavaLoader;
import com.github.hokkaydo.eplbot.Strings;
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
public class JavaRunner {
    private static final String directoryPath = "/src/main/java/com/github/hokkaydo/eplbot/module/code/JavaLoader/temp/";
    private static final String outputPath = System.getProperty("user.dir") + directoryPath;
    private static final String javaPath = "com.github.hokkaydo.eplbot.module.code.JavaLoader.temp";
    public static String javaParse(String input) {
        String safe = safeImports(input);
        if (!input.equals(safe)){return "Unvalid imports";};
        String className = regexClassName(input);
        String packagedInput = packageBuilder(input);
        writeFile(packagedInput,outputPath+"/"+className+".java");
        String res =  run(className);
        return res;
    }
    public static String run(String className) {
        String filePath = outputPath + "/" + className + ".java";
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            int compilationResult = compiler.run(null, null, new PrintStream(errorStream), filePath);

            if (compilationResult == 0) {
                String classNamePath = javaPath + "." + className;
                ClassLoader classLoader = JavaRunner.class.getClassLoader();
                Class<?> cls = classLoader.loadClass(classNamePath);
                Method mainMethod = cls.getMethod("main", String[].class);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream customOut = new PrintStream(outputStream);
                PrintStream originalOut = System.out;
                System.setOut(customOut);
                mainMethod.invoke(null, (Object) new String[0]);
                System.setOut(originalOut);
                String capturedOutput = outputStream.toString();
                File directory = new File(outputPath);
                File[] contents = directory.listFiles();
                for (File file : contents) {
                    file.delete();
                }
                return capturedOutput;
            } else {
                File directory = new File(outputPath);
                File[] contents = directory.listFiles();
                for (File file : contents) {
                    file.delete();
                }
                String compilationError = errorStream.toString();
                return "Compilation failed:\n" + compilationError;
            }

        } catch (Exception e) {
            return e.toString();
        }
    }

    public static void writeFile(String  input, String path){
        File file = new File(path);

        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(input);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String packageBuilder(String input){
        return "package "+javaPath+";\n" + input;

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
            "java.lang.Runtime"
        );
        String regex = "(?i)import\\s+" + String.join("|", dangerousImports).replaceAll("\\.", "\\\\.") + "\\s*;";
        return input.replaceAll(regex, "");
    }

}