CREATE TABLE warned_confessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    moderator_id INTEGER,
    author_id INTEGER,
    message_id INTEGER,
    message_content TEXT,
    timestamp TEXT
)