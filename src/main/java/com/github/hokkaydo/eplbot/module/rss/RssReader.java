package com.github.hokkaydo.eplbot.module.rss;

import com.github.hokkaydo.eplbot.Config;
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
        for(String url : Config.<List<String>>getGuildValue(guildId, "RSS_FEEDS")){
            SortedSet<Article> results;
            try {
                results = read(url);
            } catch (IOException | FeedException e) {
                throw new IllegalStateException(e);
            }
            for (Article result : results) {
                if(articles.contains(result.hashCode())) continue;
                articles.add(result.hashCode());
                MessageEmbed embed = new EmbedBuilder()
                                             .setTitle(result.title())
                                             .setDescription(result.description()
                                                                     .replace("<b>", "")
                                                                     .replace("</b>", "")
                                                                     .replace("<br />", " ")
                                                                     .replace("<br/>", "\n")
                                             )
                                             .addField("", result.link(), false)
                                             .setTimestamp(result.publishedDate().toInstant())
                                             .setThumbnail(result.imgURL())
                                             .setAuthor(URI.create(result.link()).getHost(), result.link())
                                             .setColor(Config.getGuildValue(guildId, "RSS_FEEDS_COLOR"))
                                             .build();
                TextChannel textChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildValue(guildId, "RSS_FEEDS_CHANNEL_ID"));
                if(textChannel == null) {
                    MessageUtil.sendAdminMessage(Strings.getString("WARNING_RSS_CHANNEL_ID_INVALID"), guildId);
                    futures.get(guildId).cancel(true);
                    return;
                }
                textChannel.sendMessageEmbeds(embed).queue();
            }
        }
    }

    public void launch(Long guildId) {
        futures.put(guildId, service.scheduleAtFixedRate(() -> this.run(guildId), 1, Config.<Long>getGuildValue(guildId, "RSS_UPDATE_PERIOD"), TimeUnit.MINUTES));
    }

    public void stop(Long guildId) {
        futures.get(guildId).cancel(true);
        futures.remove(guildId);
    }
}
