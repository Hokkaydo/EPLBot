package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.CRUDRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;

import java.util.List;

public interface CourseRepository extends CRUDRepository<Course> {

    /**
     * Returns a list containing, for given quarters, a list of quarter's courses
     * */
    List<List<Course>> getByGroupIdAndQuarters(int id, int... quarters);

    /**
     * Returns a list containing a list for each quarter containing a list of quarter's courses
     * */
    List<List<Course>> getByGroupId(int id);

}
