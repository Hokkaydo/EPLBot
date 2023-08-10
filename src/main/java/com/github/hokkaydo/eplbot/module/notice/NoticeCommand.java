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
import java.util.Arrays;
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
    private static final String NOTICE_SELECT_MENU_NAME_SUFFIX = "-notice-select-menu";
    private static final String NOTICE_MODAL_NAME_SUFFIX = "-notice-modal";
    private static final String NOTICE = "notice";
    private static final String GROUP = "group";

    NoticeCommand(Map<String, List<String[]>> courses) {
        this.courses = courses;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<String> groupOption = context.options().stream().filter(o -> o.getName().equals(GROUP)).map(OptionMapping::getAsString).findFirst();
        Optional<Integer> quarterOption = context.options().stream().filter(o -> o.getName().equals("quarter")).map(OptionMapping::getAsInt).findFirst();
        if(groupOption.isEmpty() || quarterOption.isEmpty()) return;

        String group = groupOption.get();
        int quarter = quarterOption.get();

        String key = context.author().getId() + NOTICE_SELECT_MENU_NAME_SUFFIX;

        StringSelectMenu selectMenu = StringSelectMenu.create(key)
                                              .addOption(group, group)
                                              .addOptions(Arrays.stream(courses.get(group).get(quarter - 1)).map(s -> SelectOption.of(s, s)).toList())
                                              .setMinValues(1)
                                              .setMaxValues(1)
                                              .build();
        noticeData.put(key, new Object[]{group, quarter});

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
        String key = authorId + NOTICE_MODAL_NAME_SUFFIX;
        noticeData.put(key, new Object[]{event.getInteraction().getValues().get(0)});

        String oldValue = getOldValue(authorId, groupName, quarter);

        Modal modal = Modal.create(key, String.format("Formulaire d'avis - %s %s", groupName, quarter))
                              .addActionRow(TextInput.create(NOTICE, "Avis", TextInputStyle.PARAGRAPH).setPlaceholder("Entre votre avis").setValue(oldValue).build())
                              .build();
        event.replyModal(modal).queue();
    }

    private String getOldValue(String authorId, String groupName, int quarter) {
        //TODO retrieve from database
        return "";
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
                        .setMaxValue(6)
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
