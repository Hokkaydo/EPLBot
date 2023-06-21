package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class IssueCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> titleOpt = context.options().stream().filter(o -> o.getName().equals("title")).findFirst();
        Optional<OptionMapping> bodyOpt = context.options().stream().filter(o -> o.getName().equals("body")).findFirst();
        Optional<OptionMapping> labelOpt = context.options().stream().filter(o -> o.getName().equals("label")).findFirst();
        if(titleOpt.isEmpty() || bodyOpt.isEmpty() || labelOpt.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("ERROR_OCCURRED")).queue();
            return;
        }
        String title = titleOpt.get().getAsString();
        String body = bodyOpt.get().getAsString();
        String label = labelOpt.get().getAsString();
        HttpRequest request = HttpRequest.newBuilder()
                                      .POST(HttpRequest.BodyPublishers.ofString(
                                              "{owner:'Hokkaydo', repo:'EPLBot', title:'%s', body:'%s', label:'%s'}".formatted(title, body, label)
                                      ))
                                      .uri(URI.create("https://api.github.com/repos/Hokkaydo/EPLBot/issues"))
                                      .header("Authorization", "Bearer %s".formatted(System.getenv("GITHUB_APP_TOKEN")))
                                      .build();
        HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body)
                .thenApply(JSONTokener::new)
                .thenApply(JSONArray::new)
                .thenApply(a -> a.getJSONObject(0))
                .thenApply(o -> o.getInt("number"))
                .thenAccept(i -> context.replyCallbackAction().setContent(Strings.getString("COMMAND_ISSUE_SUCCESSFUL").formatted(i)).queue());
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
                        .addChoice("changes", "changes")
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
