package com.github.hokkaydo.eplbot.module.preferences;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Supplier;

public class PreferencesCommand implements Command {

    private final long guildId;

    PreferencesCommand(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void executeCommand(CommandContext context) {
        long userId = context.user().getIdLong();

        var themeOpt = context.getOption("theme");
        if (themeOpt.isEmpty()) {
            boolean dark = UserPreferencesStore.isDarkTheme(userId, guildId);
            context.replyCallbackAction()
                    .setContent(Strings.getString("command.preferences.current")
                            .formatted(dark ? "🌑 Dark" : "☀️ Light"))
                    .queue();
            return;
        }

        String theme = themeOpt.get().getAsString();
        UserPreferencesStore.setPreference(userId, UserPreferencesStore.GLOBAL_GUILD_ID, "theme", theme);
        context.replyCallbackAction()
                .setContent(Strings.getString("command.preferences.theme_set")
                        .formatted("dark".equals(theme) ? "🌑 Dark" : "☀️ Light"))
                .queue();
    }

    @Override
    public String getName() {
        return "preferences";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.preferences.description");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "theme",
                        Strings.getString("command.preferences.option.theme.description"), false)
                        .addChoice("Dark", "dark")
                        .addChoice("Light", "light")
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
        return () -> Strings.getString("command.preferences.help");
    }
}
