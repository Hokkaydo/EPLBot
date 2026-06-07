package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LatexRenderer {

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
    private static final int TIMEOUT_SECONDS = 30;
    private static final String DOCKER_IMAGE =
            Optional.ofNullable(System.getenv("TEX_DOCKER_IMAGE")).orElse("minidocks/latex");
    private static final String DOC_TEMPLATE = """
            \\documentclass[preview,border=4pt]{standalone}
            \\usepackage{amsmath,amssymb,amsfonts}
            \\begin{document}
            %s
            \\end{document}
            """;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    private LatexRenderer() {}

    public static CompletableFuture<byte[]> renderToImage(String content) {
        return CompletableFuture.supplyAsync(() -> doRender(content));
    }

    private static byte[] doRender(String content) {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("tex-");
            Files.writeString(tmpDir.resolve("input.tex"), DOC_TEMPLATE.formatted(content));

            AtomicReference<Process> processRef = new AtomicReference<>();
            final Path finalTmpDir = tmpDir;

            ScheduledFuture<?> timer = SCHEDULER.schedule(() -> {
                Process p = processRef.get();
                if (p != null && p.isAlive()) p.destroyForcibly();
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "docker", "run", "--rm",
                        "-v", finalTmpDir.toAbsolutePath() + ":/work",
                        "--memory", "512m",
                        "--cpus", "1",
                        "--network", "none",
                        DOCKER_IMAGE,
                        "sh", "-c",
                        "cd /work && pdflatex -interaction=nonstopmode input.tex > pdflatex.log 2>&1 " +
                        "&& convert -density 300 input.pdf output.png"
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                processRef.set(process);

                // drain stdout (captured via redirectErrorStream to avoid blocking)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (reader.readLine() != null) { /* drain */ }
                }

                int exitCode = process.waitFor();
                timer.cancel(false);

                if (exitCode != 0) {
                    Path logPath = finalTmpDir.resolve("pdflatex.log");
                    String log = Files.exists(logPath) ? Files.readString(logPath) : "(no log)";
                    throw new LatexCompilationException(log);
                }

                Path outputPng = finalTmpDir.resolve("output.png");
                if (!Files.exists(outputPng))
                    throw new LatexCompilationException("pdflatex succeeded but no output.png produced");

                return Files.readAllBytes(outputPng);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LatexCompilationException("Rendering interrupted");
            } catch (IOException e) {
                throw new LatexCompilationException("IO error: " + e.getMessage());
            } finally {
                timer.cancel(false);
            }
        } catch (IOException e) {
            throw new LatexCompilationException("Could not create temp directory: " + e.getMessage());
        } finally {
            if (tmpDir != null) {
                try {
                    deleteRecursively(tmpDir);
                } catch (IOException e) {
                    Main.LOGGER.warn("[TexModule] Failed to delete temp dir {}: {}", tmpDir, e.getMessage());
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path child : entries.toList()) deleteRecursively(child);
            }
        }
        Files.deleteIfExists(path);
    }

    public static class LatexCompilationException extends RuntimeException {
        public LatexCompilationException(String message) {
            super(message);
        }
    }
}
