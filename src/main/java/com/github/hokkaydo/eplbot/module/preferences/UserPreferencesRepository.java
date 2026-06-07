package com.github.hokkaydo.eplbot.module.preferences;

import java.util.Optional;

public interface UserPreferencesRepository {
    Optional<String> getPreference(long userId, long guildId, String key);
    void setPreference(long userId, long guildId, String key, String value);
}
