package com.github.hokkaydo.eplbot.module.notice.model;

import com.github.hokkaydo.eplbot.module.graderetrieve.model.Course;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;

import java.util.Date;

/**
 * This record combines notices for courses and groups. This induces that whether course whether courseGroup is null
 * */
public record Notice(String content, String authorId, Course course, CourseGroup courseGroup, Date timestamp) {

}
