package com.github.hokkaydo.eplbot.module.helper;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.helper.model.CourseHelper;
import com.github.hokkaydo.eplbot.module.helper.repository.CourseHelperRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class HelperCommand extends ListenerAdapter implements Command {

    private final long guildId;
    private static final String TUTOR = "tutor";
    private final CourseHelperRepository courseHelperRepository;

    public HelperCommand(Long guildId, CourseHelperRepository courseHelperRepository) {
        this.guildId = guildId;
        this.courseHelperRepository = courseHelperRepository;
    }

    @Override
    public void executeCommand(CommandContext context) {
        String action = context.getOption("action").map(OptionMapping::getAsString).orElseThrow(() -> new IllegalStateException("Should not arise"));
        switch (action) {
            case "manage" -> manage(context);
            case "list" -> list(context);
            case "allow_ping" -> ping(context);
            case TUTOR -> tutor(context);
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    private void manage(CommandContext context) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("category");
        List<String> toRemove = new ArrayList<>();
        List<SelectOption> options = Config.<List<String>>getGuildVariable(guildId, "HELPER_CATEGORY_IDS")
                                             .stream()
                                             .map(c -> {
                                                 Category cat = Main.getJDA().getCategoryById(c);
                                                 if (cat == null) {
                                                     toRemove.add(c);
                                                     return null;
                                                 }
                                                 return SelectOption.of(cat.getName(), cat.getId());
                                             })
                                             .filter(Objects::nonNull)
                                             .toList();
        if (options.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.helper.no_category")).queue();
            return;
        }
        menu.addOptions(options);
        menu.setRequiredRange(1, 1);
        context.replyCallbackAction().setActionRow(menu.build()).queue();
        if (toRemove.isEmpty()) return;
        List<String> newIds = new ArrayList<>(Config.getGuildState(guildId, "HELPER_CATEGORY_IDS"));
        newIds.removeAll(toRemove);
        Config.updateValue(guildId, "HELPER_CATEGORY_IDS", newIds);

    }

    private void list(CommandContext context) {
        List<HelperPing> helpers = courseHelperRepository.readByChannelId(context.channel().getIdLong())
                                         .stream()
                                         .map(c -> Main.getJDA()
                                                           .retrieveUserById(c.userId())
                                                           .map(u -> new HelperPing(u, c.allowsPing(), c.isTutor()))
                                                           .complete())
                                         .filter(Objects::nonNull)
                                         .sorted(this::helpersComparator)
                                         .toList();
        context.replyCallbackAction()
                .setContent(
                        helpers.isEmpty() ?
                                Strings.getString("command.helper.list.no_helper") :
                                helpers.stream()
                                        .map(this::formatHelper)
                                        .reduce(Strings.getString("command.helper.list.header"), "%s%n%s"::formatted)
                )
                .queue();
    }

    private void ping(CommandContext context) {
        List<CourseHelper> courses = courseHelperRepository.readByUserId(context.user().getIdLong());
        if(courses.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.helper.no_course")).queue();
            return;
        }

        StringSelectMenu.Builder pingMenu = StringSelectMenu.create("ping");

        List<SelectOption> options = courses.stream()
                                             .map(c -> {
                                                 TextChannel channel = Main.getJDA().getTextChannelById(c.channelId());
                                                 if (channel == null) {
                                                     courseHelperRepository.deleteByChannelId(c.channelId());
                                                     return null;
                                                 }
                                                 return SelectOption.of(channel.getName(), channel.getId()).withDefault(c.allowsPing());
                                             })
                                             .filter(Objects::nonNull)
                                             .toList();
        if (options.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.helper.no_category")).queue();
            return;
        }
        pingMenu.addOptions(options);
        pingMenu.setRequiredRange(0, options.size());
        context.replyCallbackAction().setActionRow(pingMenu.build()).queue();
    }

    private void tutor(CommandContext context) {
        List<CourseHelper> courses = courseHelperRepository.readByUserId(context.user().getIdLong());
        if(courses.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.helper.no_course")).queue();
            return;
        }

        StringSelectMenu.Builder tutorMenu = StringSelectMenu.create(TUTOR);

        List<SelectOption> options = courses.stream()
                                             .map(c -> {
                                                 TextChannel channel = Main.getJDA().getTextChannelById(c.channelId());
                                                 if (channel == null) {
                                                     courseHelperRepository.deleteByChannelId(c.channelId());
                                                     return null;
                                                 }
                                                 return SelectOption.of(channel.getName(), channel.getId()).withDefault(c.isTutor());
                                             })
                                             .filter(Objects::nonNull)
                                             .toList();

        tutorMenu.addOptions(options);
        tutorMenu.setRequiredRange(0, options.size());
        context.replyCallbackAction().setActionRow(tutorMenu.build()).queue();
    }

    private int helpersComparator(HelperPing t1, HelperPing t2) {
        if(t1.isTutor() != t2.isTutor())
            return t1.isTutor() ? 1 : -1;
        if (t1.allowsPing() != t2.allowsPing())
            return t1.allowsPing() ? 1 : -1;
        return 0;
    }

    private String formatHelper(HelperPing helper) {
        return helper.user.getAsMention() + " " + (helper.allowsPing ? ":loudspeaker:" : "") + (helper.isTutor ? ":star2:" : "");
    }

    @Override
    public String getName() {
        return "helper";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.helper.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING,"action", Strings.getString("command.helper.option.action.description"),true)
                        .addChoice("manage", "manage")
                        .addChoice("list", "list")
                        .addChoice("allow_ping", "allow_ping")
                        .addChoice(TUTOR, TUTOR)
        );
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.helper.help");
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        switch (event.getComponentId().split("-")[0]) {
            case "category" -> handleCategoryMenu(event);
            case "courses" -> handleCourseMenu(event);
            case "allow_ping" -> handlePingMenu(event);
            case TUTOR -> handleTutorMenu(event);
            default -> event.reply(Strings.getString("error_occurred")).setEphemeral(true).queue();
        }
    }

    private void handleCategoryMenu(StringSelectInteractionEvent event) {
        if(event.getSelectedOptions().isEmpty()) {
            event.reply(Strings.getString("error_occurred")).setEphemeral(true).queue();
            return;
        }
        String category = event.getSelectedOptions().getFirst().getValue();
        StringSelectMenu.Builder menu = StringSelectMenu.create("courses");

        List<Long> selectedCourses = courseHelperRepository.readByUserId(event.getUser().getIdLong()).stream().map(CourseHelper::channelId).toList();

        List<SelectOption> availableCourses = new ArrayList<>(Optional.ofNullable(Main.getJDA().getCategoryById(Long.parseLong(category)))
                                                                      .orElseThrow(() -> new IllegalStateException("Category doesn't exist !"))
                                                                      .getChannels()
                                                                      .stream()
                                                                      .map(TextChannel.class::cast)
                                                                      .map(s -> SelectOption.of(s.getName(), s.getId()).withDefault(selectedCourses.contains(s.getIdLong())))
                                                                      .toList());

        if(availableCourses.isEmpty()) {
            MessageUtil.sendAdminMessage(Strings.getString("command.helper.category_without_course").formatted(category), guildId);
            event.getInteraction().reply(Strings.getString("error_occurred")).setEphemeral(true).queue();
            return;
        }
        menu.setRequiredRange(0, availableCourses.size());
        menu.addOptions(availableCourses);

        event.getInteraction().editSelectMenu(menu.build()).queue();
    }

    private void handleCourseMenu(StringSelectInteractionEvent event) {
        Guild guild = Main.getJDA().getGuildById(guildId);
        if (guild == null) {
            event.reply(Strings.getString("error_occurred")).queue();
            return;
        }

        // Clear non-selected courses
        event.getSelectMenu().getOptions()
                .stream()
                .filter(o -> !event.getSelectedOptions().contains(o))
                .map(o -> Main.getJDA().getTextChannelById(o.getValue()))
                .filter(Objects::nonNull)
                .forEach(channel -> {
                    channel.getManager().removePermissionOverride(event.getUser().getIdLong()).reason("Helper deletion").queue();
                    courseHelperRepository.delete(new CourseHelper(channel.getIdLong(), event.getUser().getIdLong(), false, false));
                });

        // Avoid already selected courses
        List<String> oldIds = courseHelperRepository.readByUserId(event.getUser().getIdLong())
                                      .stream()
                                      .map(c -> String.valueOf(c.channelId()))
                                      .toList();

        // Add new courses
        event.getSelectedOptions()
                .stream()
                .filter(o -> !oldIds.contains(o.getValue()))
                .map(o -> Main.getJDA().getTextChannelById(o.getValue()))
                .filter(Objects::nonNull)
                .forEach(channel -> {
                    channel.getManager().putMemberPermissionOverride(
                                    event.getUser().getIdLong(),
                                    Permission.VIEW_CHANNEL.getRawValue() | Permission.MESSAGE_SEND.getRawValue(),
                                    0
                            )
                            .reason("Helper permission")
                            .queue();
                    courseHelperRepository.create(new CourseHelper(channel.getIdLong(), event.getUser().getIdLong(), false, false));
                });
        event.reply(Strings.getString("command.helper.success")).setEphemeral(true).queue();
    }

    private void handlePingMenu(StringSelectInteractionEvent event) {
        event.getSelectedOptions().forEach(o -> courseHelperRepository.updatePing(Long.parseLong(o.getValue()), event.getUser().getIdLong(), true));

        event.getSelectMenu().getOptions()
                .stream()
                .filter(o -> !event.getSelectedOptions().contains(o))
                .forEach(o -> courseHelperRepository.updatePing(Long.parseLong(o.getValue()), event.getUser().getIdLong(), false));
        event.reply(Strings.getString("command.helper.success")).setEphemeral(true).queue();
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return channel instanceof TextChannel;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    private void handleTutorMenu(StringSelectInteractionEvent event) {
        event.getSelectedOptions().forEach(o -> courseHelperRepository.updateTutor(Long.parseLong(o.getValue()), event.getUser().getIdLong(), true));

        event.getSelectMenu().getOptions()
                .stream()
                .filter(o -> !event.getSelectedOptions().contains(o))
                .forEach(o -> courseHelperRepository.updateTutor(Long.parseLong(o.getValue()), event.getUser().getIdLong(), false));
        event.reply(Strings.getString("command.helper.success")).setEphemeral(true).queue();
    }


    private record HelperPing(User user, boolean allowsPing, boolean isTutor) {}

}