package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class ResetAllCommand implements Command {


        private final PointsProcessor processor;

        public ResetAllCommand(PointsProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
           if(context.interaction().getGuild() == null) return;
              String confirmation = context.options().getFirst().getAsString().toLowerCase(Locale.ROOT);
              if (!confirmation.equals("oui")) {
                  context.replyCallbackAction().setContent(Strings.getString("RESET_ALL_POINTS_COMMAND_CANCELLED")).queue();
                  return;}
                this.processor.resetAllPoints();
                context.replyCallbackAction().setContent(Strings.getString("RESET_ALL_POINTS_COMMAND_SUCCESSFUL")).queue();
        }

        @Override
        public String getName() {
            return "resetallpoints";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "RESET_ALL_POINTS_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {

            return List.of(new OptionData(OptionType.STRING, "confirmation", Strings.getString("RESET_ALL_POINTS_COMMAND_OPTION_CONFIRM_DESCRIPTION"), true));
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
            return true;
        }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("RESET_ALL_POINTS_COMMAND_HELP");
    }




}
