package com.github.hokkaydo.eplbot.module.confession.model;

import java.sql.Timestamp;

public record WarnedConfession(
        long moderatorId,
        long authorId,
        String messageContent,
        Timestamp timestamp) {
}
