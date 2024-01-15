package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class WikiCommand implements Command {

    private static final WikiScrapper scrapper = new WikitionaryScrapper();
    private static final int MAX_FIELD_SIZE = 1024;
    @Override
    public void executeCommand(CommandContext context) {
        if(context.options().isEmpty()) throw new IllegalStateException("Should never arise");
        String query = context.options().getFirst().getAsString();
        Optional<List<List<String>>> response = scrapper.query(query);
        if(response.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("WIKI_COMMAND_NOT_FOUND")).queue();
            return;
        }
        context.replyCallbackAction().setEmbeds(toEmbed(query,scrapper.getFormattedQueryLink(query), response.get()).build()).queue();
    }

    private EmbedBuilder toEmbed(String query, String link, List<List<String>> response) {
        EmbedBuilder builder = new EmbedBuilder()
                                       .setTitle(query, link)
                                       .addField("Étymologie", response.getFirst().stream().reduce("", (a, b) -> STR."\{a}\n- \{b}"),false)
                                       .setColor(Color.GREEN);

        StringBuilder content = new StringBuilder(response.get(1).getLast());
        boolean first = true;
        int count = 1;
        for (String definition : response.get(2)) {
            if(content.length() + definition.length() + STR."\n\{count}. ".length() > MAX_FIELD_SIZE) {
                builder.addField(new MessageEmbed.Field(first ? response.get(1).getFirst() : "", content.toString(), false));
                first = false;
                content = new StringBuilder();
            }
            content.append(STR."\n\{count++}. ").append(definition);
        }
        builder.addField(new MessageEmbed.Field(first ? response.get(1).getFirst() : "", content.toString(), false));
        return builder;
    }

    @Override
    public String getName() {
        return "wiki";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("WIKI_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "mot", "Mot à définir"));
    }

    @Override
    public boolean ephemeralReply() {
        return false;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("WIKI_COMMAND_HELP");
    }

    private interface WikiScrapper {

        /**
         * Query a word definition
         * @param query word to query definition for
         * @return an {@link Optional} containing a list of interesting entries if found, empty otherwise
         * */
        Optional<List<List<String>>> query(String query);
        /**
         * Get formatted query link
         * @param query the queried word
         * @return a link corresponding to the query
         * */
        String getFormattedQueryLink(String query);
    }

    private static class WikitionaryScrapper implements WikiScrapper {

        private static final String BASE_LINK = "https://fr.wiktionary.org/wiki/";
        private static final String NO_RESULT_FOUND = "Pas de résultat pour";
        private static final String ETYMOLOGY = "Étymologie";
        @Override
        public Optional<List<List<String>>> query(String query) {
            Document document;
            try {
                document = Jsoup.connect(BASE_LINK + query).get();
            } catch (IOException e) {
                return Optional.empty();
            }
            if(!document.body().getElementsMatchingText(NO_RESULT_FOUND).isEmpty())
                return Optional.empty();
            List<String> etymology = document.stream()
                                             .filter(el -> el.id().equals(ETYMOLOGY))
                                             .map(Element::parent)
                                             .filter(Objects::nonNull)
                                             .map(Element::nextElementSibling)
                                             .filter(Objects::nonNull)
                                             .findFirst()
                                             .map(Element::children)
                                             .map(ArrayList::new)
                                             .orElse(new ArrayList<>())
                                             .stream()
                                             .map(Element::text)
                                             .toList();
            Optional<Element> typeEl = document.stream()
                                               .filter(el -> el.id().equals(ETYMOLOGY))
                                               .map(Element::parent)
                                               .filter(Objects::nonNull)
                                               .map(Element::nextElementSibling)
                                               .filter(Objects::nonNull)
                                               .map(Element::nextElementSibling)
                                               .filter(Objects::nonNull)
                                               .findFirst();
            String type = typeEl
                                  .map(Element::children)
                                  .map(ArrayList::new)
                                  .map(ArrayList::getFirst)
                                  .map(Element::text)
                                  .orElse("");
            String spelling = typeEl
                                      .map(Element::nextElementSiblings)
                                      .map(ArrayList::new).stream()
                                      .flatMap(Collection::stream)
                                      .filter(el -> el.tagName().equals("p"))
                                      .findFirst()
                                      .map(Element::text)
                                      .orElse("");

            List<String> definitions = typeEl
                                               .map(Element::nextElementSiblings)
                                               .map(ArrayList::new).stream()
                                               .flatMap(Collection::stream)
                                               .filter(el -> el.tagName().equals("ol"))
                                               .findFirst()
                                               .map(Element::children)
                                               .map(ArrayList::new)
                                               .orElse(new ArrayList<>())
                                               .stream()
                                               .peek(element -> element.children().stream().filter(el -> el.tagName().equals("ul")).forEach(Element::remove))
                                               .map(Element::text)
                                               .toList();


            return Optional.of(List.of(etymology, List.of(type, spelling), definitions));
        }

        @Override
        public String getFormattedQueryLink(String query) {
            return BASE_LINK + query;
        }

    }

}
