package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.ExamsRetrieveThread;

import java.util.Optional;

public interface ExamRetrieveThreadRepository {

    Optional<ExamsRetrieveThread> readByMessageId(Long id);

    void create(ExamsRetrieveThread model);

}
