package com.github.hokkaydo.eplbot.module.addcommand;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;

import net.dv8tion.jda.api.entities.Member;

public class CustomCommandManager {

    private final Path commandsPath = Path.of(Main.PERSISTENCE_DIR_PATH + "/custom_commands");
    private final Map<String,CommandItem> commands = new HashMap<>();
    public final Long guildId;

    class CommandItem {
        String content;
        String authorId;

        public CommandItem(String content, String authorId) {
            this.content = content;
            this.authorId = authorId;
        }

        @Override
        public String toString() {
            return  '\'' + content + '\'' +
                    " by " + authorId;
        }
    }

    public CustomCommandManager(Long guildId) {
        this.guildId = guildId;
        loadCommands();
    }

    public void loadCommands(){
        try(FileReader stream = new FileReader(commandsPath.toFile())) {
            int c;
            StringBuilder commandName = new StringBuilder();
            StringBuilder commandContent = new StringBuilder();
            StringBuilder authorId = new StringBuilder();
            int index = 0;
            while((c = stream.read()) != -1) {
                if(c == ';') {
                    index++;
                    continue;
                }
                if(c == '\n') {
                    commands.put(commandName.toString(), new CommandItem(commandContent.toString(), authorId.toString()));
                    commandName = new StringBuilder();
                    commandContent = new StringBuilder();
                    authorId = new StringBuilder();
                    index = 0;
                    continue;
                }
                if (index == 0) commandName.append((char) c);
                else if (index == 1) commandContent.append((char) c);
                else if (index == 2) authorId.append((char) c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(commands);
    }

    public List<Command> getCommands() {
        List<Command> new_commands = new ArrayList<>();
        commands.forEach((name, item) -> new_commands.add(new CustomCommand(name, item.content)));
        System.out.println(new_commands);
        return new_commands;
    }

    public void addCommand(Member author,  String commandName, String commandContent) {
        //sanitize commandName
        if(commandName.contains(";")) 
            commandName = commandName.replace(";", "");

        if(existsCommand(commandName)) return;
        commands.put(commandName, new CommandItem(commandContent, author.getId()));
        Main.getCommandManager().addCommands(author.getGuild(), List.of(new CustomCommand(commandName, commandContent)));
        storeCommands();
    }

    public void removeCommand(Member author,  String commandName) {
        //sanitize commandName
        if(commandName.contains(";")) 
            commandName = commandName.replace(";", "");

        if(!existsCommand(commandName)) return;

        CommandItem command = commands.get(commandName);

        if(!command.authorId.equals(author.getId())) return;

        commands.remove(commandName);
        Main.getCommandManager().removeCommands(author.getGuild(), List.of(new CustomCommand(commandName, command.content)));
        storeCommands();
    }


    public boolean existsCommand(String name) {
        return commands.containsKey(name);
    }
    
    private void storeCommands() {
        try(FileWriter stream = new FileWriter(commandsPath.toFile())) {
            commands.forEach((name, item) -> {
                try {
                    stream.append(name + ";" + item.content + ";" + item.authorId + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
