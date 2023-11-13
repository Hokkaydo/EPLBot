package com.github.hokkaydo.eplbot.module.graderetrieve.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Record representing a group of courses
 * @param id          group id
 * @param englishName English group frenchName (common, map, info, gbio, elec, meca, fyki, gc)
 * @param frenchName      French group frenchName (Tronc commun, Filière en Mathématiques Appliquées, ...)
 * @param courses  Array of six {@link Course} representing each year's group's courses
 * */
public record CourseGroup(int id, String englishName, String frenchName, List<List<Course>> courses) {

    public static CourseGroup of(String groupName, JSONObject object) {
        String name = object.getString("name");
        List<List<Course>> courses = new ArrayList<>();
        JSONArray coursesArr = object.getJSONArray("courses");
        for (int i = 0; i < coursesArr.length(); i++) {
            JSONArray quarter = coursesArr.getJSONArray(i);
            List<Course> course = new ArrayList<>();
            for (int j = 0; j < quarter.length(); j++) {
                JSONArray courseArray = quarter.getJSONArray(j);
                course.add(new Course(courseArray.getString(0), courseArray.getString(1), i+1, 0));
            }
            courses.add(course);
        }
        return new CourseGroup(-1, groupName, name, courses);
    }

}