package com.github.hokkaydo.eplbot.module.helper.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.helper.model.CourseHelper;

import java.util.List;

public interface CourseHelperRepository extends CRUDRepository<CourseHelper> {

    List<CourseHelper> readByChannelId(Long channelId);

    List<CourseHelper> readByUserId(Long userId);

    void deleteByChannelId(Long channelId);

    void updatePing(Long channelId, Long userId, boolean allowPing);

    void updateTutor(Long channelId, Long userId, boolean tutor);

}
