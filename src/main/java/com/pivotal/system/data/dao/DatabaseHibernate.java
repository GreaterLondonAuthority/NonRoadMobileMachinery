/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.PivotalException;
import org.hibernate.engine.SessionFactoryImplementor;

import java.sql.SQLException;

/**
 * Extends the standard Database class to cater for opening
 * a connection given to us from hibernate
 * This is 'sneaking around under the bonnet' of hibernate and getting at the
 * connection pool directly. There's no harm in this other than the fact that
 * we are bypassing the hibernate persistence layer so changes made using this
 * connection will not be seen by the rest of the application for a while (if ever)
 */
public class DatabaseHibernate extends Database {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseHibernate.class);

    /**
     * Create a class for a new connection that has the same details as the
     * current Hibernate session
     */
    public DatabaseHibernate() {

        try {
            SessionFactoryImplementor factory = (SessionFactoryImplementor)HibernateUtils.getSessionFactory();
            dbConnection = factory.getConnectionProvider().getConnection();
            try {
                name = HibernateUtils.getDataSource().getName();
            }
            catch (Exception e) {
                logger.error("Never going to happen");
            }
        }
        catch (Exception e) {
            throw new PivotalException("Hibernate native connection not available");
        }
    }

    @Override
    public void open() throws Exception {
    }

    @Override
    public void close() {
        SessionFactoryImplementor factory = (SessionFactoryImplementor)HibernateUtils.getSessionFactory();
        if (factory!=null) {
            try {
                factory.getConnectionProvider().closeConnection(dbConnection);
            }
            catch (SQLException e) {
                logger.error("Cannot close connection - {}", PivotalException.getErrorMessage(e));
            }
        }
    }
}
