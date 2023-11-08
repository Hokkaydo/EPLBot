package com.github.hokkaydo.eplbot.module.code;
import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.Map;


public class CodeCommand extends ListenerAdapter implements Command{
    private static final String TEMP_DIR = System.getProperty("user.dir")+"\\src\\temp";
    private final Map<String, Runner> RUNNER_MAP;
    public CodeCommand() {
        RUNNER_MAP = Map.of(
            "python", new PythonRunner(),
            "rust", new RustCompiler(),
            "java", new JavaRunner()
        );
    }
    @Override
    public void executeCommand(CommandContext context) {
        Guild guild = context.interaction().getGuild();
        if(guild == null) return;
        if (context.options().size() <= 1) {
            context.interaction().replyModal(Modal.create(context.author().getId() + "submitCode","Execute du code")
                                                     .addActionRow(TextInput.create("language", "Language", TextInputStyle.PARAGRAPH).setPlaceholder("python|rust|java").setRequired(true).build())
                                                     .addActionRow(TextInput.create("body", "Code", TextInputStyle.PARAGRAPH).setPlaceholder("Code").setRequired(true).build())
                                                     .build()).queue();
            return;
        } 
        context.replyCallbackAction().setContent("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").setEphemeral(false).queue();
        context.options()
                .get(0)
                .getAsAttachment()
                .getProxy()
                .downloadToFile(new File("%s"+File.pathSeparator+"input.txt".formatted(TEMP_DIR)))
                .thenAcceptAsync(file -> {
                    String content = readFromFile(file).orElse("");
                    Runner runner = RUNNER_MAP.get(context.options().get(1).getAsString());
                    String result = runner.run(content, Config.getGuildVariable(guild.getIdLong(), "COMMAND_CODE_TIMELIMIT"));
                    messageLengthCheck(context.channel(), content, result , context.options().get(1).getAsString());
                    file.delete();
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
        
    }
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains("Code")) return;
        Optional<ModalMapping> lang = Optional.ofNullable(event.getInteraction().getValue("language"));
        Optional<ModalMapping> body = Optional.ofNullable(event.getInteraction().getValue("body"));
        Guild guild = event.getGuild();
        if(lang.isEmpty() || body.isEmpty() || guild == null) return;

        Integer runTimeout = Config.getGuildVariable(guild.getIdLong(), "COMMAND_CODE_TIMELIMIT");
        event.getInteraction().reply("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").queue();

        String languageOption = Objects.requireNonNull(lang.get().getAsString());
        String bodyStr = Objects.requireNonNull(body.get().getAsString());
        Runner runner = RUNNER_MAP.get(languageOption);
        messageLengthCheck(event.getMessageChannel(),bodyStr, runner.run(bodyStr, runTimeout),languageOption);
    }
    private void messageLengthCheck(MessageChannel textChannel, String bodyStr, String result,String codeName){
        try {
            textChannel.sendMessage("```"+codeName.toLowerCase()+"\n"+bodyStr+"\n```").queue();
        } catch (IllegalArgumentException error1){
            try {
                FileWriter myWriter = new FileWriter("%s"+File.pathSeparator+"responseCode.txt".formatted(TEMP_DIR));
                myWriter.write(bodyStr);
                myWriter.close();
                File serverFile = new File("%s"+File.pathSeparator+"responseCode.txt".formatted(TEMP_DIR));
                FileUpload file = FileUpload.fromData(serverFile,"responseCode.txt");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());
            } catch (IOException error3) {
                error1.printStackTrace();
            } catch (Exception e) {
                textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDEDFILESIZE")).queue();
            }
        }
        try {
            textChannel.sendMessage("`"+result+"`").queue();
        } catch (IllegalArgumentException error2){
            try {
                FileWriter myWriter = new FileWriter("%s"+File.pathSeparator+"result.txt".formatted(TEMP_DIR));
                myWriter.write(result);
                myWriter.close();
                File serverFile = new File("%s"+File.pathSeparator+"result.txt".formatted(TEMP_DIR));
                FileUpload file = FileUpload.fromData(serverFile,"result.txt");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());


            } catch (IOException error3) {
                error3.printStackTrace();
            } catch (Exception e) {
                textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDEDFILESIZE")).queue();
            }
        }
    }
    private Optional<String> readFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Optional.of(reader.lines().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
    @Override
    public String getName() {
        return "compile";
    }
    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CODE_DESCRIPTION");
    }
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.ATTACHMENT, "file", Strings.getString("COMMAND_CODE_FILE_OPTION_DESCRIPTION"), false),
                new OptionData(OptionType.STRING, "language", Strings.getString("COMMAND_CODE_LANG_OPTION_DESCRIPTION"), false)
                        .addChoice("python", "python")
                        .addChoice("rust", "rust")
                        .addChoice("java", "java")
        );
    }
    @Override
    public boolean ephemeralReply() {
        return false;
    }
    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }
    @Override
    public boolean adminOnly() {
        return false;
    }
    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_CODE_HELP");
    }

}

