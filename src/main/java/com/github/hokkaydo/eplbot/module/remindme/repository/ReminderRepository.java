package com.github.hokkaydo.eplbot.module.remindme.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.remindme.model.Reminder;

import java.util.List;

public interface ReminderRepository extends CRUDRepository<Reminder> {

    long createAndGetId(Reminder reminder);

    List<Reminder> getByGuildId(long guildId);

    void deleteById(long id);

}
