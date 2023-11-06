package com.github.hokkaydo.eplbot.module.graderetrieve.model;

import java.util.List;

/**
 * Record representing a group of courses
 * @param id          group id
 * @param englishName English group frenchName (common, map, info, gbio, elec, meca, fyki, gc)
 * @param frenchName      French group frenchName (Tronc commun, Filière en Mathématiques Appliquées, ...)
 * @param courses  Array of six {@link Course} representing each year's group's courses
 * */
public record CourseGroup(int id, String englishName, String frenchName, List<List<Course>> courses) {

}