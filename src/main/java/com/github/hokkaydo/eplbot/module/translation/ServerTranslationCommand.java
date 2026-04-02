package com.github.hokkaydo.eplbot.module.translation;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.translation.model.NameDescription;
import com.github.hokkaydo.eplbot.module.translation.repository.NameDescriptionRepository;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ServerTranslationCommand implements Command {

    private final NameDescriptionRepository repository;
    ServerTranslationCommand(NameDescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executeCommand(CommandContext context) {

        if (context.getOption("subcommand").map(OptionMapping::getAsString).orElse("").equals("list")) {
            List<NameDescription> data = repository.readAllByGuildId(context.author().getGuild().getIdLong());
            List<String> languages = data.stream().map(NameDescription::lang).distinct().toList();
            if (languages.isEmpty()) {
                context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.list_no_languages")).queue();
            } else {
                StringBuilder sb = new StringBuilder("\n");
                languages.forEach(lang -> sb.append("- ").append(lang).append("\n"));
                context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.list_languages").formatted(sb.toString())).queue();
            }
            return;
        }

        Optional<String> langOption = context.getOption("language").map(OptionMapping::getAsString);
        if (langOption.isEmpty() || langOption.get().isBlank()) {
            context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.missing_language")).queue();
            return;
        }

        switch (context.getOption("subcommand").map(OptionMapping::getAsString).orElse("")) {
            case "store" -> store(context, langOption.get());
            case "translate" -> translate(context, langOption.get());
            case "restore" -> restore(context, langOption.get());
            case "clear" -> clear(context, langOption.get());
            default -> context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.invalid_subcommand")).queue();
        }
    }

    private void store(CommandContext context, String lang) {
        Long guildId = context.author().getGuild().getIdLong();
        for (var channel : context.author().getGuild().getChannels()) {
            String description = "";
            if (channel instanceof StandardGuildMessageChannel messageChannel) {
                description = messageChannel.getTopic();
            }
            repository.create(new NameDescription(guildId, channel.getIdLong(), channel.getName(), description, lang, NameDescription.Type.CHANNEL));
        }

        for (var category : context.author().getGuild().getCategories()) {
            repository.create(new NameDescription(guildId, category.getIdLong(), category.getName(), "", lang, NameDescription.Type.CATEGORY));
        }

        for (var role : context.author().getGuild().getRoles()) {
            repository.create(new NameDescription(guildId, role.getIdLong(), role.getName(), "", lang, NameDescription.Type.ROLE));
        }
        context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.store_success")).queue();
    }

    private void restore(CommandContext context, String lang) {
        List<NameDescription> data = repository.readAllByGuildIdAndLang(context.author().getGuild().getIdLong(), lang);
        if (data.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.restore_no_data")).queue();
            return;
        }
        for (var item : data) {
            switch (item.type()) {
                case CHANNEL -> {
                    GuildChannel channel = context.author().getGuild().getGuildChannelById(item.id());
                    if (channel == null) continue;
                    channel.getManager().setName(item.name()).queue();
                    if (channel instanceof StandardGuildMessageChannel messageChannel)
                        messageChannel.getManager().setTopic(item.description()).queue();
                }
                case CATEGORY -> {
                    var category = context.author().getGuild().getCategoryById(item.id());
                    if (category != null)
                        category.getManager().setName(item.name()).queue();
                }
                case ROLE -> {
                    var role = context.author().getGuild().getRoleById(item.id());
                    boolean canManage = role != null && context.author()
                                                                .getGuild()
                                                                .getSelfMember()
                                                                .getRoles()
                                                                .stream()
                                                                .filter(r -> r.getGuild().getIdLong() == context.author().getGuild().getIdLong())
                                                                .max((r1, r2) -> r2.getPosition() - r1.getPosition())
                                                                .map(r -> r.canInteract(role))
                                                                .orElse(false);
                    if (canManage)
                        role.getManager().setName(item.name()).queue();
                }
            }
        }
        context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.restore_success")).queue();
    }

    private void translate(CommandContext context, String lang) {
        List<NameDescription> data = repository.readAllByGuildId(context.author().getGuild().getIdLong());
        context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.translating")).queue();
        for (var item : data) {
            switch (item.type()) {
                case CHANNEL -> {
                    GuildChannel channel = context.author().getGuild().getGuildChannelById(item.id());
                    if (channel == null) continue;
                    TranslationModule.translate(item.name(), TranslationModule.FR, lang).ifPresent(name -> channel.getManager().setName(name).queue());
                    if (channel instanceof StandardGuildMessageChannel messageChannel)
                        TranslationModule.translate(item.description(), TranslationModule.FR, lang).ifPresent(description -> messageChannel.getManager().setTopic(description).queue());
                }
                case CATEGORY -> {
                    var category = context.author().getGuild().getCategoryById(item.id());
                    if (category == null) continue;
                    TranslationModule.translate(item.name(), TranslationModule.FR, lang).ifPresent(name -> category.getManager().setName(name).queue());
                }
                case ROLE -> {
                    var role = context.author().getGuild().getRoleById(item.id());
                    boolean canManage = role != null && context.author()
                                                                .getGuild()
                                                                .getSelfMember()
                                                                .getRoles()
                                                                .stream()
                                                                .filter(r -> r.getGuild().getIdLong() == context.author().getGuild().getIdLong())
                                                                .min((r1, r2) -> r2.getPosition() - r1.getPosition())
                                                                .map(r -> r.canInteract(role))
                                                                .orElse(false);
                    if (!canManage) continue;
                    TranslationModule.translate(item.name(), TranslationModule.FR, lang).ifPresent(name -> role.getManager().setName(name).queue());
                }
            }
        }
    }

    private void clear(CommandContext context, String lang) {
        repository.deleteAllByGuildIdAndLang(context.author().getGuild().getIdLong(), lang);
        context.replyCallbackAction().setContent(Strings.getString("command.servertranslation.clear_success")).queue();
    }

    @Override
    public String getName() {
        return "servertranslation";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.servertranslation.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "subcommand", "Subcommand", true)
                        .addChoice("store", "store")
                        .addChoice("translate", "translate")
                        .addChoice("restore", "restore")
                        .addChoice("clear", "clear")
                        .addChoice("list", "list"),
                new OptionData(OptionType.STRING, "language", "Language to use for the operation", false)
        );
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.servertranslation.help");
    }

}
