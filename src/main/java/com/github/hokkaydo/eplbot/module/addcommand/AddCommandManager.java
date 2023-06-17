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

import net.dv8tion.jda.api.entities.Guild;

public class AddCommandManager {

    private final Path commandsPath = Path.of(Main.PERSISTENCE_DIR_PATH + "/custom_commands");
    private final Map<String,String> commands = new HashMap<>();
    public final Long guildId;

    public AddCommandManager(Long guildId) {
        this.guildId = guildId;
        loadCommands();
    }

    public void loadCommands(){
        try(FileReader stream = new FileReader(commandsPath.toFile())) {
            int c;
            StringBuilder commandName = new StringBuilder();
            StringBuilder commandContent = new StringBuilder();
            boolean isCommandName = true;
            while((c = stream.read()) != -1) {
                if(c == ';') {
                    isCommandName = false;
                    continue;
                }
                if(c == '\n') {
                    commands.put(commandName.toString(), commandContent.toString());
                    commandName = new StringBuilder();
                    commandContent = new StringBuilder();
                    isCommandName = true;
                    continue;
                }
                if(isCommandName) commandName.append((char) c);
                else commandContent.append((char) c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(commands);
    }

    public List<Command> getCommands() {
        List<Command> new_commands = new ArrayList<>();
        commands.forEach((name, content) -> new_commands.add(new CustomCommand(name, content)));
        return new_commands;
    }

    public void addCommand(Guild guild,  String commandName, String commandContent) {
        //sanitize commandName
        if(commandName.contains(";")) 
            commandName = commandName.replace(";", "");

        if(existsCommand(commandName)) return;
        commands.put(commandName, commandContent);
        Main.getCommandManager().addCommands(guild, List.of(new CustomCommand(commandName, commandContent)));
        storeCommands();
    }


    public boolean existsCommand(String name) {
        return commands.containsKey(name);
    }
    
    private void storeCommands() {
        try(FileWriter stream = new FileWriter(commandsPath.toFile())) {
            commands.forEach((name, content) -> {
                try {
                    stream.append(name + ";" + content + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
