package com.github.hokkaydo.eplbot.module.translation.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.translation.model.NameDescription;

import java.util.List;

public interface NameDescriptionRepository extends CRUDRepository<NameDescription> {
    List<NameDescription> readAllByGuildId(Long guildId);
    List<NameDescription> readAllByGuildIdAndLang(Long guildId, String lang);
    void deleteAllByGuildIdAndLang(Long guildId, String lang);
}
