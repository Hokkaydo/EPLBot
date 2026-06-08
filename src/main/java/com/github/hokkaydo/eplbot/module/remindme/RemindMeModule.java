package com.github.hokkaydo.eplbot.module.remindme;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.remindme.model.Reminder;
import com.github.hokkaydo.eplbot.module.remindme.repository.ReminderRepository;
import com.github.hokkaydo.eplbot.module.remindme.repository.ReminderRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RemindMeModule extends Module {

    private final ReminderRepository repository;
    private final RemindMeCommand command;
    private ScheduledExecutorService executor;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public RemindMeModule(@NotNull Long guildId) {
        super(guildId);
        this.repository = new ReminderRepositorySQLite(DatabaseManager.getDataSource());
        this.command = new RemindMeCommand(repository, this);
    }

    @Override
    public void enable() {
        super.enable();
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }
        loadAndScheduleAll();
    }

    @Override
    public void disable() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
        if (executor != null) executor.shutdown();
        super.disable();
    }

    void schedule(Reminder reminder) {
        long delay = Math.max(0, reminder.triggerTime() - Instant.now().getEpochSecond());
        ScheduledFuture<?> future = executor.schedule(() -> fireReminder(reminder), delay, TimeUnit.SECONDS);
        futures.put(reminder.id(), future);
    }

    private void loadAndScheduleAll() {
        repository.getByGuildId(getGuildId()).forEach(this::schedule);
    }

    private void fireReminder(Reminder reminder) {
        futures.remove(reminder.id());
        repository.deleteById(reminder.id());
        Main.getJDA().retrieveUserById(reminder.userId()).queue(user -> {
            if (user == null) return;
            user.openPrivateChannel().queue(channel ->
                    channel.sendMessage(Strings.getString("command.remindme.dm").formatted(reminder.subject())).queue()
            );
        });
    }

    @Override
    public String getName() {
        return "remindme";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(command);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
