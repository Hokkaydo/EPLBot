package com.github.hokkaydo.eplbot.database;

import com.github.hokkaydo.eplbot.SQLiteDatasourceFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

public class DatabaseManager {

    private static final List<String> TABLE_TEMPLATES = List.of(
            """
                    CREATE TABLE IF NOT EXISTS configuration
                    (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        key         TEXT,
                        value       TEXT,
                        guild_id    INTEGER,
                        state       INTEGER
                    )
                    """,    /* STATE : 0 = config, 1 = state */
            """
                    CREATE TABLE IF NOT EXISTS course_groups
                    (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_code  TEXT,
                        french_name TEXT
                    )
                    """,
            """
                    CREATE TABLE IF NOT EXISTS courses
                    (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        course_code TEXT,
                        course_name TEXT,
                        quarter     INTEGER,
                        group_id    INTEGER,
                        FOREIGN KEY(group_id) REFERENCES course_groups(id)
                    )
                    """,
            """
                    CREATE TABLE IF NOT EXISTS warned_confessions
                    (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        moderator_id    INTEGER,
                        author_id       INTEGER,
                        message_content TEXT,
                        timestamp       INTEGER
                    )
                    """,
            """
                    CREATE TABLE IF NOT EXISTS exams_thread
                    (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        thread_id INTEGER,
                        path      TEXT
                    )
                    """,
            """
                    CREATE TABLE IF NOT EXISTS mirrors
                    (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        channelAId INTEGER,
                        channelBId INTEGER
                    )
                    """
    );
    private static DataSource dataSource;

    private DatabaseManager() {}

    public static void regenerateDatabase(boolean drop) {
        JdbcTemplate template = new JdbcTemplate(DatabaseManager.getDataSource());
        if (drop)
            template.execute("DROP TABLE IF EXISTS configuration;" +
                                     "DROP TABLE IF EXISTS mirrors;" +
                                     "DROP TABLE IF EXISTS courses;" +
                                     "DROP TABLE IF EXISTS group_courses;" +
                                     "DROP TABLE IF EXISTS exams_thread;" +
                                     "DROP TABLE IF EXISTS warned_confessions;");
        TABLE_TEMPLATES.forEach(template::execute);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void initialize(String persistenceDirPath) {
        dataSource = SQLiteDatasourceFactory.create(persistenceDirPath + "/database.sqlite");
    }


}
