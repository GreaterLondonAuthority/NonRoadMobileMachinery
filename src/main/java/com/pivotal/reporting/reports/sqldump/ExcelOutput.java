/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports.sqldump;

import com.googlecode.compress_j2me.lzc.LZCOutputStream;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Provides a means to create and populate an excel file
 */
public class ExcelOutput extends SQLOutput {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExcelOutput.class);

    private Workbook workbook;
    private Sheet currentWorksheet;
    private int rowCount;
    private CellStyle cellStyleBold;
    private CellStyle cellStyleNormal;
    private CellStyle cellStyleNormalNumber;
    private CellStyle cellStyleNormalReal;
    private CellStyle cellStyleNormalDate;
    private CellStyle cellStyleNormalTimestamp;
    private CellStyle cellStyleNormalTime;
    private int numberOfColumns;

    /**
     * Create an Excel channel
     * If the filename ends with XLS then a 97-2007 style workbook
     * is created otherwise a 2010 XSLX workbook is created
     *
     * @param filename File to write to
     * @param compression Compression algorithm to use
     * @param noHeader True if headers are not to be output
     */
    public ExcelOutput(String filename, String compression, boolean noHeader) {
        super(filename, compression, noHeader);

        // Open an output stream and workbook

        try {
            if (Common.doStringsMatch(Common.getFilenameExtension(filename), "xls"))
                workbook=new HSSFWorkbook();
            else
                workbook=new XSSFWorkbook();

            // Create some shared objects

            CreationHelper createHelper = workbook.getCreationHelper();

            Font font=workbook.createFont();
            font.setFontName("arial");
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
            cellStyleBold=workbook.createCellStyle();
            cellStyleBold.setFont(font);
            cellStyleBold.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            font=workbook.createFont();
            font.setFontName("arial");
            font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
            cellStyleNormal=workbook.createCellStyle();
            cellStyleNormal.setFont(font);
            cellStyleNormal.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            cellStyleNormalNumber=workbook.createCellStyle();
            cellStyleNormalNumber.setFont(font);
            cellStyleNormalNumber.setDataFormat(createHelper.createDataFormat().getFormat("0"));
            cellStyleNormalNumber.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            cellStyleNormalReal=workbook.createCellStyle();
            cellStyleNormalReal.setFont(font);
            cellStyleNormalReal.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
            cellStyleNormalReal.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            cellStyleNormalDate=workbook.createCellStyle();
            cellStyleNormalDate.setFont(font);
            cellStyleNormalDate.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy"));
            cellStyleNormalDate.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            cellStyleNormalTimestamp=workbook.createCellStyle();
            cellStyleNormalTimestamp.setFont(font);
            cellStyleNormalTimestamp.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            cellStyleNormalTimestamp.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

            cellStyleNormalTime=workbook.createCellStyle();
            cellStyleNormalTime.setFont(font);
            cellStyleNormalTime.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm:ss"));
            cellStyleNormalTime.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        }
        catch (Exception e) {
            throw new PivotalException("Cannot open new workbook [" + filename + "] ", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new worksheet for all the next rows
     */
    public void newSection(String name) {

        // Auto size the columns of the current worksheet

        if (currentWorksheet!=null)
            setAutoColumnWidths();

        // Create a new sheet

        if (Common.isBlank(name))
            currentWorksheet=workbook.createSheet();
        else
            currentWorksheet=workbook.createSheet(name);

        // Reset the row count

        rowCount=0;
    }

    /**
     * Created a list of map matching each excel cell with a value from a list object
     * the number of cells per row is calculated fromt the size of cell header
     *
     * @param values      a list of values to be added to excel sheet
     * @param cellHeaders an array of headers for the excel document
     *
     * @return a list of map matching each excel cell with a value from a list object
     */
    public List<Map<String, Object>> getCells(List<Object> values, String[] cellHeaders) {

        List<Map<String, Object>> cellDetailList = new ArrayList<>();
        Map<String, Object> cellDetails = new LinkedHashMap<>();

        int index = 0;

        for (int i = 0; i < values.size(); i++) {

            cellDetails.put(cellHeaders[index], values.get(i));

            if ((i + 1) % cellHeaders.length == 0) {

                cellDetailList.add(cellDetails);
                cellDetails = new LinkedHashMap<>();

                index = 0;
            }
            else {
                index += 1;
            }

        }

        return cellDetailList;

    }

    /**
     * Goes through a list of map and adds each entry as a row in excel file
     * @param cellList a list of cell map
     */
    public void addRows(List<Map<String, Object>> cellList){

        for(Map<String, Object> cell : cellList){

            addRow(cell);
        }

    }

    /**
     * {@inheritDoc}
     *
     * Adds a row to the current worksheet
     */
    public void addRow(Map<String,Object> rowValues) {

        // If we don't have a worksheet then create one

        if (currentWorksheet==null) newSection();

        // If this is the first row in this worksheet, then we need
        // to output the headers

        if (rowCount==0) {
            numberOfColumns=rowValues.size();
            if (!noHeader) addHeaderRow(rowValues);
        }

        // Add the row to the worksheet

        Z_addRow(rowValues);
    }

    /**
     * Closes the spreadsheet
     */
    public void close() {

        // Save the workbook to the file

        OutputStream workbookStream=null;
        try {
            setAutoColumnWidths();
            if (Common.doStringsMatch(compression,"gzip"))
                workbookStream=new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
            else if (Common.doStringsMatch(compression, "compress"))
                workbookStream = new BufferedOutputStream(new LZCOutputStream(new FileOutputStream(filename)));
            else
                workbookStream=new BufferedOutputStream(new FileOutputStream(filename));
            workbook.write(workbookStream);
        }
        catch (Exception e) {
            throw new PivotalException("Cannot save workbook [" + filename + "] ", e);
        }
        finally {

            // Close the stream

            Common.close(workbookStream);
        }
    }

    /**
     * Adds a header row to the worksheet
     *
     * @param rowValues Map of column values
     */
    private void addHeaderRow(Map<String, Object> rowValues) {
        if (!Common.isBlank(rowValues)) {

            // Create a row

            Row row=currentWorksheet.createRow(0);

            // Loop through all the keys, creating columns for each one

            int cellNumber=0;
            for (String key : rowValues.keySet()) {

                // Create the cell

                Cell cell=row.createCell(cellNumber);

                // Set it's value

                addCellValue(cell, key);

                // Bolden the headers

                cell.setCellStyle(cellStyleBold);
                cellNumber++;
            }

            // Freeze the header

            currentWorksheet.createFreezePane(0, 1, 0, 1);
            row.setHeight((short)(row.getHeight() * 1.5));
            rowCount++;
        }
    }

    /**
     * Adds a data row to the worksheet
     *
     * @param rowValues Map of column values
     *
     */
    private void Z_addRow(Map<String, Object> rowValues) {
        if (!Common.isBlank(rowValues)) {

            // Create a row

            Row row=currentWorksheet.createRow(rowCount);

            // Loop through all the keys, creating columns for each one

            int cellNumber=0;
            for (Object value : rowValues.values()) {

                // Create the cell

                Cell cell=row.createCell(cellNumber);

                // Set it's value

                addCellValue(cell, value);
                cellNumber++;
            }
        }
        rowCount++;
    }

    /**
     * Adds the value to the specified cell taking care to match the
     * data type of the object to that of an appropriate excel type
     *
     * @param cell The cell to update
     * @param value Value to add to the cell
     */
    private void addCellValue(Cell cell, Object value) {

        // Check for an empty value

        if (value==null) {
            cell.setCellType(Cell.CELL_TYPE_BLANK);
            cell.setCellValue((String)null);
        }

        // Set it's numeric value

        else if (value instanceof Number) {

            // Is it a whole number

            if (value.getClass().equals(Integer.class) ||
                value.getClass().equals(Long.class) ||
                value.getClass().equals(java.math.BigInteger.class) ||
                value.getClass().equals(java.math.BigDecimal.class) ||
                value.getClass().equals(Short.class)) {
                cell.setCellValue(((Number)value).doubleValue());
                cell.setCellStyle(cellStyleNormalNumber);
            }

            // Must be a real type

            else {
                cell.setCellValue((Double) value);
                cell.setCellStyle(cellStyleNormalReal);
            }

        }

        // Is it a boolean

        else if (value.getClass().equals(Boolean.class)) {
            cell.setCellValue((Boolean) value);
            cell.setCellStyle(cellStyleNormal);
        }

        // Is it a time

        else if (value.getClass().equals(Time.class)) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(cellStyleNormalTime);
        }

        // Is it a timestamp

        else if (value.getClass().equals(Timestamp.class)) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(cellStyleNormalTimestamp);
        }

        // Is it a date

        else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            cell.setCellStyle(cellStyleNormalDate);
        }

        // Is it a calendar

        else if (value.getClass().equals(Calendar.class)) {
            cell.setCellValue((Calendar) value);
            cell.setCellStyle(cellStyleNormalDate);
        }

        // Is it an array of bytes, then convert it to a UTF-8 string by default
        // This is the best we can do given that byte arrays can't be represented any
        // other reasonable way

        else if (value.getClass().isArray()) {
            Class dataType=value.getClass().getComponentType();
            if (dataType.equals(byte.class))
                try {
                    String tmpValue=new String((byte[])value, "UTF-8");
                    cell.setCellValue(tmpValue.length()>5000?new String(tmpValue.substring(1,5000)) + "...(truncated)":tmpValue);
                    cell.setCellStyle(cellStyleNormal);
                }
                catch (Exception e) {
                    logger.error("Cannot convert byte array [{}]", dataType.getName());
                }
            else
                logger.error("Unknown array data type [{}]", dataType.getName());
        }

        // Must be a string
        // Excel has a limit of 32k for a cell but this would be virtually useless so
        // we limit it to something manageable

        else {
            String tmpValue=value.toString();
            cell.setCellValue(tmpValue.length()>5000?new String(tmpValue.substring(1,5000)) + "...(truncated)":tmpValue);
            cell.setCellStyle(cellStyleNormal);
        }
    }

    /**
     * Causes the columns to be auto-sized to fit their content
     * Caution - this can be very slow for large spreadsheets
     */
    private void setAutoColumnWidths() {

        // Set all the columns to auto size

        for (int i=0; i<numberOfColumns; i++)
            currentWorksheet.autoSizeColumn(i);

    }

}
