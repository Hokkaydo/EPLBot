package com.github.hokkaydo.eplbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class BotStatusManager {

    private final JDA jda;
    private final List<Activity> statuses;
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private int lastIndex = -1;

    /**
     * Constructeur
     *
     * @param jda      instance JDA du bot
     * @param statuses liste d'Activity à afficher
     */
    public BotStatusManager(JDA jda, List<Activity> statuses) {
        this.jda = jda;
        this.statuses = statuses;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // ne bloque pas la fermeture du JVM
            t.setName("StatusUpdater");
            return t;
        });
    }

    /**
     * Démarre la mise à jour périodique des statuts
     *
     * @param initialDelaySec délai initial avant la première mise à jour
     * @param minDelaySec     délai minimum entre deux statuts
     * @param maxDelaySec     délai maximum entre deux statuts
     */
    public void start(long initialDelaySec, long minDelaySec, long maxDelaySec) {
        scheduledTask = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                updateStatus();
                long nextDelay = minDelaySec + random.nextInt((int) (maxDelaySec - minDelaySec + 1));
                scheduledTask = scheduler.schedule(this, nextDelay, TimeUnit.SECONDS);
            }
        }, initialDelaySec, TimeUnit.SECONDS);
    }

    /**
     * Met à jour le status avec anti-répétition
     */
    private void updateStatus() {
        if (statuses.isEmpty()) return;

        int index;
        do {
            index = random.nextInt(statuses.size());
        } while (statuses.size() > 1 && index == lastIndex);

        lastIndex = index;
        jda.getPresence().setActivity(statuses.get(index));
    }

    /**
     * Stoppe la mise à jour des statuts et shutdown du scheduler
     */
    public void stop() {
        if (scheduledTask != null) scheduledTask.cancel(false);
        scheduler.shutdownNow();
    }
}