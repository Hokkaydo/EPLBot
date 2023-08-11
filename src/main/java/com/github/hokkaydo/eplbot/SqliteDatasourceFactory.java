package com.github.hokkaydo.eplbot;


import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

public class SqliteDatasourceFactory {
    public static DataSource create(String path) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:sqlite:"+path);
        return dataSource;
    }
}
