package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;

import java.util.List;

public interface CourseRepository {

    /**
     * Returns a list containing, for given quarters, a list of quarter's courses
     * */
    List<List<Course>> getByGroupIdAndQuarters(int id, int... quarters);

}
