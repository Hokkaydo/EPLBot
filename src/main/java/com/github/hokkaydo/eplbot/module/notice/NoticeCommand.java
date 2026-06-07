package com.github.hokkaydo.eplbot.module.notice;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepository;
import com.github.hokkaydo.eplbot.module.notice.model.Notice;
import com.github.hokkaydo.eplbot.module.notice.repository.NoticeRepository;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class NoticeCommand extends ListenerAdapter implements Command {

    private static final int HASTEBIN_MAX_CONTENT_LENGTH = 350_000;
    private final Map<String, Object[]> noticeData = new HashMap<>();
    private final List<Course> courses;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> alreadyUsedTimers = new HashMap<>();

    private static final String NOTICE_SELECT_MENU_NAME_SUFFIX = "-notice-select-menu";
    private static final String NOTICE_MODAL_NAME_SUFFIX = "-notice-modal";
    private static final String NOTICE = "notice";
    private static final String GROUP = "group";
    private static final String WRITE_ACTION = "write";
    private static final String READ_ACTION = "read";
    private final NoticeRepository noticeRepository;
    private final CourseRepository courseRepository;
    private final CourseGroupRepository groupRepository;

    private final long guildId;

    NoticeCommand(NoticeRepository noticeRepository, CourseRepository courseRepository, CourseGroupRepository groupRepository, long guildId) {
        this.courses = courseRepository.readAll();
        this.noticeRepository = noticeRepository;
        this.courseRepository = courseRepository;
        this.groupRepository = groupRepository;
        this.guildId = guildId;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<String> groupOption = context.options().stream().filter(o -> o.getName().equals(GROUP)).map(OptionMapping::getAsString).findFirst();
        Optional<Integer> quarterOption = context.options().stream().filter(o -> o.getName().equals("quarter")).map(OptionMapping::getAsInt).findFirst();
        Optional<String> actionOption = context.options().stream().filter(o -> o.getName().equals("action")).map(OptionMapping::getAsString).findFirst();
        if(groupOption.isEmpty() || actionOption.isEmpty() || quarterOption.isEmpty()) return;

        String group = groupOption.get();
        String action = actionOption.get();
        int quarter = quarterOption.get();

        if (courses.stream().noneMatch(c -> c.code().equalsIgnoreCase(group)) && groupRepository.readAll().stream().noneMatch(g -> g.groupCode().equalsIgnoreCase(group))) {
            context.replyCallbackAction().setContent(Strings.getString("command.notice.unknown_subject_id")).queue();
            return;
        }

        int groupId = groupRepository.readByGroupCode(group).map(CourseGroup::id).orElse(1);

        List<SelectOption> list = courses.stream().filter(course -> course.courseGroupId() == groupId && course.quarter() == quarter).map(c -> SelectOption.of("%s - %s".formatted(c.code(), c.name()), c.code())).toList();

        String key = context.author().getId() + NOTICE_SELECT_MENU_NAME_SUFFIX;

        StringSelectMenu selectMenu = StringSelectMenu.create(key)
                                              .addOption(group, group)
                                              .addOptions(list)
                                              .setMinValues(1)
                                              .setMaxValues(1)
                                              .build();
        noticeData.put(key, new Object[]{group, action});

        context.replyCallbackAction().setContent(Strings.getString("command.notice.select_message")).addComponents(ActionRow.of(selectMenu)).queue();
    }

    @Override
    public String getName() {
        return NOTICE;
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.notice.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, GROUP, Strings.getString("command.notice.option.group.description"), true)
                        .addChoices(groupRepository.readAll().stream().map(g -> new Choice(g.groupCode(), g.groupCode())).toList()),
                new OptionData(OptionType.INTEGER, "quarter", Strings.getString("command.notice.option.quarter.description"), true)
                        .setMinValue(1)
                        .setMaxValue(6),
                new OptionData(OptionType.STRING, "action", Strings.getString("command.notice.option.action.description"), true)
                        .addChoice(WRITE_ACTION, WRITE_ACTION)
                        .addChoice(READ_ACTION, READ_ACTION)
        );
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.notice.help");
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains(NOTICE_MODAL_NAME_SUFFIX)) return;
        ReplyCallbackAction callbackAction = event.deferReply(true);

        String authorId = event.getModalId().split(NOTICE_MODAL_NAME_SUFFIX)[0];
        String subjectId = (String)noticeData.get(event.getModalId())[0];
        Date timestamp = new Date();
        String type = courses.stream().anyMatch(c -> c.code().equals(subjectId)) ?  "course" : GROUP;
        String content = Objects.requireNonNull(event.getInteraction().getValue(NOTICE)).getAsString();

        storeNotice(authorId, subjectId, timestamp, type, content);
        callbackAction.setContent(Strings.getString("command.notice.success")).queue();
    }

    @Override
    public void onGenericSelectMenuInteraction(@NotNull GenericSelectMenuInteractionEvent event) {
        String name = event.getComponentId();
        if(!name.contains(NOTICE_SELECT_MENU_NAME_SUFFIX)) return;

        String authorId = name.split(NOTICE_SELECT_MENU_NAME_SUFFIX)[0];

        if(!noticeData.containsKey(name)) {
            event.reply("Expired").queue();
            return;
        }

        Object[] data = noticeData.get(name);
        if(!alreadyUsedTimers.containsKey(name)) {
            alreadyUsedTimers.put(
                    name,
                    executor.schedule(() -> {
                        noticeData.remove(name);
                        alreadyUsedTimers.remove(name);
                    }, 5L, TimeUnit.MINUTES)
            );
        }else {
            alreadyUsedTimers.get(name).cancel(true);
            alreadyUsedTimers.put(
                    name,
                    executor.schedule(() -> {
                        noticeData.remove(name);
                        alreadyUsedTimers.remove(name);
                    }, 5L, TimeUnit.MINUTES)
            );
        }
        String groupName = (String)data[0];
        String action = (String)data[1];
        String selectedCourse = (String) event.getInteraction().getValues().getFirst();

        if(action.equals(WRITE_ACTION)) {
            event.replyModal(writeNotice(authorId + NOTICE_MODAL_NAME_SUFFIX, selectedCourse, authorId, groupName)).queue();
        } else if(action.equals(READ_ACTION)) {
            String notices = readNotices(selectedCourse);
            event.reply(notices.isEmpty() ? Strings.getString("command.notice.no_notice_found") : notices).queue();
        }
    }

    private Modal writeNotice(String modalKey, String selectedValue, String authorId, String subjectId) {
        noticeData.put(modalKey, new Object[]{selectedValue});
        TextInput.Builder textBuilder = TextInput.create(NOTICE, TextInputStyle.PARAGRAPH)
                                                .setPlaceholder("Entrez votre avis");
        getOldValue(authorId, selectedValue, courses.stream().anyMatch(c -> c.code().equals(selectedValue))).map(Notice::content).ifPresent(textBuilder::setValue);
        String name = courses.stream().anyMatch(c -> c.code().equals(subjectId)) ?
                              selectedValue + " " + courseRepository.getByCourseCode(selectedValue).map(Course::name).orElse(""):
                              selectedValue;
        return Modal.create(modalKey, String.format("Avis - %s", name))
                       .addComponents(Label.of("Avis", textBuilder.build()))
                       .build();
    }

    private List<Notice> getNotices(String subjectId, String type) {
        return noticeRepository.readBySubjectId(subjectId, type.equals("course"));
    }

    private String toString(List<String> links) {
        if(links.isEmpty()) return  "";
        return " - " + links.getFirst() + links.stream().skip(1).reduce("", (a, b) -> a + "\n - " + b);
    }

    private void storeNotice(String authorId, String subjectId, Date timestamp, String type, String content) {
        getOldValue(authorId, subjectId, type.equals("course"))
                .ifPresentOrElse(
                        old ->
                                noticeRepository.update(old, new Notice(
                                        content,
                                        authorId,
                                        type.equals(GROUP) ? null : courseRepository.getByCourseCode(subjectId).orElse(null),
                                        type.equals(GROUP) ? groupRepository.readByGroupCode(subjectId).orElse(null) : null,
                                        timestamp)),
                        () ->
                                noticeRepository.create(new Notice(
                                        content,
                                        authorId,
                                        type.equals(GROUP) ? null : courseRepository.getByCourseCode(subjectId).orElse(null),
                                        type.equals(GROUP) ? groupRepository.readByGroupCode(subjectId).orElse(null) : null,
                                        timestamp))
                );
    }

    private Optional<Notice> getOldValue(String authorId, String subjectId, boolean isCourse) {
        return noticeRepository.readByAuthorIdAndSubjectId(authorId, subjectId, isCourse);
    }

    void shutdown() {
        executor.shutdown();
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
        return false;
    }

    private String readNotices(String selectedCourse) {
        String type = courses.stream().anyMatch(c -> c.code().equals(selectedCourse)) ? "course" : GROUP;
        List<Notice> notices = getNotices(selectedCourse, type);

        StringBuilder contentBuilder = new StringBuilder();
        for (Notice notice : notices) {
            contentBuilder.append(notice.content()).append("\n\n");
        }

        String content = contentBuilder.toString();

        HttpClient client = HttpClient.newHttpClient();
        List<String> links =  new ArrayList<>();
        List<CompletableFuture<Void>> requests = new ArrayList<>();

        while(content.length() > HASTEBIN_MAX_CONTENT_LENGTH) {
            requests.add(MessageUtil.hastebinPost(client, content.substring(0, HASTEBIN_MAX_CONTENT_LENGTH)).thenAccept(links::add));
            content = content.substring(HASTEBIN_MAX_CONTENT_LENGTH);
        }
        requests.add(MessageUtil.hastebinPost(client, content).thenAccept(links::add));
        requests.forEach(CompletableFuture::join);
        return links.isEmpty() ? Strings.getString("command.notice.no_notice_found") : toString(links);
    }

}
