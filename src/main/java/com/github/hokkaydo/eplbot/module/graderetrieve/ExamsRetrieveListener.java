package com.github.hokkaydo.eplbot.module.graderetrieve;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExamsRetrieveListener extends ListenerAdapter {

    private static final Path MESSAGE_IDS_STORAGE_PATH = Path.of(Main.PERSISTENCE_DIR_PATH + "/message_ids");
    private static final Path ZIP_PATH = Path.of(Main.PERSISTENCE_DIR_PATH + "/exams.zip");
    private final Long guildId;
    private Long examsRetrieveChannelId;
    private int quarter = 1;
    private final Map<Long, String> threadIdToPath = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    private Long zipMessageId = 0L;

    public ExamsRetrieveListener(Long guildId) throws IOException {
        this.guildId = guildId;
        loadMessageIdsPath();
        loadLectures();
    }

    public void setGradeRetrieveChannelId(Long examsRetrieveChannelId, int quarter) {
        this.examsRetrieveChannelId = examsRetrieveChannelId;
        this.quarter = quarter;
        sendMessages();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.getChannel().getType().isThread()) return;
        if(!event.isFromGuild() || event.getGuild().getIdLong() != guildId) return;
        if(!threadIdToPath.containsKey(event.getChannel().asThreadChannel().getIdLong())) return;
        if(event.getMessage().getAttachments().isEmpty()) return;
        String path = threadIdToPath.get(event.getChannel().asThreadChannel().getIdLong());
        if(path == null) return;
        try {
            addFiles(path, event.getMessage().getAttachments());
        } catch (IOException ignored) {
            throw new IllegalStateException("Could not update exams zip file");
        }
    }

    private void addFiles(String path, List<Message.Attachment> attachments) throws IOException {
        if(!Files.exists(ZIP_PATH)) {
            Files.createFile(ZIP_PATH);
            createZipFile(path, attachments);
            return;
        }
        updateZipFile(path, attachments);
    }

    private void updateZipFile(String path, List<Message.Attachment> attachments) {
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + ZIP_PATH.toUri());
        attachments.stream()
                .map(a -> new Tuple<>(a.getFileName(), a.getProxy().downloadToPath()))
                .forEach(t -> t.b.thenAccept(tempPath -> {
                    try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                        String[] split = path.split("/");
                        StringBuilder current = new StringBuilder();
                        for (String s : split) {
                            current.append(s).append("/");
                            createDirectoryInZip(fs, current.toString());
                        }
                        Path nf = fs.getPath(path, t.a);
                        Files.write(nf, Files.readAllBytes(tempPath), StandardOpenOption.CREATE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).join());
        updateSentZip();
    }

    private void createDirectoryInZip(FileSystem fs, String current) {
        try {
            fs.provider().createDirectory(fs.getPath(current));
        } catch (IOException ignored) {
            //Ignored
        }
    }

    private void createZipFile(String path, List<Message.Attachment> attachments) throws IOException {
        final FileOutputStream fos = new FileOutputStream(ZIP_PATH.toFile());
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        attachments.stream()
                .map(a -> new Tuple<>(a.getFileName(), a.getProxy().download()))
                .forEach(t -> t.b.thenAccept(fis -> {
                    ZipEntry zipEntry = new ZipEntry(path + "/" + t.a);
                    try {
                        zipOut.putNextEntry(zipEntry);
                        byte[] bytes = new byte[1024];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                        zipOut.closeEntry();
                        fis.close();
                    } catch (IOException ignored) {
                        //Ignored
                    }
                }).join());
        zipOut.close();
        fos.close();
        updateSentZip();
    }

    private void updateSentZip() {
        Long channelId = Config.getGuildVariable(guildId, "DRIVE_ADMIN_CHANNEL_ID");
        if(channelId == 0) {
            MessageUtil.sendAdminMessage(Strings.getString("DRIVE_ADMIN_CHANNEL_NOT_SETUP"),guildId);
            return;
        }
        TextChannel channel = Main.getJDA().getTextChannelById(channelId);
        if(channel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("DRIVE_ADMIN_CHANNEL_NOT_SETUP"), guildId);
            return;
        }
        if(zipMessageId == 0) {
            channel.sendMessage("Archive d'examens de cette session").addFiles(FileUpload.fromData(ZIP_PATH)).queue(m -> {
                zipMessageId = m.getIdLong();
                storeMessageIds();
                m.pin().queue();
            });
            return;
        }
        channel.retrieveMessageById(zipMessageId).queue(m -> m.editMessageAttachments(AttachedFile.fromData(ZIP_PATH)).queue());
    }

    private void sendMessages() {
        TextChannel channel = Main.getJDA().getChannelById(TextChannel.class, examsRetrieveChannelId);
        if(channel == null) return;
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            List<List<String[]>> lectures = entry.getValue().lectures;
            for (int i = 0; i < lectures.size(); i++) {
                if((i+1)%2 != Math.abs(quarter-2)) continue;
                List<String[]> quarterLectures = lectures.get(i);
                for (int j = 0; j < quarterLectures.size(); j++) {
                    String[] strings = quarterLectures.get(j);
                    int finalI = i;
                    int finalJ = j;
                    channel.sendMessage("%s - %s (BAC%d - %s)".formatted(strings[0], strings[1], (int)Math.ceil((i+1) /2.0), entry.getValue().groupName.toUpperCase())).queue(m -> {
                        m.createThreadChannel(strings[0]).queue();
                        threadIdToPath.put(m.getIdLong(), "%s/Q%d/%s".formatted(entry.getKey(), finalI + 1, strings[0]));
                        if(finalI == lectures.size() - 1 && finalJ == quarterLectures.size() - 1) {
                            storeMessageIds();
                        }
                    });
                }
            }
        }
    }

    private void storeMessageIds() {
        try(FileWriter stream = new FileWriter(MESSAGE_IDS_STORAGE_PATH.toFile())) {
            threadIdToPath.forEach((k, v) -> {
                try {
                    stream.append(k.toString()).append(";").append(v).append("\n");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            stream.append("ZIP:").append(zipMessageId.toString()).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadLectures() throws JSONException {
        InputStream stream = Strings.class.getClassLoader().getResourceAsStream("lectures.json");
        assert stream != null;
        JSONObject object = new JSONObject(new JSONTokener(stream));
        if(object.isEmpty()) return;
        JSONArray names = object.names();
        for (int i = 0; i < names.length(); i++) {
            groups.put(names.getString(i), Group.of(names.getString(i), object.getJSONObject(names.getString(i))));
        }
    }

    private void loadMessageIdsPath() throws IOException {
        if(!Files.exists(MESSAGE_IDS_STORAGE_PATH)) {
            Files.createFile(MESSAGE_IDS_STORAGE_PATH);
            return;
        }
        try(BufferedReader stream = new BufferedReader(new FileReader(MESSAGE_IDS_STORAGE_PATH.toFile()))) {
            String line;
            while((line = stream.readLine()) != null) {
                if(line.startsWith("ZIP:")) {
                    zipMessageId = Long.parseLong(line.replaceFirst("ZIP:", ""));
                    continue;
                }
                String[] s = line.split(";");
                threadIdToPath.put(Long.parseLong(s[0]), s[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record Group(String groupName, String name, List<List<String[]>> lectures) {

        static Group of(String groupName, JSONObject object) {
            String name = object.getString("name");
            List<List<String[]>> lectures = new ArrayList<>();
            JSONArray lecturesArr = object.getJSONArray("lectures");
            for (int i = 0; i < lecturesArr.length(); i++) {
                JSONArray quarter = lecturesArr.getJSONArray(i);
                List<String[]> course = new ArrayList<>();
                for (int j = 0; j < quarter.length(); j++) {
                    JSONArray courseArray = quarter.getJSONArray(j);
                    course.add(new String[]{courseArray.getString(0), courseArray.getString(1)});
                }
                lectures.add(course);
            }
            return new Group(groupName, name, lectures);
        }

    }

    private record Tuple<A, B>(A a, B b){}

}
