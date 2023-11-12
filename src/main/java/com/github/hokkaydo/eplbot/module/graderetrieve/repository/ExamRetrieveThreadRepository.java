package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.ExamsRetrieveThread;

import java.util.Optional;

public interface ExamRetrieveThreadRepository extends CRUDRepository<ExamsRetrieveThread> {

    Optional<ExamsRetrieveThread> readByMessageId(Long id);

}
