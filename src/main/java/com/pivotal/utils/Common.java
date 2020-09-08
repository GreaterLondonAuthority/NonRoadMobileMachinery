/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import au.com.bytecode.opencsv.CSVReader;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.activation.FileTypeMap;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Some general purpose methods
 */
public class Common extends HttpUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Common.class);
    private static final org.slf4j.Logger Perflogger = org.slf4j.LoggerFactory.getLogger("Performance");

    /**
     * Default encoding for all character I/O
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String ENCODING = "ENCODING";

    private static String msTemporaryDirectory = System.getProperty("java.io.tmpdir");
    private static String msApplicationName = null;
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    static protected SSLContext SecureSocketContext;
    private static ArrayList<TempFile> temporaryFiles;

    // Useful for return empty strings where required
    public static final String emptyString = "";


    static {
        SecureSocketContext = null;
        temporaryFiles = new ArrayList<>();
    }

    /**
     * Takes an array of name value pairs and tuns them into a map
     * If the array is an odd number, then the last value is set to null
     * If a name is empty or null, the pair is ignored
     *
     * @param values Array of name followed by value
     * @return Map of name values
     */
    public static Map<String, String> getMapFromPairs(String... values) {
        Map<String, String> returnValue = null;
        if (!isBlank(values)) {
            returnValue = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i += 2) {
                if (!isBlank(values[i])) {
                    if (i == values.length - 1) returnValue.put(values[i], null);
                    else returnValue.put(values[i], values[i + 1]);
                }
            }
        }
        return returnValue;
    }

    /**
     * Logs messages to the INFO category for the given logger taking care of formatted strings
     *
     * @param message Message to send
     * @param values  Replacement parameter values in the message
     */
    public static void logPerformanceInfo(String message, Object... values) {
        if (Perflogger.isInfoEnabled() && !isBlank(message)) {
            if (isBlank(values)) Perflogger.info(message);
            else Perflogger.info(message, values);
        }
    }

    /**
     * Sets the name of the application
     *
     * @param name Name of the application
     */
    public static void setAplicationName(String name) {
        msApplicationName = name;
    }

    /**
     * Gets the name of the application
     *
     * @return Name of the application
     */
    public static String getAplicationName() {
        return msApplicationName;
    }

    /**
     * Sets the temporary directory to use for the class
     *
     * @param sDir Full spec directory
     */
    public static void setTemporaryDirectory(String sDir) {
        msTemporaryDirectory = sDir;
    }

    /**
     * Returns a full path name of a suitable temporary filename
     *
     * @return String
     */
    public static String getTemporaryDirectory() {
        String sTmpFolder = msTemporaryDirectory;
        if (isBlank(sTmpFolder)) sTmpFolder = System.getProperty("java.io.tmpdir");
        return sTmpFolder;
    }

    /**
     * Returns a full path name of a suitable temporary filename
     *
     * @return File
     */
    public static File getTemporaryFile() {
        return new File(getTemporaryFilename(null));
    }

    /**
     * Returns a full path name of a suitable temporary filename using the
     * extension provided.  If sExtension is null then .tmp is used
     *
     * @param sExtension Extension to give the file
     * @return File
     */
    public static File getTemporaryFile(String sExtension) {
        return new File(getTemporaryFilename(sExtension));
    }

    /**
     * Returns a full path name of a suitable temporary filename
     *
     * @return String
     */
    public static String getTemporaryFilename() {
        return getTemporaryFilename(null);
    }

    /**
     * Returns a full path name of a suitable temporary filename using the
     * extension provided.  If sExtension is null then .tmp is used
     *
     * @param sExtension Extension to give the file
     * @return String
     */
    public static String getTemporaryFilename(String sExtension) {

        String returnValue;
        if (sExtension != null)
            returnValue = getTemporaryDirectory() + File.separator + (msApplicationName == null ? "" : msApplicationName) + Thread.currentThread().getId() + '-' + System.nanoTime() + '.' + trim(sExtension.trim(), ".");
        else
            returnValue = getTemporaryDirectory() + File.separator + (msApplicationName == null ? "" : msApplicationName) + Thread.currentThread().getId() + '-' + System.nanoTime() + ".tmp";

        return returnValue;
    }

    /**
     * Adds a file for background cleanup some time in the future
     *
     * @param file File to delete
     */
    synchronized public static void addFileForDeletion(File file) {
        if (file != null) temporaryFiles.add(new TempFile(file));
    }

    /**
     * Adds a file for background cleanup some time in the future
     *
     * @param file File to delete
     * @param expiry date/time after which file will be deleted
     */
    synchronized public static void addFileForDeletion(File file, Calendar expiry) {
        if (file != null) temporaryFiles.add(new TempFile(file, expiry));
    }

    /**
     * Adds a file for background cleanup some time in the future
     *
     * @param file File to delete
     * @param seconds number of seconds to wait before file can be deleted
     */
    synchronized public static void addFileForDeletion(File file, int seconds) {
        if (file != null) temporaryFiles.add(new TempFile(file, seconds));
    }

    /**
     * Returns the list of temporary files to be deleted
     *
     * @param clear If true, the list is cleared also
     * @return Set of files
     */
    synchronized public static List<File> getFilesForDeletion(boolean clear) {
        List<File> tmp = new ArrayList<>();

        // Get list of expired files
        Iterator<TempFile> tempFileIterator = temporaryFiles.iterator();
        while (tempFileIterator.hasNext()) {
            TempFile tempFile = tempFileIterator.next();
            if (tempFile.hasExpired()) {
                tmp.add(tempFile.getFile());
                if (clear) tempFileIterator.remove();
            }
        }

        return tmp;
    }

    /**
     * Checks the string value for null or "" after being stripped of whitespace
     * A huge speed improvement is gained b checking the first character before
     * applying the replaceAll regexp search
     *
     * @param sValue Value to check
     * @return Boolean
     */
    public static boolean isBlank(String sValue) {
        return sValue == null || sValue.length() == 0 || (" \t\n\r".indexOf(sValue.charAt(0)) > -1 && sValue.replaceAll("(^[ \t\n\r]*)|([ \t\n\r]*$)", "").isEmpty());
    }

    /**
     * Will check the values to ensure that non are blank
     *
     * @param values the values to check are not blank
     * @return True if all the values are not blank
     */
    public static boolean notBlank(Object... values) {
        for (Object value : values) {
            if (isBlank(value) ||
                    (value instanceof String && isBlank((String) value)) ||
                    (value instanceof File && isBlank((File) value)) ||
                    (value instanceof Collection && isBlank((Collection) value)) ||
                    (value instanceof Object[] && isBlank((Object[]) value)) ||
                    (value instanceof char[] && isBlank((char[]) value)) ||
                    (value instanceof byte[] && isBlank((byte[]) value)) ||
                    (value instanceof Map && isBlank((Map) value)) ||
                    (value instanceof NodeList && isBlank((NodeList) value)))

                // Then one value is blank

                return false;
        }
        return true;
    }

    /**
     * Checks the string value for null
     *
     * @param sValue Value to check
     * @return Boolean
     */
    public static boolean isBlank(Object sValue) {
        return sValue == null;
    }

    /**
     * Checks the string value for null or "". If blank - return default value
     *
     * @param sValue       Value to check
     * @param defaultValue Value to return if sValue is blank
     * @return String sValue if it is non-blank else default value
     */
    public static String getNonBlank(String sValue, String defaultValue) {
        return isBlank(sValue) ? defaultValue : sValue;
    }

    /**
     * Checks the file is not null and exists
     *
     * @param file Value to check
     * @return Boolean
     */
    public static boolean isBlank(File file) {
        return file == null || !file.exists();
    }

    /**
     * Checks the collection to see if it is empty
     *
     * @param objValue list of values
     * @return Boolean
     */
    public static boolean isBlank(Collection objValue) {
        return objValue == null || objValue.isEmpty();
    }

    /**
     * Checks the array to see if it is empty
     *
     * @param aobjValue array of values
     * @return Boolean
     */
    public static boolean isBlank(Object[] aobjValue) {
        return aobjValue == null || aobjValue.length == 0;
    }

    /**
     * Checks the array to see if it is empty
     *
     * @param acValue array of characters
     * @return Boolean
     */
    public static boolean isBlank(char[] acValue) {
        return acValue == null || acValue.length == 0;
    }

    /**
     * Checks the array to see if it is empty
     *
     * @param acValue array of bytes
     * @return Boolean
     */
    public static boolean isBlank(byte[] acValue) {
        return acValue == null || acValue.length == 0;
    }

    /**
     * Checks the map to see if it is empty
     *
     * @param objValue list of values
     * @return Boolean
     */
    public static boolean isBlank(Map objValue) {
        return objValue == null || objValue.isEmpty();
    }

    /**
     * Checks the NodeList to see if it is empty
     *
     * @param objValue list of values
     * @return Boolean
     */
    public static boolean isBlank(NodeList objValue) {
        return objValue == null || objValue.getLength() == 0;
    }

    /**
     * Trims the specified strings from the beginning of the supplied string
     *
     * @param sValue     String to trim
     * @param sTrimChars String to remove from start and end
     * @return Trimmed string
     */
    public static String trimLead(String sValue, String sTrimChars) {
        String sReturn = sValue;
        if (sTrimChars != null && sValue != null) {
            while (sReturn.startsWith(sTrimChars)) {
                sReturn = sReturn.substring(sTrimChars.length());
            }
        }
        return sReturn;
    }

    /**
     * Trims the specified strings from the end of the supplied string
     *
     * @param sValue     String to trim
     * @param sTrimChars String to remove from start and end
     * @return Trimmed string
     */
    public static String trimTrail(String sValue, String sTrimChars) {
        String sReturn = sValue;
        if (sTrimChars != null && sValue != null) {
            while (sReturn.endsWith(sTrimChars)) {
                sReturn = sReturn.substring(0, sReturn.length() - sTrimChars.length());
            }
        }
        return sReturn;
    }

    /**
     * Trims the specified strings from the beginning and end of the supplied string
     *
     * @param sValue     String to trim
     * @param sTrimChars String to remove from start and end
     * @return Trimmed string
     */
    public static String trim(String sValue, String sTrimChars) {
        return trimLead(trimTrail(sValue, sTrimChars), sTrimChars);
    }

    /**
     * Trims out empty elements from the start of the array
     *
     * @param asValues Array to trim
     * @return Trimmed array
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static String[] trimLead(String[] asValues) {
        String[] returnValues = asValues;
        if (!isBlank(asValues)) {
            int start;
            for (start = 0; start < asValues.length && isBlank(asValues[start]); start++) {
            }
            returnValues = Arrays.copyOfRange(asValues, start, asValues.length);
        }
        return returnValues;
    }

    /**
     * Trims out empty elements from the end of the array
     *
     * @param asValues Array to trim
     * @return Trimmed array
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public static String[] trimTrail(String[] asValues) {
        String[] returnValues = asValues;
        if (!isBlank(asValues)) {
            int end;
            for (end = asValues.length - 1; end > -1 && isBlank(asValues[end]); end--) {
            }
            returnValues = Arrays.copyOfRange(asValues, 0, end + 1);
        }
        return returnValues;
    }

    /**
     * Trims out empty elements from the start and end of the array
     *
     * @param asValues Array to trim
     * @return Trimmed array
     */
    public static String[] trim(String[] asValues) {
        return trimTrail(trimLead(asValues));
    }

    /**
     * Splits a comma separated list of values that may or may not be quoted, into
     * a list of trimmed strings
     *
     * @param sValues comma separated list
     * @return String array of values
     */
    public static String[] splitQuotedStrings(String sValues) {
        return splitQuotedStrings(sValues, '\'');
    }

    /**
     * Splits a comma separated list of values that may or may not be quoted, into
     * a list of trimmed strings
     *
     * @param sValues comma separated list
     * @param cQuote  Quote character to use
     * @return String array of values
     */
    public static String[] splitQuotedStrings(String sValues, char cQuote) {
        return splitQuotedStrings(sValues, cQuote, ',');
    }

    /**
     * Splits a comma separated list of values that may or may not be quoted, into
     * a list of trimmed strings
     *
     * @param sValues  comma separated list
     * @param cQuote   Quote character to use
     * @param cDivider Divider character to use
     * @return String array of values
     */
    public static String[] splitQuotedStrings(String sValues, char cQuote, char cDivider) {

        List<String> vValues = new ArrayList<>();
        boolean bInQuotes = false;
        StringBuilder sValue = new StringBuilder();

        // Loop through every character

        if (!isBlank(sValues)) {
            for (int iCnt = 0; iCnt < sValues.length(); iCnt++) {

                // Have we found a boundary ?

                if (sValues.charAt(iCnt) == cDivider && !bInQuotes) {
                    vValues.add(trim(sValue.toString(), " "));
                    sValue = new StringBuilder();
                }

                // Is this a possible string ?

                else if (sValues.charAt(iCnt) == cQuote) {

                    // If we're inside quotes check to see if this is a double quote

                    if (bInQuotes) {
                        if (iCnt < sValues.length() - 1) {
                            if (sValues.charAt(iCnt + 1) == cQuote) {
                                iCnt++;
                                sValue.append(cQuote);
                            }
                            else {
                                bInQuotes = false;
                            }
                        }
                        else {
                            bInQuotes = false;
                        }
                    }
                    else {
                        bInQuotes = true;
                    }
                }
                else {
                    sValue.append(sValues.charAt(iCnt));
                }
            }
            if (sValue.length() > 0 || sValues.charAt(sValues.length() - 1) == cQuote || sValues.charAt(sValues.length() - 1) == cDivider) {
                vValues.add(sValue.toString().trim());
            }
        }

        // Convert the vector to an array

        if (isBlank(vValues)) {
            return null;
        }
        else {
            String[] asTmp = new String[vValues.size()];
            return vValues.toArray(asTmp);
        }
    }

    /**
     * Splits a string using the divider
     * This method is purely here so that it can be used from Velocity templates
     *
     * @param sValues  list of values
     * @param sDivider divider regex
     * @return String array of values
     */
    public static String[] split(String sValues, String sDivider) {
        return sValues.split(sDivider);
    }

    /**
     * Splits a string using the divider
     * This method is purely here so that it can be used from Velocity templates
     *
     * @param sValues  list of values
     * @param sDivider divider regex
     * @param iLimit   Maximum number of array elements
     * @return String array of values
     */
    public static String[] split(String sValues, String sDivider, int iLimit) {

        String[] asReturn;
        String[] asTmp;
        if (iLimit <= 0) {
            asReturn = sValues.split(sDivider);
        }
        else {
            asReturn = new String[]{};
            asTmp = sValues.split(sDivider, iLimit + 1);
            if (asTmp.length > iLimit) {
                System.arraycopy(asTmp, 0, asReturn, 0, iLimit);
            }
            else {
                asReturn = asTmp;
            }
        }
        return asReturn;
    }

    /**
     * Convenient way of sending a file to the client inline for browsing within the client
     *
     * @param fileName The name of the file to use (this is what the file will be called for the user)
     * @param fileIn   Input file to send to the client
     * @param response The response to send the file
     * @throws IOException if an error occurs
     */
    public static void sendFileAsDownloadInline(String fileName, File fileIn, HttpServletResponse response) throws IOException {
        sendFileAsDownload(fileName, fileIn, false, response);
    }

    /**
     * Convenient way of sending a file to the client as an attachment
     *
     * @param fileName The name of the file to use (this is what the file will be called for the user)
     * @param fileIn   Input file to send to the client
     * @param response The response to send the file
     * @throws IOException if an error occurs
     */
    public static void sendFileAsDownloadAttachment(String fileName, File fileIn, HttpServletResponse response) throws IOException {
        sendFileAsDownload(fileName, fileIn, true, response);
    }

    /**
     * This will send the text to the user as a specific text file
     *
     * @param fileName The name of the file to use (this is what the file will be called for the user)
     * @param text     The text to send to the user as a file
     * @param response The response to send the file
     */
    public static void sendTextAsFileDownloadAttachment(String fileName, String text, HttpServletResponse response) {
        InputStream textStream = null;
        try {
            final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            textStream = new ByteArrayInputStream(bytes);
            sendInputStreamAsDownload(fileName, "text/plain", textStream, (long) bytes.length, true, response);
        }
        catch (Exception e) {
            close(textStream);
        }
    }

    /**
     * Convenient way of sending a file to the client
     *
     * @param fileName   The name of the file to use (this is what the file will be called for the user)
     * @param fileIn     Input file to send to the client
     * @param attachment true if the user should be asked to save the file or false if the file should be opened within the browser
     * @param response   The response to send the file
     * @throws IOException if an error occurs
     */
    public static void sendFileAsDownload(String fileName, File fileIn, boolean attachment, HttpServletResponse response) throws IOException {
        if (isBlank(fileIn)) {
            logger.error("The input filename doesn't exist or is invalid");
            throw new PivotalException("The input filename doesn't exist or is invalid");
        }
        else {
            InputStream fileStream = null;
            try {
                fileStream = new BufferedInputStream(new FileInputStream(fileIn), DEFAULT_BUFFER_SIZE);
                sendInputStreamAsDownload(fileName, getMimeType(fileIn), fileStream, fileIn.length(), attachment, response);
            }
            catch (Exception e) {
                close(fileStream);
            }
        }
    }

    /**
     * Convenient way of sending a file to the client
     *
     * @param fileName    The name of the file to use (this is what the file will be called for the user)
     * @param mimeType    The mimetype for the content
     * @param inputStream The inout stream to the content to send to the user
     * @param fileSize    The size of the file (if known)
     * @param attachment  true if the user should be asked to save the file or false if the file should be opened within the browser
     * @param response    The response to send the file
     * @throws IOException if an error occurs
     */
    public static void sendInputStreamAsDownload(String fileName, String mimeType, InputStream inputStream, Long fileSize, boolean attachment, HttpServletResponse response) throws IOException {

        // We need to set the correct headers on the response and then pipe the file to the client

        ServletOutputStream output = response.getOutputStream();
        response.setContentType(String.format("%s;charset=UTF-8", mimeType));
        response.setHeader("Content-Disposition", String.format("%s; filename=%s", attachment ? "attachment" : "inline", fileName));
        response.setHeader("Content-Description", fileName);
        if (!Common.isBlank(fileSize)) response.setHeader("Content-Length", fileSize + "");
        Common.pipeInputToOutputStream(inputStream, output, true);
    }

    /**
     * Convenient way of sending data from an input file to an output stream
     * in the most efficient way possible
     *
     * @param fileIn Input file to read from
     * @param objOut Output stream to write to
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(File fileIn, OutputStream objOut) throws IOException {
        pipeInputToOutputStream(fileIn, objOut, false);
    }

    /**
     * Convenient way of sending data from an input file to an output stream
     * in the most efficient way possible
     *
     * @param fileIn       Input file to read from
     * @param objOut       Output stream to write to
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(File fileIn, OutputStream objOut, boolean ignoreErrors) throws IOException {
        pipeInputToOutputStream(fileIn, objOut, true, ignoreErrors);
    }

    /**
     * Convenient way of sending data from an input file to an output stream
     * in the most efficient way possible
     *
     * @param fileIn       Input file to read from
     * @param objOut       Output stream to write to
     * @param bCloseOutput True if the output stream should be closed on exit
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(File fileIn, OutputStream objOut, boolean bCloseOutput, boolean ignoreErrors) throws IOException {
        if (isBlank(fileIn)) {
            logger.error("The input filename doesn't exist or is invalid");
            if (!ignoreErrors) throw new PivotalException("The input filename doesn't exist or is invalid");
        }
        else {

            InputStream fileStream = null;
            try {
                fileStream = new BufferedInputStream(new FileInputStream(fileIn), DEFAULT_BUFFER_SIZE);
                pipeInputToOutputStream(fileStream, objOut, bCloseOutput, ignoreErrors);
            }
            catch (Exception e) {
                close(fileStream);
            }
        }
    }

    /**
     * Convenient way of sending data from an input stream to an output file
     * in the most efficient way possible
     *
     * @param objIn   Input stream to read from
     * @param fileOut Output file to write to
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, File fileOut) throws IOException {
        pipeInputToOutputStream(objIn, fileOut, false);
    }

    /**
     * Convenient way of sending data from an input stream to an output file
     * in the most efficient way possible
     *
     * @param objIn   Input stream to read from
     * @param fileOut Output file to write to
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, String fileOut) throws IOException {
        pipeInputToOutputStream(objIn, fileOut, false);
    }

    /**
     * Convenient way of sending data from an input stream to an output file
     * in the most efficient way possible
     *
     * @param objIn        Input stream to read from
     * @param fileOut      Output file to write to
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, String fileOut, boolean ignoreErrors) throws IOException {
        pipeInputToOutputStream(objIn, new File(fileOut), ignoreErrors);
    }

    /**
     * Convenient way of sending data from an input stream to an output file
     * in the most efficient way possible
     *
     * @param objIn        Input stream to read from
     * @param fileOut      Output file to write to
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void pipeInputToOutputStream(InputStream objIn, File fileOut, boolean ignoreErrors) throws IOException {
        if (fileOut == null) {
            logger.error("The output filename doesn't exist or is invalid");
            if (!ignoreErrors) throw new PivotalException("The output filename doesn't exist or is invalid");
        }
        else {

            // Create the parentage for the folders if they don't exist

            File parent = fileOut.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            OutputStream fileStream = null;
            try {
                fileStream = new FileOutputStream(fileOut);
                pipeInputToOutputStream(objIn, fileStream, true, ignoreErrors);
            }
            catch (Exception e) {
                close(fileStream);
                if (!ignoreErrors) throw e;
            }
        }
    }

    /**
     * Convenient way of sending data from an input stream to an output stream
     * in the most efficient way possible
     *
     * @param objIn  Input stream to read from
     * @param objOut Output stream to write to
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, OutputStream objOut) throws IOException {
        pipeInputToOutputStream(objIn, objOut, true, false);
    }

    /**
     * Convenient way of sending data from an input stream to an output stream
     * in the most efficient way possible
     *
     * @param objIn        Input stream to read from
     * @param objOut       Output stream to write to
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, OutputStream objOut, boolean ignoreErrors) throws IOException {
        pipeInputToOutputStream(objIn, objOut, true, ignoreErrors);
    }

    /**
     * Convenient way of sending data from an input stream to an output stream
     * in the most efficient way possible
     * If the bCloseOutput flag is false, then the output stream remains open
     * so that further writes can be made to the stream
     *
     * @param objIn        Input stream to read from
     * @param objOut       Output stream to write to
     * @param bCloseOutput True if the output stream should be closed on exit
     * @param ignoreErrors True if this method must not throw any socket errors
     * @throws IOException if an error occurs
     */
    public static void pipeInputToOutputStream(InputStream objIn, OutputStream objOut, boolean bCloseOutput, boolean ignoreErrors) throws IOException {

        OutputStream objBufferedOut = objOut;
        InputStream objBufferedIn = objIn;

        if (objIn != null && objOut != null) {
            try {
                // Buffer the streams if they aren't already

                if (!objBufferedOut.getClass().equals(BufferedOutputStream.class)) {
                    objBufferedOut = new BufferedOutputStream(objBufferedOut, DEFAULT_BUFFER_SIZE);
                }
                if (!objBufferedIn.getClass().equals(BufferedInputStream.class)) {
                    objBufferedIn = new BufferedInputStream(objBufferedIn, DEFAULT_BUFFER_SIZE);
                }

                // Push the data

                int iTmp;
                while ((iTmp = objBufferedIn.read()) != -1) {
                    objBufferedOut.write((byte) iTmp);
                }
                objBufferedOut.flush();
                objOut.flush();
            }
            catch (IOException e) {
                if (!ignoreErrors && !(e instanceof java.net.SocketException)) {
                    logger.error(PivotalException.getErrorMessage(e));
                    throw e;
                }
                else {
                    logger.debug(PivotalException.getErrorMessage(e));
                }
            }
            finally {
                close(objBufferedIn);
                if (bCloseOutput) close(objBufferedOut);
            }
        }
    }

    /**
     * Pads a string either from the front
     * If the string is already larger than the limit then it is returned unchanged
     *
     * @param sValue   String to extend
     * @param sPadChar String to use to pad out the value
     * @param iSize    Size to pad out to
     * @return String
     */
    public static String padLeft(String sValue, String sPadChar, int iSize) {
        return padString(sValue, sPadChar, iSize, true);
    }

    /**
     * Pads a string either from the rear
     * If the string is already larger than the limit then it is returned unchanged
     *
     * @param sValue   String to extend
     * @param sPadChar String to use to pad out the value
     * @param iSize    Size to pad out to
     * @return String
     */
    public static String padRight(String sValue, String sPadChar, int iSize) {
        return padString(sValue, sPadChar, iSize, false);
    }

    /**
     * Pads a string either from the front or rear
     * If the string is already larger than the limit then it is returned unchanged
     *
     * @param sValue   String to extend
     * @param sPadChar String to use to pad out the value
     * @param iSize    Size to pad out to
     * @param bPadLeft True if the padding is at the beginning
     * @return String
     */
    private static String padString(String sValue, String sPadChar, int iSize, boolean bPadLeft) {

        String sReturn = sValue;
        if (sReturn == null) sReturn = "";
        if (sPadChar != null && !sPadChar.isEmpty() && iSize > 0) {
            StringBuilder sBuffer = new StringBuilder(sReturn);
            while (sBuffer.length() < iSize) {
                if (bPadLeft) {
                    sBuffer.insert(0, sPadChar);
                }
                else {
                    sBuffer.append(sPadChar);
                }
            }
            sReturn = sBuffer.toString();
        }
        return sReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Integer without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Int Returns 0 if not a number of null
     */
    public static boolean parseBoolean(String sValue) {
        boolean bReturn = false;
        try {
            if (!isBlank(sValue)) {
                sValue = sValue.trim().toLowerCase();
                bReturn = isYes(sValue) || isYes(parseNumber(sValue));
            }
        }
        catch (Exception e) {
            logger.debug("Problem parsing boolean - %s", PivotalException.getErrorMessage(e));
        }
        return bReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Short without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Int Returns 0 if not a number of null
     */
    public static short parseShort(String sValue) {
        short iReturn;
        try {
            iReturn = Short.parseShort(sValue);
        }
        catch (Exception e) {
            iReturn = 0;
        }
        return iReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Integer without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Int Returns 0 if not a number of null
     */
    public static int parseInt(String sValue) {
        int iReturn;
        try {
            iReturn = Integer.parseInt(sValue);
        }
        catch (Exception e) {
            iReturn = 0;
        }
        return iReturn;
    }


    /**
     * Casts a double to Int
     *
     * @param dValue Double value to cast
     * @return Int Value
     */
    public static int getIntVal(double dValue) {
        int iReturn;
        try {
            iReturn = (int) dValue;
        }
        catch (Exception e) {
            iReturn = 0;
        }
        return iReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Integer without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @param radix  Radix to use
     * @return Int Returns 0 if not a number of null
     */
    public static int parseInt(String sValue, int radix) {
        int iReturn;
        try {
            iReturn = Integer.parseInt(sValue, radix);
        }
        catch (Exception e) {
            iReturn = 0;
        }
        return iReturn;
    }

    /**
     * Provides a safe way of converting a Long to an int without
     * causing the caller too much stress if the Long is null
     *
     * @param sValue Long to convert to an int
     * @return Int Returns 0 if null
     */
    public static int parseInt(Long sValue) {
        int iReturn;
        if (sValue == null) iReturn = 0;
        else iReturn = sValue.intValue();
        return iReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Integer without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Long Returns 0 if not a number of null
     */
    public static long parseLong(String sValue) {
        long lReturn;
        try {
            lReturn = Long.parseLong(sValue);
        }
        catch (Exception e) {
            lReturn = 0;
        }
        return lReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Integer without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @param radix  Radix to use
     * @return Long Returns 0 if not a number of null
     */
    public static long parseLong(String sValue, int radix) {
        long lReturn;
        try {
            lReturn = Long.parseLong(sValue, radix);
        }
        catch (Exception e) {
            lReturn = 0;
        }
        return lReturn;
    }

    /**
     * Provides a safe way of parsing a string for an Float without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Float Returns 0 if not a number of null
     */
    public static float parseFloat(String sValue) {
        float fReturn;
        try {
            fReturn = Float.parseFloat(sValue);
        }
        catch (Exception e) {
            fReturn = 0;
        }
        return fReturn;
    }

    /**
     * Provides a safe way of parsing a string for a Number without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Long Returns 0 if not a number of null
     */
    public static Number parseNumber(String sValue) {
        Number objReturn;
        try {
            objReturn = Double.parseDouble(sValue);
        }
        catch (Exception e) {
            objReturn = 0;
        }
        return objReturn;
    }

    /**
     * Provides a safe way of parsing a string for a Double without
     * causing the caller too much stress if the string is not actually a number
     *
     * @param sValue String to convert to a number
     * @return Long Returns 0 if not a number of null
     */
    public static Double parseDouble(String sValue) {
        Double objReturn;
        try {
            objReturn = Double.parseDouble(sValue);
        }
        catch (Exception e) {
            objReturn = 0.0;
        }
        return objReturn;
    }

    /**
     * This method return true if the parameter nValue can in any way be equated
     * to the idea of YES e.g. 1, 1.0 etc
     *
     * @param nValue Value to check
     * @return True if the value represents Yes
     */
    public static boolean isYes(Number nValue) {

        boolean bReturn = false;
        if (nValue != null) bReturn = nValue.doubleValue() != 0;
        return bReturn;
    }

    /**
     * This method return true if the parameter sValue can in any way be equated
     * to the idea of YES e.g. yes, Y, true, 1 etc
     *
     * @param sValue Value to check
     * @return True if the value represents Yes
     */
    public static boolean isYes(String sValue) {

        boolean bReturn = false;
        if (!isBlank(sValue)) bReturn = sValue.trim().matches("(?i)(y|yes|t|true|on|1)");
        return bReturn;
    }

    /**
     * This method return true if the parameter sValue can in any way be equated
     * to the idea of YES e.g. yes, Y, true, 1 etc
     *
     * @param sValue Value to check
     * @return True if the value represents Yes
     */
    public static boolean isYes(Object sValue) {

        boolean bReturn = false;
        if (sValue != null) {
            try {
                bReturn = isYes(sValue.toString());
            }
            catch (Exception e) {
                logger.error("Problem checking isYes - " + PivotalException.getErrorMessage(e));
            }
        }
        return bReturn;
    }

    /**
     * Returns a date object from parsing the date string which can be in any one
     * of the standard BASIS formats
     *
     * @param sDate String form of the date
     * @return Date
     */
    public static Date parseDate(String sDate) {
        return parseDate(sDate, false);
    }

    /**
     * Returns a date object from parsing the date string which can be in any one
     * of the standard BASIS formats. If it can't match any pattern will return null.
     *
     * @param sDate      String form of the date
     * @param isEuropean If true the parser will use the pattern D2/M2/Y2 and D2/M2/Y4 instead of M2/D2/Y2 and M2/D2/Y2.
     * @return Date
     */
    public static Date parseDate(String sDate, boolean isEuropean) {

        Date objReturn = null;

        if (!isBlank(sDate)) {
            objReturn = new Date(0);
            String[] asPatterns = {"^[a-z]{3} [0-9]{1,2}, [0-9]{4}$",              // 1     'M3 D2, Y4'     12     Jun 21, 1980
                    "^[a-z]{3,9} [0-9]{1,2}, [0-9]{4}$",            // 2     'M9 D2, Y4'     18     June 21, 1980
                    "^[a-z]{3} [0-9]{1,2} [0-9]{4}$",               // 3     'M3 D2 Y4'         11     Jun 21 1980
                    "^[a-z]{3,9} [0-9]{1,2} [0-9]{4}$",             // 4     'M9 D2 Y4'         17     June 21 1980
                    "^[0-9]{1,2}-[a-z]{3,9}-[0-9]{4}$",             // 5     'D2-M3-Y4'         11     21-Jun-1980
                    "^[0-9]{1,2}[a-z]{3,9}[0-9]{4}$",               // 6     'D2M3Y4'         9     21Jun1980
                    "^[0-9]{1,2}[a-z]{3,9}[0-9]{2}$",               // 7     'D2M3Y2'         7     21Jun80
                    "^[0-9]{1,2}-[a-z]{3,9}-[0-9]{2}$",             // 8     'D2-M3-Y2'         9     21-Jun-80
                    "^[0-9]{1,2} [a-z]{3,9} [0-9]{2}$",             // 9     'D2 M3 Y2'         9     21 Jun 80
                    "^[0-9]{1,2} [a-z]{3,9} [0-9]{4}$",             // 10 'D2 M3 Y4'         11     21 Jun 1980
                    "^[0-9]{1,2}\\/[0-9]{1,2}\\/[0-9]{2}$",         // 11 'M2/D2/Y2'         8     06/21/80
                    "^[0-9]{1,2}\\/[0-9]{1,2}\\/[0-9]{4}$",         // 12 'M2/D2/Y4'         10     06/21/1980
                    "^[a-z]{5,9} [a-z]{3,9} [0-9]{1,2} [0-9]{4}$",  // 13 'W9 M9 D2 Y4'     27     Saturday June 21 1980
                    "^[a-z]{5,9} [a-z]{3,9} [0-9]{1,2}, [0-9]{4}$", // 14 'W9 M9 D2, Y4'     28     Saturday June 21, 1980
                    "^[a-z]{5,9} [0-9]{1,2}-[a-z]{3,9}-[0-9]{4}$",  // 15 'W9 D2-M9-Y4'     27     Saturday 21-June-1980
                    "^[a-z]{5,9} [0-9]{1,2}-[a-z]{3}-[0-9]{4}$",    // 16 'W9 D2-M3-Y4'     21     Saturday 21-Jun-1980
                    "^[a-z]{3} [0-9]{1,2}-[a-z]{3,9}-[0-9]{4}$",    // 17 'W3 D2-M3-Y4'     15     Sat 21-Jun-1980
                    "^[a-z]{3} [0-9]{1,2}-[a-z]{3,9}-[0-9]{2}$",    // 18 'W3 D2-M3-Y2'     13     Sat 21-Jun-80
                    "^[0-9]{4}-[0-9]{0,3}$",                        // 19 'Y4-J3'             7     1980-173
                    "^[0-9]{3,5}$",                                 // 20 'Y2J3'             5     80173
                    "^[0-9]{8}$",                                   // 21 'Y4M2D2'             8     19800621
                    "^[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{2}$",         // 22 'D2.M2.Y2'         8     21.06.80
                    "^[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{4}$",         // 23 'D2.M2.Y4'         10     21.06.1980
                    "^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}$",             // 24 'Y4-M2-D2'         10     1980-06-21
                    "^[0-9]{6}$",                                   // 25 'Y2M2D2'             6     800621
                    "^[0-9]{1,2}\\/[a-z]{3,9}\\/[0-9]{2}$",         // 26 'D2/M3/Y2'         9     21/Jun/80
                    "^[0-9]{1,2}\\/[a-z]{3,9}\\/[0-9]{4}$",         // 27 'D2/M3/Y4'         11     21/Jun/1980
                    "^[0-9]{1,2}\\/[0-9]{4}$"                       // 28 'W2/Y4'             7     37/1980
            };

            Map<String, Integer> asMonths = new HashMap<>();
            asMonths.put("january", 0);
            asMonths.put("february", 1);
            asMonths.put("march", 2);
            asMonths.put("april", 3);
            asMonths.put("may", 4);
            asMonths.put("june", 5);
            asMonths.put("july", 6);
            asMonths.put("august", 7);
            asMonths.put("september", 8);
            asMonths.put("october", 9);
            asMonths.put("november", 10);
            asMonths.put("december", 11);
            asMonths.put("jan", 0);
            asMonths.put("feb", 1);
            asMonths.put("mar", 2);
            asMonths.put("apr", 3);
            asMonths.put("jun", 5);
            asMonths.put("jul", 6);
            asMonths.put("aug", 7);
            asMonths.put("sep", 8);
            asMonths.put("oct", 9);
            asMonths.put("nov", 10);
            asMonths.put("dec", 11);

            // Check the date against the patterns

            Calendar objCalendar = Calendar.getInstance();
            int iCentury = Calendar.getInstance().get(Calendar.YEAR) - Calendar.getInstance().get(Calendar.YEAR) % 100;
            objCalendar.clear();
            if (sDate != null) {
                String[] asBits;
                for (int iCnt = 0; iCnt < asPatterns.length && !objCalendar.isSet(Calendar.YEAR); iCnt++) {
                    if (sDate.toLowerCase().matches(asPatterns[iCnt])) {
                        switch (iCnt + 1) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                asBits = sDate.split("[ ]|, ");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[1]));
                                objCalendar.set(Calendar.MONTH, asMonths.get(asBits[0].substring(0, 3).toLowerCase()));
                                objCalendar.set(Calendar.YEAR, parseInt(asBits[2]));
                                break;
                            case 5:
                            case 8:
                            case 9:
                            case 10:
                            case 26:
                            case 27:
                                asBits = sDate.split("[ -\\./]");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[0]));
                                objCalendar.set(Calendar.MONTH, asMonths.get(asBits[1].substring(0, 3).toLowerCase()));
                                if (asBits[2].length() == 2) {
                                    objCalendar.set(Calendar.YEAR, iCentury + parseInt(asBits[2]));
                                }
                                else {
                                    objCalendar.set(Calendar.YEAR, parseInt(asBits[2]));
                                }
                                break;
                            case 22:
                            case 23:
                                asBits = sDate.split("[ -\\.//]");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[0]));
                                objCalendar.set(Calendar.MONTH, parseInt(asBits[1]) - 1);
                                if (asBits[2].length() == 2) {
                                    objCalendar.set(Calendar.YEAR, iCentury + parseInt(asBits[2]));
                                }
                                else {
                                    objCalendar.set(Calendar.YEAR, parseInt(asBits[2]));
                                }
                                break;
                            case 6:
                            case 7:
                                asBits = sDate.split("[a-zA-Z]{3}");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[0]));
                                if (asBits[1].length() == 2) {
                                    objCalendar.set(Calendar.YEAR, iCentury + parseInt(asBits[1]));
                                }
                                else {
                                    objCalendar.set(Calendar.YEAR, parseInt(asBits[1]));
                                }
                                asBits = sDate.split("[0-9][^a-zA-Z]");
                                objCalendar.set(Calendar.MONTH, asMonths.get(asBits[1].substring(0, 3).toLowerCase()));
                                break;
                            case 11:
                            case 12:
                                asBits = sDate.split("/");
                                if (isEuropean) {
                                    objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[0]));
                                    objCalendar.set(Calendar.MONTH, parseInt(asBits[1]) - 1);
                                }
                                else {
                                    objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[1]));
                                    objCalendar.set(Calendar.MONTH, parseInt(asBits[0]) - 1);
                                }
                                if (asBits[2].length() == 2) {
                                    objCalendar.set(Calendar.YEAR, iCentury + parseInt(asBits[2]));
                                }
                                else {
                                    objCalendar.set(Calendar.YEAR, parseInt(asBits[2]));
                                }
                                break;
                            case 13:
                            case 14:
                                asBits = sDate.split("[ -]|, ");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[2]));
                                objCalendar.set(Calendar.MONTH, asMonths.get(asBits[1].substring(0, 3).toLowerCase()));
                                objCalendar.set(Calendar.YEAR, parseInt(asBits[3]));
                                break;
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                                asBits = sDate.split("[ -]|, ");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[1]));
                                objCalendar.set(Calendar.MONTH, asMonths.get(asBits[2].substring(0, 3).toLowerCase()));
                                if (asBits[3].length() == 2) {
                                    objCalendar.set(Calendar.YEAR, iCentury + parseInt(asBits[3]));
                                }
                                else {
                                    objCalendar.set(Calendar.YEAR, parseInt(asBits[3]));
                                }
                                break;
                            case 19:
                                asBits = sDate.split("-");
                                objCalendar.set(Calendar.YEAR, parseInt(asBits[0]));
                                objCalendar.set(Calendar.DAY_OF_YEAR, parseInt(asBits[1]));
                                break;
                            case 20:
                                objCalendar.set(Calendar.YEAR, iCentury + parseInt(sDate.substring(0, 2)));
                                objCalendar.set(Calendar.DAY_OF_YEAR, parseInt(sDate.substring(2)));
                                break;
                            case 21:
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(sDate.substring(6, 8)));
                                objCalendar.set(Calendar.MONTH, parseInt(sDate.substring(4, 6)) - 1);
                                objCalendar.set(Calendar.YEAR, parseInt(sDate.substring(0, 4)));
                                break;
                            case 24:
                                asBits = sDate.split("[-]");
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(asBits[2]));
                                objCalendar.set(Calendar.MONTH, parseInt(asBits[1]) - 1);
                                objCalendar.set(Calendar.YEAR, parseInt(asBits[0]));
                                break;
                            case 25:
                                objCalendar.set(Calendar.DAY_OF_MONTH, parseInt(sDate.substring(4, 6)));
                                objCalendar.set(Calendar.MONTH, parseInt(sDate.substring(2, 4)) - 1);
                                objCalendar.set(Calendar.YEAR, iCentury + parseInt(sDate.substring(0, 2)));
                                break;
                            case 28:
                                asBits = sDate.split("/");
                                objCalendar.set(Calendar.YEAR, parseInt(asBits[1]));
                                objCalendar.set(Calendar.WEEK_OF_YEAR, parseInt(asBits[0]));
                                break;

                            default:
                                break;
                        }
                    }
                }

                // If no BASIS pattern was found, then try it against the standard Java
                // otherwise clear the time element from the BASIS date

                if (objCalendar.isSet(Calendar.YEAR)) {
                    objReturn = getDate(objCalendar.getTime());
                }
                else {
                    objReturn = parseDateTime(DateFormat.FULL, DateFormat.FULL, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.FULL, DateFormat.LONG, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.FULL, DateFormat.MEDIUM, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.FULL, DateFormat.SHORT, sDate);

                    if (objReturn == null) objReturn = parseDateTime(DateFormat.LONG, DateFormat.FULL, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.LONG, DateFormat.LONG, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.LONG, DateFormat.MEDIUM, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.LONG, DateFormat.SHORT, sDate);

                    if (objReturn == null) objReturn = parseDateTime(DateFormat.MEDIUM, DateFormat.FULL, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.MEDIUM, DateFormat.LONG, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.MEDIUM, DateFormat.MEDIUM, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.MEDIUM, DateFormat.SHORT, sDate);

                    if (objReturn == null) objReturn = parseDateTime(DateFormat.SHORT, DateFormat.FULL, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.SHORT, DateFormat.LONG, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.SHORT, DateFormat.MEDIUM, sDate);
                    if (objReturn == null) objReturn = parseDateTime(DateFormat.SHORT, DateFormat.SHORT, sDate);
                }
            }
        }

        return objReturn;
    }

    /**
     * Returns a date object from parsing the time string which can be in any one
     * of the standard BASIS formats
     *
     * @param sTime String form of the time
     * @return Date
     */
    public static Date parseTime(String sTime) {

        String[] asPatterns = {"^[0-9]{1,6}$",                                 // 1     'H2M2S2','M2S2','S2'
                "^[0-9]{1,2}.[0-9]{1,2}.[0-9]{1,2}$",           // 2     'H2 M2 S2'
                "^[0-9]{1,2}.[0-9]{1,2}$",                      // 3     'H2 M2'
                "^[0-9]{1,2}\\.[0-9]{1,2} *(am|pm)$",           // 4     'H2.M2 am/pm'
                "^[0-9]{1,2}.[0-9]{1,2}.[0-9]{1,2}\\s+(am|pm)$"                        // 5     'H2M2S2 am/pm'
        };

        // Check the date against the patterns

        Calendar objReturn = Calendar.getInstance();
        int iTmp;
        objReturn.set(Calendar.HOUR_OF_DAY, 0);
        objReturn.set(Calendar.MINUTE, 0);
        objReturn.set(Calendar.SECOND, 0);
        objReturn.set(Calendar.MILLISECOND, 0);
        if (sTime != null) {
            sTime = sTime.trim().toLowerCase().replaceAll(":", " ");
            for (int iCnt = 0; iCnt < asPatterns.length; iCnt++) {
                if (sTime.toLowerCase().matches(asPatterns[iCnt])) {
                    switch (iCnt + 1) {
                        case 1:
                        case 2:
                            sTime = sTime.replaceAll("[^0-9]", "");
                            if (sTime.length() < 6) sTime = "000000".substring(sTime.length()) + sTime;
                            iTmp = parseInt(sTime.substring(0, 2)) * 3600 + parseInt(sTime.substring(2, 4)) * 60 + parseInt(sTime.substring(4, 6));
                            objReturn.set(Calendar.SECOND, iTmp);
                            break;

                        case 3:
                            sTime = sTime.replaceAll("[^0-9]", "");
                            if (sTime.length() < 4) sTime = "0000".substring(sTime.length()) + sTime;
                            iTmp = parseInt(sTime.substring(0, 2)) * 3600 + parseInt(sTime.substring(2, 4)) * 60;
                            objReturn.set(Calendar.SECOND, iTmp);
                            break;

                        case 4:
                            String sTimeTest = sTime.replaceAll("[^0-9]", "").split("(am|pm)")[0].trim();
                            if (sTimeTest.length() < 4) sTimeTest = "0000".substring(sTimeTest.length()) + sTimeTest;
                            iTmp = parseInt(sTimeTest.substring(0, 2)) * 3600 + parseInt(sTimeTest.substring(2, 4)) * 60;
                            if (sTime.endsWith("pm")) iTmp += 43200;
                            objReturn.set(Calendar.SECOND, iTmp);
                            break;
                        case 5:
                            String[] parts = sTime.split("(am|pm)")[0].split("\\s");
                            iTmp = parseInt(parts[0]) * 3600 + parseInt(parts[1]) * 60 + parseInt(parts[2]);
                            if (sTime.endsWith("pm")) iTmp += 43200;
                            objReturn.set(Calendar.SECOND, iTmp);
                            break;

                        default:
                            break;
                    }
                }
            }
            // If no BASIS pattern was found, then try it against the standard Java

            if (!objReturn.isSet(Calendar.YEAR)) {
                try {
                    objReturn.setTime(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).parse(sTime));
                }
                catch (Exception e) {
                    logger.debug(PivotalException.getErrorMessage(e));
                }
            }
        }

        // Clear out the time part of the date

        return objReturn.getTime();
    }

    /**
     * This method parses the time and date into a single Date object
     *
     * @param sDateTime DateTime string in format YYYYMMDDHHMMSS or YYYY-MM-DD HH:MM:SS (as returned by DataSourceUtils.getValueOf)
     * @return Date object
     */
    public static Date parseDateTime(String sDateTime) {
        Date dReturn = null;

        if (!isBlank(sDateTime) && sDateTime.length() == 14 && !parseNumber(sDateTime).equals(0)) {
            dReturn = parseDateTime(sDateTime.substring(0, 8), sDateTime.substring(8, 14));
        }
        else if (!isBlank(sDateTime) && sDateTime.contains(" ")) {
            int i = sDateTime.lastIndexOf(' ');
            dReturn = parseDateTime(sDateTime.substring(0, i), sDateTime.substring(i + 1));
        }

        return dReturn;
    }

    /**
     * This method parses the time and date into a single Date object
     *
     * @param lDateTime DateTime in milliseconds
     * @return Date object
     */
    public static Date parseDateTime(long lDateTime) {

        Date dReturn = null;

        try {
            Calendar objTarget = Calendar.getInstance();
            objTarget.setTimeInMillis(lDateTime);
            dReturn = objTarget.getTime();
        }
        catch (Exception e) {
            logger.warn("Error in parseDateTime " + PivotalException.getErrorMessage(e));
        }

        return dReturn;
    }

    /**
     * This method parses the time and date into a single Date object
     *
     * @param sDate Date string
     * @param sTime Time string
     * @return Date object
     */
    public static Date parseDateTime(String sDate, String sTime) {
        return parseDateTime(sDate, sTime, false);
    }

    /**
     * This method parses the time and date into a single Date object
     *
     * @param sDate      Date string
     * @param sTime      Time string
     * @param isEuropean If true the date parser will use the pattern D2/M2/Y2 and D2/M2/Y4 instead of M2/D2/Y2 and M2/D2/Y2.
     * @return Date object
     */
    public static Date parseDateTime(String sDate, String sTime, boolean isEuropean) {

        // Parse the time and date objects

        Date returnValue = null;
        try {
            Calendar objTime = Calendar.getInstance();
            objTime.setTime(parseTime(sTime));
            Calendar objDate = Calendar.getInstance();
            objDate.setTime(parseDate(sDate, isEuropean));

            // Add the time element to the date

            objDate.set(Calendar.HOUR_OF_DAY, objTime.get(Calendar.HOUR_OF_DAY));
            objDate.set(Calendar.MINUTE, objTime.get(Calendar.MINUTE));
            objDate.set(Calendar.SECOND, objTime.get(Calendar.SECOND));
            objDate.set(Calendar.MILLISECOND, 0);
            returnValue = objDate.getTime();
        }
        catch (Exception e) {
            logger.debug("Cannot parse date " + sDate);
        }

        return returnValue;
    }

    /**
     * Attempts to parse a date/time string using the built-in (and
     * rather lame) parse
     *
     * @param dateFormat Style of date format to use
     * @param timeFormat Style of time format to use
     * @param sDate      Date string to parse
     * @return Date object
     */
    private static Date parseDateTime(int dateFormat, int timeFormat, String sDate) {
        Date objReturn = null;
        try {
            objReturn = DateFormat.getDateTimeInstance(dateFormat, timeFormat).parse(sDate);
        }
        catch (Exception e) {
            logger.debug("Cannot parse date " + sDate);
        }
        return objReturn;
    }

    /**
     * This method parses the date/time according to the specified format
     * and return a Date object
     *
     * @param dateTime Date/Time string
     * @param format   the Date/Time format to be used in the SimpleDateFormat
     * @return Date object
     */
    public static Date parseDateTimeInFormat(String dateTime, String format) {
        Date ret = null;

        if (!isBlank(dateTime) && !isBlank(format)) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(format);
                ret = formatter.parse(dateTime);
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }

        return ret;
    }

    /**
     * Returns today as a date object with the time element zeroed
     *
     * @return Date object
     */
    public static Date getDate() {
        return getDate(new Date());
    }

    /**
     * Returns the passed date with all the time elements zeroed
     *
     * @param objDate Date to modify
     * @return Date object
     */
    public static Date getDate(Date objDate) {
        Date objReturn = objDate;
        if (objDate != null) {
            Calendar objCalendar = Calendar.getInstance();
            objCalendar.setTime(objDate);
            objCalendar.set(Calendar.HOUR_OF_DAY, 0);
            objCalendar.set(Calendar.MINUTE, 0);
            objCalendar.set(Calendar.SECOND, 0);
            objCalendar.set(Calendar.MILLISECOND, 0);
            objReturn = objCalendar.getTime();
        }
        return objReturn;
    }

    /**
     * Returns the item number for the given value name in the list
     *
     * @param asValues array of values
     * @param sValue   Value to match
     * @return Item position or -1 if it doesn't exist
     */
    public static int getItem(String[] asValues, String sValue) {
        int iReturn = -1;
        if (!isBlank(asValues)) {
            for (int iCnt = 0; iCnt < asValues.length && iReturn == -1; iCnt++) {
                if (doStringsMatch(asValues[iCnt], sValue)) iReturn = iCnt;
            }
        }
        return iReturn;
    }

    /**
     * Splits a string using the divider and returns the item given by iItem
     *
     * @param sValues  list of values
     * @param sDivider divider regex
     * @param iItem    Item from list
     * @return String value
     */
    public static String getItem(String sValues, String sDivider, int iItem) {

        String sReturn = null;
        if (iItem < 0 || isStringEmpty(sDivider) || isStringEmpty(sValues)) {
            sReturn = sValues;
        }
        else {
            try {
                String[] asTmp = sValues.split(sDivider);
                if (iItem < asTmp.length) sReturn = asTmp[iItem];
            }
            catch (Exception e) {
                logger.debug("Error in getItem - " + PivotalException.getErrorMessage(e));
            }
        }
        return sReturn;
    }

    /**
     * Gets the named value from the map string which is expected to be
     * a comma separated list of key value pairs
     *
     * @param mapString e.g. xxx=yyy,zzz=kkk
     * @param name      Name of the item to get
     * @return String value
     */
    public static String getItem(String mapString, String name) {
        String retunValue = null;
        if (!isBlank(mapString) && !isBlank(name)) {
            Map<String, String> map = getMapFromString(mapString);
            if (!isBlank(map)) {
                retunValue = map.get(name);
            }
        }
        return retunValue;
    }

    /**
     * Creates a map from a comma separated list of key value pairs
     *
     * @param values e.g. xxx=yyy,zzz=kkk
     * @return Map of strings
     */
    public static Map<String, String> getMapFromString(String values) {
        Map<String, String> returnValue = null;

        // Check we have something

        if (!isBlank(values)) {
            List<String> list = splitToList(values, " *, *");

            // Make sure we have a list

            if (!isBlank(list)) {
                for (String value : list) {

                    // Split each key value pair

                    if (!isBlank(value) && value.contains("=")) {
                        String name = value.split(" *= *", 2)[0];
                        String part = value.split(" *= *", 2)[1];

                        // If both key and value aren't empty add them to the map

                        if (!isBlank(name) && !isBlank(part)) {
                            if (returnValue == null) returnValue = new LinkedCaseInsensitiveMap<>();
                            returnValue.put(name, part);
                        }
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * Returns true or false if the strings match
     *
     * @param sValue1 String 1 to match
     * @param sValue2 String 2 to match
     * @return boolean
     */
    public static boolean doStringsMatch(String sValue1, String sValue2) {
        return doStringsMatch(sValue1, sValue2, true);
    }

    /**
     * Returns true or false if the strings match
     *
     * @param sValue1         String 1 to match
     * @param sValue2         String 2 to match
     * @param caseInsensitive True if the match is case insensitive
     * @return boolean
     */
    public static boolean doStringsMatch(String sValue1, String sValue2, boolean caseInsensitive) {
        if (sValue1 == null && sValue2 == null) {
            return true;
        }
        else if (sValue1 == null || sValue2 == null) {
            return false;
        }
        else {
            if (caseInsensitive) return sValue1.equalsIgnoreCase(sValue2);
            else return sValue1.equals(sValue2);
        }
    }

    /**
     * Returns true or false if the strings match
     *
     * @param sValue1 String 1 to match
     * @param sValue2 String 2 to match
     * @return boolean
     */
    public static boolean doStringsMatch(String sValue1, String... sValue2) {
        if (sValue1 == null && sValue2 == null) {
            return true;
        }
        if (sValue1 == null || sValue2 == null) {
            return false;
        }
        for (String value : sValue2) {
            if (sValue1.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    /**
     * Checks the string value for null or ""
     *
     * @param sValue Value to check
     * @return Boolean
     */
    public static boolean isStringEmpty(String sValue) {
        return sValue == null || sValue.isEmpty();
    }

    /**
     * Splits a string using the divider
     * This method is purely here so that it can be used from Velocity templates
     *
     * @param sValues list of values
     * @return String array of values
     */
    public static List<String> splitToList(String sValues) {
        return splitToList(sValues, " *, *");
    }


    /**
     * Splits a string into a List of Integers using the divider
     * If any of the elements fails to parse into an Integer, a 0 will be returned in it's place
     *
     * @param sValues  list of values
     * @param sDivider divider regex
     * @return List of integers found
     */
    public static List<Integer> splitToIntegerList(String sValues, String sDivider) {
        List<Integer> objReturn = new ArrayList<>();
        if (sValues != null) {
            String[] asTmp = split(sValues, sDivider);
            if (asTmp != null) {
                for(String curr : asTmp){
                    objReturn.add(Common.parseInt(curr));
                }
            }
        }
        return objReturn;
    }

    /**
     * Splits a string using the divider
     * This method is purely here so that it can be used from Velocity templates
     *
     * @param sValues  list of values
     * @param sDivider divider regex
     * @return String array of values
     */
    public static List<String> splitToList(String sValues, String sDivider) {
        List<String> objReturn = null;
        if (sValues != null) {
            String[] asTmp = split(sValues, sDivider);
            if (asTmp != null) objReturn = new ArrayList<>(Arrays.asList(asTmp));
        }
        return objReturn;
    }

    /**
     * Joins values in a string list into a single comma separated string
     *
     * @param asValues List of strings to concatenate
     *                 =     * @return String
     */
    public static String join(Collection asValues) {
        return join(asValues, ",");
    }

    /**
     * Joins values in a string list into a single string using the separator
     *
     * @param asValues List of strings to concatenate
     * @param sSep     separator string
     * @return String
     */
    public static String join(Collection asValues, String sSep) {
        if (asValues == null) return null;
        StringBuilder sReturn = new StringBuilder();
        boolean bGotSomething = false;
        for (Object sValue : asValues) {
            if (sValue != null) {
                if (bGotSomething) sReturn.append(sSep);
                sReturn.append(sValue);
                bGotSomething = true;
            }
        }
        return sReturn.toString();
    }

    /**
     * Joins values in a string list into a single comma separated string
     *
     * @param asValues Array of strings to concatenate
     * @return String
     */
    public static String join(String... asValues) {
        return join(asValues, ",");
    }

    /**
     * Joins values in a string array into a single string using the separator
     *
     * @param asValues Array of strings to concatenate
     * @param sSep     separator string
     * @return String
     */
    public static String join(String[] asValues, String sSep) {
        if (asValues == null) return null;
        StringBuilder sReturn = new StringBuilder();
        boolean bGotSomething = false;
        for (String sValue : asValues) {
            if (sValue != null) {
                if (bGotSomething) sReturn.append(sSep);
                sReturn.append(sValue);
                bGotSomething = true;
            }
        }
        return sReturn.toString();
    }

    /**
     * Joins values in a string list into a single comma separated string
     *
     * @param asValues List of strings to concatenate
     * @return String
     */
    public static String join(Number... asValues) {
        return join(asValues, ",");
    }

    /**
     * Joins values in a string array into a single string using the separator
     *
     * @param asValues Array of strings to concatenate
     * @param sSep     separator string
     * @return String
     */
    public static String join(Number[] asValues, String sSep) {
        if (asValues == null) return null;
        StringBuilder sReturn = new StringBuilder();
        boolean bGotSomething = false;
        for (Number sValue : asValues) {
            if (sValue != null) {
                if (bGotSomething) sReturn.append(sSep);
                sReturn.append(sValue).append("");
                bGotSomething = true;
            }
        }
        return sReturn.toString();
    }

    /**
     * Returns a string of the date using standard formatting rules
     * These rules are defined in the Java DateFormat class and use the
     * SimpleDateFormat class to accomplish the formatting
     *
     * @param objDate Date to format
     * @param sFormat Template to use for format
     * @return String
     * @see SimpleDateFormat for the formatting rules
     */
    public static String dateFormat(Date objDate, String sFormat) {
        Calendar objCalendar = Calendar.getInstance();
        if (objDate != null && !isBlank(sFormat)) {
            objCalendar.setTime(objDate);
            DateFormat objTmp = new SimpleDateFormat(sFormat);
            objTmp.setTimeZone(objCalendar.getTimeZone());
            return objTmp.format(objDate);
        }
        else return null;
    }

    /**
     * Returns a string of the date using standard formatting rules
     *
     * @param sDate   String date to format
     * @param sFormat Template to use for format
     * @return String
     * @see SimpleDateFormat for the formatting rules
     */
    public static String dateFormat(String sDate, String sFormat) {
        return dateFormat(parseDate(sDate), sFormat);
    }

    /**
     * Return the number of days between two dates
     *
     * @param sDate start date
     * @param eDate end date
     * @return long number of days
     */
    public static long dateCalculateDays(Date sDate, Date eDate) {

        Calendar sCalendar = Calendar.getInstance();
        sCalendar.setTime(sDate);
        Calendar eCalendar = Calendar.getInstance();
        eCalendar.setTime(eDate);
        return daysBetween(sCalendar, eCalendar);
    }

    /**
     * Return the number of days between two Calendar dates
     *
     * @param startDate start date
     * @param endDate   End Date
     * @return long number of days
     */
    private static long daysBetween(Calendar startDate, Calendar endDate) {
        Calendar date = (Calendar) startDate.clone();
        long daysBetween = 0;
        while (date.before(endDate)) {
            date.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return daysBetween;
    }

    /**
     * Returns a string of the date using standard formatting rules
     * These rules are defined in the Java DateFormat class and use the
     * SimpleDateFormat class to accomplish the formatting
     *
     * @param objDate Date to format
     * @param sFormat Template to use for format
     * @return String
     * @see SimpleDateFormat for the formatting rules
     */
    public static String formatDate(Date objDate, String sFormat) {
        return dateFormat(objDate, sFormat);
    }

    /**
     * Returns a string of the date using standard formatting rules
     *
     * @param sDate   String date to format
     * @param sFormat Template to use for format
     * @return String
     * @see SimpleDateFormat for the formatting rules
     */
    public static String formatDate(String sDate, String sFormat) {
        return dateFormat(sDate, sFormat);
    }

    /**
     * Formats a date based upon the time in milliseconds
     *
     * @param lDate   Milliseconds
     * @param sFormat Format
     * @return String
     * @see SimpleDateFormat for the formatting rules
     */
    public static String dateFormat(long lDate, String sFormat) {
        return dateFormat(new Date(lDate), sFormat);
    }

    /**
     * Returns the number of units difference between the 2 dates
     *
     * @param dStartDate Date to compare
     * @param dEndDate   Date to compare
     * @param iUnit      Calendar unit
     * @return long
     */
    public static long diffDate(Date dStartDate, Date dEndDate, int iUnit) {

        // SS-1294 This has been refactored to return a long. If the dates are years between (and the type is ms)
        // the int would return the wrong number as the value overflows an integer.

        // Short-circuit if it's an easy calculation

        long iReturn = 0;
        if (iUnit == Calendar.MILLISECOND) {
            iReturn = (dEndDate.getTime() - dStartDate.getTime());
        }
        else if (iUnit == Calendar.SECOND) {
            iReturn = (dEndDate.getTime() - dStartDate.getTime()) / 1000;
        }
        else if (iUnit == Calendar.MINUTE) {
            iReturn = (dEndDate.getTime() - dStartDate.getTime()) / 60000;
        }
        else if (iUnit == Calendar.HOUR || iUnit == Calendar.HOUR_OF_DAY) {
            iReturn = (dEndDate.getTime() - dStartDate.getTime()) / 3600000;
        }
        else if (iUnit == Calendar.DATE) {
            iReturn = (dEndDate.getTime() - dStartDate.getTime()) / 86400000;
        }
        else {
            Calendar objCalendar = Calendar.getInstance();
            objCalendar.setTime(dStartDate);
            while (objCalendar.getTime().getTime() < dEndDate.getTime()) {
                objCalendar.add(iUnit, 1);
                iReturn++;
            }
        }
        return iReturn;
    }

    /**
     * Returns true if the end date is after the start date
     *
     * @param dStartDate Date to compare
     * @param dEndDate   Date to compare
     * @return True if the end date is after the start date
     */
    public static boolean after(Date dStartDate, Date dEndDate) {

        // Just work out if the end date is greater than the start date

        return notBlank(dStartDate, dEndDate) && dEndDate.after(dStartDate);
    }

    /**
     * Adds the period of time to the date and returns it
     *
     * @param dDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Date addDate(Date dDate, int iPeriod, int iCnt) {

        Calendar objCalendar = Calendar.getInstance();
        objCalendar.setTime(dDate);
        objCalendar.add(iPeriod, iCnt);
        return objCalendar.getTime();
    }

    /**
     * Adds the period of time to the date and returns it
     *
     * @param timestamp   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Timestamp addTimestamp(Timestamp timestamp, int iPeriod, int iCnt) {

        Calendar objCalendar = Calendar.getInstance();
        objCalendar.setTime(timestamp);
        objCalendar.add(iPeriod, iCnt);
        return new Timestamp(objCalendar.getTimeInMillis());
    }

    /**
     * Adds the period of time to the date and returns it
     *
     * @param dDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @param dPoint  true if need date time is end of day else false
     * @return Date
     */
    public static Date addDate(Date dDate, int iPeriod, int iCnt, boolean dPoint) {

        Calendar objCalendar = Calendar.getInstance();
        objCalendar.setTime(dDate);
        objCalendar.add(iPeriod, iCnt);
        if (dPoint) {
            objCalendar.set(Calendar.HOUR_OF_DAY, 23);
            objCalendar.set(Calendar.MINUTE, 59);
            objCalendar.set(Calendar.SECOND, 0);
            objCalendar.set(Calendar.MILLISECOND, 0);
        }
        return objCalendar.getTime();
    }

    /**
     * Adds the period of time to the date and returns it
     *
     * @param dDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Date addDate(Date dDate, int iPeriod, Number iCnt) {
        return addDate(dDate, iPeriod, iCnt == null ? 0 : iCnt.intValue());
    }

    /**
     * Adds the period of time to the date string and returns it as a date
     *
     * @param sDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Date addDate(String sDate, int iPeriod, int iCnt) {

        Calendar objCalendar = Calendar.getInstance();
        objCalendar.setTime(parseDate(sDate));
        objCalendar.add(iPeriod, iCnt);
        return objCalendar.getTime();
    }

    /**
     * Adds the period of time to the date string and returns it as a date
     *
     * @param sDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Date addDate(String sDate, int iPeriod, Number iCnt) {
        return addDate(sDate, iPeriod, iCnt == null ? 0 : iCnt.intValue());
    }

    /**
     * Adds the period of time to the date string and returns it as a date
     *
     * @param sDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @return Date
     */
    public static Date addDate(String sDate, int iPeriod, String iCnt) {
        return addDate(sDate, iPeriod, iCnt == null ? 0 : parseInt(iCnt));
    }

    /**
     * Adds the period of time to the date string and returns it as a String
     *
     * @param sDate   Date to add to
     * @param iPeriod Calendar period to add
     * @param iCnt    Number of periods to add/subtract
     * @param sFormat Template to use for format
     * @return String representing the operation result.
     * @see SimpleDateFormat for the formatting rules
     */
    public static String addDate(String sDate, int iPeriod, int iCnt, String sFormat) {

        Calendar objCalendar = Calendar.getInstance();
        objCalendar.setTime(parseDate(sDate));
        objCalendar.add(iPeriod, iCnt);
        return dateFormat(objCalendar.getTime(), sFormat);
    }

    /**
     * Returns the value of the specified time field in the date
     *
     * @param objDate    Date to format
     * @param iDateField Field of the Calendar to get
     * @return Integer
     * @see Date for the field names
     */
    public static int dateValue(Date objDate, int iDateField) {
        try {
            Calendar objCalendar = Calendar.getInstance();
            objCalendar.setTime(objDate);
            return objCalendar.get(iDateField);
        }
        catch (Exception e) {
            logger.error("Problem calling dateValue with date [" + objDate + "] and date field [" + iDateField + ']');
            return 0;
        }
    }

    /**
     * Gets the text of the <body> portion of an HTML stream
     *
     * @param sHtml Stream to strip
     * @return sText returned pure text
     */
    public static String getBodyTextFromHtml(String sHtml) {

        String sReturn = sHtml;

        // Get the Body part if there is one

        int iPos;
        if ((iPos = sReturn.toLowerCase().indexOf("<body")) >= 0) sReturn = sReturn.substring(iPos);
        if ((iPos = sReturn.toLowerCase().indexOf("</body>")) >= 0) sReturn = sReturn.substring(0, iPos);

        // Now get rid of all the script sections

        sReturn = Pattern.compile("<script.*?</script>", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");

        // Replace line breaks and paragraphs

        sReturn = sReturn.replaceAll("\r\n", "");
        sReturn = sReturn.replaceAll("\n", "");
        sReturn = sReturn.replaceAll("\t", "");

        sReturn = Pattern.compile("<div", Pattern.CASE_INSENSITIVE).matcher(sReturn).replaceAll("\n<div");
        sReturn = Pattern.compile("<tr", Pattern.CASE_INSENSITIVE).matcher(sReturn).replaceAll("\n<tr");
        sReturn = Pattern.compile("<br>", Pattern.CASE_INSENSITIVE).matcher(sReturn).replaceAll("\n");
        sReturn = Pattern.compile("<p>", Pattern.CASE_INSENSITIVE).matcher(sReturn).replaceAll("\n\n");
        sReturn = sReturn.replaceAll("&nbsp;", " ");

        // Finally get rid of all the tags

        sReturn = trim(sReturn.replaceAll("<[^\\d\\w]*[\\d\\w]*\\s*([\\d\\w]*\\s*=\\s*((\"[^\"]*\")|('[^']'*)|(\\w\\d))\\s*)*[^>]*>", ""), "\n").trim();
        sReturn = sReturn.replaceAll("\n +", "\n");
        sReturn = sReturn.replaceAll(" +\n", "\n");
        while (sReturn.contains("\n\n\n")) {
            sReturn = sReturn.replaceAll("\n\n\n", "\n\n");
        }
        while (sReturn.contains("    ")) {
            sReturn = sReturn.replaceAll("    ", "   ");
        }
        return sReturn;
    }

    /**
     * Returns now as a date for velocity
     *
     * @return Date
     */
    public static Date getNow() {
        return new Date();
    }

    /**
     * Sorts a map of strings by their contents
     *
     * @param map Map to sort
     * @return Sorted map
     */
    public static Map<String, String> sortMap(Map<String, String> map) {
        Map<String, String> returnValue = null;
        if (!isBlank(map)) {
            returnValue = new LinkedHashMap<>();
            Map<String, String> tmp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                tmp.put(entry.getValue(), entry.getKey());
            }
            for (Map.Entry<String, String> entry : tmp.entrySet()) {
                returnValue.put(entry.getValue(), entry.getKey());
            }
        }
        return returnValue;
    }

    /**
     * Sorts a map of strings by their contents
     *
     * @param map Map to sort
     * @return Sorted map
     */
    public static Map<String, Object> sortMapByKey(Map<String, Object> map) {
        Map<String, Object> returnValue = null;
        if (!isBlank(map)) {
            returnValue = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            returnValue.putAll(map);
        }
        return returnValue;
    }

    /**
     * Returns the map as a linked case insensitive hash map
     *
     * @return Linked hash map
     */
    public static Map<String, Object> getLinkedCaseInsensitiveMap() {
        return new LinkedCaseInsensitiveMap<>();
    }

    /**
     * Returns the map as a linked case insensitive hash map
     * It only makes sense to call this with a linked hash map of course
     *
     * @param map Map of string based keys
     * @return Linked hash map
     */
    public static Map<String, Object> getLinkedCaseInsensitiveMap(Map<String, Object> map) {
        Map<String, Object> returnValue = new LinkedCaseInsensitiveMap<>();
        if (!isBlank(map)) returnValue.putAll(map);
        return returnValue;
    }

    /**
     * Sorts the multidimensional array using the column specified
     *
     * @param asValues    Two dimensional array
     * @param iSortColumn Column to sort on
     * @return Sorted array
     */
    public static String[][] sortArray(String[][] asValues, int iSortColumn) {
        try {
            if (asValues != null && iSortColumn > -1) {
                List<Map<String, Object>> objSort = new ArrayList<>();
                for (String[] asRow : asValues) {
                    Map<String, Object> objRow = new HashMap<>();
                    objRow.put("value", asRow);
                    if (iSortColumn < asRow.length) objRow.put("sort", asRow[iSortColumn]);
                    else objRow.put("sort", "");
                    objSort.add(objRow);
                }
                Collections.sort(objSort, new Comparator("sort"));
                int iCnt = 0;
                for (Map<String, Object> objRow : objSort) {
                    asValues[iCnt] = (String[]) objRow.get("value");
                    iCnt++;
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem sorting list " + e.getMessage());
        }
        return asValues;
    }

    /**
     * Sorts the elements of the list
     *
     * @param objList List of strings
     * @return Sorted list
     */
    public static List<String> sortList(List<String> objList) {
        try {
            if (!isBlank(objList)) {
                objList = new ArrayList<>(objList);
                Collections.sort(objList, String.CASE_INSENSITIVE_ORDER);
            }
        }
        catch (Exception e) {
            logger.error("Problem sorting list " + e.getMessage());
        }
        return objList;
    }

    /**
     * Sorts the elements of the list using the method name provided
     *
     * @param objList List of maps
     * @param method  Method to call to get comparator value
     * @return Sorted list
     */
    public static <t> List<t> sortListObjects(List<t> objList, String method) {
        try {
            if (!isBlank(objList)) {
                objList = new ArrayList<>(objList);
                Collections.sort(objList, new Comparator(objList.get(0).getClass(), method));
            }
        }
        catch (Exception e) {
            logger.error("Problem sorting list " + e.getMessage());
        }
        return objList;
    }

    /**
     * Sorts the elements of the list assuming they are actually a list of maps
     * and the second parameter is the map key to use for sorting
     * This is ideal for sorting lists of rows returned from Slaves
     *
     * @param objList List of maps
     * @param sKey    Key into map to get sort value
     * @return Sorted list
     */
    public static List<Map<String, String>> sortList(List<Map<String, String>> objList, String sKey) {
        try {
            if (!isBlank(objList)) {
                objList = new ArrayList<>(objList);
                Collections.sort(objList, new Comparator(sKey));
            }
        }
        catch (Exception e) {
            logger.error("Problem sorting list " + e.getMessage());
        }
        return objList;
    }

    /**
     * Returns a string that is suitable for using in a tag
     *
     * @param sValue Value to be encoded
     * @return Encoded string
     */
    public static String encodeHTML(String sValue) {

        if (sValue != null) {
            sValue = sValue.replaceAll("\n", "<br>").replaceAll("\\p{Cntrl}", " ").replaceAll("<br>", "\n");
            StringBuilder sResult = new StringBuilder();
            StringCharacterIterator objIterator = new StringCharacterIterator(sValue);
            char cChar = objIterator.current();
            while (cChar != CharacterIterator.DONE) {
                if (cChar == '<') {
                    sResult.append("&lt;");
                }
                else if (cChar == '>') {
                    sResult.append("&gt;");
                }
                else if (cChar == '&') {
                    sResult.append("&amp;");
                }
                else if (cChar == '\n') {
                    sResult.append("<br>");
                }
                else if (cChar == '"') {
                    sResult.append("&quot;");
                }
                else {
                    //the char is not a special one
                    //add it to the sResult as is
                    sResult.append(cChar);
                }
                cChar = objIterator.next();
            }
            return sResult.toString();
        }
        else {
            return null;
        }
    }

    /**
     * Returns a date object from parsing the time string which can be in any one
     * of the normal time formats
     *
     * @param sTime String form of the time
     * @return Date
     */
    public static Date parseDuration(String sTime) {

        String[] asPatterns = {"^([0-9]{1,2})[.: ]([0-9]{1,2})[.: ]([0-9]{1,2})(\\.([0-9]{1,3}))?$",   // 1     'H2:M2:S2.m3'
                "^([0-9]{1,2})[.:]([0-9]{1,2})(\\.([0-9]{1,3}))?$",                     // 2     'M2:S2.m3'
                "^([0-9]+)(\\.([0-9]*))?$"                                              // 3     'S2.m3'
        };

        // Check the date against the patterns

        Calendar objReturn = Calendar.getInstance();
        int iTmp;
        objReturn.set(Calendar.HOUR_OF_DAY, 0);
        objReturn.set(Calendar.MINUTE, 0);
        objReturn.set(Calendar.SECOND, 0);
        objReturn.set(Calendar.MILLISECOND, 0);
        if (sTime != null) {
            sTime = sTime.trim();
            for (int iCnt = 0; iCnt < asPatterns.length; iCnt++) {
                Matcher objMatcher = Pattern.compile(asPatterns[iCnt], Pattern.CASE_INSENSITIVE).matcher(sTime);
                if (objMatcher.find()) {
                    switch (iCnt + 1) {
                        case 1:
                            iTmp = parseNumber(objMatcher.group(3)).intValue();
                            iTmp += parseNumber(objMatcher.group(2)).intValue() * 60;
                            iTmp += parseNumber(objMatcher.group(1)).intValue() * 3600;
                            objReturn.set(Calendar.SECOND, iTmp);
                            if (objMatcher.group(5) != null) {
                                objReturn.set(Calendar.MILLISECOND, parseNumber(objMatcher.group(5)).intValue());
                            }
                            break;

                        case 2:
                            iTmp = parseNumber(objMatcher.group(2)).intValue();
                            iTmp += parseNumber(objMatcher.group(1)).intValue() * 60;
                            objReturn.set(Calendar.SECOND, iTmp);
                            if (objMatcher.group(4) != null) {
                                objReturn.set(Calendar.MILLISECOND, parseNumber(objMatcher.group(4)).intValue());
                            }
                            break;

                        case 3:
                            iTmp = parseNumber(objMatcher.group(1)).intValue();
                            objReturn.set(Calendar.SECOND, iTmp);
                            if (objMatcher.group(3) != null) {
                                objReturn.set(Calendar.MILLISECOND, parseNumber(objMatcher.group(3)).intValue());
                            }
                            break;

                        default:
                            break;
                    }
                }
            }
        }

        // Clear out the time part of the date

        return objReturn.getTime();
    }

    /**
     * Sometime, particularly in Applet development where you don't have control
     * of the amount of memory allocated the the JVM, it is necessary to try and
     * give the system a prod to tell it to relinquish dead resources.
     * This shouldn't be necessary but it often is.
     * It appears that if you group multiple calls the gc() together, the JVM
     * finally gets the message and actually does something.
     */
    public static void garbageCollect() {
        System.runFinalization();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
    }

    /**
     * Left justifies the text source to the maximum number of characters using
     * the specified line break separator
     *
     * @param sSource    String to justify
     * @param iWidth     Number of characters
     * @param sSeparator String to use as the delimiter
     * @return Modified string
     */
    public static String justifyLeft(String sSource, int iWidth, String sSeparator) {

        String sReturn = null;
        if (!isBlank(sSource)) {
            StringBuilder objBuffer = new StringBuilder(sSource);
            int iLastSpace = -1;
            int iLineStart = 0;
            int i = 0;

            while (i < objBuffer.length()) {
                if (objBuffer.charAt(i) == ' ') iLastSpace = i;
                if (objBuffer.charAt(i) == '\n') {
                    iLastSpace = -1;
                    iLineStart = i + 1;
                }
                if (i > iLineStart + iWidth - 1) {
                    if (iLastSpace != -1) {
                        objBuffer.setCharAt(iLastSpace, '\n');
                        iLineStart = iLastSpace + 1;
                        iLastSpace = -1;
                    }
                    else {
                        objBuffer.insert(i, '\n');
                        iLineStart = i + 1;
                    }
                }
                i++;
            }
            sReturn = objBuffer.toString();
            if (sSeparator != null) sReturn = sReturn.replaceAll(" *\n *", sSeparator);
        }
        return sReturn;
    }

    /**
     * Returns a duration object for the string version of duration
     *
     * @param sDuration Duration string
     * @return Duration
     */
    public static Duration getDuration(String sDuration) {
        return new Duration(parseDuration(sDuration));
    }

    /**
     * Returns a duration object for this time
     *
     * @param lMilliseconds Number of milliseconds
     * @return Duration
     */
    public static Duration getDuration(long lMilliseconds) {
        return new Duration(lMilliseconds);
    }

    /**
     * Returns a duration object for this time
     *
     * @param objDate Date to use
     * @return Duration
     */
    public static Duration getDuration(Date objDate) {
        return new Duration(objDate);
    }

    /**
     * Formats the number using the format string
     * This method uses the Java DecimalFormat class to do the formatting
     *
     * @param objNumber Number to format
     * @param sFormat   Format mask to use
     * @return String
     * @see DecimalFormat for the formatting rules
     */
    public static String formatNumber(Number objNumber, String sFormat) {

        if (objNumber == null) objNumber = 0;
        if (isBlank(sFormat)) {
            return objNumber.toString();
        }
        else {
            DecimalFormat objFormatter = new DecimalFormat(sFormat);
            return objFormatter.format(objNumber);
        }
    }

    /**
     * A holder for duration values
     * This class allows the manipulation of time strings/objects as though
     * they were actually durations
     */
    public static class Duration {

        private int miHours;
        private int miMinutes;
        private int miSeconds;
        private int miMilliseconds;

        /**
         * Prevent outside instantiation
         */
        private Duration() {
        }

        /**
         * Creates a duration object using a millisecond count
         *
         * @param lMilliseconds Number of milliseconds
         */
        protected Duration(long lMilliseconds) {
            miHours = (int) lMilliseconds / 3600000;
            lMilliseconds -= miHours * 3600000;
            miMinutes = (int) lMilliseconds / 60000;
            lMilliseconds -= miMinutes * 60000;
            miSeconds = (int) lMilliseconds / 1000;
            miMilliseconds = (int) lMilliseconds - miSeconds * 1000;
        }

        /**
         * Creates a duration object using a date
         *
         * @param objDate Date object to use
         */
        protected Duration(Date objDate) {
            if (objDate != null) {
                Calendar objCalendar = Calendar.getInstance();
                objCalendar.setTime(objDate);
                miHours = objCalendar.get(Calendar.HOUR_OF_DAY);
                miMinutes = objCalendar.get(Calendar.MINUTE);
                miSeconds = objCalendar.get(Calendar.SECOND);
                miMilliseconds = objCalendar.get(Calendar.MILLISECOND);
            }
        }

        /**
         * Creates a duration using the explicit values
         *
         * @param iHours        Number of hours
         * @param iMinutes      Number of minutes
         * @param iSeconds      Number of seconds
         * @param iMilliseconds Number of milliseconds
         */
        protected Duration(int iHours, int iMinutes, int iSeconds, int iMilliseconds) {
            miHours = iHours;
            miMinutes = iMinutes;
            miSeconds = iSeconds;
            miMilliseconds = iMilliseconds;
        }

        /**
         * Returns the number of hours
         *
         * @return int
         */
        public int getHours() {
            return miHours;
        }

        /**
         * Returns the number of hours
         *
         * @return int
         */
        public int getMilliseconds() {
            return miMilliseconds;
        }

        /**
         * Returns the number of hours
         *
         * @return int
         */
        public int getMinutes() {
            return miMinutes;
        }

        /**
         * Returns the number of seconds
         *
         * @return int
         */
        public int getSeconds() {
            return miSeconds;
        }

        /**
         * Returns the number of seconds in total
         *
         * @return float
         */
        public float getValue() {
            return miSeconds + miMinutes * 60 + miHours * 3600 + miMilliseconds / 1000;
        }

        /**
         * Returns the duration as a standard formatted string
         *
         * @return String version of the duration
         */
        public String toString() {
            return toString(false);
        }

        /**
         * Returns the duration as a standard formatted string
         *
         * @param bFull If true, shows milliseconds too
         * @return String version of the duration
         */
        public String toString(boolean bFull) {
            return formatNumber(miHours, "00") + ':' + formatNumber(miMinutes, "00") + ':' + formatNumber(miSeconds, "00") +
                    (bFull ? ':' + formatNumber(miMilliseconds, "000") : "");
        }
    }

    /**
     * A holder for temp files
     * This class allows the timed removal of temp files
     */
    public static class TempFile {

        private File file;
        private Calendar expiry;

        public TempFile(File file) {
            this.file = file;
            this.expiry = Calendar.getInstance();
        }

        public TempFile(File file, Calendar expiry) {
            this.file = file;
            this.expiry = expiry;
        }

        public TempFile(File file, int seconds) {
            this(file);
            setExpiry(seconds);
        }

        public void setFile(File file) {
            this.file = file;
        }

        public File getFile() {
            return this.file;
        }

        public void setExpiry(Calendar expiry) {
            this.expiry = expiry;
        }

        public void setExpiry(int seconds) {
            this.expiry.add(Calendar.SECOND, seconds);
        }

        public boolean hasExpired() {
            return Calendar.getInstance().after(this.expiry);
        }

        public void setExpired() {
            this.expiry = Calendar.getInstance();
        }

        public Calendar getExpiry() {
            return this.expiry;
        }
    }

    /**
     * Reads the contents of the file and returns them as a string
     *
     * @param filename File to read
     * @return Contents of the file
     */
    public static String readTextFile(File filename) {
        if (filename == null) {
            return null;
        }
        else {
            return readTextFile(filename.getAbsolutePath());
        }
    }

    /**
     * Reads the contents of the file and returns them as a string
     *
     * @param sFilename File to read
     * @return Contents of the file
     */
    public static String readTextFile(String sFilename) {
        String sReturn = null;
        StringBuilder sTmp = new StringBuilder();

        BufferedReader objIn = null;
        try {
            objIn = new BufferedReader(new InputStreamReader(new FileInputStream(sFilename), "UTF-8"), DEFAULT_BUFFER_SIZE);
            while ((sReturn = objIn.readLine()) != null) {
                if (sTmp.length() > 0) sTmp.append('\n');
                sTmp.append(sReturn);
            }
            sReturn = sTmp.toString();
        }
        catch (IOException e) {
            logger.error("Problem reading file " + e.getMessage());
        }
        finally {
            close(objIn);
        }
        return sReturn;
    }

    /**
     * Writes a string to the specified file
     *
     * @param sValue  String to write to the file
     * @param charset Character set to use
     */
    public static File writeTextToTempFile(String sValue, String charset) {
        File tmpFile = null;
        try {
            if (!isStringEmpty(sValue)) {
                tmpFile = new File(getTemporaryFilename());
                writeTextFile(tmpFile, new String(sValue.getBytes(charset), charset));
            }
        }
        catch (Exception e) {
            logger.error("Problem writing temp file " + e.getMessage());
        }
        return tmpFile;
    }

    /**
     * Writes a string to the specified file
     *
     * @param filename File to write to
     * @param sValue   String t write to the file
     */
    public static void writeTextFile(File filename, String sValue) {
        if (filename != null) writeTextFile(filename.getAbsolutePath(), sValue);
    }

    /**
     * Writes a string to the specified file
     *
     * @param sFilename File to write to
     * @param sValue    String t write to the file
     */
    public static void writeTextFile(String sFilename, String sValue) {
        writeTextFile(sFilename, sValue, null);
    }

    /**
     * Writes a string to the specified file
     *
     * @param sFilename   File to write to
     * @param sValue      String t write to the file
     * @param compression Compression algorithm to use
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void writeTextFile(String sFilename, String sValue, String compression) {
        Writer objOut = null;
        try {
            File objFile = new File(sFilename);
            if (objFile.exists()) objFile.delete();
            if (doStringsMatch(compression, "gzip") || doStringsMatch(compression, "compress")) {
                objOut = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(objFile)), DEFAULT_ENCODING));
            }
            else {
                objOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(objFile), DEFAULT_ENCODING));
            }
            objOut.write(sValue);
        }
        catch (Exception e) {
            logger.error("Problem writing file " + e.getMessage());
        }
        finally {
            close(objOut);
        }
    }

    /**
     * This function provides a safe way of getting the recipients from a message
     * header - the normal JavaMail API methods are prone to failure if the
     * TO and CC headers are not properly formed so we need to take care of it
     *
     * @param message       Message to interrogate
     * @param recipientType Type of recipient to get
     * @return Array of address objects
     */
    public static Address[] getRecipients(MimeMessage message, Message.RecipientType recipientType) {

        Address[] returnValue = null;
        try {

            // Try the normal way first

            try {
                returnValue = message.getRecipients(recipientType);
            }
            catch (AddressException e) {
                logger.debug("Malformed addresses in message - " + message.getSubject());
            }

            // If that didn't work, then get them manually

            if (returnValue == null) {
                String[] headers = null;
                if (recipientType.equals(Message.RecipientType.TO)) {
                    headers = message.getHeader("To");
                    if (headers == null) headers = message.getHeader("to");
                    if (headers == null) headers = message.getHeader("TO");
                }
                else if (recipientType.equals(Message.RecipientType.CC)) {
                    headers = message.getHeader("Cc");
                    if (headers == null) headers = message.getHeader("cc");
                    if (headers == null) headers = message.getHeader("CC");
                }
                else if (recipientType.equals(Message.RecipientType.BCC)) {
                    headers = message.getHeader("Bcc");
                    if (headers == null) headers = message.getHeader("bcc");
                    if (headers == null) headers = message.getHeader("BCC");
                }

                // If we got something, then form them together and remove dodgy characters

                if (headers != null) {
                    String header = join(headers, ",");
                    header = header.replaceAll(" *[,;]+ *", ",").replaceAll(",,*", ",").replaceAll("^,|,$", "");
                    returnValue = InternetAddress.parse(header);
                }
            }
        }
        catch (MessagingException e) {
            logger.warn("Problem determining message recipients");
        }
        return returnValue;
    }

    /**
     * Returns true if the folder implements the UID interface
     *
     * @param folder Folder to check
     * @return boolean True if the UID methods are available
     */
    public static boolean folderImplementsUID(Folder folder) {
        boolean returnValue = false;
        try {
            Class[] classes = folder.getClass().getInterfaces();
            if (classes != null) {
                for (int iCnt = 0; iCnt < classes.length && !returnValue; iCnt++) {
                    returnValue = classes[iCnt].getName().equalsIgnoreCase(UIDFolder.class.getName());
                }
            }
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /*
     *
     * Returns a message identifier that may or may not be unique
     *
     * @param objMessage Message to get identifier for
     *
     * @return String Identifier
     *
     */
    public static String getMessageID(MimeMessage message) {
        String returnValue = null;
        try {
            if (folderImplementsUID(message.getFolder())) {
                returnValue = String.valueOf(((UIDFolder) message.getFolder()).getUID(message));
            }
            else {
                returnValue = message.getMessageID();
            }
        }
        catch (Exception e) {
            logger.warn("Cannot retrieve message identifier - " + PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Check that the message retrieved using the message number is the correct
     * message by comparing the message ID.  If the message ID does not match loop
     * through the existing messages looking for the message ID and return a match.
     * <p/>
     * If there is no match return a null message object.
     *
     * @param messageToCheck MimeMessage to check
     * @param id             ID to check
     * @param folder         Folder to look for message
     * @return Either the original message object if the IDs, the message object that matches the ID
     * or null if no matching message is found.
     */
    protected static MimeMessage checkMessageID(MimeMessage messageToCheck, String id, Folder folder) {

        boolean found = false;

        logger.debug("Checking Message ID: " + id);
        try {

            // Check if the IDs match - if not look for the message
            if (!getMessageID(messageToCheck).equalsIgnoreCase(id)) {
                logger.debug("Checking Message ID does not match the test message ID: " + getMessageID(messageToCheck));

                // Lets have a sanity check here
                int messageCount = folder.getMessageCount();
                if (messageCount > 100) messageCount = 100;
                for (int i = 1; i <= messageCount && !found; i++) {
                    messageToCheck = (MimeMessage) folder.getMessage(i);

                    // If we find the message then set flag to true and
                    // leave the for loop
                    if (getMessageID(messageToCheck).equalsIgnoreCase(id)) {
                        logger.debug("Message ID found");
                        found = true;
                    }
                }

                // Check if the message is found, if not set the return message to null
                if (!found) {
                    // return a null object
                    logger.debug("Message ID not found");
                    messageToCheck = null;
                }
            }
        }
        catch (Exception e) {
            logger.error("Error checking the message ID - " + PivotalException.getErrorMessage(e));
        }
        return messageToCheck;
    }

    /**
     * Returns a mail message using the message ID and/or it's message number for
     * folders that don't support UID
     *
     * @param folder    Folder to get email from
     * @param messageID Identifier of message
     * @param messageNo Number of the message in the sequence
     * @return MimeMessage
     */
    public static MimeMessage getMessageFromFolder(Folder folder, String messageID, int messageNo) {
        MimeMessage objReturn = null;
        try {
            if (folder.exists() && folder.getMessageCount() > 0) {
                if (messageID != null && folderImplementsUID(folder)) {
                    objReturn = (MimeMessage) ((UIDFolder) folder).getMessageByUID(parseLong(messageID));
                }
                else {
                    objReturn = (MimeMessage) folder.getMessage(messageNo);
                    if (messageID != null) objReturn = checkMessageID(objReturn, messageID, folder);
                }
            }
        }
        catch (Exception e) {
            logger.warn("Cannot retrieve message from folder - Folder:" + folder.getName() + " ID:" + messageID + " No:" + messageNo + PivotalException.getErrorMessage(e));
        }
        return objReturn;
    }

    /**
     * Finds the text portion of the message and returns it
     * If there is an HTML part, then the text is tripped from this portion,
     * otherwise the text/plain part is returned if there is one
     *
     * @param objMessage MIME Message to interrogate
     * @return Text body
     */
    public static String getBodyText(MimeMessage objMessage) {

        String sReturn = "";
        boolean bPartIsText = false;

        try {
            // If we're a plain text message then return the content
            if (objMessage.isMimeType("text/plain")) {
                sReturn = readEncodedBodyText(objMessage);
                bPartIsText = true;
            }

            // If we're an HTML type then return the stripped part
            else if (objMessage.isMimeType("text/html")) {
                sReturn = (String) objMessage.getContent();
            }

            // A multipart - we're looking for the alternative part
            // this is stored as the first part of the main message normally
            else if (objMessage.isMimeType("multipart/*")) {
                BodyPart objHtmlPart = getBodyPartByMimetype((Multipart) objMessage.getContent(), "text/html");

                // See if we found something
                if (objHtmlPart != null) {
                    sReturn = (String) objHtmlPart.getContent();
                }
                else {
                    BodyPart objTextPart = getBodyPartByMimetype((Multipart) objMessage.getContent(), "text/plain");
                    if (objTextPart != null) {
                        sReturn = readEncodedBodyText(objTextPart);
                        bPartIsText = true;
                    }
                }
            }

            // Clean up the returned text
            if (bPartIsText) {
                sReturn = sReturn.replaceAll("\r", "");
                sReturn = sReturn.replaceAll("\n[ \t] *", "\n");
                while (sReturn.contains("\n\n\n")) {
                    sReturn = sReturn.replaceAll("\n\n\n", "\n\n");
                }
                sReturn = trim(sReturn, "\n");
            }
            else {
                sReturn = sReturn.replaceAll("(?i)&nbsp;", "\n");
                sReturn = sReturn.replaceAll("(?i)<br>", "\n");
                sReturn = sReturn.replaceAll("(?i)<p>", "\n\n");
                sReturn = sReturn.replaceAll("<[^>]*>", "");
                sReturn = sReturn.replaceAll("\r", "");
                sReturn = sReturn.replaceAll("\n[ \t] *", "\n");
                while (sReturn.contains("\n\n\n")) {
                    sReturn = sReturn.replaceAll("\n\n\n", "\n\n");
                }
                sReturn = trim(sReturn, "\n");
            }
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
        return replaceSmartChars(sReturn);
    }

    /**
     * Returns a string has all the MS "smart" characters replaced with their
     * normal ASCII equivalents
     *
     * @param sValue Value to be parsed
     * @return Cleaned string
     */
    public static String replaceSmartChars(String sValue) {
        String sReturn = sValue.replaceAll("\u0092|\u0091", "'").replaceAll("\u0093|\u0094", "\"").replaceAll("\u0096", "-");
        sReturn = sReturn.replaceAll("\u2018|\u2019", "'").replaceAll("\u202A|\u202B", "\"").replaceAll("\u2013", "-");

        sReturn = sReturn.replace((char) 8220, '"').replace((char) 8221, '"').replace((char) 8222, '"');
        sReturn = sReturn.replace((char) 8218, '\'').replace((char) 8216, '\'').replace((char) 8217, '\'');
        sReturn = sReturn.replace((char) 8211, '-').replace((char) 8212, '-');
        return sReturn;
    }

    /**
     * Reads the text content of a message part and returns it as an ISO-8859-1
     * encoded string.  Java works with Unicode and the JavaMail API seems to get
     * confused when ISO messages come through.  So we get the native version of
     * the text and then convert it to ISO
     *
     * @param objPart Part of a message to decode
     * @return String Returned string value
     */
    public static String readEncodedBodyText(Part objPart) {
        String sReturn = "";
        try {
            sReturn = (String) objPart.getContent();
            byte[] abTmp = sReturn.getBytes(getCharacterSet(objPart.getContentType()));
            sReturn = new String(abTmp, DEFAULT_ENCODING);
        }
        catch (Exception e) {
            logger.warn("Error decoding email body - " + PivotalException.getErrorMessage(e));
        }
        return sReturn;
    }

    /**
     * Returns the character set of the content type
     *
     * @param sContentType Content type to check
     * @return String Character set
     */
    private static String getCharacterSet(String sContentType) {
        String sCharset = "ISO-8859-1";
        if (sContentType != null) {
            try {
                if (sContentType.toLowerCase().contains("charset=")) {
                    sCharset = sContentType.toLowerCase().split("charset=")[1].split(";")[0];
                    sCharset = sCharset.replaceAll("[^0-9a-zA-Z-]", "").toUpperCase();
                }
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }
        return sCharset;
    }

    /**
     * Recurses through the whole message structure looking for a part that
     * has the specified mimetype
     *
     * @param objPart   Multipart object to look through
     * @param sMimetype Mimetype of the part to get
     * @return BodyPart of the found item or null if can't be found
     */
    public static MimeBodyPart getBodyPartByMimetype(Multipart objPart, String sMimetype) {

        MimeBodyPart objReturn = null;
        try {
            for (int iCnt = 0; iCnt < objPart.getCount() && objReturn == null; iCnt++) {
                MimeBodyPart objTmp = (MimeBodyPart) objPart.getBodyPart(iCnt);
                if (objTmp.isMimeType(sMimetype) && !stringContains(objTmp.getDisposition(), Part.ATTACHMENT)) {
                    objReturn = objTmp;
                }
                else if (objTmp.isMimeType("multipart/*")) {
                    objReturn = getBodyPartByMimetype((Multipart) objTmp.getContent(), sMimetype);
                }
            }
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
        return objReturn;
    }

    /**
     * Checks if a string contains another string using case sensitive/insensitive matching.
     *
     * @param sString        String to test against
     * @param sStringToFind  String to search for.
     * @param bCaseSensitive Perform Case sensitive search (True/False) .
     * @return True/False
     */
    public static boolean stringContains(String sString, String sStringToFind, boolean bCaseSensitive) {
        if (sString == null || sStringToFind == null) {
            return false;
        }
        else if (bCaseSensitive) {
            return sString.contains(sStringToFind);
        }
        else {
            return sString.toLowerCase().contains(sStringToFind.toLowerCase());
        }
    }

    /**
     * Returns a string having all words in the following format:
     * First Letter: capitalized
     * Remaining Letters: lower case
     * <p/>
     * It handles the following cases:
     * 1) either "Last Name, First Name" or "First Name Last Name"
     * 2) names like Dalla-Santa and O'Brien
     *
     * @param sString String to process
     * @return the string having the new format
     */
    public static String formatPersonName(String sString) {
        if (isBlank(sString)) {
            return "";
        }
        else {
            String ret = sString;
            if (sString.split(",").length == 2) {
                ret = sString.split(",")[1] + ' ' + sString.split(",")[0];
            }

            //handles names like Dalla-Santa
            ret = ret.replaceAll("-", " - ").replaceAll("\\s+", " ").trim().toLowerCase();
            //handles names like O'Hara
            ret = ret.replaceAll("'", " ' ").replaceAll("\\s+", " ").trim().toLowerCase();

            ret = WordUtils.capitalize(ret);
            ret = ret.replaceAll(" - ", "-");
            ret = ret.replaceAll(" ' ", "'");
            return ret;
        }
    }

    /*
     *
     * Checks if a string contains another string (case insensitive).
     *
     * @param sString String to test against
     * @param sStringToFind String to search for.
     *
     * @return True/False
     *
     */
    public static boolean stringContains(String sString, String sStringToFind) {
        return stringContains(sString, sStringToFind, false);
    }

    /**
     * Creates a string from an object
     *
     * @param value Object to serialize
     * @return Serialized form of object
     */
    public static String serialize(Serializable value) {

        // Create a byte stream to act as the repository for the object

        ByteArrayOutputStream store = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        String returnValue = null;
        try {

            // Create an object stream object to carry out the serializing

            stream = new ObjectOutputStream(store);

            // Write the object to the store

            stream.writeObject(value);

            // Convert the store to a recoverable string

            returnValue = Base64.encodeBytes(store.toByteArray());
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
        finally {
            close(stream, store);
        }

        // Clean up after ourselves

        return returnValue;
    }

    /**
     * Re-creates an object from the supplied string - expects the object to have
     * been previously serialized using Serialize
     *
     * @param value Serialized string to de-serialize
     * @return Object
     */
    @SuppressWarnings("unchecked")
    public static <t> t deserialize(String value) {

        // Convert the string to an array of bytes

        t returnValue = null;
        ObjectInputStream stream = null;
        try {

            // We expect the string to be in Base64

            byte[] bytes = Base64.decode(value, Base64.NO_OPTIONS);

            // Create a byte stream of this array

            ByteArrayInputStream store = new ByteArrayInputStream(bytes);

            // Create an object input stream to read the bytes

            stream = new ObjectInputStream(store);
            returnValue = (t) stream.readObject();
        }
        catch (Exception e) {
            logger.error("Cannot deserialise object - " + e.getMessage() + " - " + e.getClass().toString());
        }
        finally {
            close(stream);
        }

        // Return object

        return returnValue;
    }

    /**
     * Returns the filename body of the given full path filename
     * This takes care of any DOS or UNIX variants
     *
     * @param sName Full path name of the file
     * @return Filename minus the path and extension
     */
    public static String getFilename(String sName) {
        String sReturn = sName;

        // If there is something to do

        if (sName != null) {
            int iTmp = sName.replace('\\', '/').lastIndexOf('/');
            if (iTmp >= 0) sReturn = sReturn.substring(iTmp + 1);
        }
        return sReturn;
    }

    /**
     * Returns the filename body of the given full path filename
     * This takes care of any DOS or UNIX variants
     *
     * @param sName Full path name of the file
     * @return Filename minus the path and extension
     */
    public static String getFilenameBody(String sName) {
        String sReturn = sName;

        // If there is something to do

        if (!isBlank(sName)) {
            sReturn = getFilename(sName);
            if (sReturn.indexOf('.') > 0) sReturn = sReturn.substring(0, sReturn.lastIndexOf('.'));
        }
        return sReturn;
    }

    /**
     * Returns the filename extension of the given full path filename
     * This takes care of any DOS or UNIX variants
     *
     * @param sName Full path name of the file
     * @return Extension of the file
     */
    public static String getFilenameExtension(String sName) {
        String sReturn = null;

        // If there is something to do

        if (sName != null) {
            String sTmp = getFilename(sName);
            if (sTmp.indexOf('.') > 0 && sTmp.indexOf('.') < sTmp.length() - 1) {
                sReturn = sTmp.substring(sTmp.lastIndexOf('.') + 1);
            }
        }
        return sReturn;
    }

    /**
     * Returns the HTML from the supplied version without all the Script and event
     * handlers and any other troublesome markup
     *
     * @param sHtml Stream to clean
     * @return sText returned cleaned HTML
     */
    public static String getCleanHtml(String sHtml) {

        String sReturn = sHtml;

        // Remove all the script parts

        sReturn = Pattern.compile("<script.*?</script>", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");

        // Now all the event handlers

        sReturn = Pattern.compile("on[a-z_]+ *= *\"[^\"]*\"", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");
        sReturn = Pattern.compile("on[a-z_]+ *= *'[^']*'", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");

        // Get rid of any hrefs

        sReturn = Pattern.compile("href *= *\"[^\"]*\"", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");
        sReturn = Pattern.compile("href *= *'[^']*'", Pattern.CASE_INSENSITIVE & Pattern.DOTALL).matcher(sReturn).replaceAll("");

        return sReturn;
    }

    /**
     * Creates an MD5 digest value for a given string
     *
     * @param sValue Value to create digest from
     * @return String (128 character string)
     */
    public static String getMD5String(String sValue) {

        return getMD5String(sValue, true);
    }

    /**
     * Creates an MD5 digest value for a given string
     *
     * @param sValue        Value to create digest from
     * @param base64Encoded True if the code should be base64 encoded
     * @return String (128 character string)
     */
    public static String getMD5String(String sValue, boolean base64Encoded) {

        // Convert the string into an array of bytes

        String sReturn = null;
        if (!isBlank(sValue)) {
            try {
                byte[] abBytes = sValue.getBytes(DEFAULT_ENCODING);

                // Calculate the digest value and get it back as a string

                if (base64Encoded) sReturn = Base64.encodeBytes(DigestUtils.md5Digest(abBytes));
                else sReturn = DigestUtils.md5DigestAsHex(abBytes);
            }
            catch (Exception e) {
                logger.error("Problem encoding string - " + PivotalException.getErrorMessage(e));
            }
        }

        return sReturn;
    }

//    /**
//     * Creates an CRC32 value for a given string
//     *
//     * @param sValue Value to create checksum from
//     * @return String (128 character string)
//     */
//    public static String getCRC32String(String sValue) {
//
//        // Convert the string into an array of bytes
//
//        String sReturn = null;
//        if (!isBlank(sValue)) {
//            try {
//                byte[] abBytes = sValue.getBytes(DEFAULT_ENCODING);
//
//                // Calculate the digest value and get it back as a string
//
//                CRC32 crc = new CRC32();
//                crc.update(abBytes);
//                sReturn = crc.getValue() + "";
//            }
//            catch (Exception e) {
//                logger.error("Problem encoding string - " + PivotalException.getErrorMessage(e));
//            }
//        }
//
//        return sReturn;
//    }

    /**
     * Decodes the URL and returns a sensible string
     * If the value is null then "" is returned - this makes it possible to use
     * this function in-line without constantly having to wrap the encoding inside
     * try catch blocks
     *
     * @param sValue Value to encode
     * @return Encoded string
     */
    public static String decodeURL(String sValue) {
        return decodeURL(sValue, System.getProperty(ENCODING, DEFAULT_ENCODING));
    }

    /**
     * Decodes the URL and returns a sensible string
     * If the value is null then "" is returned - this makes it possible to use
     * this function in-line without constantly having to wrap the encoding inside
     * try catch blocks
     *
     * @param sValue   Value to encode
     * @param encoding Encoding character set to use
     * @return Encoded string
     */
    public static String decodeURL(String sValue, String encoding) {
        String sReturn = "";
        if (!isBlank(sValue)) {
            try {
                sReturn = URLDecoder.decode(sValue, encoding);

                // For some reason, when used with AJAX, the + characters
                // are further converted to their hex entity - to be on the safe
                // side, it's better to convert these to hex space entities here

                sReturn = sReturn.replaceAll("\\+", "%20");
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }
        return sReturn;
    }

    /**
     * Encodes the URL and returns a sensible string
     * If the value is null then "" is returned - this makes it possible to use
     * this function in-line without constantly having to wrap the encoding inside
     * try catch blocks
     *
     * @param sValue Value to encode
     * @return Encoded string
     */
    public static String encodeURL(String sValue) {
        return encodeURL(sValue, System.getProperty(ENCODING, DEFAULT_ENCODING));
    }

    /**
     * Encodes the URL and returns a sensible string
     * If the value is null then "" is returned - this makes it possible to use
     * this function in-line without constantly having to wrap the encoding inside
     * try catch blocks
     *
     * @param sValue   Value to encode
     * @param encoding Encoding character set to use
     * @return Encoded string
     */
    public static String encodeURL(String sValue, String encoding) {
        String sReturn = "";
        if (!isBlank(sValue)) {
            try {
                sReturn = URLEncoder.encode(sValue, encoding);

                // For some reason, when used with AJAX, the + characters
                // are further converted to their hex entity - to be on the safe
                // side, it's better to convert these to hex space entities here

                sReturn = sReturn.replaceAll("\\+", "%20");
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }
        return sReturn;
    }

    /**
     * Encodes the value as a base64 string
     *
     * @param sValue Value to encode
     * @return Encoded string
     */
    public static String encodeBase64(String sValue) {
        String sReturn = "";
        if (!isBlank(sValue)) {
            try {
                sReturn = Base64.encodeBytes(sValue.getBytes(DEFAULT_ENCODING));
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }
        return sReturn;
    }

    /**
     * Decodes the value from a base64 string
     *
     * @param sValue Value to encode
     * @return Decoded string
     */
    public static String decodeBase64(String sValue) {
        String sReturn = "";
        if (!isBlank(sValue)) {
            try {
                sReturn = new String(Base64.decode(sValue.getBytes(DEFAULT_ENCODING)));
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }
        return sReturn;
    }

    /**
     * Throws a new Exception. It's useful for aborting the processing
     * of a Velocity script.
     *
     * @param message The message of the Exception
     */
    public static void throwException(String message) {
        throw new PivotalException(message);
    }

    /**
     * General purpose timestamp. The initial motivation was to have a timestamp
     * string to append to file names.
     *
     * @return current timestamp in the format yyyy.MM.dd.HH.mm.ss
     */
    public static String getFormattedTimestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        return formatter.format(new Date());
    }

    /**
     * Returns the current date time in a time stamp object
     *
     * @return timestamp
     */
    public static Timestamp getTimestamp() {
        return getTimestamp(null);

    }

    /**
     * Returns the date in a time stamp object
     *
     * @return timestamp
     */
    public static Timestamp getTimestamp(Date date) {

        if (date == null)
            return new Timestamp(new Date().getTime());
        else
            return new Timestamp(date.getTime());
    }


    /**
     * Return the current date and time. Motivation was to use in the reports
     *
     * @return current Date object with current date and time
     */
    public static Date getDateTime() {
        return new Date();
    }

    /**
     * Returns the tail of the file as a string
     *
     * @param fileObj File to read
     * @param length  Length of the tail
     * @return Tail of the file
     */
    public static String getTail(File fileObj, int length) {

        RandomAccessFile objFile = null;
        StringWriter objOut = new StringWriter();
        try {
            objFile = new RandomAccessFile(fileObj, "r");
            if (length < objFile.length()) objFile.seek(objFile.length() - length);
            String sLine = objFile.readLine();
            boolean first = true;
            while (sLine != null) {
                if (!first) objOut.write(sLine + '\n');
                first = false;
                sLine = objFile.readLine();
            }
        }
        catch (IOException e1) {
            throw new PivotalException(e1);
        }
        finally {
            close(objFile, objOut);
        }
        return objOut.toString();
    }

    /**
     * Sleep for a period of time
     *
     * @param milliseconds Number of milliseconds to sleep
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Parse the string with '-' character and concatenate the parsed string
     *
     * @param sval String to parse
     * @return concatenated string
     */
    public static String parseString(String sval) {
        if (sval != null && sval.indexOf('-') > 0) {
            sval = sval.substring(0, sval.indexOf('-')).trim() + " - " + sval.substring(sval.indexOf('-') + 1, sval.length()).trim();
        }
        return sval;
    }

    /**
     * Copies a file from one filename to another
     * If the filename is a directory, then the destination is expected to be a
     * directory also.  In this scenario, all the files (including subdirectories)
     * within the source directory are copied recursively to the destination
     *
     * @param sFromFileName File to copy
     * @param sToFileName   Filename and location of destination
     * @throws IOException Error if there is a problem with the copy
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void copyFile(String sFromFileName, String sToFileName) throws IOException {

        File objFrom = new File(sFromFileName);
        File objTo = new File(sToFileName);

        // If the source doesn't exist

        if (!objFrom.exists()) {
            throw new IOException("copyFile: no such source file: " + sFromFileName);
        }

        // If we can't read the source

        if (!objFrom.canRead()) {
            throw new IOException("copyFile: source file is unreadable: " + sFromFileName);
        }

        // If the the source is a folder but the destination is a normal file

        if (objTo.exists() && objFrom.isDirectory() && !objTo.isDirectory()) {
            throw new IOException("copyFile: source is a directory and cannot overwrite destination file: " + sFromFileName + " to:" + sToFileName);
        }

        // If the destination is a directory but the source is a file then create
        // an empty file within the destination directory with the same name

        if (objTo.exists() && !objFrom.isDirectory() && objTo.isDirectory()) objTo = new File(objTo, objFrom.getName());

        // If the destination doesn't exist and the source is a folder and we can't
        // create the destination folders

        if (!objTo.exists()) {
            boolean cannotCreateDirectories = false;

            // If the source is a folder then create the destination as a folder

            if (objFrom.isDirectory()) cannotCreateDirectories = !objTo.mkdirs();

                // If the source is a file, then create the parentage for the destination file

            else if (!objTo.getParentFile().exists()) cannotCreateDirectories = !objTo.getParentFile().mkdirs();

            // If the folders failed to be created

            if (cannotCreateDirectories) {
                throw new IOException("copyFile: cannot create destination directory: " + sFromFileName + " to:" + sToFileName);
            }
        }

        // If the destination folder is not writable

        if (objTo.isDirectory() && !objTo.canWrite()) {
            throw new IOException("copyFile: destination file is unwriteable: " + sToFileName);
        }

        // If destination is a sub-folder of the source

        if (objTo.getAbsolutePath().startsWith(objFrom.getAbsolutePath())) {
            throw new IOException("copyFile: cannot copy overlapping files/directories: " + sFromFileName + " to:" + sToFileName);
        }

        // If the source is a file

        if (!objFrom.isDirectory()) {

            // Only copy if we have to

            if (!objTo.exists() || objFrom.length() != objTo.length() || objFrom.lastModified() != objTo.lastModified()) {

                // Copy the file from one place to the other

                FileInputStream objInput = null;
                FileOutputStream objOutput = null;
                try {
                    objInput = new FileInputStream(objFrom);
                    objOutput = new FileOutputStream(objTo);
                    pipeInputToOutputStream(objInput, objOutput, false, true);
                }
                finally {
                    close(objInput, objOutput);

                    // Harmonise the last modified timestamps

                    if (objTo.exists()) objTo.setLastModified(objFrom.lastModified());
                }
            }
        }
        else if (objFrom.isDirectory()) {

            // Loop through all the contents of the From directory

            String[] asFiles = objFrom.list();
            for (String sFile : asFiles) {
                copyFile(objFrom.getAbsolutePath() + File.separator + sFile, objTo.getAbsolutePath() + File.separator + sFile);
            }
        }
    }

    /**
     * Returns a string representation of the map in a JSON type syntax
     *
     * @param values Map of values
     * @return String
     */
    public static String getMapAsString(Map<String, Object> values) {
        return getMapAsString(values, false);
    }

    /**
     * Returns a string representation of the map in a JSON type syntax
     *
     * @param values Map of values
     * @return String
     */
    public static String getMapAsString(Map<String, Object> values, boolean encloseKeys) {
        String returnValue = "";
        if (!isBlank(values)) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (!isBlank(returnValue)) returnValue += ',';
                returnValue += '\"' + entry.getKey() + "\":";
                if (entry.getValue() == null) {
                    returnValue += "null";
                }
                else {
                    if (entry.getValue() instanceof Map<?, ?>)
                        returnValue += getMapAsString((Map<String, Object>) entry.getValue(), encloseKeys);
                    else returnValue += '\"' + entry.getValue().toString().replaceAll("\"", "\\\\") + '\"';
                }
            }
        }
        return '{' + returnValue + '}';
    }

    /**
     * Returns a string representation of the list of objects in a JSON type syntax
     *
     * @param values List of Objects
     * @return String
     */
    public static String getListAsString(List<Map<String, Object>> values, boolean encloseKeys) {
        String returnValue = "";
        if (!isBlank(values)) {
            for (Map<String, Object> curr : values) {
                if (!isBlank(returnValue)) returnValue += ',';
                returnValue += getMapAsString(curr, encloseKeys);
            }
        }
        return '[' + returnValue + ']';
    }

    /**
     * Deletes all files and sub-directories under dir
     * If a deletion fails, the method stops attempting to delete
     *
     * @param dir Directory or file to delete
     */
    public static void deleteDir(String dir) {
        deleteDir(new File(dir));
    }

    /**
     * Deletes all files and sub-directories under dir
     * If a deletion fails, the method stops attempting to delete
     *
     * @param dir Directory or file to delete
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteDir(File dir) {
        if (dir != null) {

            // Recurse down the subdirectories

            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    deleteDir(new File(dir, aChildren));
                }
            }

            // The directory/file is now empty so delete it

            dir.delete();
        }
    }

    /**
     * This method will evaluate an XPath expression from an XML document.
     * Both parameters should be String and the return will NodeList that matches the expression.
     * <p/>
     * The typical usage is like this:
     * <code>NodeList nodes = evaluateXPath(expression, message); </code>
     * <code>for (int i = 0, n = nodes.getLength(); i < n; i++) { </code>
     * <code>Node node = nodes.item(i);  </code>
     * <code>    NamedNodeMap attributes = node.getAttributes();  </code>
     * <code>    System.out.println(attributes.getNamedItem("name").getNodeValue());  </code>
     * <code>    System.out.println(attributes.getNamedItem("id").getNodeValue()); </code>
     * <code>}
     *
     * @param expression the XPath expression to be evaluated
     * @param xml        the XML document
     * @return NodeList that matches the expression
     */
    public static NodeList evaluateXPath(String expression, String xml) {

        NodeList nodes = null;

        if (!isBlank(expression) && !isBlank(xml)) {
            try {
                XPath xpath = XPathFactory.newInstance().newXPath();
                nodes = (NodeList) xpath.evaluate(expression, new InputSource(new StringReader(xml)), XPathConstants.NODESET);
            }
            catch (Exception e) {
                logger.error("Unable to parse XPath expression", e);
            }
        }

        return nodes;
    }

    /**
     * Lists all the files within a directory return File objects
     *
     * @param sDir Directory name to search
     * @return List of files
     */
    public static List<File> listFiles(String sDir) {

        return listFiles(new File(sDir), null);
    }

    /**
     * Lists all the files within a directory return File objects
     *
     * @param objDir Directory to search
     * @return List of files
     */
    public static List<File> listFiles(File objDir) {

        return listFiles(objDir, null);
    }

    /**
     * Lists all the files within a directory returning File objects
     * All filenames must match the sFilePattern or if null, all files are
     * returned
     *
     * @param sDir         Directory name to search
     * @param sFilePattern Regex pattern of matching filenames
     * @return List of files
     */
    public static List<File> listFiles(String sDir, String sFilePattern) {
        return listFiles(new File(sDir), sFilePattern);
    }

    /**
     * Lists all the files within a directory returning File objects
     * All filenames must match the sFilePattern or if null, all files are
     * returned
     *
     * @param sDir           Directory name to search
     * @param sFilePattern   Regex pattern of matching filenames
     * @param includeFolders True if folder objects should be included
     * @return List of files
     */
    public static List<File> listFiles(String sDir, String sFilePattern, boolean includeFolders) {
        return listFiles(new File(sDir), sFilePattern, includeFolders, true);
    }

    /**
     * Lists all the files within a directory returning File objects
     * All filenames must match the sFilePattern or if null, all files are
     * returned
     *
     * @param sDir           Directory name to search
     * @param sFilePattern   Regex pattern of matching filenames
     * @param includeFolders True if folder objects should be included
     * @param searchFolders  True if the folder should be searched to find matching files
     * @return List of files
     */
    public static List<File> listFiles(String sDir, String sFilePattern, boolean includeFolders, boolean searchFolders) {
        return listFiles(new File(sDir), sFilePattern, includeFolders, searchFolders);
    }

    /**
     * Lists all the files within a directory returning File objects
     * All filenames must match the sFilePattern or if null, all files are
     * returned
     *
     * @param objDir       Directory to search
     * @param sFilePattern Regex pattern of matching filenames
     * @return List of files
     */
    public static List<File> listFiles(File objDir, String sFilePattern) {
        return listFiles(objDir, sFilePattern, false, true);
    }

    /**
     * Lists all the files within a directory returning File objects
     * All filenames must match the sFilePattern or if null, all files are
     * returned
     *
     * @param objDir         Directory to search
     * @param sFilePattern   Regex pattern of matching filenames
     * @param includeFolders True if folder objects should be included
     * @param searchFolders  True if the folder should be searched to find matching files
     * @return List of files
     */
    public static List<File> listFiles(File objDir, String sFilePattern, boolean includeFolders, boolean searchFolders) {

        // Check for any contents

        List<File> objReturn = null;
        if (objDir != null && objDir.exists() && objDir.isDirectory()) {
            File[] asChildren = objDir.listFiles();
            if (asChildren != null && asChildren.length > 0) {
                for (File objFile : asChildren) {

                    // If the filename matches the pattern or there is no pattern then
                    // add it to the return list

                    if ((sFilePattern == null || objFile.getName().matches(sFilePattern)) && (!objFile.isDirectory() || includeFolders)) {
                        if (objReturn == null) objReturn = new ArrayList<>();
                        objReturn.add(objFile);
                    }

                    // If the file is a directory then recurs down the tree

                    if (objFile.isDirectory() && searchFolders) {
                        List<File> objTmp = listFiles(objFile, sFilePattern);
                        if (objTmp != null) {
                            if (objReturn == null) objReturn = new ArrayList<>();
                            objReturn.addAll(objTmp);
                        }
                    }
                }
            }
        }
        return objReturn;
    }

    /**
     * Call String static format function
     *
     * @param bindedString Template String
     * @param args         List of arguments
     * @return Template string with the binds replaced by the values passed in the remaining args
     */
    public static String formatString(String bindedString, Object... args) {
        return String.format(bindedString, args);
    }

    /**
     * Generates a Guid String
     *
     * @return Guid String
     */
    public static String generateGUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns a list of string arrays from the CSV input
     * Expects the CSV to be well formed using the normal delimiters
     *
     * @param csvContent Comma separated values
     * @return List of rows
     */
    public static List<String[]> readCSV(String csvContent) {
        List<String[]> returnValue = null;

        // Check for the obvious

        if (!isBlank(csvContent)) {

            // Create a reader and read all the values

            CSVReader reader = new CSVReader(new StringReader(csvContent));
            try {
                returnValue = reader.readAll();
            }
            catch (Exception e) {
                logger.error("Problem reading CSV string - " + PivotalException.getErrorMessage(e));
            }
            finally {
                close(reader);
            }
        }

        return returnValue;
    }

    /**
     * Returns an object from parsing the JSON string
     *
     * @param jsonText Comma separated values
     * @return Java Object
     */
    public static Object readJSON(String jsonText) {
        return JsonMapper.parseJson(jsonText);
    }

    /**
     * Returns a DomHelper object from parsing the XML string
     *
     * @param xmlText Comma separated values
     * @return Java Object
     */
    public static DomHelper readXML(String xmlText) {
        try {
            return new DomHelper(xmlText);
        }
        catch (Exception e) {
            logger.error("Cannor parse XML - " + PivotalException.getErrorMessage(e));
            return null;
        }
    }

    /**
     * Returns an underscored string as a camel humped string
     * e.g. steve_o_hara becomes SteveOHara
     * The string is also reduced to lowercase except the humps
     *
     * @param value Underscore separated sting of words
     * @return Camel humped string
     */
    public static String getCamelHumpString(String value) {
        return getCamelHumpString(value, false);
    }

    /**
     * Returns an underscored string as a camel humped string
     * e.g. steve_o_hara becomes SteveOHara
     * The string is also reduced to lowercase except the humps
     *
     * @param value        Underscore separated sting of words
     * @param notFirstWord True if the first word is not camel humped
     * @return Camel humped string
     */
    public static String getCamelHumpString(String value, boolean notFirstWord) {
        return getCamelHumpString(value, "[_ ]+", notFirstWord);
    }

    /**
     * Returns an underscored string as a camel humped string
     * e.g. steve_o_hara becomes SteveOHara
     * The string is also reduced to lowercase except the humps
     *
     * @param value         Separated string of words
     * @param wordSeperator The seperator(s) between the words
     * @param notFirstWord  True if the first word is not camel humped
     * @return Camel humped string
     */
    public static String getCamelHumpString(String value, String wordSeperator, boolean notFirstWord) {
        return getCamelHumpString(value, wordSeperator, "", notFirstWord);
    }

    /**
     * Returns an underscored string as a camel humped string
     * e.g. steve_o_hara becomes SteveOHara or Steve O Hara depending upon the returnWordSeperator
     * The string is also reduced to lowercase except the humps
     *
     * @param value               Separated string of words
     * @param wordSeperator       The seperator(s) between the words
     * @param returnWordSeperator The seperator between the words in the returned string
     * @param notFirstWord        True if the first word is not camel humped
     * @return Camel humped string
     */
    public static String getCamelHumpString(String value, String wordSeperator, String returnWordSeperator, boolean notFirstWord) {

        String returnValue = value;
        if (!isBlank(value)) {
            List<String> words = splitToList(value.trim().toLowerCase(), wordSeperator);
            int start = notFirstWord ? 1 : 0;
            for (int i = start; i < words.size(); i++) {
                words.set(i, WordUtils.capitalize(words.get(i)));
            }
            returnValue = join(words, returnWordSeperator);
        }
        return returnValue;
    }

    /**
     * Returns the contents of the file in a byte array
     *
     * @param file File to read
     * @return Byte array
     * @throws IOException An error
     */
    public static byte[] readBinaryFile(File file) throws IOException {
        InputStream is = null;

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.

        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large " + file.getName());
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes

        int offset = 0;
        int numRead;

        try {
            is = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        }

        // Close the input stream and return bytes

        finally {
            close(is);
        }
        return bytes;
    }


    /**
     * Scales an image
     *
     * @param sbi     image to scale
     * @param dWidth  width of destination image
     * @param dHeight height of destination image
     * @return scaled image
     */
    public static BufferedImage scaleImage(BufferedImage sbi, int dWidth, int dHeight) {
        return getScaledImage(sbi, dWidth, dHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img           the original image to be scaled
     * @param targetWidth   the desired width of the scaled instance,
     *                      in pixels
     * @param targetHeight  the desired height of the scaled instance,
     *                      in pixels
     * @param hint          one of the rendering hints that corresponds to
     *                      {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *                      {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *                      {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *                      {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *                      scaling technique that provides higher quality than the usual
     *                      one-step technique (only useful in downscaling cases, where
     *                      {@code targetWidth} or {@code targetHeight} is
     *                      smaller than the original dimensions, and generally only when
     *                      the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledImage(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();

            // Check to see if we are actually scaling up!
            // In this case, we cannot do it in steps because it won't
            // add any benefit so just set the starting point to the
            // target

            if (w < targetWidth) {
                w = targetWidth;
            }
            if (h < targetHeight) {
                h = targetHeight;
            }
        }
        else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * Method to call org.apache.common.net class SubnetUtils to check if an IP address is contained on a subnet
     * range is given in Subnetwork's CIDR notation  eg 192.168.1.1/24
     *
     * @param range     the range of the Subnet
     * @param ipAddress the IP to check in subnet range
     * @return an inRange with the response
     * @deprecated Use IpInSubnetRange
     */
    public static boolean isInSubnetRange(String range, String ipAddress) {
        return IpInSubnetRange(ipAddress, range);
    }

    /**
     * Method to call org.apache.common.net class SubnetUtils to check if an IP address is contained on multiple subnet ranges.
     * Each range is given in Subnetwork's CIDR notation  eg 192.168.1.1/24
     *
     * @param range     the range of the Subnet , String [] of subnet ranges
     * @param ipAddress the IP to check in subnet range
     * @return an inRange with the response
     * @deprecated Use IpInSubnetRange
     */
    public static boolean isInSubnetRange(String[] range, String ipAddress) {
        return IpInSubnetRange(ipAddress, range);
    }

    /**
     * Method to call org.apache.common.net class SubnetUtils to check if an IP address is contained on a subnet
     * range is given in Subnetwork's CIDR notation  eg 192.168.1.1/24
     *
     * @param ipAddress the IP to check in subnet range
     * @param range     the range of the Subnet (semi-colon separated list of addresses)
     * @return an inRange with the response
     */
    public static boolean IpInSubnetRange(String ipAddress, String range) {
        boolean inRange = false;
        if (!isBlank(range) && !isBlank(ipAddress)) inRange = IpInSubnetRange(ipAddress, range.split(" *; *"));
        return inRange;
    }

    /**
     * Method to call org.apache.common.net class SubnetUtils to check if an IP address is contained on multiple subnet ranges.
     * Each range is given in Subnetwork's CIDR notation  eg 192.168.1.1/24
     *
     * @param ranges    the range of the Subnet , String [] of subnet ranges
     * @param ipAddress the IP to check in subnet range
     * @return an inRange with the response
     */
    public static boolean IpInSubnetRange(String ipAddress, String... ranges) {
        boolean inRange = false;
        SubnetUtils subnet;
        try {
            if (!isBlank(ranges) && !isBlank(ipAddress)) {
                for (String range : ranges) {
                    logger.debug("Checking IP Addrress " + ipAddress + " is withing subnet range " + range);
                    subnet = new SubnetUtils(range.trim());
                    if (subnet.getInfo().isInRange(ipAddress.trim())) {
                        inRange = true;
                        break;
                    }
                }
            }
            logger.debug("Is IP in range -" + inRange);

        }
        catch (Exception e) {
            logger.error("Problem while checking IP subnet range ", e);
        }

        return inRange;
    }

    /**
     * Gets the non-private IP address from the X header value
     *
     * @param s X Header containing forwarded value
     * @return IP Address
     */
    public static String findNonPrivateIpAddress(String s) {
        Pattern ipAddressPattern = Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");
        Pattern privateIpAddressPattern = Pattern.compile("(^127\\.0\\.0\\.1)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)");
        Matcher matcher = ipAddressPattern.matcher(s);
        while (matcher.find()) {
            if (!privateIpAddressPattern.matcher(matcher.group(0)).find()) return matcher.group(0);
            matcher.region(matcher.end(), s.length());
        }
        return null;
    }

    /**
     * Returns the tme difference between now and the date specified
     *
     * @param date Stat date - if null, returns 0
     * @return Time difference in milliseconds
     */
    public static int getTimeDifference(Date date) {
        if (date == null) return 0;
        else return (int) (new Date().getTime() - date.getTime());
    }

    /**
     * Escapes the characters that are significant t abormal SQL LIKE statement
     *
     * @param criteria String to escape
     * @return Escaped string
     */
    public static String encodeLike(String criteria) {
        if (!isBlank(criteria)) return criteria.replaceAll("'", "''").replaceAll("%", "\\\\%").replaceAll("_", "\\\\_");
        else return criteria;
    }

    /**
     * Returns the mime type of the filename
     *
     * @param filename Filename to use
     * @return The mime type string
     */
    public static String getMimeType(String filename) {
        return FileTypeMap.getDefaultFileTypeMap().getContentType(filename);
    }

    /**
     * Returns the mime type of the file
     *
     * @param file File to use
     * @return The mime type string
     */
    public static String getMimeType(File file) {
        return FileTypeMap.getDefaultFileTypeMap().getContentType(file);
    }

    /**
     * Escapes the characters in the passed in string using XML entities.
     * <p/>
     * For example: "break" & "butter" becomes &quot;bread&quot; &amp; &quot;butter&quot;.
     * <p/>
     * Supports only the five basic XML entities (gt,lt,quot,amp,apos). Does not support DTDs or external entities.
     * <p/>
     * Note that unicode characters greater than 0x7f are currently escaped to their numerical \\u equivalent. This may change in future releases.
     *
     * @param str The string to escape, may be null.
     * @return A new escaped String, null if null string input.
     */
    public static String escapeXml(String str) {
        String escaped = str;
        if (!Common.isBlank(escaped)) {
            escaped = StringEscapeUtils.escapeXml(str);
        }
        return escaped;
    }

    /**
     * Reads all the input from a socket as text and returns when there is nothing left
     *
     * @param socket Socket to read
     * @return String of the received content
     * @throws IOException Error if there is a problem
     */
    public static String readAll(Socket socket) throws IOException {
        return readAll(socket, false);
    }

    /**
     * Reads all the input from a socket as text and returns when there is nothing left
     *
     * @param socket        Socket to read
     * @param ignoreTimeout if true, any read timeouts are ignored
     * @return String of the received content
     * @throws IOException Error if there is a problem
     */
    public static String readAll(Socket socket, boolean ignoreTimeout) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()), DEFAULT_BUFFER_SIZE);
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        catch (SocketTimeoutException e) {
            if (!ignoreTimeout)
                logger.warn("Socket timed out reading from [%s] on port [%d] after [%d] seconds and [%d] characters", socket.getRemoteSocketAddress(), socket.getLocalPort(), socket.getSoTimeout() / 1000, sb.length());
        }
        return sb.toString();
    }

    /**
     * Returns true if nothing is currently using the port
     *
     * @param port Port number
     * @return True if port is available
     */
    public static boolean isPortAvailable(int port) {

        try {
            ServerSocket srv = new ServerSocket(port);
            close(srv);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Uses the XStream library to serialise any object into an XML stream
     *
     * @param object Object to serialise
     * @return XML rendition
     */
    public static String toXML(Object object) {
        XStream converter = new XStream();
        return converter.toXML(object);
    }


    /**
     * Closes the objects quietly - just saves having to repeat
     * the try/catch blocks everywhere when we don't really care
     * if it fails or not
     * It uses reflection to test if there is a close() method for
     * each of the objects and if there is, invokes it
     *
     * @param objects Streams to close
     * @return True if it worked OK
     */
    public static boolean close(Object... objects) {
        boolean returnValue = true;
        if (!isBlank(objects)) {
            for (Object object : objects) {
                if (object != null) {

                    try {
                        // Try it the fastest way first

                        if (OutputStream.class.isAssignableFrom(object.getClass())) ((OutputStream) object).close();
                        else if (InputStream.class.isAssignableFrom(object.getClass())) ((InputStream) object).close();
                        else if (Writer.class.isAssignableFrom(object.getClass())) ((Writer) object).close();
                        else if (Reader.class.isAssignableFrom(object.getClass())) ((Reader) object).close();
                        else if (ResultSet.class.isAssignableFrom(object.getClass())) ((ResultSet) object).close();
                        else if (Statement.class.isAssignableFrom(object.getClass())) ((Statement) object).close();
                        else if (InitialContext.class.isAssignableFrom(object.getClass()))
                            ((InitialContext) object).close();
                        else if (Connection.class.isAssignableFrom(object.getClass())) ((Connection) object).close();
                        else if (RandomAccessFile.class.isAssignableFrom(object.getClass()))
                            ((RandomAccessFile) object).close();
                        else if (Socket.class.isAssignableFrom(object.getClass())) ((Socket) object).close();
                        else if (java.net.ServerSocket.class.isAssignableFrom(object.getClass()))
                            ((java.net.ServerSocket) object).close();

                            // Old school reflection

                        else {
                            logger.debug("Invoking close method using reflection - add [%s] to the list of checks in Common.close()", object.getClass().getName());
                            Method method;
                            method = object.getClass().getMethod("close");
                            method.invoke(object);
                        }
                    }
                    catch (NoSuchMethodException e) {
                        logger.warn("Object doesn't have a close method");
                    }
                    catch (IllegalAccessException e) {
                        logger.warn("Problem getting close method - " + PivotalException.getErrorMessage(e, PivotalException.getStackTrace(e)));
                    }
                    catch (Exception e) {
                        logger.debug("Problem closing stream - " + PivotalException.getErrorMessage(e));
                        returnValue = false;
                    }
                }
            }
        }
        return returnValue;
    }

    /**
     * Finds the first matching string using the regular expression and returns them
     * as a list
     *
     * @param content Content to search
     * @param regexp  Regular expression to use
     * @return Matching string or null if no matches found
     */
    public static String findFirst(String content, String regexp) {
        List<String> allMatches = find(content, regexp);
        return isBlank(allMatches) ? null : allMatches.get(0);
    }

    /**
     * Finds all the matching strings using the regular expression and returns them
     * as a list
     *
     * @param content Content to search
     * @param regexp  Regular expression to use
     * @return List of strings or null if no matches found
     */
    public static List<String> find(String content, String regexp) {
        List<String> allMatches = null;
        if (!Common.isBlank(content) && !Common.isBlank(regexp)) {
            try {
                Matcher m = Pattern.compile(regexp).matcher(content);
                while (m.find()) {
                    if (allMatches == null) allMatches = new ArrayList<>();
                    allMatches.add(m.group(m.groupCount()));
                }
            }
            catch (Exception e) {
                logger.error("problem searching content - " + PivotalException.getErrorMessage(e));
            }
        }
        return allMatches;
    }

    /**
     * Attempts to read the data from a serialized file
     *
     * @param dataFile File to read data from
     * @return True if the data could be read from file
     * @throws Exception Occurs if the file cannot written
     */
    public static <t> t deserializeFromFile(File dataFile) throws Exception {
        return deserializeFromFile(dataFile, false);
    }

    /**
     * Attempts to read the data from a serialized file
     *
     * @param dataFile File to read data from
     * @param isGzip   True if the file is compressed
     * @return True if the data could be read from file
     * @throws Exception Occurs if the file cannot written
     */
    @SuppressWarnings("unchecked")
    public static <t> t deserializeFromFile(File dataFile, boolean isGzip) throws Exception {
        t returnValue = null;
        if (!isBlank(dataFile)) {
            InputStream fileIn = null;
            ObjectInputStream in = null;
            try {
                if (isGzip)
                    fileIn = new GZIPInputStream(new BufferedInputStream(new FileInputStream(dataFile), 0x7FFF));
                else fileIn = new BufferedInputStream(new FileInputStream(dataFile), DEFAULT_BUFFER_SIZE);
                in = new ObjectInputStream(fileIn);
                returnValue = (t) in.readObject();
            }
            finally {
                Common.close(fileIn, in);
            }
        }
        return returnValue;
    }

    /**
     * Writes the data to a serialized file
     *
     * @param data     Arbitrary data object
     * @param dataFile File to save data to
     * @throws Exception Occurs if the file cannot read
     */
    public static void serializeToFile(Serializable data, File dataFile) throws Exception {
        serializeToFile(data, dataFile, false);
    }

    /**
     * Writes the data to a serialized file
     *
     * @param data     Arbitrary data object
     * @param dataFile File to save data to
     * @param gzip     True if the output should be compressed
     * @throws Exception Occurs if the file cannot read
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void serializeToFile(Serializable data, File dataFile, boolean gzip) throws Exception {
        if (!isBlank(dataFile)) dataFile.delete();
        OutputStream fileOut = null;
        ObjectOutputStream out = null;
        try {
            if (!Common.isBlank(data)) {
                if (gzip)
                    fileOut = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(dataFile)), DEFAULT_BUFFER_SIZE);
                else fileOut = new BufferedOutputStream(new FileOutputStream(dataFile), DEFAULT_BUFFER_SIZE);
                out = new ObjectOutputStream(fileOut);
                out.writeObject(data);
            }
        }
        finally {
            Common.close(fileOut, out);
        }
    }

    // The list of java character mappings (it could be one regex if I knew how to match the $2 to the correct character)
    public static final Map<String, Character> escapedCharacterConversionMap = new HashMap<String, Character>() {
        {
            put("(\\\\)+(n)", '\n');
            put("(\\\\)+(t)", '\t');
            put("(\\\\)+(b)", '\b');
            put("(\\\\)+(r)", '\r');
            put("(\\\\)+(f)", '\f');
            put("(\\\\)+(\")", '"');
            put("(\\\\)+(')", '\'');
        }

        private static final long serialVersionUID = 4700795007809504935L;
    };

    /**
     * Will filter the provided string to convert the escaped values back to their character escaped equivalents.
     * It uses the default escaped character conversion map.
     *
     * @param original The original string to convert
     * @return The filtered string with the original characters or the provided string if values are empty
     */
    public static String replaceEscapedCharacters(String original) {
        return replaceWithPatterns(original, escapedCharacterConversionMap);
    }

    /**
     * Will filter the provided string to convert the escaped values back to their character escaped equivalents.
     *
     * @param original               The original string to convert
     * @param characterConversionMap The map containing the escaped string to character encodings
     * @return The filtered string with the original characters or the provided string if values are empty
     */
    public static String replaceWithPatterns(String original, Map<String, Character> characterConversionMap) {
        if (Common.isBlank(original) || characterConversionMap == null) return original;

        // We need to filter this to remove any java escaped values
        // This will reduce all escapes down to the original - such as //n
        // We do not need to filter the " and ' as these characters are already
        // correct (as they are characters, whereas /n is both '/' and 'n' - we want it to be '/n'

        String filtered = original;

        // Now the actual bytes have to be corrected

        for (Map.Entry<String, Character> entry : characterConversionMap.entrySet()) {
            filtered = filtered.replaceAll(entry.getKey(), entry.getValue().toString());
        }

        return filtered;
    }

    /**
     * Returns the day number of the date provided
     *
     * @param date Date to get day number from
     * @return Day number of the year
     */
    public static int getDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * The single place that this deprecated feature is called from within the application
     *
     * @param thread Thread to stop
     */
    @SuppressWarnings("deprecation")
    public static void stopThread(Thread thread) {
        if (thread != null) {
            thread.stop();
        }
    }

    /**
     * Returns a selector that is suitble for use by JQuery based on the name
     *
     * @param name Name of the input
     * @return String suitable as a JQuery selector
     */
    public static String getJQuerySelector(String name) {
        return getJQuerySelector(name, null);
    }

    /**
     * Returns a selector that is suitable for use by JQuery based on the name and the index
     *
     * @param name  Name of the input
     * @param index Index (optional) of the input
     * @return String suitable as a JQuery selector
     */
    public static String getJQuerySelector(String name, Number index) {
        if (Common.isBlank(name)) return name;
        else return name.replaceAll("[.]", "\\\\\\\\.") + (index == null ? "" : index);
    }

    /**
     * Useful function for Velocity to be able to serialise a map
     * into a querystring
     *
     * @param values Map of values
     * @return Query string
     */
    public static String getQueryString(Map<String, Object> values) {
        String returnValue = null;
        if (!isBlank(values)) {
            for (Map.Entry entry : values.entrySet()) {
                if (!isBlank(entry.getKey())) {
                    returnValue = (returnValue == null ? "" : returnValue + "&") + entry.getKey();
                    if (!isBlank(entry.getValue())) {
                        returnValue += "=" + encodeURL(entry.getValue() + "");
                    }
                }
            }
        }
        return returnValue;
    }


    /**
     * Convert a Calendar unit (e.g. <code>Calendar.SECOND</code>, <code>Calendar.MINUTE</code>, etc...) into a string
     * (e.g. seconds, minutes), looking it up in the I18n class.
     *
     * @param unit   Calendar unit.
     * @param plural Should the return value be pluralised ("seconds" instead of "second")?
     * @return The string
     */
    public static String getDateUnit(int unit, boolean plural) {

        String key;

        if (unit == Calendar.MILLISECOND) {
            key = "date.units.millisecond";
        }
        else if (unit == Calendar.SECOND) {
            key = "date.units.second";
        }
        else if (unit == Calendar.MINUTE) {
            key = "date.units.minute";
        }
        else if (unit == Calendar.HOUR || unit == Calendar.HOUR_OF_DAY) {
            key = "date.units.hour";
        }
        else if (unit == Calendar.DATE) {
            key = "date.units.day";
        }
        else {
            return "";
        }

        if (plural) {
            key += "s";
        }

        return key;
    }

    /**
     * <p>Gets a formatted {@link java.lang.String} date from a given {@link java.sql.Timestamp} and a date format.</p>
     * <p>If timestamp is null, the current timestamp will be used. If the format is null, "dd-MM-yyy" will be used.</p>
     *
     * @param timestamp the given {@link java.sql.Timestamp}
     * @param format    the {@link java.lang.String} date format
     * @return {@link java.lang.String} representing the date
     */
    public static String getDateAsString(Timestamp timestamp, String format) {

        if (Common.isBlank(format)) {
            format = "dd-MM-yyyy";
        }

        if (Common.isBlank(timestamp)) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }

        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(new Date(timestamp.getTime()));
    }

    /**
     * This will give the number of the steps need to ignore in the chart
     *
     * @param size in size of the list
     *
     * @return int value how much chart value need to ignore
     */
    public static int getSteps(int size) {
        int step;
        if (size > 500) {
            step = 40;
        }
        else if (size > 100) {
            step = 20;
        }
        else if (size > 60) {
            step = 10;
        }
        else if (size > 20) {
            step = 5;
        }
        else {
            step = 1;

        }
        return step;
    }

    /**
     * Cleans strings in a parameter map to get rid of injected css and html
     * To prevent XSS
     *
     * @param userData Map to be cleaned
     *
     * @return new map containing cleaned data
     */
    public static Map<String, Object> cleanUserData(Map<String, Object>userData) {

        Map<String, Object>cleanData = new HashMap<>();

        for(String keyValue : userData.keySet()) {
            if (userData.get(keyValue) instanceof String)
                cleanData.put(keyValue, cleanUserData((String)userData.get(keyValue)));
            else if (userData.get(keyValue) instanceof String[]) {
                List<String> newData = new ArrayList<>();
                for(String thisValue : (String[]) userData.get(keyValue))
                    newData.add(cleanUserData(thisValue));

                cleanData.put(keyValue, newData.toArray());
            }
            else
                cleanData.put(keyValue, userData.get(keyValue));
        }

        return cleanData;
    }

    public static Map<String, String[]> getCleanParameterMap(HttpServletRequest request) {

        Map<String, String[]>parameterMap = new HashMap<>();

        if (request != null) {

            parameterMap = request.getParameterMap();
            for(String keyValue : parameterMap.keySet()) {
                String[] thisValue = parameterMap.get(keyValue);
                for(int index=0; index<thisValue.length; index++)
                    thisValue[index]=cleanUserData(thisValue[index]);

                parameterMap.put(keyValue, thisValue);
            }
        }

        return parameterMap;
    }

    /**
     * Cleans the passed string of css and html
     * to prevent XSS
     *
     * @param userValue Value to be cleaned
     *
     * @return Cleaned string
     */
    public static String cleanUserData(String userValue) {

        if (isBlank(userValue))
            return userValue;
        else
            return Jsoup.clean(userValue, Whitelist.simpleText());
    }
}
