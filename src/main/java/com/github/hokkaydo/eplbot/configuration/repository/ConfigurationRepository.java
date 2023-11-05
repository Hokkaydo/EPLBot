package com.github.hokkaydo.eplbot.configuration.repository;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;

import java.util.List;

public interface ConfigurationRepository {

    void updateGuildVariable(Long guildId, String key, String value);
    void updateGuildState(Long guildId, String key, String value);

    List<ConfigurationModel> getGuildStates();

    List<ConfigurationModel> getGuildVariables();

}
