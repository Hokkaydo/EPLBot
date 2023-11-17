package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;

import java.util.List;

public interface WarnedConfessionRepository extends CRUDRepository<WarnedConfession> {
    List<WarnedConfession> readByAuthor(long author);
    void deleteByAuthor(long author);
}
