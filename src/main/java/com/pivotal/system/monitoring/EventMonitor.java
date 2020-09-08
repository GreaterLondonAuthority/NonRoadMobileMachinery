/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring;

import com.pivotal.api.PoolBroker;
import com.pivotal.reporting.reports.Report;
import com.pivotal.system.security.UserManager;
import com.pivotal.system.data.cache.CacheAccessorFactory;
import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.data.dao.DatabaseApp;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.utils.VelocityUtils;
import com.pivotal.web.servlet.ServletHelper;
import com.pivotal.web.servlet.VelocityResourceCache;
import com.sun.management.OperatingSystemMXBean;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

/**
 * Provides the main system event monitoring functionality
 * This mostly means checking for storage of events to the audit/log tables
 */
public class EventMonitor extends Monitor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EventMonitor.class);

    /** Constant <code>EVENT_DISPATCHER="dispatcher"</code> */
    public static final String EVENT_DISPATCHER = "dispatcher";
    /** Constant <code>EVENT_HOUSEKEEPING="housekeeping"</code> */
    public static final String EVENT_HOUSEKEEPING = "housekeeping";
    /** Constant <code>EVENT_TYPE_DISPATCHER_REQUEST="request"</code> */
    public static final String EVENT_TYPE_DISPATCHER_REQUEST = "request";
    /** Constant <code>EVENT_TYPE_USER_SESSION_UPDATE="update_status"</code> */
    public static final String EVENT_TYPE_USER_SESSION_UPDATE = "update_status";
    /** Constant <code>EVENT_TYPE_USER_UPDATE="update_account"</code> */
    public static final String EVENT_TYPE_USER_UPDATE = "update_account";
    /** Constant <code>EVENT_REQUEST_DO_NOT_AUDIT="dontaudit"</code> */
    public static final String EVENT_REQUEST_DO_NOT_AUDIT = "dontaudit";

    /** Constant <code>EVENT_REPORT="report"</code> */
    public static final String EVENT_REPORT = "report";

    private static List<Event> eventQueue=new ArrayList<>();

    private static Date lastTruncateLogTable;
    private static EventMonitor instance;

    private static long lastSystemTime = System.nanoTime();
    private static long lastProcessCpuTime;

    /**
     * Initialise the system
     *
     * @param name          Name of process
     * @param period        Refresh rate
     * @param deadPeriod    Timeout, after which it is restarted
     *
     * @return EventMonitor
     */
    public static EventMonitor init(String name, int period, int deadPeriod) {
        if (instance!=null) {
            instance.stopMonitor();
        }
        instance = new EventMonitor();
        instance.setMonitorName(name);
        instance.setPeriod(period);
        instance.setDeadPeriod(deadPeriod);
        return instance;
    }

    /**
     * Returns the running instance
     *
     * @return Instance of the monitor
     */
    public static EventMonitor getInstance() {
        if (instance==null)
            throw new PivotalException("Instance hasn't been initialised yet");
        else
            return instance;
    }

    /**
     * Will shut down the manager
     */
    public static void shutdown() {
        if (instance!=null) instance.stopMonitor();
    }

    /** {@inheritDoc} */
    @Override
    public void runTask() {

        // Get a list of events to work on

        Progress localProgress = new Progress();
        List<Event> events = getEventQueue(true);

        // Process any dispatcher events

        if (isRunning) {
            if (!HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS, HibernateUtils.SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS_DEFAULT)) {
                try {
                    checkDispatcherEvents(events);
                }
                catch (Exception e) {
                    logger.error("Problem handling Dispatcher events - {}", PivotalException.getErrorMessage(e));
                }
                logger.debug("checkDispatcherEvents completed in {} seconds", localProgress.getSecondsElapsed(true));
            }
        }

        // Process any user status events

        if (isRunning) {
            try {
                checkUserStatusEvents(events);
            }
            catch (Exception e) {
                logger.error("Problem handling User Status events - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("checUserStatusEvents completed in {} seconds", localProgress.getSecondsElapsed(true));
        }

        // Adds log entries for all the KPIs

        if (isRunning) {
            try {
                addHouseKeepingInfo();
            }
            catch (Exception e) {
                logger.error("Problem handling Housekeeping - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("addHouseKeepingInfo completed in {} seconds", localProgress.getSecondsElapsed(true));
        }

        // Check for any data source caches that need invalidating

        if (isRunning) {
            try {
                checkDatasourceCacheInvalidateTriggers();
            }
            catch (Exception e) {
                logger.error("Problem handling Cache Invalidation - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("checkDatasourceCacheInvalidateTriggers completed in {} seconds", localProgress.getSecondsElapsed(true));
        }

        // Delete old log entries, but only do this once per day

        if (isRunning) {
            try {
                truncateLogTable();
            }
            catch (Exception e) {
                logger.error("Problem handling Log Truncation - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("truncateLogTable completed in {} seconds", localProgress.getSecondsElapsed(true));
        }

        // Check to see if the Hibernate cache is stale

        if (isRunning) {
            try {
                checkForStaleHibernate();
            }
            catch (Exception e) {
                logger.error("Problem handling Stale Hibernate - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("checkForStaleHibernate completed in {} seconds", localProgress.getSecondsElapsed(true));
        }

        // Clear out any old files that can be deleted

        if (isRunning) {
            try {
                clearDeletedFiles();
            }
            catch (Exception e) {
                logger.error("Problem handling temporary file deletion - {}", PivotalException.getErrorMessage(e));
            }
            logger.debug("clearDeletedFiles completed in {} seconds", localProgress.getSecondsElapsed(true));
        }
    }

    /**
     * Gently clears out files that have been marked for deletion
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void clearDeletedFiles() {

        List<File> files = Common.getFilesForDeletion(true);
        if (!Common.isBlank(files)) {
            for (File file : files) {
                if (file.exists()) file.delete();
            }
        }
    }

    /**
     * Adds a system log entry to the database with the default types etc
     *
     * @param type Type of the event
     * @param duration Duration or counter value
     */
    private static void addLogEntry(String type, long duration) {
        addLogEntry(new Date(), EVENT_HOUSEKEEPING, type, false, false, duration, 1, 0);
    }

    /**
     * Adds a system log entry to the database with the default types etc
     *
     * @param type Type of the event
     * @param duration Duration or counter value
     * @param total Total value to use
     */
    private static void addLogEntry(String type, long duration, long total) {
        addLogEntry(new Date(), EVENT_HOUSEKEEPING, type, false, false, duration, 1, total);
    }

    /**
     * Adds a log entry to the system for the given counter name
     *
     * @param timeAdded Time of the event
     * @param event Event name
     * @param type Type of the event
     * @param cumulative True if the duration is to be added to the existing value
     * @param incrementTotal True if the total should be incremented
     * @param increment Increment amount
     * @param duration Duration or count value
     */
    private static void addLogEntry(Date timeAdded, String event, String type, boolean cumulative, boolean incrementTotal, long duration, int increment, long total) {

        // We store these in the log table aggregated on the minute they occur in
        // If the action is not cumulative, then we only add the entry once for any particular minute

        Calendar tmp=Calendar.getInstance();
        tmp.setTime(timeAdded);
        tmp.set(Calendar.SECOND, 0);
        tmp.set(Calendar.MILLISECOND, 0);

        // Get existing log entry if it exists

        LogEntity log=new LogEntity();
        List<LogEntity> logs = HibernateUtils.selectEntities("from LogEntity where status=? and report_name=? and date_added=? and server_id=?",
                                                                 event,
                                                                 type,
                                                                 tmp.getTime(),
                                                                 ServletHelper.getAppIdentity());

        // If we don't have any entry, then add it

        boolean save=false;
        if (Common.isBlank(logs)) {
            log.setServerId(ServletHelper.getAppIdentity());
            log.setStatus(event);
            log.setReportName(type);
            log.setDateAdded(new Timestamp(tmp.getTime().getTime()));
            log.setTotal(0L);
            log.setDuration(0L);
            save=true;
        }
        else
            log = logs.get(0);

        // If this is a simple visit notification, then update the visit count
        // else update the duration

        if (cumulative)
            log.setDuration(log.getDuration() + duration);
        else {
            save = save || log.getDuration()!=duration;
            log.setDuration(duration);
        }

        // Update the totals

        if (incrementTotal)
            log.setTotal(log.getTotal() + increment);
        else {
            save = save || log.getTotal()!=total;
            log.setTotal(total);
        }

        // Save the log entry

        if (save) HibernateUtils.save(log);
    }

    /**
     * Adds all the timing events to the performance logs
     *
     * @param events List of events to work on
     */
    private static void checkDispatcherEvents(List<Event> events) {

        // Get a list of events to work on
        // We need to accumulate these on minute boundaries otherwise it will create an enormous
        // number of database updates

        Map<String, Event> eventList=new HashMap<>();
        for (int i=events.size()-1; i>=0; i--) {
            Event event=events.get(i);
            if (event.isName(EVENT_DISPATCHER)) {
                Calendar tmp=Calendar.getInstance();
                tmp.setTime(event.timeAdded);
                tmp.set(Calendar.SECOND, 0);
                tmp.set(Calendar.MILLISECOND, 0);
                String key = tmp.getTime() + event.getType();
                if (eventList.containsKey(key)) {
                    event.duration += eventList.get(key).duration;
                    event.count = eventList.get(key).count + 1;
                }
                else {
                    event.count++;
                }
                eventList.put(key, event);
                events.remove(i);
            }
        }

        // If we have something to work on

        if (!Common.isBlank(eventList)) {
            logger.debug("Working on {} DispatcherEvents", eventList.size());
            for (Event event : eventList.values()) {

                // We store these in the log table aggregated on the minute they occur in

                addLogEntry(event.timeAdded, EVENT_DISPATCHER, event.type, true, true, event.duration, event.count, 0);
            }
        }
    }

    /**
     * Adds all the user status events to the performance logs
     *
     * @param events List of events to work on
     */
    private static void checkUserStatusEvents(List<Event> events) {

        // Get a list of events to work on - we need to ignore the ones for the same session
        // that are older than the latest

        Map<String, Event> eventList=new HashMap<>();
        for (int i=events.size()-1; i>=0; i--) {
            Event event=events.get(i);
            if (event.isName(EVENT_TYPE_USER_SESSION_UPDATE)) {
                eventList.put(event.getSubtype(), event);
                events.remove(i);
            }
        }

        // If we have something to work on

        if (!Common.isBlank(eventList)) {
            logger.debug("Working on {} UserStatusEvents", eventList.size());
            Database db = new DatabaseHibernate();
            for (Event event : eventList.values()) {
                Map<String, Object> values = new HashMap<>();
                values.put("last_access", event.getTimeAdded());
                db.updateRecord("user_status", String.format("sessionid='%s'", event.getType()), values, false);
                if (db.isInError()) {
                    logger.error("Cannot create user session - {}", db.getLastError());
                }
            }
            db.close();
        }
    }

    /**
     * This method runs all the datasource trigger reports to see if
     * any caches can be invalidated
     */
    private static void checkDatasourceCacheInvalidateTriggers() {

        // Get a list of all the datasources to process

        List<DatasourceEntity> datasources = HibernateUtils.selectEntities("from DatasourceEntity where useCache=true and cacheTriggerReport is not null");
        if (!Common.isBlank(datasources)) {

            // Loop round all the data sources

            for (DatasourceEntity datasource : datasources) {
                logger.debug("Running cache trigger report for [{}]", datasource.getName());
                Writer output=new StringWriter();
                VelocityEngine engine;
                DatabaseApp source=null;
                try {
                    engine= VelocityUtils.getEngine();
                    source=new DatabaseApp(datasource);

                    // Open a connection to the database(s)

                    logger.debug("Opening source {}", datasource.getName());
                    source.open();
                    source.setMaximumResults(0);

                    // Now add the useful stuff to the context

                    logger.debug("Creating velocity context");
                    Context context= VelocityUtils.getVelocityContext();
                    context.put("ClearCache", false);
                    context.put("Source", source);

                    // Carry out the transformation using the script in the database

                    String report= Report.getScript(datasource.getCacheTriggerReport());
                    engine.evaluate(context, output, HibernateUtils.class.getSimpleName(), report);

                    // Check the context to see if the magic variable "ClearCache" is set to true

                    if ((Boolean)context.get("ClearCache")) {
                        logger.debug("Clearing cache for [{}]", datasource.getName());
                        CacheEngine.clear(datasource.getId());
                    }
                }
                catch (Throwable e) {
                    logger.error("Problem running trigger report for [{}] - {}", datasource.getName(), PivotalException.getErrorMessage(e));
                }
                finally{
                    Common.close(source);
                }
            }
        }
    }

    /**
     * This method keeps the log table under some sort of control by
     * only keeping a maximum of X months worth of entries in it
     * Only do this check once per day
     */
    private static void truncateLogTable() {

        try {
            if (lastTruncateLogTable==null || Common.getDay(lastTruncateLogTable) != Common.getDay(new Date())) {
                logger.debug("Archiving {} log table entries older than {} months", Common.getAplicationName(),
                        HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LOG_MONTHS, HibernateUtils.SETTING_LOG_MONTHS_DEFAULT));
                Date date = Common.addDate(new Date(), Calendar.MONTH, -HibernateUtils.getSystemSetting(HibernateUtils.SETTING_LOG_MONTHS, HibernateUtils.SETTING_LOG_MONTHS_DEFAULT));
                HibernateUtils.executeSQL(String.format("delete from log where date_added < '%s'", Common.formatDate(date, "yyyy-MM-dd")));
                lastTruncateLogTable = new Date();
            }
        }
        catch (Exception e) {
            logger.error("Cannot truncate log entries - {}", PivotalException.getErrorMessage(e));
        }
    }


    /**
     * This method checks to see when the last update was made in the database
     * and compares that with the static date we have here.  If there is a difference,
     * the hibernate caches are destroyed and re-created.
     * This is necessary in a clustered environment where a change to the database
     * may have occurred on another node so we need to reload from the database.
     * This wouldn't be the case of course if we were to be using a distributed cache.
     */
    private void checkForStaleHibernate() {

        // Find the most recent audit entry

        List<Object> lastChanges = HibernateUtils.selectSQLEntities("select time_added from change_log order by 1 desc limit 1");
        if (!Common.isBlank(lastChanges)) {
            Timestamp lastChange = (Timestamp)lastChanges.get(0);
            if (HibernateUtils.lastUpdate!=null && HibernateUtils.lastUpdate.getTime()!=lastChange.getTime()) {

                // Clear the hibernate cache

                logger.debug("Force clearing the stale Hibernate cache");
                HibernateUtils.clearCache();

                // Get the latest value for the period

                monitorPeriod = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_MONITOR_PERIOD, monitorPeriod);
            }
            HibernateUtils.lastUpdate = lastChange;
        }
    }

    /**
     * Adds some useful log entries to the system so that we can show historical
     * performance data
     */
    private void addHouseKeepingInfo() {

        // Get the CPU usage

        addLogEntry("cpu.percent", (long) getCpuUsage());

        // Get the memory available

        long percent = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100 / Runtime.getRuntime().maxMemory();
        addLogEntry("heap.percent.used", percent, Runtime.getRuntime().maxMemory());

        // Get the perm memory available

        for (MemoryPoolMXBean mx : ManagementFactory.getMemoryPoolMXBeans()) {
            percent = mx.getUsage().getUsed() * 100 / mx.getUsage().getMax();
            if (mx.getName().matches("(?i).*perm gen"))
                addLogEntry("permgen.percent.used", percent, mx.getUsage().getMax());
            else
                addLogEntry("permgen.percent.used." + mx.getName().replaceAll(" ", "").trim().toLowerCase(), percent);
        }

        // Get a count of the active threads

        addLogEntry("threads.count", Thread.activeCount());

        // Get the current load average

        long loadAverage = (long)(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage() * 1000);
        if (loadAverage>=0) addLogEntry("load.average", loadAverage);

        // Get the file descriptors if they are available

        try {
            MBeanServer server=ManagementFactory.getPlatformMBeanServer();
            ObjectName oName = new ObjectName("java.lang:type=OperatingSystem");
            AttributeList attrs = server.getAttributes(oName, new String[]{"OpenFileDescriptorCount", "MaxFileDescriptorCount"});
            if (!Common.isBlank(attrs)) {
                List<Attribute> list=attrs.asList();
                percent = (Long) list.get(0).getValue() * 1000 / (Long) list.get(1).getValue();
                addLogEntry("file.descriptors", percent, (Long) list.get(1).getValue());
            }
        }
        catch (Exception e) {
            logger.debug("Problem getting file descriptor counts - {}", PivotalException.getErrorMessage(e));
        }

        // Get the number of used connections for all data sources

        Collection<DataSource> pools= PoolBroker.getInstance().getAllActivePools();
        if (!Common.isBlank(pools)) {
            for(DataSource src : pools){
                try {
                    addLogEntry(src.getName() + ".connection.pool.active", src.getNumActive());
                }
                catch (Exception e) {
                    logger.debug("Problem getting pool status information for [{}] - {}", src.getName(), PivotalException.getErrorMessage(e));
                }
            }
        }

        // Add on the cache stats

        Map<String,Object> stats= CacheAccessorFactory.getInstance().getStatistics();
        if (!Common.isBlank(stats) && stats.containsKey("HitRate"))
            addLogEntry("cache.stats.hitrate", ((Double) stats.get("HitRate")).longValue());

        // Get the velocity cache stats

        addLogEntry("velocity.template.cache.size", (long) VelocityResourceCache.getStats().getSize());
        addLogEntry("velocity.template.cache.hitrate", (long) VelocityResourceCache.getStats().getHitRate());
        addLogEntry("velocity.template.cache.missrate", (long) VelocityResourceCache.getStats().getMissRate());

        // Add the NRMM source

        try {
            addLogEntry("jdbc.pool.active", HibernateUtils.getDataSource().getNumActive());
        }
        catch (Exception e) {
            logger.error("Problem getting connection parameters - {}", PivotalException.getErrorMessage(e));
        }

        // Clear out any old sessions

        Integer sessionTimeout = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_SESSION_TIMEOUT, HibernateUtils.SETTING_SESSION_TIMEOUT_DEFAULT);
        if (sessionTimeout > 0) {
            String cutoff = Common.formatDate(Common.addDate(new Date(), Calendar.MINUTE, -sessionTimeout), "yyyy-MM-dd HH:mm:ss");
            logger.debug("Checking for session entries in user_status that are older than {} minutes", sessionTimeout);
            List<BigInteger> values = HibernateUtils.selectSQLEntities(String.format("select count(*) from user_status where last_access < '%s'", cutoff));
            if (values.get(0).longValue() > 0) {
                logger.debug("Removing {} timed out sessions from user_status", values.get(0).longValue());
                List<Object[]> rows = HibernateUtils.selectSQLEntities(String.format("select app_path,sessionid from user_status where last_access < '%s'", cutoff));
                for (Object[] row : rows) {
                    UserManager.logout(row[0].toString(), row[1].toString());
                }
            }
        }

        // Add in the login count

        List<BigInteger> values = HibernateUtils.selectSQLEntities("select count(*) from user_status");
        addLogEntry("users.count", values.get(0).intValue());
    }

    /**
     * Adds an event to the queue
     *
     * @param eventName Name of the event
     * @param eventType Type of the event
     * @param eventValue Value of the event
     */
    public synchronized static void addEvent(String eventName, String eventType, String eventValue) {
        eventQueue.add(new Event(eventName, eventType, eventValue));
    }

    /**
     * Adds an event to the queue
     *
     * @param eventName Name of the event
     * @param eventType Type of the event
     * @param eventSubType Sub type of the event
     * @param eventValue Value of the event
     */
    public synchronized static void addEvent(String eventName, String eventType, String eventSubType, String eventValue) {
        eventQueue.add(new Event(eventName, eventType, eventSubType, eventValue));
    }

    /**
     * Adds an event to the queue
     *
     * @param eventName Name of the event
     * @param eventType Type of the event
     * @param eventValue Value of the event
     */
    public synchronized static void addEvent(String eventName, String eventType, long eventValue) {
        eventQueue.add(new Event(eventName, eventType, eventValue));
    }

    /**
     * Adds an event to the queue
     *
     * @param eventName Name of the event
     * @param eventType Type of the event
     * @param eventSubType Sub type of the event
     * @param eventValue Value of the event
     */
    public synchronized static void addEvent(String eventName, String eventType, String eventSubType, long eventValue) {
        eventQueue.add(new Event(eventName, eventType, eventSubType, eventValue));
    }

    /**
     * Calculates the CPU usage as a percentage
     *
     * @return Percentage
     */
    public static double getCpuUsage() {

        // Get the CPU average

        double cpuUsage=0;
        try {
            long systemTime = System.nanoTime();
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long processCpuTime = os.getProcessCpuTime();
            cpuUsage = (double)(processCpuTime - lastProcessCpuTime ) / (systemTime - lastSystemTime );
            lastSystemTime = systemTime;
            lastProcessCpuTime = processCpuTime;
        }
        catch (Throwable e) {
            logger.debug("Cannot collect CPU time - server not capable");
        }
        return cpuUsage * 100;
    }

    /**
     * Returns a copy of the event queue
     *
     * @return Copy of the queue
     */
    public static List<Event> getEventQueue() {
        return getEventQueue(false);
    }

    /**
     * Returns a copy of the event queue and optionally clears it afterwards
     *
     * @param clear True if the queue should be cleared
     * @return Copy of the queue
     */
    protected synchronized static List<Event> getEventQueue(boolean clear) {
        List<Event> events=new ArrayList<>();
        events.addAll(eventQueue);
        if (clear) eventQueue.clear();
        return events;
    }

    /**
     * Holder for an event
     */
    public static class Event {

        String name;
        String type;
        String subtype;
        String value;
        long duration;
        int count;
        Date timeAdded = new Date();
        // Stores the start time for the event
        long timeStarted;
        // Stores the end time for the event
        long timeEnded;
        ScheduledTaskEntity task;
        DatasourceEntity source;
        Map<String,Object> parameters;

        /**
         * Adds an event o the queue for later processing
         *
         * @param name Name of the event
         * @param type Type of the event
         * @param value Value of the event
         */
        private Event(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
            timeStarted = -1;
            timeEnded = -1;
        }

        /**
         * Adds an event o the queue for later processing
         *
         * @param name Name of the event
         * @param type Type of the event
         * @param duration Value of the event
         */
        private Event(String name, String type, long duration) {
            this.name = name;
            this.type = type;
            this.duration = duration;
            timeEnded = timeAdded.getTime();
            timeStarted = timeEnded - duration;
        }

        /**
         * Adds an event o the queue for later processing
         *
         * @param name Name of the event
         * @param type Type of the event
         * @param subtype Type of the event
         * @param value Value of the event
         */
        private Event(String name, String type, String subtype, String value) {
            this.name = name;
            this.type = type;
            this.subtype = subtype;
            this.value = value;
            timeStarted = -1;
            timeEnded = -1;
        }

        /**
         * Adds an event o the queue for later processing
         *
         * @param name Name of the event
         * @param type Type of the event
         * @param subtype Type of the event
         * @param duration Value of the event
         */
        private Event(String name, String type, String subtype, long duration) {
            this.name = name;
            this.type = type;
            this.subtype = subtype;
            this.duration = duration;
            timeEnded = timeAdded.getTime();
            timeStarted = timeEnded - duration;
        }

        /**
         * Returns true if the event name matches any of the values
         *
         * @param values Array of values to check
         *
         * @return True if the name matches any of the values
         */
        public boolean isName(String... values) {
            return Common.doStringsMatch(name, values);
        }

        /**
         * Returns true if the event type matches any of the values
         *
         * @param values Array of values to check
         *
         * @return True if the type matches any of the values
         */
        public boolean isType(String... values) {
            return Common.doStringsMatch(type, values);
        }

        /**
         * Returns true if the event subtype matches any of the values
         *
         * @param values Array of values to check
         *
         * @return True if the subtype matches any of the values
         */
        public boolean isSubType(String... values) {
            return Common.doStringsMatch(subtype, values);
        }

        /**
         * Get the name of the event
         *
         * @return Name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the type of the event
         *
         * @return Type
         */
        public String getType() {
            return type;
        }

        /**
         * Get the sub-type of the event
         *
         * @return SubType
         */
        public String getSubtype() {
            return subtype;
        }

        /**
         * Get the time the event was added
         *
         * @return Date
         */
        public Date getTimeAdded() {
            return timeAdded;
        }

        /**
         * Get the value of the event
         *
         * @return String
         */
        public String getValue() {
            return value;
        }

        /**
         * Show a convenient string representation of the event
         *
         * @return Useful representation
         */
        public String toString() {
            StringBuilder out=new StringBuilder();
            out.append(name);
            if (type!=null) out.append(" type:" + type);
            if (subtype!=null) out.append(" subtype:" + subtype);
            if (value!=null) out.append(" value:" + value);
            out.append(" duration:" + duration);
            out.append(" count:" + count);
            out.append(" timeadded:" + timeAdded);
            if (task!=null) out.append(" task:" + task.getName());
            if (source!=null) out.append(" source:" + source.getName());
            return out.toString();
        }
    }

}
