package com.github.hokkaydo.eplbot.database;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.module.graderetrieve.model.CourseGroup;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            new TableModel("points", Map.of("username", TEXT, "points", INTEGER, "role", TEXT, "daily", INTEGER))
    );


    private static DataSource dataSource;

    private DatabaseManager() {}

    public static void regenerateDatabase(boolean drop) {
        JdbcTemplate template = new JdbcTemplate(DatabaseManager.getDataSource());
        if (drop)
            template.update(TABLES.stream().map(TableModel::name).map("DROP TABLE %s;"::formatted).reduce("", (a, b) -> a+b));

        TABLES.stream().map(model ->
            "CREATE TABLE IF NOT EXISTS %s ( id INTEGER PRIMARY KEY AUTOINCREMENT %s ); %s".formatted(
                    model.name(), 
                    model.parameters().entrySet().stream().map(e -> STR."\{e.getKey()} \{e.getValue()}").reduce("", "%s,%s"::formatted),
                    drop ? "delete from sqlite_sequence where name='%s';".formatted(model.name()) : ""
            )
        ).forEach(template::execute);
        if(drop) {
            CourseGroupRepository repository = new CourseGroupRepositorySQLite(dataSource, new CourseRepositorySQLite(dataSource));
            repository.create(loadCourses().toArray(new CourseGroup[]{}));
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    private static List<CourseGroup> loadCourses() throws JSONException {
        InputStream stream = Strings.class.getClassLoader().getResourceAsStream("courses.json");
        assert stream != null;
        JSONObject object = new JSONObject(new JSONTokener(stream));
        if(object.isEmpty()) return new ArrayList<>();
        JSONArray names = object.names();
        List<CourseGroup> groups = new ArrayList<>();
        for (int i = 0; i < names.length(); i++) {
            groups.add(CourseGroup.of(names.getString(i), object.getJSONObject(names.getString(i))));
        }
        return groups;
    }


    public static void initialize(String persistenceDirPath) {
        dataSource = SQLiteDatasourceFactory.create(STR."\{persistenceDirPath}/database.sqlite");
    }

}
