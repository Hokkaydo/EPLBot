package com.github.hokkaydo.eplbot.module.points.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.points.model.Points;
public interface PointsRepository extends CRUDRepository<Points>{
    int get(String user);

}
