/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collection;

/**
 * This class wraps one of either the Apache Tomcat or Apache Commons basic datasource
 * objects so that they can be used interchangeabily
 */
public class BasicDataSourceInfo {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BasicDataSourceInfo.class);

    private static final String JDBC_MYSQL = "jdbc:mysql";
    private static final String JDBC_H2 = "jdbc:h2";
    private static final String JDBC_HSQL = "jdbc:hsqldb";

    private Object datasource;

    public String serverAddress;
    public String database;
    public String name;

    /**
     * Initialises the wrapper with the datasource type
     *
     * @param datasource Datasource to wrap
     *
     * @throws Exception Error if the datasource cannot be wrapped
     */
    public BasicDataSourceInfo(Object datasource) throws Exception {
        this.datasource = datasource;
        serverAddress = getUrl();

        // Check if the URL is actually OK

        if (serverAddress==null) {
            throw new PivotalException("The datasource URL is not defined correctly - check the definition of the datasource in the context.xml");
        }

        // The database name is provided in URL

        if (isMySQL()) {
            serverAddress=getUrl().replaceFirst("([^/:])/.*", "$1");
            database=getUrl().substring(serverAddress.length() + 1).split("[?&;]",2)[0];
            name=database;
        }

        // Database name is the file name

        else if (isH2()) {
            database=getUrl().split(".+:",2)[1].split("[?&;]",2)[0];
            name=database.replaceFirst("^.+[/\\\\]", "");
        }

    }

    /**
     * Returns true if the data source is MySQL
     *
     * @return if MySQL
     */
    public boolean isMySQL() {
        return serverAddress.contains(JDBC_MYSQL);
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
     * Returns true if the data source is HSQL
     *
     * @return if HSQL
     */
    public boolean isHSQL() {
        return serverAddress.contains(JDBC_HSQL);
    }

    /**
     * Returns the default auto-commit property.
     *
     * @return true if default auto-commit is enabled
     */
    public boolean getDefaultAutoCommit() {
        return (Boolean)invokeGetMethod("getDefaultAutoCommit");
    }

    /**
     * Returns the default readOnly property.
     *
     * @return true if connections are readOnly by default
     */
    public boolean getDefaultReadOnly() {
        return (Boolean)invokeGetMethod("getDefaultReadOnly");
    }

    /**
     * Returns the default transaction isolation state of returned connections.
     *
     * @return the default value for transaction isolation state
     */
    public int getDefaultTransactionIsolation() {
        return (Integer)invokeGetMethod("getDefaultTransactionIsolation");
    }

    /**
     * Returns the default catalog.
     *
     * @return the default catalog
     */
    public String getDefaultCatalog() {
        return (String)invokeGetMethod("getDefaultCatalog");
    }

    /**
     * Returns the jdbc driver class name.
     *
     * @return the jdbc driver class name
     */
    public String getDriverClassName() {
        return (String)invokeGetMethod("getDriverClassName");
    }

    /**
     * Returns the class loader specified for loading the JDBC driver. Returns
     * <code>null</code> if no class loader has been explicitly specified.
     *
     * @return Class loader used to get the driver
     */
    public ClassLoader getDriverClassLoader() {
        return (ClassLoader)invokeGetMethod("getDriverClassLoader");
    }

    /**
     * <p>Returns the maximum number of active connections that can be
     * allocated at the same time.
     * </p>
     * <p>A negative number means that there is no limit.</p>
     *
     * @return the maximum number of active connections
     */
    public int getMaxActive() {
        return (Integer)invokeGetMethod("getMaxActive");
    }

    /**
     * <p>Returns the maximum number of connections that can remain idle in the
     * pool.
     * </p>
     * <p>A negative value indicates that there is no limit</p>
     *
     * @return the maximum number of idle connections
     */
    public int getMaxIdle() {
        return (Integer)invokeGetMethod("getMaxIdle");
    }

    /**
     * Returns the minimum number of idle connections in the pool
     *
     * @return the minimum number of idle connections
     */
    public int getMinIdle() {
        return (Integer)invokeGetMethod("getMinIdle");
    }

    /**
     * Returns the initial size of the connection pool.
     *
     * @return the number of connections created when the pool is initialized
     */
    public int getInitialSize() {
        return (Integer)invokeGetMethod("getInitialSize");
    }

    /**
     * <p>Returns the maximum number of milliseconds that the pool will wait
     * for a connection to be returned before throwing an exception.
     * </p>
     * <p>A value less than or equal to zero means the pool is set to wait
     * indefinitely.</p>
     *
     * @return the maxWait property value
     */
    public long getMaxWait() {
        return (Long)invokeGetMethod("getMaxWait");
    }

    /**
     * Returns true if we are pooling statements.
     *
     * @return true if prepared and callable statements are pooled
     */
    public boolean isPoolPreparedStatements() {
        return (Boolean)invokeGetMethod("isPoolPreparedStatements");
    }

    /**
     * Gets the value of the  property.
     *
     * @return the maximum number of open statements
     */
    public int getMaxOpenPreparedStatements() {
        return (Integer)invokeGetMethod("getMaxOpenPreparedStatements");
    }

    /**
     * Returns the  property.
     *
     * @return true if objects are validated before being borrowed from the
     * pool
     *
     */
    public boolean getTestOnBorrow() {
        return (Boolean)invokeGetMethod("getTestOnBorrow");
    }

    /**
     * Returns the value of the  property.
     *
     * @return true if objects are validated before being returned to the
     * pool
     */
    public boolean getTestOnReturn() {
        return (Boolean)invokeGetMethod("getTestOnReturn");
    }

    /**
     * Returns the value of the
     * property.
     *
     * @return the time (in miliseconds) between evictor runs
     */
    public long getTimeBetweenEvictionRunsMillis() {
        return (Integer)invokeGetMethod("getTimeBetweenEvictionRunsMillis");
    }

    /**
     * Returns the value of the  property.
     *
     * @return the number of objects to examine during idle object evictor
     * runs
     */
    public int getNumTestsPerEvictionRun() {
        return (Integer)invokeGetMethod("getNumTestsPerEvictionRun");
    }

    /**
     * Returns the  property.
     *
     * @return the value of the  property
     */
    public long getMinEvictableIdleTimeMillis() {
        return (Integer)invokeGetMethod("getMinEvictableIdleTimeMillis");
    }

    /**
     * Returns the value of the  property.
     *
     * @return true if objects examined by the idle object evictor are
     * validated
     */
    public boolean getTestWhileIdle() {
        return (Boolean)invokeGetMethod("getTestWhileIdle");
    }

    /**
     * [Read Only] The current number of active connections that have been
     * allocated from this data source.
     *
     * @return the current number of active connections
     */
    public int getNumActive() {
        return (Integer)invokeGetMethod("getNumActive");
    }


    /**
     * [Read Only] The current number of idle connections that are waiting
     * to be allocated from this data source.
     *
     * @return the current number of idle connections
     */
    public int getNumIdle() {
        return (Integer)invokeGetMethod("getNumIdle");
    }

    /**
     * Returns the password passed to the JDBC driver to establish connections.
     *
     * @return the connection password
     */
    public String getPassword() {
        return (String)invokeGetMethod("getPassword");
    }

    /**
     * Returns the JDBC connection  property.
     *
     * @return the  passed to the JDBC driver to establish
     * connections
     */
    public String getUrl() {
        return (String)invokeGetMethod("getUrl");
    }

    /**
     * Returns the JDBC connection  property.
     *
     * @return the  passed to the JDBC driver to establish
     * connections
     */
    public String getUsername() {
        return (String)invokeGetMethod("getUsername");
    }

    /**
     * Returns the validation query used to validate connections before
     * returning them.
     *
     * @return the SQL validation query
     */
    public String getValidationQuery() {
        return (String)invokeGetMethod("getValidationQuery");
    }

    /**
     * Returns the validation query timeout.
     *
     * @return the timeout in seconds before connection validation queries fail.
     * @since 1.3
     */
    public int getValidationQueryTimeout() {
        return (Integer)invokeGetMethod("getValidationQueryTimeout");
    }

    /**
     * Returns the list of SQL statements executed when a physical connection
     * is first created. Returns an empty list if there are no initialization
     * statements configured.
     *
     * @return initialization SQL statements
     * @since 1.3
     */
    public Collection getConnectionInitSqls() {
        return (Collection)invokeGetMethod("getConnectionInitSqls");
    }

    /**
     * Returns the value of the accessToUnderlyingConnectionAllowed property.
     *
     * @return true if access to the underlying connection is allowed, false
     * otherwise.
     */
    public boolean isAccessToUnderlyingConnectionAllowed() {
        return (Boolean)invokeGetMethod("isAccessToUnderlyingConnectionAllowed");
    }

    /**
     * <strong>BasicDataSource does NOT support this method. </strong>
     *
     * <p>Returns the login timeout (in seconds) for connecting to the database.
     * </p>
     * <p>Calls , so has the side effect
     * of initializing the connection pool.</p>
     *
     * @throws SQLException if a database access error occurs
     * @throws UnsupportedOperationException If the DataSource implementation
     *   does not support the login timeout feature.
     * @return login timeout in seconds
     */
    public int getLoginTimeout() throws SQLException {
        return (Integer)invokeGetMethod("getLoginTimeout");
    }


    /**
     * <p>Returns the log writer being used by this data source.</p>
     * <p>
     * Calls , so has the side effect
     * of initializing the connection pool.</p>
     *
     * @throws SQLException if a database access error occurs
     * @return log writer in use
     */
    public PrintWriter getLogWriter() throws SQLException {
        return (PrintWriter)invokeGetMethod("getLogWriter");
    }


    /**
     * Flag to remove abandoned connections if they exceed the
     * removeAbandonedTimout.
     *
     * Set to true or false, default false.
     * If set to true a connection is considered abandoned and eligible
     * for removal if it has been idle longer than the removeAbandonedTimeout.
     * Setting this to true can recover db connections from poorly written
     * applications which fail to close a connection.
     * <p>
     * Abandonded connections are identified and removed when
     *  is invoked and the following conditions hold
     * <ul><li> = true </li>
     *     <li> - 3 </li>
     *     <li> < 2 </li></ul></p>
     *
     *  @return True if the connection is abandoned and can be removed
     */
    public boolean getRemoveAbandoned() {
        return (Boolean)invokeGetMethod("getRemoveAbandoned");
    }

    /**
     * Timeout in seconds before an abandoned connection can be removed.
     *
     * Defaults to 300 seconds.
     * @return abandoned connection timeout
     */
    public int getRemoveAbandonedTimeout() {
        return (Integer)invokeGetMethod("getRemoveAbandonedTimeout");
    }

    /**
     * <p>Flag to log stack traces for application code which abandoned
     * a Statement or Connection.
     * </p>
     * <p>Defaults to false.
     * </p>
     * <p>Logging of abandoned Statements and Connections adds overhead
     * for every Connection open or new Statement because a stack
     * trace has to be generated. </p>
     *
     * @return True if aabonedments are being logged
     */
    public boolean getLogAbandoned() {
        return (Boolean)invokeGetMethod("getLogAbandoned");
    }

    /**
     * If true, this data source is closed and no more connections can be retrieved from this datasource.
     * @return true, if the data source is closed; false otherwise
     */
    public boolean isClosed() {
        return (Boolean)invokeGetMethod("isClosed");
    }

    /**
     * Invokes the specified GET method and return the result
     *
     * @param method Method name to call
     *
     * @return Object value
     */
    private Object invokeGetMethod(String method) {
        Object returnValue=null;
        try {
            returnValue = datasource.getClass().getMethod(method, new Class[]{}).invoke(datasource);
        }
        catch (Exception e) {
            logger.warn("Problem invoking method [" + method + "] via reflection - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

}
