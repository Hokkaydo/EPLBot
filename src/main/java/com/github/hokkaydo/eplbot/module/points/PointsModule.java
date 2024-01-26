package com.github.hokkaydo.eplbot.module.points;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
public class PointsModule extends Module{

        private final PointsProcessor processor;
        private final PointsCommand pointsCommand;
        private final AddPointsCommand addPointsCommand;
        private final DailyCommand dailyCommand;
        private final LeaderboardCommand leaderboardCommand;
        private final ResetCommand resetCommand;
        private final ResetAllCommand resetAllCommand;



        public PointsModule(@NotNull Long guildId) {
            super(guildId);

            this.processor = new PointsProcessor(guildId);
            this.pointsCommand = new PointsCommand(this.processor);
            this.addPointsCommand = new AddPointsCommand(this.processor);
            this.dailyCommand = new DailyCommand(this.processor);
            this.leaderboardCommand = new LeaderboardCommand(this.processor);
            this.resetCommand = new ResetCommand(this.processor);
            this.resetAllCommand = new ResetAllCommand(this.processor);


        }

        @Override
        public String getName() {
            return "points";
        }

        @Override
        public List<Command> getCommands() {
            return List.of(
                    pointsCommand,
                    addPointsCommand,
                    dailyCommand,
                    leaderboardCommand,
                    resetCommand,
                    resetAllCommand
            );
        }


        @Override
        public List<ListenerAdapter> getListeners() {
            return List.of(processor);
        }

        public List<Command> getGlobalCommands() {
            return Collections.emptyList();
        }


}
