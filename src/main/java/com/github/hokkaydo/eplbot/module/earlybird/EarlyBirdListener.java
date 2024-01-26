package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.points.PointsProcessor;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class EarlyBirdListener extends ListenerAdapter {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(4);
    private static final Random RANDOM = new Random();
    private static final List<String> MESSAGES = List.of(
            "Helloooo !\nBien dormi ?",
            "DAAAMNNN les gens, c'est l'EPLBot et on se retrouve pour une toute nouvelle journée en pleine forme (non)",
            "Holààà\nCette nuit j'ai rêvé que les profs offraient 10 points à chaque étudiant en vertu de la loi \"Protection des Étudiants\"\nC'était le feu",
            "Holà que tal\nCette nuit j'ai rêvé que Moodle était bien foutu.\nPuis j'me suis réveillé :(",
            "Yo\nComment ça va ?",
            "Hello, bien dormi les gens ?",
            "J'ai galéré à m'endormir hier soir :(\nDu coup j'ai essayé de relire mon cours de stats et j'me suis endormi instant",
            "Comment ça va ça va ça va sur la planèèèèteuh",
            "Bon matin les copains",
            "Bonne journée mes petits chats :)",
            "Ce matin j'imagine un pays sans nuageuhhh ... :notes:\nEt sinon vous avez bien dormi ?",
            "COUCOUUUU",
            "Hello\n Un bon QUOICOUBEHHH ! de bon matin ça fait toujours du bien"
    );
    private final Long guildId;
    private final List<ScheduledFuture<?>> dayLoops = new ArrayList<>();
    private final List<ScheduledFuture<?>> perfectTimeLoops = new ArrayList<>();
    private boolean waitingForAnswer;
    private final PointsProcessor pointsProcessor;
    private static final String[][] LOG_MESSAGES = {
            {"No message today", "<"},
            {"Message today!", ">="}
    };

    public EarlyBirdListener(Long guildId) {
        this.guildId = guildId;
        this.pointsProcessor = new PointsProcessor(guildId);
    }

    public void launchRandomSender() {
        long startSeconds = Config.getGuildVariable(guildId, "EARLY_BIRD_RANGE_START_DAY_SECONDS");
        long endSeconds = Config.getGuildVariable(guildId, "EARLY_BIRD_RANGE_END_DAY_SECONDS");

        long currentSeconds = LocalTime.now().getLong(ChronoField.SECOND_OF_DAY) + 60*60;
        long deltaStart = startSeconds - currentSeconds;
        if (deltaStart < 0) {
            deltaStart += 24*60*60;
        }
        Main.LOGGER.log(Level.INFO, "[EarlyBird] Trying to send in {0} seconds", deltaStart);
        dayLoops.add(EXECUTOR.schedule(() -> {
            int rnd = RANDOM.nextInt(100);
            int proba = Config.<Integer>getGuildVariable(guildId, "EARLY_BIRD_MESSAGE_PROBABILITY");
            String[] logs = LOG_MESSAGES[rnd > proba ? 0 : 1];
            Main.LOGGER.log(Level.WARNING, "[EarlyBird] %s (%d %s %d)".formatted(logs[0], proba, logs[1], rnd));
            if(RANDOM.nextInt(100) > Config.<Integer>getGuildVariable(guildId, "EARLY_BIRD_MESSAGE_PROBABILITY")) return;
            long waitTime = RANDOM.nextLong(endSeconds - startSeconds);
            Main.LOGGER.log(Level.WARNING, "[EarlyBird] Wait %d seconds before sending".formatted(waitTime));
            perfectTimeLoops.add(EXECUTOR.schedule(
                    () -> Optional.ofNullable(Main.getJDA().getGuildById(guildId))
                                  .map(guild -> guild.getTextChannelById(Config.getGuildVariable(guildId, "EARLY_BIRD_CHANNEL_ID")))
                                  .ifPresentOrElse(channel -> {
                                              String nextMessage = Config.getGuildState(guildId, "EARLY_BIRD_NEXT_MESSAGE");
                                              if(nextMessage != null && !nextMessage.isBlank()) {
                                                  channel.sendMessage(nextMessage).queue();
                                                  Config.updateValue(guildId, "EARLY_BIRD_NEXT_MESSAGE", "");
                                                  this.waitingForAnswer = true;
                                                  perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
                                                  launchRandomSender();
                                                  return;
                                              }
                                              int randomMessageIndex = RANDOM.nextInt(MESSAGES.size());
                                              channel.sendMessage(MESSAGES.get(randomMessageIndex)).queue(_ -> this.waitingForAnswer = true);
                                              perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
                                              dayLoops.removeIf(f -> f.isDone() || f.isCancelled());
                                              launchRandomSender();
                                          },
                                          () -> MessageUtil.sendAdminMessage("EARLY_BIRD_CHANNEL_ID (%s) not found".formatted(Config.getGuildVariable(guildId, "EARLY_BIRD_CHANNEL_ID")), guildId)
                                  ),
                    waitTime,
                    TimeUnit.SECONDS
            ));
        }, deltaStart, TimeUnit.SECONDS));
    }

    public void cancel() {
        perfectTimeLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        dayLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        perfectTimeLoops.clear();
        dayLoops.clear();
    }

    public void restart() {
        cancel();
        launchRandomSender();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!waitingForAnswer) return;
        String channelId = Config.getGuildVariable(guildId, "EARLY_BIRD_CHANNEL_ID");
        if(!event.getChannel().getId().equals(channelId) || event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        this.waitingForAnswer = false;
        String earlyBirdRoleId = Config.getGuildVariable(guildId, "EARLY_BIRD_ROLE_ID");
        Optional.ofNullable(Main.getJDA().getGuildById(guildId)).map(guild -> guild.getRoleById(earlyBirdRoleId)).ifPresent(role -> {
            role.getGuild().findMembersWithRoles(role).onSuccess(members -> members.stream().filter(m -> m.getUser().getIdLong() != event.getAuthor().getIdLong()).map(m -> role.getGuild().removeRoleFromMember(m.getUser(), role)).forEach(RestAction::queue));
            role.getGuild().addRoleToMember(event.getAuthor(), role).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("❤")).queue();
            this.pointsProcessor.addPoints(event.getAuthor().getName(), 100);

        });

    }

}
