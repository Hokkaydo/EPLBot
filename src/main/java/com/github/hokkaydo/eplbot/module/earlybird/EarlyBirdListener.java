package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.configuration.Config;
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

public class EarlyBirdListener extends ListenerAdapter {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);
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
    private String nextMessage;
    private boolean waitingForAnswer;

    public EarlyBirdListener(Long guildId) {
        this.guildId = guildId;
    }

    public void launchRandomSender(long startSeconds, long endSeconds, int messageProbability, long channelId) {
        long currentSeconds = LocalTime.now().getLong(ChronoField.SECOND_OF_DAY) + 60*60;
        long deltaStart = startSeconds - currentSeconds;
        if (deltaStart < 0) {
            deltaStart += 24*60*60;
        }
        dayLoops.add(EXECUTOR.schedule(() -> {
            if(RANDOM.nextInt(100) > messageProbability) return;
            long waitTime = RANDOM.nextLong(endSeconds - startSeconds);
            perfectTimeLoops.add(EXECUTOR.schedule(
                    () -> Optional.ofNullable(Main.getJDA().getGuildById(guildId))
                                  .map(guild -> guild.getTextChannelById(channelId))
                                  .ifPresent(channel -> {
                                      if(this.nextMessage != null) {
                                          channel.sendMessage(this.nextMessage).queue();
                                          this.nextMessage = "";
                                          this.waitingForAnswer = true;
                                          perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
                                          return;
                                      }
                                      int randomMessageIndex = RANDOM.nextInt(MESSAGES.size());
                                      channel.sendMessage(MESSAGES.get(randomMessageIndex)).queue(v -> this.waitingForAnswer = true);
                                      perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
                                  }),
                    waitTime,
                    TimeUnit.SECONDS
            ));
            long start = Config.getGuildVariable(guildId, "EARLY_BIRD_RANGE_START_DAY_SECONDS");
            long end = Config.getGuildVariable(guildId, "EARLY_BIRD_RANGE_END_DAY_SECONDS");
            int probability = Config.getGuildVariable(guildId, "EARLY_BIRD_MESSAGE_PROBABILITY");
            long channelId2 = Config.getGuildVariable(guildId, "EARLY_BIRD_CHANNEL_ID");
            dayLoops.removeIf(f -> f.isDone() || f.isCancelled());

            launchRandomSender(start, end, probability, channelId2);

        }, deltaStart, TimeUnit.SECONDS));
    }

    public void cancel() {
        perfectTimeLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        dayLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        perfectTimeLoops.clear();
        dayLoops.clear();
    }

    public void setNextMessage(String message) {
        this.nextMessage = message;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!waitingForAnswer) return;
        long channelId = Config.<Long>getGuildVariable(guildId, "EARLY_BIRD_CHANNEL_ID");
        if(event.getChannel().getIdLong() != channelId || event.isWebhookMessage() || event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
        this.waitingForAnswer = false;
        long earlyBirdRoleId = Config.<Long>getGuildVariable(guildId, "EARLY_BIRD_ROLE_ID");
        Optional.ofNullable(Main.getJDA().getGuildById(guildId)).map(guild -> guild.getRoleById(earlyBirdRoleId)).ifPresent(role -> {
            role.getGuild().findMembersWithRoles(role).onSuccess(members -> members.stream().filter(m -> m.getUser().getIdLong() != event.getAuthor().getIdLong()).map(m -> role.getGuild().removeRoleFromMember(m.getUser(), role)).forEach(RestAction::queue));
            role.getGuild().addRoleToMember(event.getAuthor(), role).queue();
            event.getMessage().addReaction(Emoji.fromUnicode("❤")).queue();
        });

    }

}
