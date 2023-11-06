package com.github.hokkaydo.eplbot;

import java.util.List;

public interface CRUDRepository<M> {

    void create(M model);
    List<M> readAll();
    default void update(M oldModel, M newModel) {}
    default void delete(M model) {}

}
