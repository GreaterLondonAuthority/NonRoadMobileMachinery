/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.browser;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.apache.commons.exec.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to provide server side web browser facilities
 * Primarily, it uses the rather fantastic PhantonJS native library that
 * is a wrapper around a headless Webkit browser engine
 */
public class Browser {

    public static final String INTERNAL_REQUEST_HEADER = "internal-request";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Browser.class);
    private static final String PHANTOMJS_EXE_NAME = "phantomjs";
    public static final String JQUERY_JS_RESOURCE_NAME = "javascript/jquery/jquery-1.11.1.min.js";

    private static File phantomJSPath = null;


    /**
     * Attempts to find and copy the PhantomJS executable for this architecture
     * @throws PivotalException If the EXE cannot be found or copied
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    synchronized private static void getPhantomEXE() throws PivotalException {
        if (Common.isBlank(phantomJSPath)) {

            //Work out what the constituent path will be

            String osName = System.getProperty("os.name");
            String exeName = PHANTOMJS_EXE_NAME;
            if (osName.matches("(?is)windows.*")) {
                osName = "windows";
                exeName+=".exe";
            }
            else if (osName.matches("(?is)mac.*"))
                osName = "macosx";
            else
                osName = "linux";

            // Make sure we have a temporary directory to put the executable in

            File temp = new File(Common.getTemporaryDirectory(), PHANTOMJS_EXE_NAME);
            if (!temp.exists()) temp.mkdirs();

            // Check to see if we already have the EXE available

            File exeFile = new File(temp, exeName);
            if (!exeFile.exists()) {

                // Copy the exe to the temporary folder

                String resourceName = String.format("/com/pivotal/phantomjs/native/%s/%s", osName, exeName);
                try (InputStream in = Browser.class.getResourceAsStream(resourceName)) {
                    if (in==null)
                        throw new PivotalException("Cannot find resource [%s]", resourceName);
                    Common.pipeInputToOutputStream(in, exeFile, false);

                    // Set the correct privileges

                    exeFile.setWritable(true, true);
                    exeFile.setReadable(true, false);
                    exeFile.setExecutable(true, false);
                    phantomJSPath = exeFile;
                }
                catch (Exception e) {
                    throw new PivotalException("Cannot find executable for [phantomjs]");
                }
            }
            else
                phantomJSPath = exeFile;
        }
    }

    /**
     * Convenience method that exports a single file from the browser using the specified
     * parameters.
     * If more than one export of the same URL is required, it is more efficient to use the
     * export(url, configs) version of the method with multiple configurations
     * @param url URL to use
     * @param format Export format of the output
     * @param width Width in pixels of the browser window
     * @param height Height in pixels of the browser window
     * @param zoom Zoom factor to use
     * @return A temporary file containing the export - should be deleted by the caller when they are finished with it
     */
    public static File export(String url, com.pivotal.utils.browser.ExportFormat.Format format, int width, int height, double zoom) throws PivotalException {
        Configuration config = new Configuration();
        config.setUrl(url);
        com.pivotal.utils.browser.ExportFormat exportFormat = new com.pivotal.utils.browser.ExportFormat();
        exportFormat.setViewportSize(new com.pivotal.utils.browser.ViewPortSize(width, height));
        exportFormat.setZoomFactor(zoom);
        exportFormat.setFormat(format);
        List<File> files = export(config, exportFormat);
        return Common.isBlank(files)?null:files.get(0);
    }

    /**
     * Exports the page specified by the URL
     * @param config Configuration of the browser to use
     * @param formats Array of formats to export
     * @return List of exported files
     * @throws PivotalException If the parameters are wrong or the export fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static List<File> export(Configuration config, com.pivotal.utils.browser.ExportFormat... formats) throws PivotalException {

        // Check for lunacy

        checkParameters(config, true, formats);

        // Create the browser configuration

        List<File> returnValue = new ArrayList<>();
        StringBuilder command = new StringBuilder(config.getJSON(formats[0]));

        // Add in all the conversions we want to do
        // These are done in a cascade

        for (int i=0; i<formats.length; i++) {
            com.pivotal.utils.browser.ExportFormat format = formats[i];
            command.append(format.getJSON());

            // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

            File tmpFile = Common.getTemporaryFile(format.getFormatFilenameExtension());
            returnValue.add(tmpFile);
            tmpFile.deleteOnExit();
            String padding = Common.padLeft("", " ", i*8);

            // If we want the HTML then we need to do something a little different

            if (format.getFormat().equals(com.pivotal.utils.browser.ExportFormat.Format.HTML))
                command.append(String.format("%spage.open('%s', function() {\n" +
                                             "%s    setTimeout(function() {\n" +
                                             "%s        fs.write('%s', page.content, 'w');\n",
                                             padding,config.getUrl(),
                                             padding,
                                             padding,tmpFile.getAbsolutePath().replace("\\", "\\\\")));
            else
                command.append(String.format("%spage.open('%s', function() {\n" +
                                             "%s    setTimeout(function() {\n" +
                                             "%s        page.render('%s');\n",
                                             padding,config.getUrl(),
                                             padding,
                                             padding,tmpFile.getAbsolutePath().replace("\\", "\\\\")));
        }

        // Add on th exits

        for (int i=formats.length-1; i>-1; i--) {
            String padding = Common.padLeft("", " ", i*8);
            if (i==formats.length-1) {
                command.append(String.format("%s        phantom.exit()\n",padding));
            }
            command.append(String.format("%s    },%d);\n" +
                                         "%s});\n",
                                         padding, config.getSettleTimeout(), padding));
        }

        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            commandFile.delete();
        }
        return scaleImages(returnValue, formats);
    }

    /**
     * Exports the arbitrary HTML content specified by the content parameter
     * @param content html content
     * @param config Configuration of the browser to use
     * @param formats Array of formats to export
     * @return List of exported files
     * @throws PivotalException If the parameters are wrong or the export fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static List<File> export(String content, Configuration config, com.pivotal.utils.browser.ExportFormat... formats) throws PivotalException {

        // Check for lunacy

        checkParameters(config, false, formats);

        // Save the content to a local file

        File htmlFile = Common.getTemporaryFile("html");
        Common.writeTextFile(htmlFile, content);

        // Create the browser configuration

        List<File> returnValue = new ArrayList<>();
        StringBuilder command = new StringBuilder(config.getJSON(formats[0]));

        // Add in all the conversions we want to do

        for (int i=0; i<formats.length; i++) {
            com.pivotal.utils.browser.ExportFormat format = formats[i];
            command.append(format.getJSON());

            // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

            File tmpFile = Common.getTemporaryFile(format.getFormatFilenameExtension());
            returnValue.add(tmpFile);
            tmpFile.deleteOnExit();
            command.append(String.format("try {\n" +
                            "    f = page.open('%s', function() {\n" +
                            "        setTimeout(function() {\n" +
                            "            page.render('%s');\n" +
                            (i==formats.length-1?"            phantom.exit();\n":"") +
                            "        },%d);\n" +
                            "    });\n" +
                            "}\n" +
                            "catch (e) {\n" +
                            "    console.log(e);\n" +
                            "    phantom.exit();\n" +
                            "}\n",
                            htmlFile.getAbsolutePath().replace("\\", "\\\\"),
                            tmpFile.getAbsolutePath().replace("\\", "\\\\"),
                            config.getSettleTimeout()));
        }

        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            htmlFile.delete();
            commandFile.delete();
        }
        return scaleImages(returnValue, formats);
    }

    /**
     * Checks that the parameters are OK
     *
     * @param config  Configuration of the browser
     * @param checkUrl True if the URL should be checked
     * @param formats Formats to export
     * @throws PivotalException Errors if broken
     */
    private static void checkParameters(Configuration config, boolean checkUrl, com.pivotal.utils.browser.ExportFormat... formats) throws PivotalException {
        getPhantomEXE();
        if (config==null)
            throw new PivotalException("No configuration supplied");
        if (checkUrl && Common.isBlank(config.getUrl()))
            throw new PivotalException("No URL provided for browser");
        if (Common.isBlank(formats))
            throw new PivotalException("No configuration for URL export [%s]", config.getUrl());
        if (config.getSettleTimeout()<0)
            throw new PivotalException("Cannot have a settle time less than zero [%d]", config.getSettleTimeout());
        if (config.getSettleTimeout()*formats.length>config.getTimeout())
            throw new PivotalException("Cannot have a total settle time greater than the execution timeout [%d < %d]", config.getSettleTimeout()*formats.length, config.getTimeout());

        // Add the special header

        config.addCustomHeader(INTERNAL_REQUEST_HEADER, INTERNAL_REQUEST_HEADER);
    }

    /**
     * Convenience method that exports a single file from the browser using the specified
     * parameters
     * @param elementId ID of the element to export
     * @param url URL to use
     * @param format Export format of the output
     * @return A temporary file containing the export - should be deleted by the caller when they are finished with it
     * @throws Exception if problem getting file
     */
    public static File exportElement(String elementId, String url, com.pivotal.utils.browser.ExportFormat.Format format) throws Exception {
        Configuration config = new Configuration();
        config.setUrl(url);
        com.pivotal.utils.browser.ExportFormat exportFormat = new com.pivotal.utils.browser.ExportFormat();
        exportFormat.setFormat(format);
        return exportElement(elementId, config, exportFormat);
    }

    /**
     * Exports the element from the page
     * @param elementId ID of the element to export
     * @param config Configuration of the browser to use
     * @param format Format to export
     * @return List of exported files
     * @throws Exception If the parameters are wrong or the export fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File exportElement(String elementId, Configuration config, com.pivotal.utils.browser.ExportFormat format) throws Exception {

        // Check for lunacy

        checkParameters(config, true, format);

        // Create the browser configuration

        StringBuilder command = new StringBuilder(config.getJSON(format));

        // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

        File tmpFile = Common.getTemporaryFile(format.getFormatFilenameExtension());
        tmpFile.deleteOnExit();

        command.append(String.format("\npage.open('%s', function() {\n" +
                        "    setTimeout(function() {\n" +
                        "        page.content = page.evaluate(serializeId, '%s');\n" +
                        "        page.render('%s');\n" +
                        "        phantom.exit();\n" +
                        "    }, %d);\n" +
                        "});\n",
                config.getUrl(), elementId, tmpFile.getAbsolutePath().replace("\\", "\\\\"), config.getSettleTimeout()));

        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            commandFile.delete();
        }
        return scaleImage(tmpFile, format);
    }

    /**
     * Convenience method that exports all the tags of the specified name to a list of temporary files
     * The list is in the same order that the tags were found in the DOM
     * @param tagName Name of the tags to export
     * @param url URL to use
     * @param format Export format of the output
     * @return A temporary file containing the export - should be deleted by the caller when they are finished with it
     * @throws Exception if problem
     */
    public static List<File> exportTag(String tagName, String url, com.pivotal.utils.browser.ExportFormat.Format format) throws Exception {
        Configuration config = new Configuration();
        config.setUrl(url);
        com.pivotal.utils.browser.ExportFormat exportFormat = new com.pivotal.utils.browser.ExportFormat();
        exportFormat.setFormat(format);
        return exportTag(tagName, config, exportFormat);
    }

    /**
     * Exports all the tags of the specified name to a list of temporary files
     * The list is in the same order that the tags were found in the DOM
     * @param tagName Name of the tags to export
     * @param config Configuration of the browser to use
     * @param format Format to export
     * @return List of exported files
     * @throws Exception If the parameters are wrong or the export fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static List<File> exportTag(String tagName, Configuration config, com.pivotal.utils.browser.ExportFormat format) throws Exception {

        // Check for lunacy

        checkParameters(config, true, format);

        // Create the browser configuration

        StringBuilder command = new StringBuilder(config.getJSON(format));

        // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

        String tmpFileBody = Common.getTemporaryFilename().replaceFirst(".tmp$", "");

        // Open the page and the extract all tags and fix any XMLNS issues

        command.append(String.format("\npage.open('%s', function() {\n" +
                "    setTimeout(function() {\n" +
                "        var images = page.evaluate(serializeTags, '%s');\n" +
                "        if (images.length>0) {\n" +
                "            var imageStack = [];\n" +
                "            for (var i=0; i<images.length; i++) {\n" +
                "                var image = images[i];\n" +
                "                image.content = image.content.replace(/\\shref\\s*=/ig, ' xlink:href=');\n" +
                "                image.content = image.content.replace(/(href|<img\\s+.*src)=(['\"])\\s*\\//ig, '$1=$2%s/');\n" +
                "                image.content = image.content.replace(/(href|<img\\s+.*src)=(['\"])\\s*(?!http|\\/)/ig, '$1=$2%s');\n" +
                "                if (image.content.indexOf('xmlns:xlink') < 0) {\n" +
                "                    image.content = image.content.replace(/(xmlns *= *[\"'][^\"']+[\"'])/i, '$1 xmlns:xlink=\"http://www.w3.org/1999/xlink\"');\n" +
                "                }\n" +
                "                image.fileName = '%s-' + i + '.%s';\n" +
                "                imageStack.push(image);\n" +
                "            }\n" +
                "            renderNextImage(imageStack);\n" +
                "        }\n" +
                "        else {\n" +
                "            phantom.exit();\n" +
                "        }\n" +
                "    }, %d);\n" +
                "});\n",
                config.getUrl(), tagName, config.getUrlHost(), config.getUrlHost(true), tmpFileBody.replace("\\", "\\\\"), format.getFormatFilenameExtension(), config.getSettleTimeout()));

        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            commandFile.delete();
        }

        // Return any of the extracted files

        List<File> returnValue = Common.listFiles(Common.getTemporaryDirectory(), Common.getFilename(tmpFileBody) + "-[0-9]+\\." + format.getFormatFilenameExtension(), false, false);
        return scaleImages(returnValue, format);
    }

    /**
     * Convenience method that exports a single html file from the browser using the specified
     * parameters
     * @param url URL to use
     * @param format Format of any SVG elements
     * @return List of exported files (first one is the HTML page)
     * @throws Exception If the parameters are wrong or the export fails
     */
    public static List<File> exportHtml(String url, com.pivotal.utils.browser.ExportFormat.Format format) throws Exception {
        Configuration config = new Configuration();
        config.setUrl(url);
        com.pivotal.utils.browser.ExportFormat exportFormat = new com.pivotal.utils.browser.ExportFormat();
        exportFormat.setFormat(format);
        return exportHtml(config, exportFormat);
    }

    /**
     * Exports the page as a standalone HTML page with all SVG
     * converted to images of the form defined in the format
     * The list is in the same order that the tags were found in the DOM
     * @param config Configuration of the browser to use
     * @param format Format to export
     * @return List of exported files (first one is the HTML page)
     * @throws Exception If the parameters are wrong or the export fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static List<File> exportHtml(Configuration config, com.pivotal.utils.browser.ExportFormat format) throws Exception {

        // Check for lunacy

        checkParameters(config, true, format);

        // Create the browser configuration

        StringBuilder command = new StringBuilder(config.getJSON(format));

        // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

        String tmpFileBody = Common.getTemporaryFilename().replaceFirst(".tmp$", "");

        command.append(String.format("\npage.open('%s', function() {\n" +
                        "    setTimeout(function() {\n" +
                        "        var content = page.content;\n" +
                        "        content = content.replace(/(href|<img\\s+.*src)=(['\"])\\s*\\//ig, '$1=$2%s/');\n" +
                        "        content = content.replace(/(href|<img\\s+.*src)=(['\"])\\s*(?!http|\\/)/ig, '$1=$2%s');\n" +
                        "        var fileBody = '%s';\n" +
                        "        var imageType = '%s';\n" +
                        "        var images = page.evaluate(serializeSVG, 'svg', fileBody, imageType);\n" +
                        "        var imageStack=[];\n" +
                        "        if (images.length>0) {\n" +
                        "            for (var i=0; i<images.length; i++) {\n" +
                        "                imageStack.push(images[i]);\n" +
                        "                content = content.replace(/<svg.+<\\/svg>/i,images[i].src);\n" +
                        "            }\n" +
                        "        }\n" +
                        "        fs.write(fileBody + '.html', content, 'w');\n" +
                        "        renderNextImage(imageStack);\n" +
                        "    }, %d);\n" +
                        "});\n",
                config.getUrl(), config.getUrlHost(), config.getUrlHost(true), tmpFileBody.replace("\\", "\\\\"), format.getFormatFilenameExtension(), config.getSettleTimeout()
        ));

        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            commandFile.delete();
        }

        // Return any of the extracted files

        List<File> returnValue = Common.listFiles(Common.getTemporaryDirectory(), Common.getFilename(tmpFileBody) + "\\.html", false, false);
        if (!Common.isBlank(returnValue)) {
            List<File> imageFiles = Common.listFiles(Common.getTemporaryDirectory(), Common.getFilename(tmpFileBody) + "-[0-9]+\\." + format.getFormatFilenameExtension(), false, false);
            if (!Common.isBlank(imageFiles)) returnValue.addAll(imageFiles);
        }
        return returnValue;
    }

    /**
     * Convenience method that returns the HTML content of the element given by the JQuery selector or null
     * if nothing found
     * @param jquerySelector JQuery selector (must escape " characters)
     * @param url URL to use
     * @return String of HTML content or null if not found
     * @throws Exception Error if the config is incorrect or operation fails
     */
    public static String getContent(String jquerySelector, String url) throws Exception {
        Configuration config = new Configuration();
        config.setUrl(url);
        return getContent(jquerySelector, config);
    }

    /**
     * Returns the HTML content of the element given by the JQuery selector or null
     * if nothing found
     * @param jquerySelector JQuery selector (must escape " characters)
     * @param config Configuration of the browser to use
     * @return String of HTML content or null if not found
     * @throws Exception Error if the config is incorrect or operation fails
     */
    public static String getContent(String jquerySelector, Configuration config) throws Exception {

        String returnValue = null;
        com.pivotal.utils.browser.ExportFormat format = new com.pivotal.utils.browser.ExportFormat();

        // Check for lunacy

        checkParameters(config, true, format);

        // Create the browser configuration

        StringBuilder command = new StringBuilder(config.getJSON(format));

        // Create a temporary file that will get cleaned up on stop if the caller doesn't do it

        File tmpFile = Common.getTemporaryFile("html");

        // Open the page, inject th JQuery and extract the content of the selector
        command.append(String.format("\npage.open('%s', function(status) {\n" +
                        "    if (status != 'success') {\n" +
                        "        console.log('Cannot access resource');\n" +
                        "    }\n" +
                        "    else {\n" +
                        "        setTimeout(function() {\n" +
                        "            page.includeJs(\"%s\", function() {\n" +
                        "                var content = page.evaluate(function() {\n" +
                        "                    var serializer = new XMLSerializer();\n" +
                        "                    var tmp = $(\"%s\");\n" +
                        "                    if (tmp.length>0)\n" +
                        "                        return serializer.serializeToString(tmp[0]);\n" +
                        "                    else\n" +
                        "                        return null;\n" +
                        "                });\n" +
                        "                fs.write('%s', content, 'w');\n" +
                        "                phantom.exit()\n" +
                        "            });\n" +
                        "        }, %d);\n" +
                        "    };\n" +
                        "});\n",
                config.getUrl(), ClassLoader.getSystemResource(JQUERY_JS_RESOURCE_NAME) != null ? ClassLoader.getSystemResource(JQUERY_JS_RESOURCE_NAME).toURI().toString() : "",
                jquerySelector, tmpFile.getAbsolutePath().replace("\\", "\\\\"), config.getSettleTimeout()
        ));
        // Save the command file to a temporary file

        File commandFile = Common.getTemporaryFile("js");
        Common.writeTextFile(commandFile, command.toString());

        // Now run the whole shooting match

        try {
            int exitCode = executeCommand(commandFile, config.getTimeout());
            if (exitCode!=0)
                throw new PivotalException("Problem executing browser - return code %d", exitCode);
            returnValue = Common.readTextFile(tmpFile);
        }
        catch (Exception e) {
            throw new PivotalException("Problem executing browser - %s", PivotalException.getErrorMessage(e));
        }
        finally {
            commandFile.delete();
            tmpFile.delete();
        }

        return returnValue;
    }

    /**
     * Executes the JS command file provided
     * @param commandFile Command file
     * @param timeout Timeout in milliseconds
     * @return in
     * @throws Exception If there is an issue with the file or the commands
     *
     */
    public static int executeCommand(File commandFile, int timeout) throws Exception {

        // Check for stupidity

        getPhantomEXE();
        if (commandFile==null)
            throw new PivotalException("No command file");
        if (Common.isBlank(commandFile))
            throw new PivotalException("File doesn't exist [%s]", commandFile.getAbsolutePath());
        if (commandFile.length()==0)
            throw new PivotalException("File is empty [%s]", commandFile.getAbsolutePath());

        // Build the command line

        CommandLine cmdLine = new CommandLine(phantomJSPath.getAbsolutePath());

        // Turn off web security - we assume the caller knows what they are doing or
        // they are trying to get remote resources into a local hosted file

        cmdLine.addArgument("--web-security=no");
        cmdLine.addArgument("--ssl-protocol=any");
        cmdLine.addArgument(commandFile.getAbsolutePath());

        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValues(new int[]{});

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        executor.setWatchdog(watchdog);

        LogOutputStream outputStream = new ExecOutput();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        return executor.execute(cmdLine);
    }

    /**
     * Class to capture the output from the execution run
     */
    private static class ExecOutput extends LogOutputStream {
        /**
         * Logs a line to the log system of the user.
         *
         * @param line  the line to log.
         * @param level the log level to use
         */
        @Override
        protected void processLine(String line, int level) {
            logger.info(line + " " + level);
        }
    }

    /**
     * Scales the images in the list to match the formats
     * @param files List of exported files
     * @param formats Formats to apply
     * @return List of adjusted files
     */
    private static List<File> scaleImages(List<File> files, com.pivotal.utils.browser.ExportFormat... formats) {

        // Check if anything to do

        if (!Common.isBlank(files) && !Common.isBlank(formats)) {
            for (int i = 0; i < formats.length; i++) {
                com.pivotal.utils.browser.ExportFormat format;
                if (i>=formats.length) {
                    format = formats[formats.length - 1];
                }
                else {
                    format = formats[i];
                }
                scaleImage(files.get(i), format);
            }
        }
        return files;
    }

    /**
     * Scales the file according to the format
     * @param file File to scale
     * @param format Format to use
     * @return Scaled file
     */
    private static File scaleImage(File file, com.pivotal.utils.browser.ExportFormat format) {

        // Check if anything to do

        if (!Common.isBlank(file) && !Common.isBlank(format)) {

            // Check if scaling is appropriate

            if (format.getScaleFactor()!=null && format.getScaleFactor()!=1.0) {

                // Scale the image

                try {
                    BufferedImage img = ImageIO.read(file);
                    img = Common.scaleImage(img, (int) (img.getWidth() * format.getScaleFactor()), (int) (img.getHeight() * format.getScaleFactor()));
                    ImageIO.write(img, "png", file);
                }
                catch (Exception e) {
                    logger.error("Cannot scale the image %s - %s", file.getName(), PivotalException.getErrorMessage(e));
                }
            }
        }
        return file;
    }
}
