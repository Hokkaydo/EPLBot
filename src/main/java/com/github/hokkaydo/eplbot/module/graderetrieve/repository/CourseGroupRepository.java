package com.github.hokkaydo.eplbot.module.graderetrieve.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;

import java.util.List;
import java.util.Optional;

public interface CourseGroupRepository extends CRUDRepository<CourseGroup> {

    List<CourseGroup> readByQuarters(int... quarters);

    Optional<CourseGroup> readByGroupCode(String groupCode);

}
