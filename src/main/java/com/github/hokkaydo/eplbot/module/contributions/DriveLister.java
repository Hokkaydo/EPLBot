package com.github.hokkaydo.eplbot.module.contributions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Lists the contributions folder of the EPL Drive through rclone.
 * <br>
 * The folder is read from the {@code CONTRIBUTIONS_REMOTE} environment variable (e.g. {@code remote:Folder/Contributions}),
 * and rclone reads its configuration (OneDrive token) from {@code RCLONE_CONFIG}.
 * */
class DriveLister {

    private static final String REMOTE_ENV = "CONTRIBUTIONS_REMOTE";
    private static final long TIMEOUT_MINUTES = 10;
    private static final int ERROR_LINES = 3;
    // Files permanently kept in the folder, which are not contributions
    private static final Set<String> EXCLUDED_PATHS = Set.of("Contributions Drive EPL - EPL Drive Contribution System.pdf");
    // rclone prefixes each log line with "yyyy/MM/dd HH:mm:ss ", which would make identical errors look different
    private static final String LOG_DATE_PREFIX = "^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2} ";
    // Both output streams must be drained concurrently, otherwise rclone blocks once a pipe buffer is full
    private static final ExecutorService READERS = Executors.newVirtualThreadPerTaskExecutor();

    private DriveLister() {}

    /**
     * @return the configured rclone remote path, or an empty string if not set
     * */
    static String getRemote() {
        String remote = System.getenv(REMOTE_ENV);
        return remote == null ? "" : remote.strip();
    }

    /**
     * Lists recursively all the files of the contributions folder, sorted by path, except the {@link #EXCLUDED_PATHS}
     * @return the list of files currently in the folder
     * @throws IOException if the remote is not set, if rclone fails or does not answer in time
     * */
    static List<DriveFile> list() throws IOException, InterruptedException {
        String remote = getRemote();
        if (remote.isBlank() || remote.startsWith("-"))
            throw new IOException("Environment variable %s is not set".formatted(REMOTE_ENV));

        Process process = new ProcessBuilder("rclone", "lsjson", "-R", "--files-only", "--metadata", remote).start();
        try {
            Future<String> stdout = READERS.submit(() -> readAll(process.getInputStream()));
            Future<String> stderr = READERS.submit(() -> readAll(process.getErrorStream()));
            if (!process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES))
                throw new IOException("rclone did not answer within %d minutes".formatted(TIMEOUT_MINUTES));
            if (process.exitValue() != 0)
                throw new IOException("rclone exited with code %d: %s".formatted(process.exitValue(), lastLines(get(stderr))));

            JSONArray array = new JSONArray(get(stdout));
            List<DriveFile> files = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String path = object.getString("Path");
                if (EXCLUDED_PATHS.contains(path)) continue;
                // "utime" is set by OneDrive on upload, unlike ModTime which keeps the date of the uploader's computer
                JSONObject metadata = object.optJSONObject("Metadata", new JSONObject());
                Instant uploadTime = Instant.parse(metadata.optString("utime", object.getString("ModTime")));
                files.add(new DriveFile(path, object.optLong("Size", -1), uploadTime));
            }
            files.sort(Comparator.comparing(DriveFile::path));
            return files;
        } catch (JSONException | DateTimeParseException e) {
            throw new IOException("Invalid rclone output", e);
        } finally {
            process.destroyForcibly();
        }
    }

    private static String get(Future<String> future) throws IOException, InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    private static String lastLines(String log) {
        List<String> lines = log.lines()
                                    .map(line -> line.replaceFirst(LOG_DATE_PREFIX, "").strip())
                                    .filter(line -> !line.isEmpty())
                                    .toList();
        return String.join("\n", lines.subList(Math.max(0, lines.size() - ERROR_LINES), lines.size()));
    }

    private static String readAll(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    record DriveFile(String path, long size, Instant uploadTime) {

        /**
         * @return the path wrapped in inline code, safe to display on Discord
         * */
        String displayPath() {
            return "`%s`".formatted(path.replace('`', '\''));
        }

        String displaySize() {
            if (size < 0) return "?";
            String[] units = {"o", "Ko", "Mo", "Go"};
            int unit = IntStream.range(0, units.length).filter(i -> size < Math.pow(1024, i + 1)).findFirst().orElse(units.length - 1);
            if (unit == 0) return "%d o".formatted(size);
            return String.format(Locale.FRANCE, "%.1f %s", size / Math.pow(1024, unit), units[unit]);
        }

    }

}
