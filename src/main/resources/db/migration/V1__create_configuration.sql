CREATE TABLE configuration
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT,
    value TEXT,
    guild_id INTEGER,
    state INTEGER   /*  0 = config, 1 = state */
)