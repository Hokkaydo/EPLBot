package com.github.hokkaydo.eplbot.module.globalcommand;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class IssueCommand extends ListenerAdapter implements Command {

    private Instant nextJwtRefresh;
    private GitHub github;
    private static final String ERROR_OCCURRED = "ERROR_OCCURRED";

    private final Map<String, Object[]> labelsFileModalTempStore = new HashMap<>();

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> labels = context.options().stream().filter(o -> o.getName().equals("label")).findFirst();
        if(labels.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString(ERROR_OCCURRED)).queue();
            return;
        }
        Optional<OptionMapping> file = context.options().stream().filter(o -> o.getName().equals("file")).findFirst();
        String key = STR."\{context.author().getId()}-issue-modal";
        labelsFileModalTempStore.put(key, new Object[]{labels.get().getAsString(), file.<Object>map(OptionMapping::getAsAttachment).orElse(null)});

        Modal modal = Modal.create(key, "Formulaire d'issue")
                              .addActionRow(TextInput.create("title", "Titre", TextInputStyle.SHORT).setPlaceholder("Titre").setRequired(true).build())
                              .addActionRow(TextInput.create("body", "Corps", TextInputStyle.PARAGRAPH).setPlaceholder("Corps").setRequired(true).build())
                              .build();
        context.interaction().replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains("-issue-modal")) return;
        if(!labelsFileModalTempStore.containsKey(event.getModalId())) return;
        String title = Objects.requireNonNull(event.getInteraction().getValue("title")).getAsString();
        String body = Objects.requireNonNull(event.getInteraction().getValue("body")).getAsString();

        if(nextJwtRefresh == null || nextJwtRefresh.isBefore(Instant.now())) {
            refreshJwt();
            if(github == null) {
                if(event.getGuild() != null)
                    MessageUtil.sendAdminMessage("IssueCommand: Failed to refresh GitHub JWT", event.getGuild().getIdLong());
                event.reply(Strings.getString(ERROR_OCCURRED)).queue();
                return;
            }
            nextJwtRefresh = Instant.now().plus(45, ChronoUnit.MINUTES);
        }

        String image = "";
        String key = event.getModalId();
        String label = (String) labelsFileModalTempStore.get(key)[0];
        Message.Attachment attachment = (Message.Attachment) labelsFileModalTempStore.get(key)[1];
        if(attachment != null) {
            image = "![%s](%s)".formatted(attachment.getFileName(), attachment.getUrl());
        }
        labelsFileModalTempStore.remove(key);

        try {
            GHIssue issue = github.getRepository("Hokkaydo/EPLBot").createIssue(title).body(STR."\{body}\n\{image}").label(label).create();
            event.reply(Strings.getString("COMMAND_ISSUE_SUCCESSFUL").formatted(issue.getNumber())).queue();
        } catch (IOException e) {
            event.reply(Strings.getString(ERROR_OCCURRED)).queue();
        }
    }


    private void refreshJwt() {
        try {
            RSAPrivateKey privateKey = getPrivateKey(new File(STR."\{Main.PERSISTENCE_DIR_PATH}/github.pem"));
            Algorithm algorithm = Algorithm.RSA256(privateKey);
            String token = JWT.create()
                                   .withIssuedAt(Instant.now().minusSeconds(60))
                                   .withExpiresAt(Instant.now().plusSeconds(60L * 5))
                                   .withIssuer(System.getenv("GITHUB_APPLICATION_ID"))
                                   .sign(algorithm);
            GitHub gitHubApp = new GitHubBuilder().withJwtToken(token).build();
            GHAppInstallation appInstallation = gitHubApp.getApp().getInstallationById(Integer.parseInt(System.getenv("GITHUB_APPLICATION_INSTALLATION_ID")));
            GHAppInstallationToken appInstallationToken = appInstallation.createToken().create();
            github = new GitHubBuilder().withAppInstallationToken(appInstallationToken.getToken()).build();
        } catch (JWTCreationException | IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            github = null;
        }
    }


    // PEM to PKCS8 conversion : openssl pkcs8 -topk8 -inform PEM -outform DER -in key.pem -out key2.pem -nocrypt
    public RSAPrivateKey getPrivateKey(File file) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] key = Files.readAllBytes(file.toPath());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
        PrivateKey finalKey = keyFactory.generatePrivate(keySpec);
        return (RSAPrivateKey) finalKey;
    }

    @Override
    public String getName() {
        return "issue";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_ISSUE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "label", Strings.getString("COMMAND_ISSUE_LABEL_OPTION_DESCRIPTION"), true)
                        .addChoice("bug", "bug")
                        .addChoice("new feature", "new feature")
                        .addChoice("changes", "changes"),
                new OptionData(OptionType.ATTACHMENT, "file", Strings.getString("COMMAND_ISSUE_FILE_OPTION_DESCRIPTION"), false)
        );
    }

    @Override
    public boolean ephemeralReply() {
        return true;
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
        return () -> Strings.getString("COMMAND_ISSUE_HELP");
    }

}
