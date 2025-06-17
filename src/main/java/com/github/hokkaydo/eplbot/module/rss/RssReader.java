package com.github.hokkaydo.eplbot.module.rss;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
public class RssReader {

    private final Set<Integer> articles = new HashSet<>();
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(4);
    private final Long guildId;
    private ScheduledFuture<?> task;

    RssReader(Long guild) {
        this.guildId = guild;
    }

    void launch() {
        if (task != null && !task.isCancelled()) stop();
        task = service.scheduleAtFixedRate(this::run, 0, Config.<Long>getGuildVariable(guildId, "RSS_UPDATE_PERIOD"), TimeUnit.MINUTES);
    }

    void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    private SortedSet<Article> read(String feedUrl) throws IOException, FeedException {
        URL feedSource = URI.create(feedUrl).toURL();
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedSource));
        Iterator<SyndEntry> itr = feed.getEntries().iterator();
        SortedSet<Article> results = new TreeSet<>(Comparator.comparing(Article::publishedDate));
        while (itr.hasNext()) {
            SyndEntry syndEntry = itr.next();
            results.add(
                    new Article(
                            syndEntry.getTitle(),
                            syndEntry.getDescription().getValue(),
                            syndEntry.getLink(),
                            ((SyndEnclosure)syndEntry.getEnclosures().getFirst()).getUrl(),
                            syndEntry.getPublishedDate()
                    )
            );
        }
        return results;
    }

    private void run() {
        Map<String, Timestamp> lastDateMap = Config.getGuildState(guildId, "LAST_RSS_ARTICLE_DATE");
        for(String url : Config.<List<String>>getGuildVariable(guildId, "RSS_FEEDS")) {
            SortedSet<Article> results;
            try {
                results = read(url);
            } catch (IOException | FeedException e) {
                Optional.ofNullable(Main.getJDA().getGuildById(guildId)).ifPresent(guild -> {
                    String log = "[%s] Error reading RSS feed %s: %s".formatted(guild.getName(), url, e.getMessage());
                    Main.LOGGER.error(log, e);
                    MessageUtil.sendAdminMessage(Strings.getString("warning.rss_feed_error").formatted(url, e.getMessage()), guildId);
                });
                throw new IllegalStateException(e);
            }

            Timestamp lastDate = lastDateMap.containsKey(url) ? lastDateMap.get(url) : Timestamp.from(Instant.MIN);
            if(results.last().publishedDate().toInstant().isBefore(lastDate.toInstant()) || results.last().publishedDate().toInstant().equals(lastDate.toInstant())) return;

            results.forEach(this::sendArticle);
            lastDateMap.put(url, Timestamp.from(results.last().publishedDate().toInstant()));
            Config.updateValue(guildId, "LAST_RSS_ARTICLE_DATE", lastDateMap);
        }
    }

    private void sendArticle(Article article) {
        if(articles.contains(article.hashCode())) {
            Optional.ofNullable(Main.getJDA().getGuildById(guildId)).ifPresent(guild -> {
                String log = "[%s] Article already sent: %s".formatted(guild.getName(), article.title());
                Main.LOGGER.info(log);
            });
            return;
        }
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
