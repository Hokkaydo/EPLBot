package com.github.hokkaydo.eplbot.module.remindme.model;

public record Reminder(long id, long userId, long guildId, long triggerTime, String subject) {}
