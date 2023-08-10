package com.github.hokkaydo.eplbot.module.notice;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class NoticeCommand extends ListenerAdapter implements Command {

    private final Map<String, List<String[]>> courses;
    private final Map<String, Object[]> noticeData = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                                                                         .appendPattern("yyyy-MM-dd")
                                                                         .appendOptional(DateTimeFormatter.ofPattern(" HH:mm:ss"))
                                                                         .toFormatter();

    private static final String NOTICE_SELECT_MENU_NAME_SUFFIX = "-notice-select-menu";
    private static final String NOTICE_MODAL_NAME_SUFFIX = "-notice-modal";
    private static final String NOTICE = "notice";
    private static final String GROUP = "group";
    private static final String WRITE_ACTION = "write";
    private static final String READ_ACTION = "read";

    NoticeCommand(Map<String, List<String[]>> courses) {
        this.courses = courses;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<String> groupOption = context.options().stream().filter(o -> o.getName().equals(GROUP)).map(OptionMapping::getAsString).findFirst();
        Optional<Integer> quarterOption = context.options().stream().filter(o -> o.getName().equals("quarter")).map(OptionMapping::getAsInt).findFirst();
        Optional<String> actionOption = context.options().stream().filter(o -> o.getName().equals("action")).map(OptionMapping::getAsString).findFirst();
        Optional<String> dateOption = context.options().stream().filter(o -> o.getName().equals("action")).map(OptionMapping::getAsString).findFirst();
        if(groupOption.isEmpty() || quarterOption.isEmpty() || actionOption.isEmpty()) return;


        String group = groupOption.get();
        int quarter = quarterOption.get();
        String action = actionOption.get();
        String date =  "";
        if(action.equals(READ_ACTION) && dateOption.isPresent()) {
            date = dateOption.get();
            try {
                date = dateOption.get();
                DATE_TIME_FORMATTER.parse(date);
            }catch (DateTimeParseException exception) {
                context.replyCallbackAction().setContent(Strings.getString("COMMAND_NOTICE_DATE_PARSING_ERROR").formatted(date)).queue();
            }
        }

        String key = context.author().getId() + NOTICE_SELECT_MENU_NAME_SUFFIX;

        StringSelectMenu selectMenu = StringSelectMenu.create(key)
                                              .addOption(group, group)
                                              .addOptions(Arrays.stream(courses.get(group).get(quarter - 1)).map(s -> SelectOption.of(s, s)).toList())
                                              .setMinValues(1)
                                              .setMaxValues(1)
                                              .build();
        noticeData.put(key, new Object[]{group, quarter, action, date});

        context.replyCallbackAction().setContent(Strings.getString("COMMAND_NOTICE_SELECT_MESSAGE")).addActionRow(selectMenu).queue();
    }

    @Override
    public void onGenericSelectMenuInteraction(@NotNull GenericSelectMenuInteractionEvent event) {
        String name = event.getComponentId();
        if(!name.contains(NOTICE_SELECT_MENU_NAME_SUFFIX)) return;

        String authorId = name.split(NOTICE_SELECT_MENU_NAME_SUFFIX)[0];

        Object[] data = noticeData.remove(name);
        String groupName = (String)data[0];
        int quarter = (int)data[1];
        String action = (String)data[2];
        String date = (String)data[3];
        String selectedCourse = (String) event.getInteraction().getValues().get(0);

        if(action.equals(WRITE_ACTION)) {
            event.replyModal(writeNotice(authorId + NOTICE_MODAL_NAME_SUFFIX, selectedCourse, authorId, groupName, quarter)).queue();
        } else if(action.equals(READ_ACTION)) {
            Date timestamp = new Date((date.isEmpty() ? Instant.MIN : Instant.from(DATE_TIME_FORMATTER.parse((String)data[3]))).toEpochMilli());
            event.reply(readNotices(selectedCourse, timestamp)).queue();
        }
    }

    private Modal writeNotice(String modalKey, String selectedValue, String authorId, String groupName, int quarter) {
        noticeData.put(modalKey, new Object[]{selectedValue});
        String oldValue = getOldValue(authorId, groupName, quarter);
        return Modal.create(modalKey, String.format("Formulaire d'avis - %s %s - %s", groupName, quarter, selectedValue))
                              .addActionRow(TextInput.create(NOTICE, "Avis", TextInputStyle.PARAGRAPH).setPlaceholder("Entre votre avis").setValue(oldValue).build())
                              .build();
    }

    private String readNotices(String selectedCourse, Date timestamp) {
        boolean isCourse = courses.containsKey(selectedCourse);
        long subjectId = getSubjectId(selectedCourse, isCourse);
        List<String> notices = getNotices(subjectId, isCourse, timestamp);
        StringBuilder content = new StringBuilder("```");
        for (String notice : notices) {
            content.append(notice);
        }
        content.append("```");
        return content.toString();
    }

    private List<String> getNotices(long subjectId, boolean isCourse, Date timestamp) {
        //TODO retrieve from notices table
        return Collections.singletonList("%s %s %s".formatted(subjectId, isCourse, timestamp));
    }

    private long getSubjectId(String subject, boolean isCourse) {
        if(isCourse) {
            //TODO retrieve from groups table
            return subject.length();
        }else {
            //TODO retrieve from courses table
            return subject.length() * 2L;
        }
    }

    private String getOldValue(String authorId, String groupName, int quarter) {
        //TODO retrieve from database
        return "%s %s %s".formatted(authorId, groupName, quarter);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains(NOTICE_MODAL_NAME_SUFFIX)) return;
        ReplyCallbackAction callbackAction = event.deferReply(true);

        String authorId = event.getModalId().split(NOTICE_MODAL_NAME_SUFFIX)[0];
        String subjectId = (String)noticeData.get(event.getModalId())[0];
        Date timestamp = new Date();
        String type = courses.containsKey(subjectId) ? GROUP : "course";
        String content = Objects.requireNonNull(event.getInteraction().getValue(NOTICE)).getAsString();

        try {
            storeNotice(authorId, subjectId, timestamp, type, content);
            callbackAction.setContent(Strings.getString("COMMAND_NOTICE_SUCCESSFUL")).queue();
        } catch (IOException e) {
            callbackAction.setContent(Strings.getString("ERROR_OCCURRED")).queue();
        }
    }

    private void storeNotice(String authorId, String subjectId, Date timestamp, String type, String content) throws IOException {
        //TODO store in DB
    }

    @Override
    public String getName() {
        return NOTICE;
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_NOTICE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, GROUP, Strings.getString("COMMAND_NOTICE_OPTION_GROUP_DESCRIPTION"))
                        .addChoices(courses.keySet().stream().map(s -> new Choice(s, s)).toList()),
                new OptionData(OptionType.INTEGER, "quarter", Strings.getString("COMMAND_NOTICE_OPTION_QUARTER_DESCRIPTION"))
                        .setMinValue(1)
                        .setMaxValue(6),
                new OptionData(OptionType.STRING, "action", Strings.getString("COMMAND_NOTICE_OPTION_ACTION_DESCRIPTION"))
                        .addChoice(WRITE_ACTION, WRITE_ACTION)
                        .addChoice(READ_ACTION, READ_ACTION),
                new OptionData(OptionType.STRING, "date", Strings.getString("COMMAND_NOTICE_OPTION_DATE_DESCRIPTION"))
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
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_NOTICE_HELP");
    }

}
