package com.github.hokkaydo.eplbot.module.shop;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ShopModule extends Module{

        private final ShopProcessor processor;
        private final ShopCommand shopCommand;
        private final AddItemCommand addItemCommand;




        public ShopModule(@NotNull Long guildId) {
            super(guildId);

            this.processor = new ShopProcessor(guildId);
            this.shopCommand = new ShopCommand(this.processor);
            this.addItemCommand = new AddItemCommand(this.processor);




        }

        @Override
        public String getName() {
            return "shop";
        }

        @Override
        public List<Command> getCommands() {
            return List.of(
                    shopCommand,
                    addItemCommand
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
