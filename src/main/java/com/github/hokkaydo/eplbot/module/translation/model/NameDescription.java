package com.github.hokkaydo.eplbot.module.translation.model;

public record NameDescription(Long guildId, Long id, String name, String description, String lang, Type type) {

    public enum Type {
        CHANNEL("channel"),
        CATEGORY("category"),
        ROLE("role");

        private final String name;
        Type(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public static Type fromString(String name) {
            for (Type t : Type.values()) {
                if (t.name.equalsIgnoreCase(name)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown name: " + name);
        }
    }
}
