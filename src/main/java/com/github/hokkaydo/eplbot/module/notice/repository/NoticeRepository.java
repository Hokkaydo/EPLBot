package com.github.hokkaydo.eplbot.module.notice.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.notice.model.Notice;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends CRUDRepository<Notice> {


    /**
     * Retrieve a notice based on the author and the subject. As the subject can be a course or a group, a third
     * parameter specifies which type is asked for
     * @param authorId the notice's author's id
     * @param subjectId course code if "isCourse == true" (e.g.: LEPL0000) or group name if "isCourse == false" (e.g.: MAP)
     * @param isCourse true if the asked notice is for a course, false if it is for a group
     * @return an {@link Optional} containing the notice if found, empty otherwise
     * */
    Optional<Notice> readByAuthorIdAndSubjectId(String authorId, String subjectId, boolean isCourse);

    /**
     * Retrieve a notice based on the subject. As the subject can be a course or a group, a second
     * parameter specifies which type is asked for
     * @param subjectId course code if "isCourse == true" (e.g.: LEPL0000) or group name if "isCourse == false" (e.g.: MAP)
     * @param isCourse true if the asked notice is for a course, false if it is for a group
     * @return an {@link List} containing all existing notices for given subject, empty otherwise
     * */
    List<Notice> readBySubjectId(String subjectId, boolean isCourse);


}
