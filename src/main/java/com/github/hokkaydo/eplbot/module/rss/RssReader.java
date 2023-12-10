package com.github.hokkaydo.eplbot.module.rss;

import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RssReader {

    private final Set<Integer> articles = new HashSet<>();
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private final Map<Long, ScheduledFuture<?>> futures = new HashMap<>();

    private SortedSet<Article> read(String feedUrl) throws IOException, FeedException {
        URL feedSource = new URL(feedUrl);
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
                            ((SyndEnclosure)syndEntry.getEnclosures().get(0)).getUrl(),
                            syndEntry.getPublishedDate()
                    )
            );
        }

        return results;
    }

    private void run(Long guildId) {
        Map<String, Timestamp> lastDateMap = Config.getGuildState(guildId, "LAST_RSS_ARTICLE_DATE");
        for(String url : Config.<List<String>>getGuildVariable(guildId, "RSS_FEEDS")){
            SortedSet<Article> results;
            try {
                results = read(url);
            } catch (IOException | FeedException e) {
                throw new IllegalStateException(e);
            }
            Timestamp lastDate = lastDateMap.getOrDefault(url, Timestamp.from(Instant.MIN));
            if(results.last().publishedDate().toInstant().isBefore(lastDate.toInstant()) || results.last().publishedDate().toInstant().equals(lastDate.toInstant())) return;
            for (Article result : results) {
                if(articles.contains(result.hashCode())) continue;
                articles.add(result.hashCode());
                MessageEmbed embed = new EmbedBuilder()
                                             .setTitle(result.title())
                                             .addField("", "[Voir](" + result.link() + ")", false)
                                             .setTimestamp(result.publishedDate().toInstant())
                                             .setThumbnail(result.imgURL())
                                             .setAuthor(URI.create(result.link()).getHost(), result.link())
                                             .setColor(Config.getGuildVariable(guildId, "RSS_FEEDS_COLOR"))
                                             .build();
                TextChannel textChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId, "RSS_FEEDS_CHANNEL_ID"));
                if(textChannel == null) {
                    MessageUtil.sendAdminMessage(Strings.getString("WARNING_RSS_CHANNEL_ID_INVALID"), guildId);
                    futures.get(guildId).cancel(true);
                    return;
                }
                textChannel.sendMessageEmbeds(embed).queue();
            }
            lastDateMap.put(url, Timestamp.from(results.last().publishedDate().toInstant()));
            Config.updateValue(guildId, "LAST_RSS_ARTICLE_DATE", lastDateMap);
        }
    }

    void launch(Long guildId) {
        futures.put(guildId, service.scheduleAtFixedRate(() -> this.run(guildId), 0, Config.<Long>getGuildVariable(guildId, "RSS_UPDATE_PERIOD"), TimeUnit.MINUTES));
    }

    void stop(Long guildId) {
        ScheduledFuture<?> f = futures.get(guildId);
        if(f != null )
            f.cancel(true);
        futures.remove(guildId);
    }
}
