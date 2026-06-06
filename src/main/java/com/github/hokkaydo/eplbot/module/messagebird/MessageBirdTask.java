package com.github.hokkaydo.eplbot.module.messagebird;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
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

public class MessageBirdTask {

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final Random RANDOM = new Random();
    private static final String[][] LOG_MESSAGES = {
            {"No message today", "<"},
            {"Message today!", ">="}
    };

    private final Long guildId;
    private final List<ScheduledFuture<?>> dayLoops = new ArrayList<>();
    private final List<ScheduledFuture<?>> perfectTimeLoops = new ArrayList<>();
    private final String type;
    private final Path messagesPath;
    private final List<String> messages = new ArrayList<>();
    private final String guildName;
    private final Logger logger;

    public MessageBirdTask(Long guildId, String type, Logger logger) {
        this.guildId = guildId;
        this.type = type;
        this.guildName = Optional.ofNullable(Main.getJDA().getGuildById(guildId)).map(Guild::getName).orElse("");
        this.messagesPath = Path.of("%s/%s_messages.json".formatted(Main.PERSISTENCE_DIR_PATH, type.toLowerCase()));
        this.logger = logger;
    }

    public void start() {
        reloadMessages();

        long startSeconds = Config.getGuildVariable(guildId, type + "_BIRD_RANGE_START_DAY_SECONDS");
        long endSeconds = Config.getGuildVariable(guildId, type + "_BIRD_RANGE_END_DAY_SECONDS");

        long currentSeconds = LocalTime.now().getLong(ChronoField.SECOND_OF_DAY);
        long deltaStart = startSeconds - currentSeconds;
        if (deltaStart <= 0) {
            deltaStart += 24 * 60 * 60;
        }
        long finalDeltaStart = deltaStart;
        String capped = Strings.capsFirstLetter(type);
        logger.info("[{}][{}] Trying to send in {} seconds", capped, guildName, finalDeltaStart);
        dayLoops.add(executor.schedule(() -> {
            int rnd = RANDOM.nextInt(100);
            int proba = Config.<Integer>getGuildVariable(guildId, type + "_BIRD_MESSAGE_PROBABILITY");
            String[] logs = LOG_MESSAGES[rnd > proba ? 0 : 1];
            logger.info("[{}][{}] {} ({} {} {})", Strings.capsFirstLetter(type), guildName, logs[0], proba, logs[1], rnd);
            if (rnd > proba) {
                perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
                dayLoops.removeIf(f -> f.isDone() || f.isCancelled());
                start();
                return;
            }
            long waitTime = RANDOM.nextLong(endSeconds - startSeconds);
            logger.info("[{}][{}] Wait {} seconds before sending", Strings.capsFirstLetter(type), guildName, waitTime);
            perfectTimeLoops.add(executor.schedule(
                    () -> Optional.ofNullable(Main.getJDA().getGuildById(guildId))
                                  .map(guild -> guild.getTextChannelById(Config.getGuildVariable(guildId, type + "_BIRD_CHANNEL_ID")))
                                  .ifPresentOrElse(
                                          this::sendMessage,
                                          () -> MessageUtil.sendAdminMessage("%s_BIRD_CHANNEL_ID (%s) not found".formatted(type, Config.getGuildVariable(guildId, type + "_BIRD_CHANNEL_ID")), guildId)
                                  ),
                    waitTime,
                    TimeUnit.SECONDS
            ));
        }, deltaStart, TimeUnit.SECONDS));
    }

    private void sendMessage(TextChannel channel) {
        String nextMessage = Config.getGuildState(guildId, type + "_BIRD_NEXT_MESSAGE");
        if (nextMessage != null && !nextMessage.isBlank()) {
            channel.sendMessage(nextMessage).setAllowedMentions(null).queue();
            Config.updateValue(guildId, type + "_BIRD_NEXT_MESSAGE", "");
        }else {
            int randomMessageIndex = RANDOM.nextInt(messages.size());
            channel.sendMessage(messages.get(randomMessageIndex)).queue();
        }

        Main.getJDA().listenOnce(MessageReceivedEvent.class)
                .filter(e -> e.getChannel().getId().equals(channel.getId()))
                .filter(e -> !e.isWebhookMessage())
                .filter(e -> !e.getAuthor().isBot())
                .filter(e -> !e.getAuthor().isSystem())
                .subscribe(this::processFirstAnswer);

        perfectTimeLoops.removeIf(f -> f.isDone() || f.isCancelled());
        dayLoops.removeIf(f -> f.isDone() || f.isCancelled());
        start();
    }

    public void stop() {
        perfectTimeLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        dayLoops.forEach(scheduledFuture -> scheduledFuture.cancel(true));
        perfectTimeLoops.clear();
        dayLoops.clear();
        executor.shutdown();
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void restart() {
        stop();
        start();
    }

    public void reloadMessages() {
        List<String> tmpMessages = new ArrayList<>();
        try (InputStream is = messagesPath.toUri().toURL().openStream()) {
            JSONArray array = new JSONArray(new JSONTokener(new InputStreamReader(is)));
            for (int i = 0; i < array.length(); i++) {
                tmpMessages.add(array.getString(i));
            }
            this.messages.clear();
            this.messages.addAll(tmpMessages);
        } catch (JSONException e) {
            logger.warn("Could not parse JSON file at {}", messagesPath, e);
        } catch (Exception e) {
            logger.warn("Could not read JSON file at {}", messagesPath, e);
        }
    }

    public void processFirstAnswer(MessageReceivedEvent event) {
        String messageBirdRoleId = Config.getGuildVariable(guildId, type + "_BIRD_ROLE_ID");
        String unicodeEmoji = Config.getGuildVariable(guildId, type + "_BIRD_UNICODE_REACT_EMOJI");
        Optional.ofNullable(Main.getJDA().getGuildById(guildId)).map(guild -> guild.getRoleById(messageBirdRoleId)).ifPresent(role -> {
            role.getGuild().findMembersWithRoles(role).onSuccess(members -> members.stream().filter(m -> m.getUser().getIdLong() != event.getAuthor().getIdLong()).map(m -> role.getGuild().removeRoleFromMember(m.getUser(), role)).forEach(RestAction::queue));
            role.getGuild().addRoleToMember(event.getAuthor(), role).queue();
            event.getMessage().addReaction(Emoji.fromUnicode(unicodeEmoji)).queue();
        });
    }
}
