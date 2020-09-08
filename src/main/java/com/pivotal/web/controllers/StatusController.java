/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;


import com.pivotal.api.PoolBroker;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.reporting.reports.sqldump.ExcelOutput;
import com.pivotal.system.data.cache.CacheAccessorFactory;
import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseApp;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.utils.AppConnectionProvider;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EventMonitor;
import com.pivotal.system.monitoring.Monitor;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.servlet.Initialisation;
import com.pivotal.web.servlet.ServletHelper;
import com.pivotal.web.servlet.VelocityResourceCache;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.*;

import static com.pivotal.utils.I18n.getString;

/**
 * Handles requests for viewing and managing the status of the system
 */
@Authorise
@Controller
@RequestMapping(value = "/status")
public class StatusController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StatusController.class);

    /**
     * Populates the context with information about the JVM
     *
     * @param model Context to populate
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value={"","/","/jvm"})
    private String populateJvm(Model model) {

        // Get the disk information

        model.addAttribute("FileSystems", File.listRoots());

        // Get the JVM parameters

        model.addAttribute("OS", ManagementFactory.getOperatingSystemMXBean());
        model.addAttribute("Management", ManagementFactory.getRuntimeMXBean());
        model.addAttribute("SystemProperties", new TreeMap<>(ManagementFactory.getRuntimeMXBean().getSystemProperties()));
        model.addAttribute("Runtime", Runtime.getRuntime());

        return "status";
    }

    /**
     * Populates the context with metrics about the JVM
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/jvm/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    private Map<String,Object> populateJvmMetrics() {

        // Get the memory available

        Map<String,Object> ret = new HashMap<>();
        ret.put("heap", getJsonValues(Runtime.getRuntime().maxMemory(),Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),1000000));

        // Get the permanent generation sizes

        for (MemoryPoolMXBean mx : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mx.getName().matches("(?i).*perm gen")) {
                ret.put("permgen", getJsonValues(mx.getUsage().getMax(),mx.getUsage().getUsed(),1000000));
                break;
            }
        }

        // Get all the file descriptors

        try {
            MBeanServer server=ManagementFactory.getPlatformMBeanServer();
            ObjectName oName = new ObjectName("java.lang:type=OperatingSystem");
            AttributeList attrs = server.getAttributes(oName, new String[]{"OpenFileDescriptorCount", "MaxFileDescriptorCount"});
            if (!Common.isBlank(attrs)) {
                List<Attribute> list=attrs.asList();
                ret.put("fileDescriptors", getJsonValues((long)list.get(1).getValue(),(long)list.get(0).getValue()));
            }
        }
        catch (Exception e) {
            logger.debug("Problem getting file descriptor counts - {}", PivotalException.getErrorMessage(e));
        }

        // Get the disk information

        for (File file : File.listRoots()) {
            ret.put("fileSystem-" + file.toString(), getJsonValues(file.getTotalSpace(),file.getTotalSpace() - file.getUsableSpace(),1000000000));
        }

        // Add on the system monitor stuff

        ret.put("cpu", getJsonValues(100 * Runtime.getRuntime().availableProcessors(), EventMonitor.getCpuUsage()));

        return ret;
    }

    /**
     * Populates the context with information about the system
     *
     * @param model Context to populate
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/system")
    private String populateSystem(Model model) {

        // Add on the servlet context

        model.addAttribute("Management", ManagementFactory.getRuntimeMXBean());
        model.addAttribute("SystemProperties", new TreeMap<>(ManagementFactory.getRuntimeMXBean().getSystemProperties()));

        return "status";
    }

    /**
     * Populates the context with information about the servlet
     *
     * @param session Session to use
     * @param model Context to populate
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/servlet")
    private String populateServlet(HttpSession session, Model model) {

        // Add on the servlet context

        Map<String,Object> objMap=new TreeMap<>();
        Enumeration objTmp=session.getServletContext().getAttributeNames();
        while (objTmp.hasMoreElements()) {
            String sKey=(String)objTmp.nextElement();
            objMap.put(sKey, session.getServletContext().getAttribute(sKey));
        }
        model.addAttribute("ApplicationAttributes", objMap);
        model.addAttribute("Application", session.getServletContext());

        return "status";
    }

    /**
     * Populates the context with information about the threads
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/threads")
    private String populateThreads() {
        return "status";
    }

    /**
     * Populates the context with information about the threads
     *
     * @param model Context to populate
     */
    @RequestMapping(value="/threads/list")
    private void populateThreadList(Model model) {

        // Add the threads to a map to rename all the duplicates

        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        Map<String, Thread> threadMap=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int runnableCount = 0;
        for (Thread tmp : threads) {
            if (tmp !=null) {

                // We create a sorting key to get everything out in the best order
                // Rannables are always at the top

                String threadName = tmp.getName();
                if (tmp.getState().equals(Thread.State.RUNNABLE)) {
                    threadName = String.format("A %d %d", 100000 - threadName.length(), tmp.getId());
                    runnableCount++;
                }

                // Followed by anything tht starts with the application name

                else if (tmp.getName().startsWith(Common.getAplicationName())) {
                    threadName = String.format("B %s %d", threadName, tmp.getId());
                }

                // Finally everything else

                else {
                    threadName = String.format("C %s %d", threadName, tmp.getId());
                }

                // Take care of where the thread is in there with the same name

                int threadCnt=1;
                while (threadMap.containsKey(threadName)) {
                    threadName = threadName + '(' + threadCnt + ')';
                    threadCnt+=1;
                }
                threadMap.put(threadName, tmp);
            }
        }

        model.addAttribute("RunnableCount", runnableCount);
        model.addAttribute("Threads", threadMap);
        model.addAttribute("ThreadInfo", ManagementFactory.getThreadMXBean());
    }

    /**
     * Populates the context with information about the threads
     *
     * @param model Context to populate
     */
    @RequestMapping(value="/threads/stacktrace")
    private void populateStackTrace(Model model, @RequestParam(value = "id") long id) {
        model.addAttribute("Thread", ManagementFactory.getThreadMXBean().getThreadInfo(id, 30));
    }

    /**
     * Kills the specified thread
     *
     * @param id Thread ID
     */
    @RequestMapping(value="/killthread", method= RequestMethod.GET)
    @ResponseBody
    public void killThread(@RequestParam(value = "id") Integer id) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        for (Thread tmp : threads) {
            if (tmp.getId()==id) {
                try {
                    Common.stopThread(tmp);
                }
                catch (Throwable e) {
                    logger.error(PivotalException.getErrorMessage(e));
                }
            }
        }
    }

    /**
     * Attempts to force a garbage collection
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/jvm/gc"}, method=RequestMethod.GET)
    @ResponseBody
    public Object get() {
        Common.garbageCollect();
        return null;
    }

    /**
     * Attempts to force a garbage collection
     *
     * @return Always returns null to indicate no template
     * @param days a int.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @RequestMapping(value={"/jvm/cleartemp"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearTempFiles(@RequestParam(value="days", required=true) int days) {

        // check the days are valid

        if (days<1) days=1;
        final Date old=Common.addDate(new Date(), Calendar.DATE, -days);

        // Get a list of all the files in the temporary folder that are old

        File tmpfolder=new File(Common.getTemporaryDirectory());
        File[] files=tmpfolder.listFiles(new FileFilter() {
            public boolean accept(File file) {
                Date lastModified=new Date(file.lastModified());
                return lastModified.before(old);
            }
        });

        // Delete all the files

        for (File file : files)
            file.delete();

        return null;
    }

    /**
     * Populates the context with information about the log files
     *
     * @param model Context to populate
     * @return Returns the appropriate template
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value={"/log"})
    public String populateLog(Model model) {

        // Get the app log file

        List<File> files=Common.listFiles(Initialisation.getLogDir(), "app.log", false, false);
        if (!Common.isBlank(files)) {
            model.addAttribute("AppLog", files.get(0));
            if (!getPreferences().containsKey("selectedLog"))
                putPreference("selectedLog", files.get(0));
        }

        // Get a list of the App files

        files=Common.listFiles(Initialisation.getLogDir(), "app.log.+", false, false);
        if (!Common.isBlank(files)) {
            Collections.sort(files, new Comparator() {
                public int compare(Object o1, Object o2) {
                    File f1 = (File) o1;
                    File f2 = (File) o2;
                    return f2.lastModified() < f1.lastModified() ? -1 : f2.lastModified() == f1.lastModified() ? 0 : 1;
                }
            });
        }
        model.addAttribute("AppLogs", files);


        // Get a list of the other log files

        Map<String,List<File>> extraLogs = new LinkedHashMap<>();
        files=Common.listFiles(Initialisation.getLogDir(), "(?!app.log).*", false, false);
        if (!Common.isBlank(files)) {
            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (File file : files) {
                if (file.length()>0)
                    names.add(file.getName().replaceAll("\\.[0-9\\-]+", ""));
            }
            for (String name : names) {
                String body = Common.getFilenameBody(name);
                files=Common.listFiles(Initialisation.getLogDir(), body + "\\..+", false, false);
                if (!Common.isBlank(files)) {
                    Collections.sort(files, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            File f1 = (File) o1;
                            File f2 = (File) o2;
                            return f1.getName().compareTo(f2.getName());
                        }
                    });
                }
                extraLogs.put(name, files);
            }
        }
        model.addAttribute("Logs", extraLogs);

        return "status";
    }

    /**
     * Services the constituent pages of the status screens
     *
     * @param response Response object
     * @param filename Filename to work with
     * @param fontSize Font size in percent
     * @param tailingLength Length of tail to show
     */
    @RequestMapping(value="/logcontent")
    @ResponseBody
    public void getLogContent(HttpServletResponse response,
                                @RequestParam(value = "filename") String filename,
                                @RequestParam(value = "size", required = false, defaultValue = "100") int fontSize,
                                @RequestParam(value = "length", required = false, defaultValue = "5000") int tailingLength
                                ) {

        // Get the selected log file

        File file = new File(Initialisation.getLogDir().getAbsolutePath() + File.separator +  filename);
        if (file.exists()) {
            RandomAccessFile fileObj=null;
            try {

                // Get and save the logging settings

                boolean tailing= ServletHelper.parameterExists("tail");
                boolean attached=ServletHelper.parameterExists("attached");
                putPreference("fontSize", fontSize);
                putPreference("tailSize", 5000);
                getPreferences().getSession().put("selectedLogName", filename);

                // If we just want the last x lines

                if (tailing) {
                    getPreferences().getSession().put("autoTail", ServletHelper.parameterExists("auto"));
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().println("<html><body style='font-size:" + fontSize + "%'><pre>");
                    fileObj=new RandomAccessFile(file, "r");
                    if (tailingLength<fileObj.length()) fileObj.seek(fileObj.length() - tailingLength);
                    String line=fileObj.readLine();
                    boolean first=true;
                    while (line!=null) {
                        if (!first) response.getWriter().println(line);
                        first=false;
                        line=fileObj.readLine();
                    }
                }

                // Send the whole file

                else {
                    getPreferences().getSession().put("autoTail", false);
                    ServletOutputStream output=response.getOutputStream();
                    if (attached) {
                        response.setContentType("text/plain;charset=UTF-8");
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '"');
                        response.setHeader("Content-Description", filename);
                        response.setHeader("Content-Length", file.length() + "");
                    }
                    else
                        output.print("<html><body style='font-size:" + fontSize + "%'><pre>");
                    Common.pipeInputToOutputStream(file, output, true);
                }
            }
            catch (IOException e1) {
                throw new PivotalException(e1);
            }
            finally {
                Common.close(fileObj);
            }
        }
    }

    /**
     * Services the constituent pages of the status screens
     *
     * @param response Response object
     * @param filename Filename to work with
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @RequestMapping(value={"/logcontent/xls"})
    public void getLogContentXls(HttpServletResponse response,
                                   @RequestParam(value = "filename") String filename) {

        // Get the selected log file

        File fileObj = new File(Initialisation.getLogDir().getAbsolutePath() + '/' +  filename);
        if (fileObj.exists()) {
            BufferedReader objFile=null;
            ExcelOutput output=null;
            File tmpFile=new File(Common.getTemporaryFilename(".xls"));
            try {

                // Create an Excel output

                response.setContentType(ServletHelper.getServletContext().getMimeType(tmpFile.getName()));
                output=new ExcelOutput(tmpFile.getPath(), null, false);
                output.newSection(fileObj.getName());

                // Write the log file out, line by line

                objFile=new BufferedReader(new FileReader(fileObj));
                String sLine=objFile.readLine();
                while (sLine!=null) {
                    if (!Common.isBlank(sLine)) {
                        Map<String, Object>row=new LinkedHashMap<>();
                        String[] items=sLine.split("[ \\t]+",4);
                        if (!Common.isBlank(items)) {
                            if (items.length<2 || Common.parseDateTime(items[0], items[1])==null) {
                                row.put("Timestamp", null);
                                row.put("Type", null);
                                row.put("Thread", null);
                                row.put("Class", null);
                                row.put("Message", sLine.replaceFirst("\\t", "        "));
                            }
                            else {
                                row.put("Timestamp", new java.sql.Timestamp(Common.parseDateTime(items[0], items[1]).getTime()));
                                row.put("Type", items.length>2?items[2]:null);
                                if (items[3].startsWith("[")) {
                                    String thread=items[3].split("\\] *",2)[0];
                                    String tmp=items[3].substring(thread.length());
                                    items=new String[3];
                                    items[0]=thread;
                                    items[1]=tmp.split(" *- *", 2)[0];
                                    items[2]=tmp.split(" *- *", 2)[1];
                                }
                                else
                                    items=items[3].split("( *)|( *- *)",3);
                                row.put("Thread", items[0].replaceAll("[\\[\\]]"," ").trim());
                                row.put("Class", items.length>1?items[1].replaceAll("[\\[\\]]","").trim():null);
                                row.put("Message", items.length>2?items[2]:null);
                            }
                            output.addRow(row);
                        }
                    }
                    sLine=objFile.readLine();
                }
                output.close();
                output=null;

                // Send the file to the response stream

                String tmp=fileObj.getName() + ".xls";
                response.setHeader("Content-Disposition", "attachment; filename=\"" + tmp + '"');
                response.setHeader("Content-Description", tmp);
                response.setHeader("Content-Length", tmpFile.length() + "");
                Common.pipeInputToOutputStream(tmpFile, response.getOutputStream(), true);
            }
            catch (IOException e1) {
                throw new PivotalException(e1);
            }
            finally {
                Common.close(objFile, output);
                if (tmpFile.exists()) tmpFile.delete();
            }
        }
    }

    /**
     * Serves the log configuration file
     *
     * @param model Model to use to carry the report info
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value={"/logconfig"}, method=RequestMethod.GET)
    public String getLogconfig(Model model) {
        model.addAttribute("Content", Common.readTextFile(Initialisation.getLogPropertiesFilename()));
        model.addAttribute("ContentType", "properties");
        return "/media/editor";
    }

    /**
     * Saves the log configuration file
     *
     * @param session Session to use
     * @param logConfig a {@link java.lang.String} object.
     */
    @RequestMapping(value={"/logconfig"}, method= RequestMethod.POST)
    @ResponseBody
    public void saveLogconfig(HttpSession session, @RequestParam(value="logconfig", required=false) String logConfig) {
        if (!Common.doStringsMatch(Common.readTextFile(Initialisation.getLogPropertiesFilename()), logConfig)) {
            Initialisation.initLoggingEnv(session.getServletContext(), logConfig);
            NotificationManager.addNotification(getString("system.log.config.reloaded"), Notification.NotificationLevel.Info, Notification.NotificationGroup.Admin, Notification.NotificationType.Application, true);
            logger.info("Reloaded log4j properties from file");
        }
    }

    /**
     * Populates the context with information about the active pools
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/pool")
    public String populatePools() {
        return "status";
    }


    /**
     * Populates the context with information about the active pools
     *
     * @param model Context to populate
     */
    @RequestMapping(value="/pool/statistics")
    public void populatePoolStats(Model model) {
        Collection<DataSource> pools= PoolBroker.getInstance().getAllActivePools();
        if (!Common.isBlank(pools)) {
            List<Map<String,Object>> returnValue = new ArrayList<>();
            for(DataSource src : pools){
                try {
                    Map<String, Object>poolFieldsSet=new HashMap<>();
                    poolFieldsSet.put("name",src.getName());
                    poolFieldsSet.put("initialSize",src.getInitialSize());
                    poolFieldsSet.put("size",src.getSize());
                    poolFieldsSet.put("activeConnections",src.getNumActive());
                    poolFieldsSet.put("idleConnections",src.getNumIdle());
                    poolFieldsSet.put("maxActive",src.getMaxActive());
                    poolFieldsSet.put("maxIdle",src.getMaxIdle());
                    poolFieldsSet.put("minIdle",src.getMinIdle());
                    poolFieldsSet.put("maxWait",src.getMaxWait());

                    // Get the associated data source
                    poolFieldsSet.put("source", HibernateUtils.getEntity(DatasourceEntity.class, src.getName()));

                    returnValue.add(poolFieldsSet);
                }
                catch (Exception e) {
                    logger.error("Problem getting pool status information for [{}] - {}", src.getName(), PivotalException.getErrorMessage(e));
                }
            }
            model.addAttribute("Pools", returnValue);
        }

        // Add on the App connection pool

        try {
            model.addAttribute("ApponnectionPool", HibernateUtils.getDataSource());
            model.addAttribute("AppConnectionProvider", AppConnectionProvider.class);
        }
        catch (Exception e) {
            logger.error("Problem getting connection parameters - {}", PivotalException.getErrorMessage(e));
        }

        // Add on the cache stats

        model.addAttribute("VelocityCache", VelocityResourceCache.getStats());
        model.addAttribute("CacheStats", CacheAccessorFactory.getInstance().getStatistics());

        // Add the Monitor stats

        model.addAttribute("Monitors", Monitor.getMonitors());
    }


    /**
     * Attempts to kill a pool with the given name
     *
     * @return True if it worked OK
     * @param poolName a {@link java.lang.String} object.
     * @param forceKill a boolean.
     */
    @RequestMapping(value={"/pool/kill"}, method=RequestMethod.GET)
    public @ResponseBody
    JsonResponse killPool(@RequestParam(value="poolname", required=true) String poolName, @RequestParam(value="forcekill", required=false) boolean forceKill) {
        JsonResponse returnValue = new JsonResponse();

        // check if pool exists

        if (!PoolBroker.getInstance().poolExists(poolName))
            returnValue.setError("The pool %s does not exist", poolName);
        else {

            // Try and destroy the pool

            if (!PoolBroker.getInstance().destroyPool(poolName,forceKill)) {
                if (forceKill)
                    returnValue.setError("Could not destroy the pool %s", poolName);
                else
                    returnValue.setError("Could not destroy the pool %s - probably has open connections on loan", poolName);
            }

            // Clear the pool cache

            clearPool(poolName);
        }
        return returnValue;
    }

    /**
     * Attempts to kill all the pools
     *
     * @param forceKill a boolean.
     * @return a {@link java.lang.Object} object.
     */
    @RequestMapping(value={"/pool/killall"}, method=RequestMethod.GET)
    @ResponseBody
    public Object killAllPools(@RequestParam(value="forcekill", required=false) boolean forceKill) {

        PoolBroker.getInstance().destroyAllPools(forceKill);
        return null;
    }

    /**
     * Clears the cache for this connection
     *
     * @return Always returns null to indicate no template
     * @param poolName a {@link java.lang.String} object.
     */
    @RequestMapping(value={"/pool/clear"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearPool(@RequestParam(value="poolname", required=true) String poolName) {

        // check if pool exists
        if(!Common.isBlank(poolName)) {
            DatasourceEntity datasource= HibernateUtils.getEntity(DatasourceEntity.class, poolName);
            if (datasource!=null) CacheEngine.clear(datasource.getId());
        }
        return null;
    }


    /**
     * Clears the velocity resource cache
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/velocity/clear"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearVelocity() {
//        WebServiceUtils.clearCaches();
        return null;
    }

    /**
     * Clears the velocity resource cache
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/velocity/clearstats"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearVelocityStats() {
        VelocityResourceCache.clearStats();
        return null;
    }

    /**
     * Clears the connection provider stats
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/connection/clearstats"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearConnectionStats() {
        AppConnectionProvider.clearStats();
        return null;
    }

    /**
     * Clears the general cache
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/cache/clear"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearCache() {
        CacheAccessorFactory.getInstance().shutdown();
        return null;
    }

    /**
     * Clears the general cache statistics
     *
     * @return Always returns null to indicate no template
     */
    @RequestMapping(value={"/cache/clearstats"}, method=RequestMethod.GET)
    @ResponseBody
    public Object clearCacheStats() {
        CacheAccessorFactory.getInstance().clearStatistics();
        return null;
    }

    /**
     * Populates the context with the Hibernate page
     *
     * @param model Context to populate
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value="/hibernate")
    public String populateHibernate(Model model) {
        populateHibernateStats(model);
        return "status";
    }

    /**
     * Populates the context with the Hibernate statistics
     *
     * @param model Context to populate
     */
    @RequestMapping(value="/hibernate/statistics")
    public void populateHibernateStats(Model model) {
        model.addAttribute("Statistics", HibernateUtils.getSessionFactory().getStatistics());
    }

    /**
     * Resets the stats
     *
     * @return Nothing
     */
    @RequestMapping(value={"/hibernate/reset"})
    @ResponseBody
    public Object resetHibernateStats() {
        HibernateUtils.getSessionFactory().getStatistics().clear();
        return null;
    }

    /**
     * Enables or disables the stats collecting
     *
     * @return Returns nothing
     * @param enable a boolean.
     */
    @RequestMapping(value={"/hibernate/stats"})
    @ResponseBody
    public Object enableHibernateStats(@RequestParam(value="enable", required=true) boolean enable) {
        HibernateUtils.getSessionFactory().getStatistics().setStatisticsEnabled(enable);
        return null;
    }

    /**
     * Returns an XML string of the current health of th system
     *
     * @return XML health status
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(value={"/health"}, produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public Map getHealth() {

        // TODO send something sensible back as a health status

        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");

        return status;
    }

    /**
     * Returns the data needed for the database template
     *
     * @param model   Model to populate
     * @return The template name to use
     */
    @RequestMapping(value = "/diagnostics", method = RequestMethod.GET)
    public String database(Model model) {
        try {
            model.addAttribute("Connection", HibernateUtils.getDataSource());
            model.addAttribute("ConnectionDriver", HibernateUtils.getDataSource().getConnection().getMetaData());
        }
        catch (Exception e) {
            logger.error("Problem getting connection parameters - {}", PivotalException.getErrorMessage(e));
        }
        return "status";
    }

    /**
     * Returns the data needed for the database query template
     *
     * @param model   Model to populate
     */
    @RequestMapping(value = "/diagnostics/database/query", method = RequestMethod.GET)
    public void databaseQuery(Model model) {

        // Get a list of the data sources

        List<Map<String,String>> returnValue=new ArrayList<>();
        returnValue.add(Common.getMapFromPairs("text", "[App]", "value", "-1"));
        for (DatasourceEntity source : HibernateUtils.<DatasourceEntity>selectEntities("from DatasourceEntity order by name")) {
            returnValue.add(Common.getMapFromPairs("text",source.getName(),"value",source.getId() + ""));
        }
        model.addAttribute("DatasourceList", returnValue);
    }

    /**
     * Runs the query and sends the data back via the model
     *
     * @param model   Model to populate
     * @param query a {@link java.lang.String} object.
     * @param datasourceId a int.
     */
    @RequestMapping(value = "/diagnostics/database/results", method = RequestMethod.POST)
    public void databaseQueryExecute(Model model,
                                     @RequestParam(value="query") String query,
                                     @RequestParam(value="datasource") int datasourceId) {

        // Save the preferences

        putPreference("query", query);
        putPreference("datasource", datasourceId);
        putPreference("csv", false);

        // Get a datasource to use and open it

        Database db = null;
        try {
            if (datasourceId==-1)
                db = new DatabaseHibernate();
            else
                db = new DatabaseApp(HibernateUtils.getEntity(DatasourceEntity.class, datasourceId));
            db.open();

            // If the query isn't a select, then log it

            if (!query.matches("(?mis)^\\s*select.+")) {
                logger.warn("Non SELECT query being executed [{}] by [{}]", query, UserManager.getCurrentUser().getName());
            }

            // Run the query and return the results

            model.addAttribute("Results", db.find(query, true));
            model.addAttribute("Error", db.getLastError());
        }
        catch (Exception e) {
            model.addAttribute("Error", PivotalException.getErrorMessage(e));
        }
        finally {
            Common.close(db);
        }
    }

    /**
     * Runs the query and sends the output to a CSV response
     *
     * @param response Response object to send results to
     * @param query a {@link java.lang.String} object.
     * @param datasourceId a int.
     */
    @RequestMapping(value = "/diagnostics/database/results/csv", method = RequestMethod.GET)
    public void databaseQueryExecuteCsv(HttpServletResponse response,
                                     @RequestParam(value="query") String query,
                                     @RequestParam(value="datasource") int datasourceId) {

        // Save the preferences

        putPreference("query", query);
        putPreference("datasource", datasourceId);
        putPreference("csv", true);

        // Get a datasource to use and open it

        Database db = null;
        try {
            if (datasourceId==-1)
                db = new DatabaseHibernate();
            else
                db = new DatabaseApp(HibernateUtils.getEntity(DatasourceEntity.class, datasourceId));
            db.open();

            // If the query isn't a select, then log it

            if (!query.matches("(?mis)^\\s*select.+")) {
                logger.warn("Non SELECT query being executed [{}] by [{}]", query, UserManager.getCurrentUser().getName());
            }

            // Run the query and return the results

            db.findToCSV(query, response, db.getName() + '-' + Common.dateFormat(new Date(), "yyyy-MM-dd-HH:mm:ss") + ".csv", true);
        }
        catch (Exception e) {
            logger.error("Problem exporting to CSV - {}", PivotalException.getErrorMessage(e));
        }
        finally {
            Common.close(db);
        }
    }

   /**
     * Returns a map suitable for driving a gauge etc.
     * @param total Total count
     * @param used Used count
     * @return Map of total, used and percent
     */
    private static Map<String,Object> getJsonValues(Number total, Number used) {
        return getJsonValues(total, used, 1);
    }

    /**
     * Returns a map suitable for driving a gauge etc.
     * @param total Total count
     * @param used Used count
     * @param scale Scale to apply
     * @return Map of total, used and percent
     */
    private static Map<String,Object> getJsonValues(Number total, Number used, int scale) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("total" , Common.parseDouble(Common.formatNumber(total.doubleValue() / scale, "#.0")));
        ret.put("used", Common.parseDouble(Common.formatNumber(used.doubleValue() / scale, "#.0")));
        ret.put("percent", Common.parseDouble(Common.formatNumber(used.doubleValue() * 100 / total.doubleValue(), "#.0")));
        return ret;
    }
}
