package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends CRUDRepository<Course> {

    /**
     * Returns a list containing, for given quarters, a list of quarter's courses
     * */
    List<List<Course>> getByGroupIdAndQuarters(int id, int... quarters);

    /**
     * Returns a list containing a list for each quarter containing a list of quarter's courses
     * */
    List<List<Course>> readByGroupId(int id);

    Optional<Course> getByCourseCode(String courseCode);

}
