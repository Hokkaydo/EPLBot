package com.github.hokkaydo.eplbot.configuration.repository;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;
import com.github.hokkaydo.eplbot.database.CRUDRepository;

import java.util.List;

public interface ConfigurationRepository extends CRUDRepository<ConfigurationModel> {

    void updateGuildVariable(Long guildId, String key, String value);
    void updateGuildState(Long guildId, String key, String value);

    List<ConfigurationModel> getGuildStates();

    List<ConfigurationModel> getGuildVariables();

}
