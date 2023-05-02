package com.github.hokkaydo.eplbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    private static JDA jda;

    public static void main(String[] args) throws InterruptedException {
            final String token = System.getenv("DISCORD_BOT_TOKEN");
            jda = JDABuilder.createDefault(token)
                          .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                          .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                          .setBulkDeleteSplittingEnabled(false)
                          .setActivity(Activity.watching("you"))
                          .build();
            jda.awaitReady();
    }

    public static JDA getJDA() {
        return jda;
    }
}
