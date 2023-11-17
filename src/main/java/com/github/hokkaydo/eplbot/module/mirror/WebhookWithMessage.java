package com.github.hokkaydo.eplbot.module.mirror;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.ReceivedMessage;
import net.dv8tion.jda.internal.entities.WebhookImpl;
import net.dv8tion.jda.internal.requests.restaction.WebhookMessageCreateActionImpl;
import net.dv8tion.jda.internal.requests.restaction.WebhookMessageEditActionImpl;
import net.dv8tion.jda.internal.utils.Checks;

import java.util.function.Function;

/**
 * Wrapper around {@link WebhookImpl} which doesn't handle messages as responses to webhook HTTP calls yet
 * */
public class WebhookWithMessage {

    private final WebhookImpl webhook;
    public WebhookWithMessage(WebhookImpl webhook) {
        this.webhook = webhook;
    }

    public WebhookMessageCreateAction<Message> sendRequest() {
        checkToken();
        Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK.compile(webhook.getId(), webhook.getToken());
        Function<DataObject, Message> transform = json -> createMessage((JDAImpl) webhook.getJDA(), json);
        route = route.withQueryParams("wait", "true");
        WebhookMessageCreateActionImpl<Message> action = new WebhookMessageCreateActionImpl<>(webhook.getJDA(), route, transform);
        action.run();
        return action;
    }

    public WebhookMessageEditActionImpl<Message> editRequest(String messageId) {
        checkToken();
        Checks.isSnowflake(messageId);
        Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK_EDIT.compile(webhook.getId(), webhook.getToken(), messageId);
        route = route.withQueryParams("wait", "true");
        Function<DataObject, Message> transform = json -> createMessage((JDAImpl) webhook.getJDA(), json);

        WebhookMessageEditActionImpl<Message> action = new WebhookMessageEditActionImpl<>(webhook.getJDA(), route, transform);
        action.run();
        return action;
    }

    public WebhookMessageCreateAction<Message> sendMessage(String content) {
        return sendRequest().setContent(content);
    }

    private ReceivedMessage createMessage(JDAImpl jda, DataObject json) {
        return jda.getEntityBuilder().createMessageWithChannel(json, webhook.getChannel().asGuildMessageChannel(), false);
    }

    private void checkToken() {
        if (webhook.getToken() == null)
            throw new UnsupportedOperationException("Cannot execute webhook without a token!");
    }

    public long getIdLong() {
        return webhook.getIdLong();
    }

}
