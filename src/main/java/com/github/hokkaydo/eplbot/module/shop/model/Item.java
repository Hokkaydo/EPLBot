package com.github.hokkaydo.eplbot.module.shop.model;

public record Item(
    int id, String name, int cost, String description, int type, float multiplier){}
