package com.github.hokkaydo.eplbot.module.translation;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.translation.repository.NameDescriptionRepository;
import com.github.hokkaydo.eplbot.module.translation.repository.NameDescriptionRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class TranslationModule extends Module {

    // Google Translate API endpoint for translating from French to Dutch
    private static final String TRADUCTION_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s";
    static final String FR = "fr";

    private final TranslationListener listener;
    private final ServerTranslationCommand serverTranslationCommand;

    public TranslationModule(Long guildId) {
        super(guildId);
        listener = new TranslationListener(this);
        NameDescriptionRepository repository = new NameDescriptionRepositorySQLite(DatabaseManager.getDataSource());
        serverTranslationCommand = new ServerTranslationCommand(repository);
    }

    @Override
    public String getName() {
        return "translation";
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(listener);
    }

    @Override
    public List<Command> getCommands() {
        return List.of(serverTranslationCommand);
    }

    static Optional<String> translate(String message, String langFrom, String langTo) {
        if (message == null || message.isBlank()) return Optional.empty();
        String url = String.format(TRADUCTION_URL, langFrom, langTo, URLEncoder.encode(message, StandardCharsets.UTF_8));
        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                                          .uri(URI.create(url))
                                          .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body == null)  return Optional.empty();
            JSONArray jsonArray = new JSONArray(body);
            return Optional.of(jsonArray.getJSONArray(0).getJSONArray(0).getString(0));
        } catch (Exception e) {
            Main.LOGGER.warn("[TradModule] Error occurred while retrieving traduced message", e);
            return Optional.empty();
        }
    }

}
