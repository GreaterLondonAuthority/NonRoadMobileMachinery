package com.pivotal.system.data.dao;

import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.utils.Common;
import com.pivotal.utils.DomHelper;
import com.pivotal.utils.PivotalException;

import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>DatabaseXML class.</p>
 */
public class DatabaseXML extends Database {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseXML.class);

    protected DatasourceEntity       dataSrc;
    protected File                   sqliteFile;
    protected File                 xmlFile;
    protected String               recordNodeName;

    /**
     * Constructs a wrapper for a xml File
     *
     * @param xmlFile XML to open and convert
     * @param recordNodeName a {@link java.lang.String} object.
     */
    public DatabaseXML(File xmlFile, String recordNodeName) {
        this.xmlFile = xmlFile;
        this.recordNodeName = recordNodeName;
        name = xmlFile.getName();
    }

    /**
     * {@inheritDoc}
     *
     * Opens the connection and throws any exceptions
     */
    @Override
    public void open() throws Exception {
        super.close();

        String sql;
        Statement stat = null;
        DomHelper domHelp = new DomHelper(xmlFile);
        try {
            logger.debug("Converting XML file [{}]", xmlFile);
            sqliteFile=new File(Common.getTemporaryFilename("db3"));
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());

            // Speed up the update
            // Run the whole update in a single transaction
            dbConnection.setAutoCommit(false);
            stat = dbConnection.createStatement();



            String tableName = recordNodeName;
            logger.debug("Loading XML [{}] as table [{}]", xmlFile.getName(), tableName);
            sql = "create table " + tableName + " (";
            List<Map<String,String>> records = domHelp.getRecordList(recordNodeName);

            List<String> colNames = new ArrayList<>();
            if (!records.isEmpty()){
                colNames.addAll(records.get(0).keySet());

                //Adding size Column
                colNames.add("size");

                sql += Common.join(colNames) + ");";
                logger.debug("Executing table creation statement [{}]", sql);
                stat.executeUpdate(sql);

            }

            for (Map<String, String> currRecord : records) {
                List<String> tempNames = new ArrayList<>();
                List<String> tempValues = new ArrayList<>();

                for (String key : currRecord.keySet()) {
                    String val = currRecord.get(key);
                    tempNames.add(key);
                    tempValues.add(val.replaceAll("'", "''").trim());
                }

                //Adding size Column
                tempNames.add("size");
                tempValues.add("" + tempValues.size());

                String values = Common.join(tempValues, "','");
                // Add the record to the database
                sql = "insert into " + tableName + " (" + Common.join(tempNames) + ") values ( '" + values + "');";
                logger.debug("Inserting data [{}]", sql);
                stat.executeUpdate(sql);

            }

            dbConnection.commit();
            logger.debug("Conversion of XML file [{}] complete", xmlFile.getName());

        } catch (ClassNotFoundException e) {
            throw new PivotalException("Cannot load driver [" + "] for datasource [" + "] " + PivotalException.getErrorMessage(e));
        } catch (Exception e) {
            logger.error("Problem reading CSV file [{}] - {}",xmlFile.getAbsolutePath(), PivotalException.getErrorMessage(e));
            close();
            throw e;
        }
        finally {
            Common.close(stat);
        }
    }


    /**
     * {@inheritDoc}
     *
     * Executes the statement against the database within an implicit
     * transaction if one is not in operation
     */
    public boolean execute(String sql) {
        lastError=null;
        Statement stmt=null;
        try {
            stmt = dbConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.execute(sql);
        }
        catch (SQLException e) {
            lastError="Problem executing statement [" + sql + "] - " + PivotalException.getErrorMessage(e);
            logger.error(lastError);
        }
        finally {
            Common.close(stmt);
        }
        return !isInError();
    }
}
