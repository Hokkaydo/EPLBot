package com.github.hokkaydo.eplbot.database;

import java.util.List;

public interface CRUDRepository<M> {

    @SuppressWarnings("unchecked")
    void create(M... models);
    List<M> readAll();
    default void update(M oldModel, M newModel) {}
    default void delete(M model) {}

}
