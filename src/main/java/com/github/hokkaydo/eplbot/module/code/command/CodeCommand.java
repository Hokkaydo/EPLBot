package com.github.hokkaydo.eplbot.module.code.command;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.code.GlobalProcessIdManager;
import com.github.hokkaydo.eplbot.module.code.Runner;
import com.github.hokkaydo.eplbot.module.code.c.CRunner;
import com.github.hokkaydo.eplbot.module.code.java.JavaRunner;
import com.github.hokkaydo.eplbot.module.code.python.PythonRunner;

import net.dv8tion.jda.api.components.Components;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CodeCommand extends ListenerAdapter implements Command {

    private static final GlobalProcessIdManager ID_MANAGER = new GlobalProcessIdManager();
    private final PerformResponse response = new PerformResponse(); // The class handling the response (sending trough
                                                                    // discord etc. )
    private static final Map<String, Class<? extends Runner>> RUNNERMAP;
    /*
     * /!\
     * This is where to put every new language added
     * /!\
     */
    static {
        RUNNERMAP = Map.of(
                "java", JavaRunner.class,
                "python", PythonRunner.class,
                "c", CRunner.class);
    }

    private final long guildId;

    public CodeCommand(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void executeCommand(CommandContext context) {
        if (context.options().size() <= 2) {
            // No file given
            String currentLang = context.options().getFirst().getAsString();
            String modalName = "%s-code_submission-%s".formatted(context.author().getId(), currentLang);
            if (context.options().get(1).getAsBoolean()) {
                modalName += "-spoiler";
            }
            TextInput codeInput = TextInput.create("body", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Code")
                    .setRequired(true)
                    .build();
            context.interaction().replyModal(
                Modal.create(modalName, "Exécute du code")
                    .addComponents(Label.of("Code", codeInput))
                    .build()
                )
            .queue();
            return;
        }
        long current = Instant.now().toEpochMilli();
        File inputFile;
        try {
            inputFile = File.createTempFile("code-input-", ".txt");
        } catch (IOException e) {
            context.channel().sendMessage("Server error: could not create temp file").queue();
            return;
        }
        context.replyCallbackAction().setContent("Processing since: <t:%d:R>".formatted(current / 1000))
                .setEphemeral(false).queue(reply -> context.options()
                        .get(2)
                        .getAsAttachment()
                        .getProxy()
                        .downloadToFile(inputFile)
                        .thenAcceptAsync(file -> {
                            String code = readFromFile(file, context.channel()).orElse(null);
                            if (code == null) {
                                return;
                            }
                            Runner runner = instantiateRunner(context.options().getFirst().getAsString());
                            CompletableFuture<Pair<String, Integer>> futureResult = CompletableFuture
                                    .supplyAsync(() -> runner.run(code, Config.getGuildVariable(
                                            Objects.requireNonNull(context.interaction().getGuild()).getIdLong(),
                                            "COMMAND_CODE_TIMELIMIT")));
                            boolean hasSpoiler = context.options().get(1).getAsBoolean();

                            futureResult.thenAccept(result -> {
                                response.sendSubmittedCode(context.channel(), code,
                                        context.options().getFirst().getAsString(), hasSpoiler);
                                response.sendResult(context.channel(), result.getLeft(), result.getRight(), hasSpoiler);
                                if (file != null && !file.delete()) {
                                    Main.LOGGER.info("File isn't deleted");
                                }
                                long sent = Instant.now().toEpochMilli();
                                reply.editOriginal("Processing time: `%d ms`".formatted(sent - current)).queue();
                            });
                        })
                        .exceptionally(t -> {
                            context.channel()
                                    .sendMessage("""
                                            %s
                                            The error is: %s""".formatted(
                                            Strings.getString("command.code.unexpected_error"), t.getMessage()))
                                    .queue();
                            return null;
                        }));
    }

    /**
     * @param type the language type
     * @return a new Runner for the code to run on
     */
    private Runner instantiateRunner(String type) {
        Class<? extends Runner> runnerClass = RUNNERMAP.get(type);
        if (runnerClass == null)
            throw new IllegalArgumentException("Unsupported language: " + type);
        try {
            return runnerClass.getDeclaredConstructor(String.class)
                    .newInstance(String.valueOf(ID_MANAGER.getNextNumber()));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param file        the file with data to be read from
     * @param textChannel the channel of interaction, in case of an error
     * @return the data of the file
     */
    private Optional<String> readFromFile(File file, MessageChannel textChannel) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Optional.of(reader.lines().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            textChannel.sendMessage(Strings.getString("command.code.inaccessible_file")).queue();
            return Optional.empty();
        }
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.code.description");
    }

    @Override
    public String getName() {
        return "compile";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        OptionData codeOptions = new OptionData(OptionType.STRING, "language",
                Strings.getString("command.code.option.lang.description"), true);
        for (Map.Entry<String, Class<? extends Runner>> entry : RUNNERMAP.entrySet()) {
            codeOptions.addChoice(entry.getKey(), entry.getKey());
        }
        return List.of(
                codeOptions,
                new OptionData(OptionType.BOOLEAN, "spoiler",
                        Strings.getString("command.code.option.spoiler.description"), true),
                new OptionData(OptionType.ATTACHMENT, "file", Strings.getString("command.code.option.file.description"),
                        false));
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.code.help") + String.join(", ", RUNNERMAP.keySet());
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
    public void onModalInteraction(ModalInteractionEvent event) {
        // Check for a valid modal
        if (event.getGuild() == null || event.getGuild().getIdLong() != guildId)
            return;
        if (event.getInteraction().getType() != InteractionType.MODAL_SUBMIT
                || !event.getModalId().contains("-code_submission-"))
            return;
        Optional<ModalMapping> body = Optional.ofNullable(event.getInteraction().getValue("body"));
        Guild guild = event.getGuild();
        if (body.isEmpty() || guild == null) {
            event.getMessageChannel().sendMessage(Strings.getString("command.code.no_language_specified")).queue();
            return;
        }
        Integer runTimeout = Config.getGuildVariable(guild.getIdLong(), "COMMAND_CODE_TIMELIMIT");
        long current = Instant.now().toEpochMilli();
        event.getInteraction().reply("Processing since: <t:%d:R>".formatted(current / 1000)).queue(reply -> {
            String languageOption = event.getModalId().split("-")[2];

            String code = Objects.requireNonNull(body.get().getAsString());
            Runner runner = instantiateRunner(languageOption);
            CompletableFuture<Pair<String, Integer>> futureResult = CompletableFuture
                    .supplyAsync(() -> runner.run(code, runTimeout));
            futureResult.thenAccept(result -> {
                boolean hasSpoiler = event.getModalId().contains("spoiler");
                response.sendSubmittedCode(event.getChannel(), code, languageOption, hasSpoiler);
                response.sendResult(event.getChannel(), result.getLeft(), result.getRight(), hasSpoiler);
                long sent = Instant.now().toEpochMilli();
                reply.editOriginal("Processing time: `%d ms`".formatted(sent - current)).queue();
            });
        });
    }
}
