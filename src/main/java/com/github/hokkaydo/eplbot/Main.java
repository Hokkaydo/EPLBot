package com.github.hokkaydo.eplbot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Main {

    private static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        final String token = Dotenv.load().get("DISCORD_BOT_TOKEN");
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
