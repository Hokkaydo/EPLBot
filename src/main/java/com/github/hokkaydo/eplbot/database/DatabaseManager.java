package com.github.hokkaydo.eplbot.database;

import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for managing the database.
 * It registers the existing tables and creates them if they do not exist.
 * <br>
 * When a module needs a table, it should be registered here.
 * Each table is represented by a {@link TableModel} object.
 * */
public class DatabaseManager {

    private static final String INTEGER = "INTEGER";
    private static final String TEXT = "TEXT";
    private static final List<TableModel> TABLES = List.of(
            /* STATE : 0 = config, 1 = state */
            new TableModel("configuration", Map.of("key", TEXT, "value", TEXT, "state", INTEGER, "guild_id", INTEGER)),
            new TableModel("course_groups", Map.of("group_code", TEXT, "french_name", TEXT)),
            new TableModel("courses", Map.of("course_code", TEXT, "course_name", TEXT, "quarter", INTEGER, "group_id", INTEGER)),
            new TableModel("warned_confessions", Map.of("moderator_id", INTEGER, "author_id", INTEGER, "message_content", TEXT, "timestamp", INTEGER)),
            new TableModel("exams_thread", Map.of("message_id", INTEGER, "path", TEXT)),
            new TableModel("mirrors", Map.of("first_id", INTEGER, "second_id", INTEGER)),
            new TableModel("notices", Map.of("author_id", TEXT, "subject_id", TEXT, "content", TEXT, "timestamp", "timestamp", "type", TEXT)),
            new TableModel("bookmarks", Map.of("user_id", INTEGER, "message_id", INTEGER, "description", TEXT, "message_link", TEXT)),
            new TableModel("course_helpers", Map.of("channel_id", INTEGER, "user_id", INTEGER, "allows_ping", INTEGER, "is_tutor", INTEGER)),
            new TableModel("events", Map.of("timestamp", INTEGER, "event_type", TEXT, "user_id", INTEGER, "channel_id", INTEGER, "reaction_emoji", TEXT, "guild_id", INTEGER)),
            new TableModel("name_descriptions", Map.of("guild_id", INTEGER, "entity_id", INTEGER, "name", TEXT, "description", TEXT,  "lang", TEXT, "type", TEXT)),
            new TableModel("user_preferences", Map.of("user_id", INTEGER, "guild_id", INTEGER, "preference_key", TEXT, "value", TEXT))
    );


    private static DataSource dataSource;

    private DatabaseManager() {}


    /**
     * Regenerates the database.
     * @param drop if true, the tables are dropped before being recreated
     * */
    public static void regenerateDatabase(boolean drop) {
        JdbcTemplate template = new JdbcTemplate(DatabaseManager.getDataSource());
        if (drop)
            template.update(TABLES.stream().map(TableModel::name).map("DROP TABLE %s;"::formatted).reduce("", (a, b) -> a+b));

        for (TableModel model : TABLES) {
            // Create table if not exists
            template.execute(
                    "CREATE TABLE IF NOT EXISTS %s ( id INTEGER PRIMARY KEY AUTOINCREMENT %s ); %s".formatted(
                            model.name(),
                            model.parameters().entrySet().stream().map(e -> "%s %s".formatted(e.getKey(), e.getValue())).reduce("", "%s,%s"::formatted),
                            drop ? "delete from sqlite_sequence where name='%s';".formatted(model.name()) : ""
                    )
            );

            // Remove columns that are not in the model
            template.queryForList("PRAGMA table_info(%s);".formatted(model.name()))
                    .stream()
                    .map(row -> row.get("name"))
                    .map(Object::toString)
                    .filter(col -> !model.parameters().containsKey(col))
                    .filter(col -> !col.equals("id"))
                    .forEach(col -> template.update("ALTER TABLE %s DROP COLUMN %s;".formatted(model.name(), col)));

            // Add columns that are in the model but not in the table
            List<String> columns = template.queryForList("PRAGMA table_info(%s);".formatted(model.name()))
                                           .stream()
                                           .map(row -> row.get("name"))
                                           .map(Object::toString)
                                           .toList();

            model.parameters()
                    .entrySet()
                    .stream()
                    .filter(e -> !columns.contains(e.getKey()))
                    .forEach(e -> template.update("ALTER TABLE %s ADD COLUMN %s %s;".formatted(model.name(),e.getKey(), e.getValue())));
        }

        // Delete old tables that are still in the database and not in the TABLES list
        template.queryForList("SELECT name FROM sqlite_master WHERE type='table' AND NAME NOT LIKE 'sqlite_%';")
                .stream()
                .map(row -> row.get("name"))
                .map(Object::toString)
                .filter(name -> !TABLES.stream().map(TableModel::name).toList().contains(name))
                .map(Object::toString)
                .forEach(name -> template.update("DROP TABLE %s;".formatted(name)));

        if(drop) {
            CourseGroupRepository repository = new CourseGroupRepositorySQLite(dataSource, new CourseRepositorySQLite(dataSource));
            repository.loadCourses();
        }
    }

    /**
     * @return the data source needed for preparing statements
     * */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Initializes the database manager.
     * @param persistenceDirPath the path to the persistence directory
     * */
    public static void initialize(String persistenceDirPath) {
        dataSource = SQLiteDatasourceFactory.create(persistenceDirPath + "/database.sqlite");
    }

}
