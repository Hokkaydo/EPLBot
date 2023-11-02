CREATE TABLE courses
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    course_code TEXT,
    course_name TEXT,
    quarter     INTEGER,
    group_id    INTEGER,
    FOREIGN KEY(group_id) REFERENCES course_groups(id)
)
