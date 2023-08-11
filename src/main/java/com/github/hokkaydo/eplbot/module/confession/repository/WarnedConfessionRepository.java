package com.github.hokkaydo.eplbot.module.confession.repository;

import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;

import java.util.stream.Stream;

public interface WarnedConfessionRepository {
    Stream<WarnedConfession> readByAuthor(long author);
}
