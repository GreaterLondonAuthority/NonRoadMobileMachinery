/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.utils.Common;
import com.pivotal.reporting.reports.Report;
import com.pivotal.system.security.UserManager;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EventMonitor;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.ExceptionUtils;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Resource loader that allows us to check the database, file system and the class loader for the required template.
 * If the template is not from the database it will first attempt to locate the file from within the classpath and then
 * resolve from the file system. This allows a plugin to overwrite the system template if they require</p>
 *
* @see org.apache.velocity.runtime.resource.loader.ResourceLoader
 */
public class VelocityTemplateMixedResourceLoader extends org.apache.velocity.runtime.resource.loader.ResourceLoader {

    // Get the logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityTemplateMixedResourceLoader.class);

    // All templates starting with database should be fetched from the DB
    public static final String DATABASE_RESOURCE = "/database/";

    // Cache for the database reports
    private static Map<String, ReportEntity> reportCache;

    // Cache for the strings
    private static Map<String, String> formulaCache;

    static {
        reportCache = new ConcurrentHashMap<>();
        formulaCache = new ConcurrentHashMap<>();
    }

    // Store the path to the file based templates
    private String templatePath;

    // The location for the original classic templates
    private String templatePathClassic;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ExtendedProperties configuration) {
        templatePath = configuration.getString("path");
        templatePathClassic = new File(templatePath).getParentFile().getAbsolutePath() + "/templates-classic";
        logger.debug("Velocity template mixed resource loader initialised using {} template directory ", templatePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSourceModified(Resource resource) {
        boolean isSourceModified = getLastModified(resource) != resource.getLastModified();
        logger.debug("Resource [{}] modified - {}", resource.getName(), (isSourceModified ? "true" : "false"));

        // If we are logged in and we are allowed access, then we need to update the last
        // access timestamp for this user - we only do this for non-JSON responses because
        // they are most likely to be automated and not user initiated

        if (UserManager.isUserLoggedIn() && ServletHelper.getSession() != null) {
            EventMonitor.addEvent(EventMonitor.EVENT_TYPE_USER_SESSION_UPDATE, ServletHelper.getSession().getId(), 0);
        }
        return isSourceModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified(Resource resource) {

        // Check to see if this is an attempt to get a file from the databases

        if (resource.getName().toLowerCase().startsWith(DATABASE_RESOURCE)) {
            ReportEntity report = getReportEntity(resource.getName());
            if (report != null) {

                // Check to see if the resource is being overidden

                File reportFile = Report.getOverridingFile(report);
                if (!Common.isBlank(reportFile)) {
                    logger.debug("Resource {} was modified - {}", resource.getName(), reportFile.getAbsolutePath(), reportFile.lastModified());
                    return reportFile.lastModified();
                }
                else {
                    logger.debug("Resource {} was modified - {}", resource.getName(), report.getTimeModified());
                    if (report.getTimeModified() == null) {
                        return 0;
                    }
                    else {
                        return report.getTimeModified().getTime();
                    }
                }
            }
            else {
                logger.error("Resource {} cannot be read", resource.getName());
                return 0L;
            }
        }

        else {
            // Get the file from the file system

            File file = new File(getResourceFilename(resource.getName()));

            // Check that it is readable and then get it's modification date

            if (file.canRead()) {
                logger.debug("Resource {} was modified - {}", resource.getName(), file.lastModified());
                return file.lastModified();
            }
            else {
                logger.debug("Resource {} cannot be read - Classpath plugin resource?", resource.getName());
                return 0L;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Returns the template as a stream from either the classpath or filesystem
     */
    public synchronized InputStream getResourceStream(String templateName) throws ResourceNotFoundException {

        // Check for an invalid template

        logger.debug("Loading template {}", templateName);
        InputStream result;
        if (StringUtils.isBlank(templateName))
            throw new ResourceNotFoundException("Need to specify a file name or file path!");
        else {

            // Check to see if this is an attempt to get a file from the database

            if (templateName.toLowerCase().startsWith(DATABASE_RESOURCE)) {
                ReportEntity report = getReportEntity(templateName);
                if (report != null) {

                    // Get the resource from the report object to allow us to take into
                    // account that this might have been overridden

                    logger.debug("Found report file in database id: {}", report.getId());
                    return new BufferedInputStream(new ByteArrayInputStream(report.getFile()));
                }
                else {
                    logger.error("Cannot find the database resource - {} - ignoring", templateName);
                    result = new ByteArrayInputStream("".getBytes());
                }
            }

            else {

                // Look for resource in thread classloader first (e.g. WEB-INF\lib in
                // a servlet container) then fall back to the system classloader.

                try {
                    result = ClassUtils.getResourceAsStream(getClass(), templateName);
                }
                catch (Exception fnfe) {
                    throw (ResourceNotFoundException) ExceptionUtils.createWithCause(ResourceNotFoundException.class, "problem with template: " + templateName, fnfe);
                }

                // If the template cannot be located from the classpath then try locating it from the file system

                if (result == null) {

                    // Get the full path name to the resource we're trying to load

                    String sTemplate = getResourceFilename(templateName);
                    try {

                        // Open the file and return it as a stream

                        File file = new File(sTemplate);
                        if (file.canRead())
                            return new BufferedInputStream(new FileInputStream(file.getAbsolutePath()));
                    }
                    catch (FileNotFoundException fnfe) {
                        String msg = "ResourceLoader Error: cannot find resource " + templateName + " (" + sTemplate + ')';
                        logger.error(msg);
                        throw new ResourceNotFoundException(msg);
                    }
                }
            }
            return result;
        }
    }

    /**
     * Gets the filename to use
     *
     * @param resource Template to get
     *
     * @return Full spec filename of template
     */
    private String getResourceFilename(String resource) {

        // Cleanup the template name

        String sTemplate = resource;
        if (sTemplate.startsWith("/") || sTemplate.startsWith("\\"))
            sTemplate = sTemplate.substring(1);
        File template = new File(templatePath + '/' + sTemplate);

        // If the template doesn't exist, check to see if it's a classic template

        if (!template.exists())
            template = new File(templatePathClassic + '/' + sTemplate);

        return template.getAbsolutePath();
    }

    /**
     * Gets the report object to use
     * This method looks for specific patterns in the names of the templates to
     * determine where it might find the template
     * If the template name looks like "/database/report:xxx" then it expects that
     * the template is a report type and can can recalled from the database using
     * key xxx.
     * If neither rof these patterns can be found, then it assumes that the resource
     * is being searched using it's "folder/name"
     *
     * @param resource Template to get
     *
     * @return Report entity from database
     */
    private static ReportEntity getReportEntity(String resource) {

        ReportEntity returnValue = null;

        // Determine where to find the template from

        if (resource.matches("(?i)" + DATABASE_RESOURCE + "report:[0-9]+")) {
            returnValue = reportCache.get(resource);
            if (returnValue == null) {
                int key = Common.parseInt(resource.replaceAll("[^0-9]+", ""));
                returnValue = HibernateUtils.getEntity(ReportEntity.class, key);
                if (returnValue == null)
                    logger.warn("Cannot find database include file [{}] using key", resource);
                else {
                    logger.debug("Found database include file [{}] in database using key", returnValue.getName());
                    reportCache.put(resource, returnValue);
                }
            }
            else
                logger.debug("Found database include file [{}] in cache using key", returnValue.getName());
        }

        // The template is specified by it's folder/name

        else {
            String folder = Common.getItem(resource, "/", 2);
            String template = Common.getItem(resource, "/", 3);
            if (!Common.isBlank(folder) && !Common.isBlank(template) &&
                    !folder.startsWith("$") && !template.startsWith("$")) {

                // Try the cache first

                String key = folder + "---" + template;
                returnValue = reportCache.get(key);

                // Get the report

                if (returnValue == null) {
                    List<ReportEntity> reports = HibernateUtils.selectEntities("from ReportBlobEntity where name=? and folder.name=?", template, folder);
                    if (!Common.isBlank(reports)) {
                        returnValue = reports.get(0);
                        reportCache.put(key, returnValue);
                        logger.debug("Found database include file [{}] in database using select", returnValue.getName());
                    }
                    else
                        logger.warn("Cannot find database include file [{}]", resource);
                }
                else
                    logger.debug("Found database include file [{}] in cache", returnValue.getName());
            }
        }
        return returnValue;
    }
    /**
     * Clears the local database backed reports cache
     */
    public static void clearCache() {
        reportCache.clear();
        logger.debug("Cleared cache");
    }
}
