package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Supplier;

public class AddPointsCommand implements Command {


        private final PointsProcessor processor;

        public AddPointsCommand(PointsProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
            if (context.interaction().getGuild() == null) return;

            String username = context.options().getFirst().getAsString();
            long points = context.options().get(1).getAsLong();

            this.processor.addPoints(username, (int) points);
            String newPoints = String.valueOf(this.processor.getPoints(username));
            context.replyCallbackAction().setContent(STR."\{username} a maintenant \{newPoints} points.").queue();

        }


        @Override
        public String getName() {
            return "addpoints";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "ADD_POINTS_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {

            return List.of(
                    new OptionData(OptionType.STRING, "username", Strings.getString("ADD_POINTS_COMMAND_OPTION_USER_DESCRIPTION"), true),
                    new OptionData(OptionType.INTEGER, "points", Strings.getString("ADD_POINTS_COMMAND_OPTION_POINTS_DESCRIPTION"), true)

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
        return () -> Strings.getString("ADD_POINTS_COMMAND_HELP");
    }




}
