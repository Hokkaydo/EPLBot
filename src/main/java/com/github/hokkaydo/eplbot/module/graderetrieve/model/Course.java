package com.github.hokkaydo.eplbot.module.graderetrieve.model;


/**
 * Record representing a course
 * @param code the course code (e.g. LINMA1315)
 * @param name the French frenchName of the course (e.g. Complément d'analyse)
 * */
public record Course(String code, String name) {

}
