package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.CRUDRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;

import java.util.List;

public interface CourseGroupRepository extends CRUDRepository<CourseGroup> {

    List<CourseGroup> getByQuarters(int... quarters);
}
