package com.github.hokkaydo.eplbot.module.confession.model;

import java.sql.Timestamp;

public record WarnedConfession(
        long id,
        long moderatorId,
        long authorId,
        long messageId,
        String messageContent,
        Timestamp timestamp) {
}
