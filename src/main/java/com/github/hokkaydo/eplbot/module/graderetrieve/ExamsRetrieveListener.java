package com.github.hokkaydo.eplbot.module.graderetrieve;

import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.ExamsRetrieveThread;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.ExamRetrieveThreadRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.ExamRetrieveThreadRepositorySQLite;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExamsRetrieveListener extends ListenerAdapter {

    private static final Path ZIP_PATH = Path.of(Main.PERSISTENCE_DIR_PATH + "/exams.zip");
    private static final String THREAD_MESSAGE_FORMAT = "%s - %s (BAC%d - %s)";
    private static final String EXAMEN_STORING_PATH_FORMAT = "%s/Q%d/%s";
    private final Long guildId;
    private Long examsRetrieveChannelId;
    private int selectedQuarterToRetrieve = 1;
    private final CourseGroupRepository groupRepository;
    private Long zipMessageId;
    private final ExamRetrieveThreadRepository repository;

    ExamsRetrieveListener(Long guildId) {
        this.guildId = guildId;
        this.repository = new ExamRetrieveThreadRepositorySQLite(Main.getDataSource());
        this.zipMessageId = Config.getGuildState(guildId, "EXAM_ZIP_MESSAGE_ID");
        CourseRepository courseRepository = new CourseRepositorySQLite(Main.getDataSource());
        this.groupRepository = new CourseGroupRepositorySQLite(Main.getDataSource(), courseRepository);
    }

    void setGradeRetrieveChannelId(Long examsRetrieveChannelId, int quarter) {
        this.examsRetrieveChannelId = examsRetrieveChannelId;
        this.selectedQuarterToRetrieve = quarter;
        sendMessages();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.getChannel().getType().isThread()) return;
        if(!event.isFromGuild() || event.getGuild().getIdLong() != guildId) return;
        if(event.getMessage().getAttachments().isEmpty()) return;
        repository.readByMessageId(event.getChannel().asThreadChannel().getIdLong()).ifPresent(model -> {
            try {
                addFiles(model.path(), event.getMessage().getAttachments());
            } catch (IOException ignored) {
                throw new IllegalStateException("Could not update exams zip file");
            }
        });
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
                        Main.LOGGER.log(Level.WARNING, "Could not update exams zip file");
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
                Config.updateValue(guildId, "EXAM_ZIP_MESSAGE_ID", zipMessageId);
                m.pin().queue();
            });
            return;
        }
        channel.retrieveMessageById(zipMessageId).queue(m -> m.editMessageAttachments(AttachedFile.fromData(ZIP_PATH)).queue());
    }

    private void sendMessages() {
        TextChannel channel = Main.getJDA().getChannelById(TextChannel.class, examsRetrieveChannelId);
        if(channel == null) return;

        groupRepository.getByQuarters(selectedQuarterToRetrieve, selectedQuarterToRetrieve + 2, selectedQuarterToRetrieve + 4).forEach(group -> {
            List<List<Course>> courses = group.courses();
            for (int quarter = 0; quarter < courses.size(); quarter++) {
                List<Course> quadrimestreCourses = courses.get(quarter);
                for (Course coursesInformation : quadrimestreCourses) {

                    String coursesCode = coursesInformation.code();
                    String coursesName = coursesInformation.name();

                    int finalQuarter = quarter;

                    channel.sendMessage(
                                    THREAD_MESSAGE_FORMAT.formatted(coursesCode, coursesName, (int) Math.ceil((quarter + 1) / 2.0), group.frenchName().toUpperCase())
                            )
                            .queue(m -> {
                                m.createThreadChannel(coursesCode).queue();
                                repository.create(new ExamsRetrieveThread(m.getIdLong(), EXAMEN_STORING_PATH_FORMAT.formatted(group.englishName(), finalQuarter + 1, coursesCode)));
                            });
                }
            }
        });
    }

    private record Tuple<A, B>(A a, B b){}

}
