/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.utils;

import com.pivotal.utils.PivotalException;
import com.pivotal.web.Constants;
import org.apache.tomcat.jdbc.pool.DataSource;

/**
 * Extends a Tomcat DBCP data source so that we can capture configuration details
 * for use offline
 */
public class AppDataSource extends DataSource {

    private static final String JDBC_MYSQL = "jdbc:mysql";
    private static final String JDBC_MARIADB = "jdbc:mariadb";
    private static final String JDBC_H2 = "jdbc:h2";
    private static final String JDBC_POSTGRESQL = "jdbc:postgresql";

    private String password;
    private String serverAddress;
    private String database;
    private String description;
    private String dialect;
    private String schema = "";
    private String defaultUsername;
    private String defaultPassword;

    /**
     * Extension of the Tomcat DBCP implementation to allow us to capture and
     * extend the pooling information
     */
    public AppDataSource() {
        super();

        // Turn of the abandonment if we are in debug mode

        if (Constants.inIde()) {
            setRemoveAbandoned(false);
        }
    }

    @Override
    public void setPassword(String password) {
        super.setPassword(password);
        this.password = password;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setUrl(String url) {
        super.setUrl(url);
        serverAddress = url;

        // Check if the URL is actually OK

        if (serverAddress==null) {
            throw new PivotalException("The datasource URL is not defined correctly - check the definition of the datasource in the context.xml");
        }

        // The database name is provided in URL

        if (isMySQL()) {
            serverAddress=getUrl().replaceFirst("([^/:])/.*", "$1");
            database=getUrl().substring(serverAddress.length() + 1).split("[?&;]",2)[0];
            setName(database);
            dialect = "org.hibernate.dialect.MySQLDialect";
        }

        else if (isPostgreSQL()) {

            serverAddress=getUrl().replaceFirst("([^/:])/.*", "$1");
            database=getUrl().substring(serverAddress.length() + 1).split("[?&;]",2)[0];
            setName(database);
            dialect = "org.hibernatespatial.postgis.PostgisDialect";

        }

        // Database name is the file name

        else if (isH2()) {
            database=getUrl().split(".+:",2)[1].split("[?&;]",2)[0];
            setName(database.replaceFirst("^.+[/\\\\]", ""));
            dialect = "org.hibernate.dialect.H2Dialect";
        }
    }

    /**
     * Returns true if the data source is MySQL/mariadb
     *
     * @return if MySQL
     */
    public boolean isMySQL() {
        return serverAddress.contains(JDBC_MYSQL) || serverAddress.contains(JDBC_MARIADB);
    }

    /**
     * Returns true if the data source is H2
     *
     * @return if H2
     */
    public boolean isH2() {
        return serverAddress.contains(JDBC_H2);
    }

    /**
     * Returns true if the data source is PostgreSQL
     *
     * @return if HSQL
     */
    public boolean isPostgreSQL() {
        return serverAddress.contains(JDBC_POSTGRESQL);
    }

    /**
     * Returns the server part of the URL
     * @return Server address
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Returns the default username
     * Used to create the initial database as a connection to
     * postgres is made to a database so we need default
     * credentials
     *
     * @return Default username string
     */
    public String getDefaultUsername() {
        return defaultUsername;
    }

    public void setDefaultUsername(String defaultUsername) {
        this.defaultUsername = defaultUsername;
    }


    /**
     * Returns the default password
     * Used to create the initial database as a connection to
     * postgres is made to a database so we need default
     * credentials
     *
     * @return Default username string
     */
    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    /**
     * Gets the database name
     * @return Database part of the URL
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Allows the bean to be properly initialised
     * Description of this source
     * @return Description of the source
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the source
     * @param description Description of the source
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDialect() {
        return dialect;
    }

    /**
     * Returns the default schema for the application
     * Currently only set if postgreSQL database
     * @return String containing schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the default schema for the application
     * Currently only set if postgreSQL database
     * @param schema name of default schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }
}
