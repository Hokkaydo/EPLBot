package com.github.hokkaydo.eplbot.module.tex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LatexRenderer {

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
    private static final int TIMEOUT_SECONDS = 60;
    private static final String DOCKER_IMAGE =
            Optional.ofNullable(System.getenv("TEX_DOCKER_IMAGE")).orElse("blang/latex");
    private static final String PNG_MARKER = "---TEX-PNG---";

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    private LatexRenderer() {}

    public static CompletableFuture<byte[]> renderToImage(String content, boolean darkTheme) {
        return CompletableFuture.supplyAsync(() -> doRender(content, darkTheme));
    }

    private static String buildDocument(String content, boolean darkTheme) {
        if (darkTheme) {
            return "\\documentclass[preview,border=4pt]{standalone}\n" +
                   "\\usepackage{amsmath,amssymb,amsfonts,xcolor}\n" +
                   "\\begin{document}\n" +
                   "\\Large\n" + 
                   "{\\color{white}" + content + "}\n" +
                   "\\end{document}\n";
        }
        return "\\documentclass[preview,border=4pt]{standalone}\n" +
               "\\usepackage{amsmath,amssymb,amsfonts}\n" +
               "\\begin{document}\n" +
               content + "\n" +
               "\\end{document}\n";
    }

    private static byte[] doRender(String content, boolean darkTheme) {
        AtomicReference<Process> processRef = new AtomicReference<>();

        ScheduledFuture<?> timer = SCHEDULER.schedule(() -> {
            Process p = processRef.get();
            if (p != null && p.isAlive()) p.destroyForcibly();
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            // Feed LaTeX source via stdin; no bind mount needed.
            // PNG bytes are extracted from stdout using a base64 marker.
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm", "-i",
                    "--entrypoint", "/bin/sh",
                    "--memory", "512m",
                    "--cpus", "1",
                    "--network", "none",
                    DOCKER_IMAGE,
                    "-c",
                    "cat > /tmp/input.tex && " +
                    "pdflatex -interaction=nonstopmode -output-directory=/tmp /tmp/input.tex && " +
                    "gs -dTextAlphaBits=4 -dGraphicsAlphaBits=4 -dBATCH -dNOPAUSE -dSAFER -sDEVICE=pngalpha -r600 -sOutputFile=/tmp/output.png /tmp/input.pdf && " +
                    "echo '" + PNG_MARKER + "' && " +
                    "base64 /tmp/output.png"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            processRef.set(process);

            // Send LaTeX document to container stdin then close so pdflatex can start
            try (var stdin = process.getOutputStream()) {
                stdin.write(buildDocument(content, darkTheme).getBytes(StandardCharsets.UTF_8));
            }

            // Read stdout: pdflatex output lines, then PNG_MARKER, then base64 PNG lines
            StringBuilder logBuilder = new StringBuilder();
            StringBuilder b64Builder = new StringBuilder();
            boolean markerSeen = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (PNG_MARKER.equals(line.trim())) {
                        markerSeen = true;
                    } else if (markerSeen) {
                        b64Builder.append(line.trim());
                    } else {
                        logBuilder.append(line).append('\n');
                    }
                }
            }

            int exitCode = process.waitFor();
            timer.cancel(false);

            if (exitCode != 0 || !markerSeen) {
                String log = logBuilder.toString().strip();
                if (log.isEmpty()) log = "(no output — verify Docker image '" + DOCKER_IMAGE + "' is available and running)";
                throw new LatexCompilationException(log);
            }

            return Base64.getMimeDecoder().decode(b64Builder.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LatexCompilationException("Rendering interrupted");
        } catch (IOException e) {
            throw new LatexCompilationException("IO error: " + e.getMessage());
        } finally {
            timer.cancel(false);
        }
    }

    public static class LatexCompilationException extends RuntimeException {
        public LatexCompilationException(String message) {
            super(message);
        }
    }
}
