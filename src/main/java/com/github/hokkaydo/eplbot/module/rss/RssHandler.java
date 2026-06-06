package com.github.hokkaydo.eplbot.module.rss;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.RssReader;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Will probably be removed sonner or later
 * */
public class RssHandler {

    private final Set<Integer> articles = new HashSet<>();
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final Long guildId;
    private ScheduledFuture<?> task;
    private final RssReader rssReader = new RssReader();

    RssHandler(Long guild) {
        this.guildId = guild;
    }

    void launch() {
        if (task != null && !task.isCancelled()) stop();
        if (service.isShutdown()) service = Executors.newSingleThreadScheduledExecutor();
        task = service.scheduleAtFixedRate(this::run, 0, Config.<Long>getGuildVariable(guildId, "RSS_UPDATE_PERIOD"), TimeUnit.MINUTES);
    }

    void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        service.shutdown();
    }

    private void run() {
        Map<String, Timestamp> lastDateMap = Config.getGuildState(guildId, "LAST_RSS_ARTICLE_DATE");
        for(String url : Config.<List<String>>getGuildVariable(guildId, "RSS_FEEDS")) {
            SortedSet<Article> results;
            try {
                results = read(url);
            } catch (IOException e) {
                Optional.ofNullable(Main.getJDA().getGuildById(guildId)).ifPresent(guild -> {
                    String log = "[%s] Error reading RSS feed %s: %s".formatted(guild.getName(), url, e.getMessage());
                    Main.LOGGER.error(log, e);
                    MessageUtil.sendAdminMessage(Strings.getString("warning.rss_feed_error").formatted(url, e.getMessage()), guildId);
                });
                throw new IllegalStateException(e);
            }

            Timestamp lastDate;

            // Bizarre construction, would have preferred to use getOrDefault or 
            // at least a ternary operator, but it just stucks the task dunno why
            if (lastDateMap.containsKey(url)) {
                lastDate = lastDateMap.get(url);
            } else {
                lastDate = Timestamp.from(results.first().publishedDate().toInstant());
            }

            if(results.last().publishedDate().toInstant().isBefore(lastDate.toInstant()) || results.last().publishedDate().toInstant().equals(lastDate.toInstant())) return;

            results.forEach(this::sendArticle);
            lastDateMap.put(url, Timestamp.from(results.last().publishedDate().toInstant()));
            Config.updateValue(guildId, "LAST_RSS_ARTICLE_DATE", lastDateMap);
        }
    }

    private SortedSet<Article> read(String feedUrl) throws IOException {
        SortedSet<Article> items = new TreeSet<>(Comparator.comparing(Article::publishedDate));
        rssReader.read(feedUrl)
                .map(item -> new Article(
                                item.getTitle().orElse(""),
                                item.getDescription().orElse(""),
                                item.getLink().orElse(""),
                                item.getEnclosure().map(Enclosure::getUrl).orElse(""),
                                item.getPubDateZonedDateTime().map(ZonedDateTime::toInstant).map(Date::from).orElse(Date.from(Instant.now()))
                        )
                )
                .sorted(Comparator.comparing(Article::publishedDate))
                .forEach(items::add);
        return items;
    }

    private void sendArticle(Article article) {
        if(articles.contains(article.hashCode())) return;
        articles.add(article.hashCode());
        MessageEmbed embed = new EmbedBuilder()
                                     .setTitle(article.title())
                                     .addField("", "[Voir](%s)".formatted(article.link()), false)
                                     .setTimestamp(article.publishedDate().toInstant())
                                     .setThumbnail(article.imgURL())
                                     .setAuthor(URI.create(article.link()).getHost(), article.link())
                                     .setColor(Config.getGuildVariable(guildId, "RSS_FEEDS_COLOR"))
                                     .build();
        TextChannel textChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId, "RSS_FEEDS_CHANNEL_ID"));
        if(textChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("warning.rss_channel_id_invalid"), guildId);
            task.cancel(true);
            return;
        }
        textChannel.sendMessageEmbeds(embed).queue();
    }
}
