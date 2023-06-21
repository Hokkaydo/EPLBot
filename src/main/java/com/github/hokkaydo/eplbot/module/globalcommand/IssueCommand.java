package com.github.hokkaydo.eplbot.module.globalcommand;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class IssueCommand implements Command {

    private Instant nextJwtRefresh;
    private GitHub github;


    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> titleOpt = context.options().stream().filter(o -> o.getName().equals("title")).findFirst();
        Optional<OptionMapping> bodyOpt = context.options().stream().filter(o -> o.getName().equals("body")).findFirst();
        Optional<OptionMapping> labelOpt = context.options().stream().filter(o -> o.getName().equals("label")).findFirst();
        Optional<OptionMapping> fileOpt = context.options().stream().filter(o -> o.getName().equals("file")).findFirst();
        if(titleOpt.isEmpty() || bodyOpt.isEmpty() || labelOpt.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("ERROR_OCCURRED")).queue();
            return;
        }
        String title = titleOpt.get().getAsString();
        String body = bodyOpt.get().getAsString();
        String label = labelOpt.get().getAsString();
        String file = "";
        if(fileOpt.isPresent()) {
            file = "![%s](%s)".formatted(fileOpt.get().getAsAttachment().getFileName(), fileOpt.get().getAsAttachment().getUrl());
        }
        if(nextJwtRefresh == null || nextJwtRefresh.isBefore(Instant.now())) {
            refreshJwt();
            if(github == null) {
                context.replyCallbackAction().setContent(Strings.getString("ERROR_OCCURED")).queue();
                return;
            }
            nextJwtRefresh = Instant.now().plus(45, ChronoUnit.MINUTES);
        }

        try {
            GHIssue issue = github.getRepository("Hokkaydo/EPLBot").createIssue(title).body(body + "\n" + file).label(label).create();
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_ISSUE_SUCCESSFUL").formatted(issue.getNumber())).queue();
        } catch (IOException e) {
            context.replyCallbackAction().setContent(Strings.getString("ERROR_OCCURED")).queue();
        }
    }

    private void refreshJwt() {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(new File(Main.PERSISTENCE_DIR_PATH + "/github.pem"));
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
        } catch (JWTCreationException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            github = null;
        }
    }


    private PrivateKey getPrivateKey(File file) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String key = Files.readString(file.toPath(), Charset.defaultCharset());

        String privateKeyPEM = key
                                       .replace("-----BEGIN PRIVATE KEY-----", "")
                                       .replaceAll(System.lineSeparator(), "")
                                       .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
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
                new OptionData(OptionType.STRING, "title", Strings.getString("COMMAND_ISSUE_TITLE_OPTION_DESCRIPTION"), true),
                new OptionData(OptionType.STRING, "body", Strings.getString("COMMAND_ISSUE_BODY_OPTION_DESCRIPTION"), true),
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
