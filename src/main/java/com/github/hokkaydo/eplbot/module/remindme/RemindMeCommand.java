package com.github.hokkaydo.eplbot.module.remindme;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.remindme.model.Reminder;
import com.github.hokkaydo.eplbot.module.remindme.repository.ReminderRepository;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemindMeCommand implements Command {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([dhms])");
    private static final int SUBJECT_MAX_LENGTH = 100;

    private final ReminderRepository repository;
    private final RemindMeModule module;

    RemindMeCommand(ReminderRepository repository, RemindMeModule module) {
        this.repository = repository;
        this.module = module;
    }

    @Override
    public void executeCommand(CommandContext context) {
        String when = context.getOption("when").map(o -> o.getAsString()).orElse("");
        String subject = context.getOption("subject").map(o -> o.getAsString()).orElse("");

        if (subject.length() > SUBJECT_MAX_LENGTH) {
            context.replyCallbackAction().setContent(Strings.getString("command.remindme.subject_too_long")).queue();
            return;
        }

        long delaySeconds = parseDuration(when);
        if (delaySeconds < 0) {
            context.replyCallbackAction().setContent(Strings.getString("command.remindme.invalid_duration")).queue();
            return;
        }

        long triggerTime = Instant.now().getEpochSecond() + delaySeconds;
        long guildId = context.author() != null ? context.author().getGuild().getIdLong() : 0L;
        Reminder placeholder = new Reminder(0, context.user().getIdLong(), guildId, triggerTime, subject);
        long id = repository.createAndGetId(placeholder);
        Reminder reminder = new Reminder(id, placeholder.userId(), placeholder.guildId(), placeholder.triggerTime(), placeholder.subject());
        module.schedule(reminder);

        context.replyCallbackAction()
                .setContent(Strings.getString("command.remindme.scheduled").formatted(triggerTime))
                .queue();
    }

    private long parseDuration(String input) {
        Matcher matcher = DURATION_PART.matcher(input.toLowerCase());
        long totalSeconds = 0;
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            long amount = Long.parseLong(matcher.group(1));
            totalSeconds += switch (matcher.group(2)) {
                case "d" -> amount * 86400;
                case "h" -> amount * 3600;
                case "m" -> amount * 60;
                case "s" -> amount;
                default -> 0;
            };
        }
        return matched ? totalSeconds : -1;
    }

    @Override
    public String getName() {
        return "remindme";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.remindme.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "when", Strings.getString("command.remindme.option.when.description"), true),
                new OptionData(OptionType.STRING, "subject", Strings.getString("command.remindme.option.subject.description"), true)
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
        return () -> Strings.getString("command.remindme.help");
    }

}
