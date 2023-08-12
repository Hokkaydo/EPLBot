package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;

import java.util.List;
import java.util.stream.Stream;

public interface WarnedConfessionRepository {
    void create(WarnedConfession warnedConfession);
    List<WarnedConfession> readByAuthor(long author);
    void deleteByAuthor(long author);
}
