package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ContributeCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://api.github.com/repos/Hokkaydo/EPLBot/contributors")).build();
        HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body)
                .thenApply(JSONTokener::new)
                .thenApply(JSONArray::new)
                .thenAccept(array -> {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.addField(
                            "Dépôt",
                            """
                            Vous pouvez contribuer en soumettant une PR sur le dépôt à l'adresse https://github.com/Hokkaydo/EPLBot.
                            Pensez à jeter un coup d'œil au kanban d'avancement dans l'onglet `Projects` afin de voir ce qu'il y a à faire.
                                    """,
                            true
                    );
                    StringBuilder contributors = new StringBuilder();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String nickname = object.getString("login");
                        if(nickname.equalsIgnoreCase("Hokkaydo")) continue;
                        contributors.append(nickname).append("\n");
                    }
                    contributors.append("Hokkaydo");
                    embedBuilder.addField("Contributeur%s :heart:".formatted(!array.isEmpty() ? "s" : ""), contributors.toString(),false);
                    embedBuilder.setAuthor(Main.getJDA().getSelfUser().getName(), "https://github.com/Hokkaydo/EPLBot", Main.getJDA().getSelfUser().getAvatarUrl());
                    context.replyCallbackAction().setEmbeds(embedBuilder.build()).queue();
                });
    }

    @Override
    public String getName() {
        return "contribute";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CONTRIBUTE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
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
        return () -> Strings.getString("COMMAND_CONTRIBUTE_HELP");
    }

}
