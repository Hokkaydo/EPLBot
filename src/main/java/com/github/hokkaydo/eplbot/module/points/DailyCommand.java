package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DailyCommand implements Command {


        private final PointsProcessor processor;

        public DailyCommand(PointsProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
            if (context.interaction().getGuild() == null) return;
            String username = context.user().getName();
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);


            if (this.processor.hasClaimedDaily(username, day, month)) {
                context.replyCallbackAction().setContent(Strings.getString("DAILY_COMMAND_ALREADY_CLAIMED").formatted(username)).queue();
                return;
            }
            this.processor.daily(username, day, month);
            String newPoints = String.valueOf(this.processor.getPoints(username));
            context.replyCallbackAction().setContent(STR."Vous avez maintenant \{newPoints} points.").queue();
        }

        @Override
        public String getName() {
            return "daily";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "DAILY_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {

            return Collections.emptyList();
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
        return () -> Strings.getString("DAILY_COMMAND_HELP");
    }




}
