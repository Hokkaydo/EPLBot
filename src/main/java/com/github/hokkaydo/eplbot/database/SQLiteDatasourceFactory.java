package com.github.hokkaydo.eplbot.database;


import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

public class SQLiteDatasourceFactory {

    private SQLiteDatasourceFactory() {}
    public static DataSource create(String path) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:sqlite:"+path);
        return dataSource;
    }
}
