package com.github.hokkaydo.eplbot.module.bookmark.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.bookmark.model.BookMark;

import java.util.List;

public interface BookMarkRepository extends CRUDRepository<BookMark> {

    /**
     * Get all bookmarks of given user
     * @param userId id of user to retrieve bookmarks for
     * @return a {@link List<BookMark>} containing all user's bookmarks, may be empty
     * */
    List<BookMark> getByUserId(Long userId);

    /**
     * Delete all bookmarks linked to a particular message
     * @param messageId id of the message to delete bookmarks for
     * */
    void deleteByMessageId(Long messageId);

    /**
     * Delete a message's bookmark for given user
     * @param userId id of the user to delete the bookmark for
     * @param messageId id of the message to delete the bookmark for
     * */
    void deleteByUserAndMessageId(Long userId, Long messageId);

}
