package com.github.hokkaydo.eplbot.module.shop;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.shop.model.Item;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ShopCommand implements Command {


        private final ShopProcessor processor;


        public ShopCommand(ShopProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
            if (context.interaction().getGuild() == null) return;
            List<Item> shop = this.processor.getShop();
            StringBuilder builder = new StringBuilder();
            for (Item item : shop) {
                builder.append(item.name()).append(" - ").append(item.cost()).append(" points - ").append(item.description()).append("\n");

            }
            context.replyCallbackAction().setContent(builder.toString()).queue();


        }

        @Override
        public String getName() {
            return "shop";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "SHOP_COMMAND_DESCRIPTION");
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
        return () -> Strings.getString("SHOP_COMMAND_HELP");
    }




}
