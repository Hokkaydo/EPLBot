package com.github.hokkaydo.eplbot.module.code.command;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.code.GlobalRunner;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.net.http.HttpClient;


public class PerformResponse {

    private boolean validateMessageLength(String content){
        return content.length() < 1960;
    }
    private boolean validateHastebinLength(String content){
        return content.length() < MessageUtil.HASTEBIN_MAX_CONTENT_LENGTH;
    }



    /**
     * @param textChannel the channel of the interaction
     * @param code the code that has been submitted
     * @param lang the language submitted
     */
    public void sendSubmittedCode(MessageChannel textChannel, String code, String lang, boolean spoiler) {
        if (GlobalRunner.containsUnsafeMentions(code)) {
            textChannel.sendMessage(spoilMessage(Strings.getString("command.code.unsafe_mentions_submitted") + "\n", spoiler)).queue();
            return;
        }
        if (validateMessageLength(code)) {
            textChannel.sendMessage(spoilMessage("```%s%n%s%n```".formatted(lang.toLowerCase(), code),spoiler)).queue();
            return;
        }
        if (validateHastebinLength(code)) {
            String url = createUrlFromString(code);
            textChannel.sendMessage(spoilMessage("`The submitted code is available at : `%n<%s>".formatted(url), spoiler)).queue();
            return;
        }
        textChannel.sendMessage(spoilMessage("`The submitted code is too large: %n %s".formatted(Strings.getString("command.code.exceeded_hastebin_size")), spoiler)).queue();
    }

    /**
     * @param textChannel the channel of the interaction
     * @param input a string with the data to be written in the file
     * @return a File
     */
    private String createUrlFromString(String input){
        HttpClient client = HttpClient.newHttpClient();
        return MessageUtil.hastebinPost(client, input).join();
    }

    /**
     * @param text the string to be formatted to spoiler
     * @param isSpoiler a boolean describing the need for format
     * @return the text + || twice
     */
    private String spoilMessage(String text, boolean isSpoiler) {
        if (isSpoiler) {
            return "||%s||".formatted(text);
        } else {
            return text;
        }
    }

    /**
     * @param textChannel the channel of the interaction
     * @param result the string with the output of the code
     * @param exitCode an int with the exit code of the submitted code
     */
    @SuppressWarnings("unused") //exitCode is not used but could be later
    public void sendResult(MessageChannel textChannel, String result, int exitCode, boolean spoiler){
        if (validateMessageLength(result)){
            textChannel.sendMessage(spoilMessage("`%s`".formatted(result), spoiler)).queue();
            return;
        }
        if (validateHastebinLength(result)) {
            String url = createUrlFromString(result);
            textChannel.sendMessage(spoilMessage("`The result of the code is available at : `%n<%s>".formatted(url), spoiler)).queue();
            return;
        }
        textChannel.sendMessage(spoilMessage("`The result is too large: %n%s".formatted(Strings.getString(".code.exceeded_hastebin_size")), spoiler)).queue();
    }
}
