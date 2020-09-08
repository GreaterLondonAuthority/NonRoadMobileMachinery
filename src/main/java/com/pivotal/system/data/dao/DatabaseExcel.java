/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.utils.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pivotal.utils.PivotalException;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Extends the standard Database class to cater for creating and
 * opening an Excel file as a database
 * What it actually does is create a temporary database from the
 * contents of the spreadsheet and opens it read only
 * When the database is closed, the database is destroyed
 */
public class DatabaseExcel extends Database {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseExcel.class);

    protected DatasourceEntity dataSrc;
    protected File spreadsheetFile;
    protected File sqliteFile;

    /**
     * Constructs a wrapper for an Excel spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     */
    public DatabaseExcel(File spreadsheetFile) {
        this.spreadsheetFile=spreadsheetFile;
        name = spreadsheetFile.getName();
    }

    /**
     * {@inheritDoc}
     *
     * Opens the connection and throws any exceptions
     */
    @Override
    public void open() throws Exception {
        super.close();

        String sql=null;
        InputStream inp = null;
        Statement stat = null;
        try {
            // We now need to open the spreadsheet and turn it into a database
            // We're going to use good old SQLite - this is because it's blindingly
            // fast (native code), uses manifest typing which saves us a load of
            // hassle when dealing with data types from a spreadsheet and it's
            // all stored in a single file

            logger.debug("Converting Excel file [{}]", spreadsheetFile);
            sqliteFile=new File(Common.getTemporaryFilename("db3"));
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());

            // Speed up the update

            stat = dbConnection.createStatement();
            stat.executeUpdate("PRAGMA synchronous=OFF");

            // Run the whole update in a single transaction

            dbConnection.setAutoCommit(false);

            // Now open the spreadsheet and create the same structure

            inp = new FileInputStream(spreadsheetFile);
            Workbook wb = WorkbookFactory.create(inp);
            for (int i=0; i<wb.getNumberOfSheets(); i++) {

                // Get the sheet and create a table for it

                String tableName="sheet" + (i + 1);
                Sheet sheet = wb.getSheetAt(i);
                logger.debug("Loading sheet [{}] as table [{}]", sheet.getSheetName(), tableName);
                sql="create table " + tableName + " (";

                // We need to find the widest row so that we can figure out
                // what the column names will be

                Set<String> columns=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
                        String ref=cellRef.formatAsString().replaceAll("[0-9]+","").toLowerCase();
                        if (ref.length()==1) ref='0' + ref;
                        columns.add("col" + ref);
                    }
                }

                // Ignore empty sheets

                if (!columns.isEmpty()) {
                    sql+=Common.join(columns).replaceAll("0","") + ");";
                    logger.debug("Executing table creation statment [{}]", sql);
                    stat.executeUpdate(sql);

                    // Ok, now we go round again but this time we can put the data into
                    // the database

                    for (Row row : sheet) {
                        sql="insert into " + tableName;
                        List<String> cells=new ArrayList<>();
                        List<String> values=new ArrayList<>();
                        for (Cell cell : row) {
                            CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
                            String colName="col" + cellRef.formatAsString().replaceAll("[0-9]+","").toLowerCase();
                            switch (cell.getCellType()) {
                                case Cell.CELL_TYPE_STRING:
                                    cells.add(colName);
                                    values.add('\'' + cell.getStringCellValue().replaceAll("'","''") + '\'');
                                    break;

                                case Cell.CELL_TYPE_BOOLEAN:
                                    cells.add(colName);
                                    values.add(cell.getBooleanCellValue()?"1":"0");
                                    break;

                                case Cell.CELL_TYPE_NUMERIC:
                                    cells.add(colName);
                                    if (HSSFDateUtil.isCellDateFormatted(cell))
                                        values.add(Common.dateFormat(HSSFDateUtil.getJavaDate(cell.getNumericCellValue()),"\"yyyy-MM-dd HH:mm:ss\""));

                                    // Cleanup the numeric representation so that whole numbers stand a chance

                                    else {
                                        Double value=cell.getNumericCellValue();
                                        if (Math.floor(value)==value)
                                            values.add(value.longValue() + "");
                                        else
                                            values.add(value + "");
                                    }
                                    break;

                                default:
                                    logger.debug("Ignoring cell [{}] either in error, blank or contains a formula", cellRef.formatAsString());
                            }
                        }

                        // Don't add empty rows

                        if (!Common.isBlank(cells)) {
                            sql+=" (" + Common.join(cells) + ") values (" + Common.join(values) + ");";
                            logger.debug("Inserting data [{}]", sql);
                            stat.executeUpdate(sql);
                        }
                        else
                            logger.debug("Encountered empty row at line {}", row.getRowNum());
                    }
                }
            }

            // Commit the transaction

            dbConnection.commit();
            logger.debug("Conversion of Excel file [{}] complete", spreadsheetFile);
        }
        catch (ClassNotFoundException e) {
            throw new PivotalException("Cannot load driver [" +  "] for datasource [" + "] " + PivotalException.getErrorMessage(e));
        }
        catch (SQLException e) {
            String error="Problem inserting data into temporary database from Excel file [" + spreadsheetFile.getAbsolutePath() + " ] - " + sql + " - " + PivotalException.getErrorMessage(e);
            logger.error(error);
            super.close();
            throw new Exception(error);
        }
        catch (Exception e) {
            String error="Problem reading Excel file [" + spreadsheetFile.getAbsolutePath() + " ] - " + PivotalException.getErrorMessage(e);
            logger.error(error);
            super.close();
            throw new Exception(error);
        }

        // Clean up after ourselves

        finally {
            Common.close(stat, inp);
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

    /**
     * {@inheritDoc}
     *
     * Closes the connection and swallows any errors
     * This method would normally be protected but is set to
     * public for testing purposes
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void close() {
        super.close();

        // Now get rid of the temporary files

        try {
            if (sqliteFile!=null && sqliteFile.exists())
                sqliteFile.delete();
        }
        catch (Exception e) {
            logger.warn("Problem removing temporary sqlite database - {}", PivotalException.getErrorMessage(e));
        }
        sqliteFile=null;
    }
}
