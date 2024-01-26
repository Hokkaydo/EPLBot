package com.github.hokkaydo.eplbot.module.shop;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.shop.model.Item;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Supplier;

public class AddItemCommand implements Command {


        private final ShopProcessor processor;


        public AddItemCommand(ShopProcessor processor) {
            this.processor = processor;
        }
        @Override
        public void executeCommand(CommandContext context) {
            if (context.interaction().getGuild() == null) return;

            Item item = new Item(999, context.options().get(0).getAsString(), context.options().get(1).getAsInt(), context.options().get(2).getAsString(), context.options().get(3).getAsInt(), (float) context.options().get(4).getAsDouble());
            if (item.cost() < 0 || item.multiplier() < 0 || !(List.of(-1,0,1).contains(item.type()))) {
                context.replyCallbackAction().setContent(Strings.getString("ADD_ITEM_COMMAND_FAILURE")).queue();
                return;
            }
            this.processor.addItem(item);
            context.replyCallbackAction().setContent(Strings.getString("ADD_ITEM_COMMAND_SUCCESSFUL")).queue();


        }

        @Override
        public String getName() {
            return "additem";
        }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString( "ADD_ITEM_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {

            return List.of(new OptionData(OptionType.STRING, "name", Strings.getString("ADD_ITEM_COMMAND_OPTION_NAME_DESCRIPTION"), true),
                    new OptionData(OptionType.INTEGER, "cost", Strings.getString("ADD_ITEM_COMMAND_OPTION_COST_DESCRIPTION"), true),
                    new OptionData(OptionType.STRING, "description", Strings.getString("ADD_ITEM_COMMAND_OPTION_DESCRIPTION_DESCRIPTION"), true),
                    new OptionData(OptionType.INTEGER, "type", Strings.getString("ADD_ITEM_COMMAND_OPTION_TYPE_DESCRIPTION"), true),
                    new OptionData(OptionType.INTEGER, "multiplier", Strings.getString("ADD_ITEM_COMMAND_OPTION_MULTIPLIER_DESCRIPTION"), true)
            );
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
        return () -> Strings.getString("ADD_ITEM_COMMAND_HELP");
    }




}
