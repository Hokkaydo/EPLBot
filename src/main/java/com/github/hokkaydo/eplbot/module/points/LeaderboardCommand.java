package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.points.model.Points;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LeaderboardCommand implements Command {


        private final PointsProcessor processor;

        public LeaderboardCommand(PointsProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
            if (context.interaction().getGuild() == null) return;
            List<Points> leaderboard = this.processor.getLeaderboard();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < leaderboard.size(); i++) {
                Points point = leaderboard.get(i);
                sb.append(i + 1).append(". ").append(point.username()).append(" - ").append(point.points()).append("\n");
            }
            context.replyCallbackAction().setContent(sb.toString()).queue();
        }

        @Override
        public String getName() {
            return "leaderboard";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "LEADERBOARD_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {

            return Collections.emptyList();
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
        return () -> Strings.getString("LEADERBOARD_COMMAND_HELP");
    }




}
