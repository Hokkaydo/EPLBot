package com.github.hokkaydo.eplbot.module.data;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DataModule extends Module {

    private static final boolean AUTO_CLEANUP_ENABLED = true;
    private static final int DATA_RETENTION_DAYS = 365; // Keep data for 1 year
    private static final int CLEANUP_INTERVAL_HOURS = 24; // Run cleanup daily
    
    private final DataListener listener;
    private final DataCommand dataCommand;
    private final DataRepository dataRepository;
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> cleanupTask;

    public DataModule(@NotNull Long guildId) {
        super(guildId);
        DataWriter dataWriter = new DataWriter(DatabaseManager.getDataSource());
        dataRepository = new DataRepository(DatabaseManager.getDataSource());
        listener = new DataListener(getGuildId(), dataWriter);
        dataCommand = new DataCommand(dataRepository);
        
        if (AUTO_CLEANUP_ENABLED) {
            startCleanupTask();
        }
    }

    private void startCleanupTask() {
        cleanupTask = cleanupScheduler.scheduleAtFixedRate(
            this::performCleanup,
            24,
            CLEANUP_INTERVAL_HOURS,
            TimeUnit.HOURS
        );
    }

    private void performCleanup() {
        try {
            int deletedRows = dataRepository.cleanupOldEvents(DATA_RETENTION_DAYS);
            if (deletedRows > 0) {
                Main.LOGGER.info("[DataModule] Cleaned up {} events older than {} days for guild {}", 
                    deletedRows, DATA_RETENTION_DAYS, getGuildId());
            }
        } catch (Exception e) {
            Main.LOGGER.error("[DataModule] Error during data cleanup for guild {}: {}", 
                getGuildId(), e.getMessage(), e);
        }
    }

    /**
     * Stop the cleanup task when module is disabled
     */
    public void stopCleanupTask() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel(true);
        }
        cleanupScheduler.shutdown();
    }


    @Override
    public String getName() {
        return "datamodule";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(dataCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(listener);
    }

}
