package com.github.hokkaydo.eplbot.module.preferences;

public record UserPreferenceModel(long userId, long guildId, String key, String value) {}
