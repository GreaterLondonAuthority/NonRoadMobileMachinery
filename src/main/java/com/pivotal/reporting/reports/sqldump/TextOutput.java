/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports.sqldump;

import au.com.bytecode.opencsv.CSVWriter;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.io.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * This class creates SQL output in tab/comma separated form
 * Each section is separated by a simple line with the section name as the
 * column value
 */
public class TextOutput extends SQLOutput {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TextOutput.class);

    private CSVWriter outFile;
    private int rowCount;

    /**
     * Creates an output channel for SQL rows
     *
     * @param filename File to create
     * @param compression Compression algorithm to use
     * @param noHeader True if headers are not to be output
     */
    public TextOutput(String filename, String compression, boolean noHeader) {
        this(filename, null, compression, noHeader);
    }

    /**
     * Creates an output channel for SQL rows
     * It can optionally take a delimiter to use but will choose a suitable character
     * if one hasn't been supplied
     *
     * @param out Writer to send output to
     * @param delimiter Character to use as the delimiter
     * @param noHeader True if headers are not to be output
     */
    public TextOutput(OutputStream out, String delimiter, boolean noHeader) {
        this.noHeader=noHeader;

        // Open an output stream and workbook

        try {
            if (!Common.isBlank(delimiter))
                outFile = new CSVWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")), delimiter.trim().charAt(0));
            else
                outFile = new CSVWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));
        }
        catch (Exception e) {
            throw new PivotalException("Cannot open new workbook", e);
        }
    }

    /**
     * Factory method to create an output channel for data rows
     * It can optionally take a delimiter to use but will choose a suitable character
     * if one hasn't been supplied
     *
     * @param out Writer to send output to
     * @param delimiter Character to use as the delimiter
     * @param noHeader True if headers are not to be output
     * @return a {@link TextOutput} object.
     */
    public static TextOutput getTextOutput(OutputStream out, String delimiter, boolean noHeader) {
        return new TextOutput(out, delimiter, noHeader);
    }

    /**
     * Creates an output channel for SQL rows
     * It can optionally take a delimiter to use but will choose a suitable character
     * if one hasn't been supplied
     *
     * @param filename File to create
     * @param delimiter Character to use as the delimiter
     * @param compression Compression algorithm to use
     * @param noHeader True if headers are not to be output
     */
    public TextOutput(String filename, String delimiter, String compression, boolean noHeader) {
        super(filename, compression, noHeader);

        // Open an output stream and workbook

        try {
            Writer out;
            if (Common.doStringsMatch(compression,"gzip") || Common.doStringsMatch(compression, "compress"))
                out=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename)), "UTF-8"));
            else
                out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            if (Common.doStringsMatch(Common.getFilenameExtension(filename),"tsv"))
                outFile = new CSVWriter(out, Common.isBlank(delimiter)?'\t':delimiter.trim().charAt(0));
            else if (!Common.isBlank(delimiter))
                outFile = new CSVWriter(out, delimiter.trim().charAt(0));
            else
                outFile = new CSVWriter(out);
        }
        catch (Exception e) {
            throw new PivotalException("Cannot open new workbook [" + filename + "] ", e);
        }
    }

    /**
     * Creates an output channel for SQL rows
     * It can optionally take a delimiter to use but will choose a suitable character
     * if one hasn't been supplied
     *
     * @param filename File to create
     * @param delimiter Character to use as the delimiter
     * @param quote Character to use as the quotes
     * @param escape Character to use as the escape
     * @param lineEnd Character to use as the line end
     * @param compression Compression algorithm to use
     * @param noHeader True if headers are not to be output
     */
    public TextOutput(String filename, String delimiter, String quote, String escape, String lineEnd, String compression, boolean noHeader) {
        super(filename, compression, noHeader);

        // Check all the parameters - anything that is set to an empty string
        // is translated to the NO_CHAR value

        char delimiterChar=CSVWriter.DEFAULT_SEPARATOR;
        char quoteChar=CSVWriter.DEFAULT_QUOTE_CHARACTER;
        char escapeChar=CSVWriter.DEFAULT_ESCAPE_CHARACTER;
        String lineEnding=CSVWriter.DEFAULT_LINE_END;

        // If we are writing to a Tab sep file and we haven't overridden it

        if (Common.doStringsMatch(Common.getFilenameExtension(filename),"tsv") && delimiter==null)
            delimiterChar='\t';
        else if (delimiter!=null)
            delimiterChar=delimiter.isEmpty() ?'\0':delimiter.charAt(0);

        // Check to see if we have overridden the quote, escape and line ending characters

        if (quote!=null)
            quoteChar= quote.isEmpty() ?'\0':quote.charAt(0);
        if (escape!=null)
            escapeChar= escape.isEmpty() ?'\0':escape.charAt(0);
        if (lineEnd!=null)
            lineEnding=lineEnd.replaceAll("\\\\t","\t").replaceAll("\\\\n","\n").replaceAll("\\\\r","\r");

        try {

            // Get a suitable output stream

            Writer out;
            if (Common.doStringsMatch(compression,"gzip") || Common.doStringsMatch(compression, "compress"))
                out=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filename)), "UTF-8"));
            else
                out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));

            // Export to the file

            outFile = new CSVWriter(out, delimiterChar, quoteChar, escapeChar, lineEnding);
        }
        catch (Exception e) {
            throw new PivotalException("Cannot open new workbook [" + filename + "] ", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Creates a new section for all the next rows
     */
    public void newSection(String name) {

        // Simply output a new row with the name as the only
        // value

        if (rowCount>0 && !noHeader)
            outFile.writeNext(new String[] {""});
        if (!Common.isBlank(name))
            outFile.writeNext(new String[] {name});
        rowCount=0;
    }

    /**
     * Creates a new section for all the next rows
     *
     * @param rowValues Optional list of headers
     */
    public void newSection(Collection<Object> rowValues) {

        // Simply output a new row with the name as the only
        // value

        if (rowCount>0 && !noHeader)
            outFile.writeNext(new String[] {""});
        if (!Common.isBlank(rowValues))
            addRow(rowValues);
        rowCount=0;
    }

    /**
     * {@inheritDoc}
     *
     * Adds a row to the current section
     */
    public void addRow(Map<String, Object> rowValues) {

        // If this is the first row in this worksheet, then we need
        // to output the headers

        if (rowCount==0 && !noHeader) {
            outFile.writeNext(rowValues.keySet().toArray(new String[rowValues.keySet().size()]));
            rowCount++;
        }

        // Output the string representation of the objects

        addRow(rowValues.values());

    }

    /**
     * Adds a row to the current section
     *
     * @param rowValues Array of column values
     */
    public void addRow(Object[] rowValues) {
        addRow(Arrays.asList(rowValues));
    }

    /**
     * Adds a row to the current section
     *
     * @param rowValues List of column values
     */
    @SuppressWarnings("unchecked")
    public void addRow(Collection<Object> rowValues) {

        rowCount++;

        // Output the string representation of the objects

        List<String> outList=new ArrayList<>();
        for (Object value : rowValues) {

            // Check for a null value

            if (value==null)
                outList.add("");

            // Set it's numeric value

            else if (value instanceof Number)
                outList.add(value.toString());

            // Is it a boolean

            else if (value.getClass().equals(Boolean.class))
                outList.add((Boolean)value?"Y":"N");

            // Is it a timestamp

            else if (value.getClass().equals(Timestamp.class))
                outList.add(Common.dateFormat((Date)value,"dd/MM/yyyy HH:mm:ss"));

            // Is it a time

            else if (value.getClass().equals(Time.class))
                outList.add(Common.dateFormat((Date)value,"HH:mm:ss"));

            // Is it a date

            else if (value instanceof Date)
                outList.add(Common.dateFormat((Date)value,"dd/MM/yyyy"));

            // Is it a calendar

            else if (value.getClass().equals(Calendar.class))
                outList.add(Common.dateFormat(((Calendar)value).getTime(),"dd/MM/yyyy HH:mm:ss"));

            // Is it an array of bytes, then convert it to a UTF-8 string by default
            // This is the best we can do given that byte arrays can't be represented any
            // other reasonable way

            else if (value.getClass().isArray()) {
                Class dataType=value.getClass().getComponentType();
                if (dataType.equals(byte.class))
                    try {
                        outList.add(new String((byte[])value, "UTF-8"));
                    }
                    catch (Exception e) {
                        logger.error("Cannot convert byte array [{}]", dataType.getName());
                    }
                else
                    logger.error("Unknown array data type [{}]", dataType.getName());
            }

            // If it's a collection, then send out each value

            else if (value instanceof Collection) {
                rowCount--;
                //noinspection unchecked
                addRow((Collection<Object>)value);
            }

            // Must be a string

            else
                outList.add(value.toString());
        }

        // Output the list to the stream

        outFile.writeNext(outList.toArray(new String[outList.size()]));

    }

    /**
     * Closes the output and returns
     */
    public void close() {

        // Save the data to the file

        try {
            outFile.close();
        }
        catch (Exception e) {
            throw new PivotalException("Cannot save the data to file [" + filename + "] ", e);
        }
    }

    /**
     * Flushes the underlying stream
     */
    public void flush() {

        // Save the data to the file

        try {
            outFile.flush();
        }
        catch (Exception e) {
            throw new PivotalException("Cannot flush the data to file [" + filename + "] ", e);
        }
    }
}
