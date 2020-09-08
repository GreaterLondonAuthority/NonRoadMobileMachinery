/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.monitoring;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.Progress;
import com.pivotal.web.Constants;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is the monitoring queue handler
 */
public abstract class Monitor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Monitor.class);

    // Class constants

    public static final int DEFAULT_PERIOD_SEC = 60;
    public static final int DEFAULT_DEAD_PERIOD_SEC = 300;
    public static final int DEFAULT_PERIOD_WATCHDOG_SEC = 120;
    public static final int STOP_WAIT_TIMEOUT_SEC = 20;
    private static final int SHUTDOWN_WAIT_SEC = 1;

    protected int monitorPeriod = DEFAULT_PERIOD_SEC;
    protected int monitorDeadPeriod = DEFAULT_DEAD_PERIOD_SEC;
    protected String monitorName;

    /**
     * Queue of processes used for consumers to act upon
     */
    private Class<? extends ConsumerThread> threadClass = null;
    private BlockingQueue<ConsumerObject> processorQueue = null;
    private Set<ConsumerThread> consumers = null;
    private int maxPoolSize = 0;
    private int scaleFactor = 0;

    /**
     * The Monitor inner class
     */
    protected MonitorThread monitor;

    /**
     * The watchdog timer
     */
    private Timer watchdog;

    /**
     * A running list of all those monitors that have been created with a consumer pool
     */
    private static Set<Monitor> monitors = new HashSet<>();

    /**
     * Flags to indicate if the monitor is running
     */
    protected boolean isRunning;
    protected boolean isExecuting;
    protected boolean isShutdownComplete;
    protected boolean isTriggered;

    protected Progress progress = new Progress();
    private Date lastMonitorRun;
    private int lastRunDuration;
    private int maxRunDuration;
    private Date maxRunDate;
    private final List<Integer> averages = new ArrayList<>();
    private boolean hasWatchdog;
    private boolean isDynamicLoggingDisabled;

    /**
     * Monitor with default values
     */
    protected Monitor() {
        init(getClass().getSimpleName(), DEFAULT_PERIOD_SEC, DEFAULT_DEAD_PERIOD_SEC);
    }

    /**
     * Initialise the system
     *
     * @param name       Name of the monitor
     * @param period     Period in seconds
     * @param deadPeriod Period for determining if thread has died
     */
    private void init(String name, int period, int deadPeriod) {
        monitorName = Common.getAplicationName() + ' ' + name;
        monitorPeriod = period;
        monitorDeadPeriod = deadPeriod;
        isRunning = false;
        isShutdownComplete = true;
        isTriggered = false;
        monitor = null;
        lastMonitorRun = null;
        lastRunDuration = 0;
        maxRunDuration = 0;
        maxRunDate = null;
        averages.clear();
        hasWatchdog = true;
    }

    /**
     * Initialise the execution thread pool with consumers and set the maximum size
     * it can grow to and the way in which it will scale
     * @param threadClass The execution class to use
     * @param maxPoolSize Maximum size of the thread pool to create
     * @param scaleFactor The scale factor to use to dynamically increase the size of the pool
     */
    public void initConsumers(Class<? extends ConsumerThread> threadClass, int maxPoolSize, int scaleFactor) {
        this.threadClass = threadClass;
        this.maxPoolSize = maxPoolSize;
        this.scaleFactor = scaleFactor;
        processorQueue = new LinkedBlockingQueue<>();
        consumers = new LinkedHashSet<>();
    }

    /**
     * Returns the list of monitors that are currently running
     * @return List of monitors
     */
    public static List<Monitor> getMonitors() {
        return Common.sortListObjects(new ArrayList<>(monitors), "getMonitorName");
    }

    /**
     * Returns the period of the monitor cycle
     *
     * @return Period in seconds
     */
    public int getPeriod() {
        return monitorPeriod;
    }

    /**
     * Sets the period of the monitor cycle
     * This will need a restart to take affect
     *
     * @param monitorPeriod Period in seconds
     */
    public void setPeriod(int monitorPeriod) {
        this.monitorPeriod = monitorPeriod;
    }

    /**
     * Sets the period after which the monitor is assumed dead
     * This will need a restart to take affect
     *
     * @param monitorDeadPeriod a int.
     */
    public void setDeadPeriod(int monitorDeadPeriod) {
        this.monitorDeadPeriod = monitorDeadPeriod;
    }

    /**
     * Gets the name of the background monitor thread
     *
     * @return Name of the monitor thread
     */
    public String getMonitorName() {
        return monitorName;
    }

    /**
     * Sets the name of the background monitor thread
     * This will need a restart to take affect
     *
     * @param monitorName a {@link java.lang.String} object.
     */
    public void setMonitorName(String monitorName) {
        this.monitorName = Common.getAplicationName() + ' ' + monitorName;
    }

    /**
     * Sets that there should be no watchdog
     */
    public void requiresNoWatchdog() {
        this.hasWatchdog = false;
    }

    /**
     * Sets that there should be a watchdog
     */
    public void requiresWatchdog() {
        this.hasWatchdog = true;
    }

    /**
     * Starts the monitor
     *
     * @throws com.pivotal.utils.PivotalException Errors
     */
    public void startMonitor() throws PivotalException {
        stopMonitor();
        try {
            isRunning = true;
            isTriggered = true;
            monitor = new MonitorThread();
            monitor.start();

            // Add a watchdog timer if we need one

            if (hasWatchdog) {
                watchdog = new Timer(monitorName + " Watchdog");
                watchdog.schedule(new Watchdog(), DEFAULT_PERIOD_WATCHDOG_SEC * 1000, DEFAULT_PERIOD_WATCHDOG_SEC * 1000);
            }
            logger.info("Started %s", monitorName);
            monitors.add(this);
        }
        catch (Exception e) {
            isRunning = false;
            throw new PivotalException(e);
        }
    }

    /**
     * Stops the monitor
     */
    public void stopMonitor() {
        isRunning = false;
        isShutdownComplete = false;

        // Cancel the watchdog timer

        if (watchdog!=null) {
            watchdog.cancel();
        }

        // Close down the monitor

        closeMonitor();

        // Shutdown all the consumers and clear the pool

        closeConsumerPool();

        watchdog = null;
        monitor = null;
        monitors.remove(this);
    }

    /**
     * Causes the Monitor to wake up if it is sleeping and start another
     * schedule cycle
     */
    public final void triggerMonitor() {
        if (monitor != null && monitor.isAlive()) {
            logger.debug("Triggered %s", monitorName);
            isTriggered = true;
            monitor.interrupt();
        }
        else {
            logger.debug("Restart %s", monitorName);
            startMonitor();
        }
    }

    /**
     * Returns true if the monitor is still running
     *
     * @return True if running
     */
    public final boolean isTriggered() {
        return isTriggered;
    }

    /**
     * Returns true if the monitor is still running
     *
     * @return True if running
     */
    public final boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns true if the monitor is still executing a task
     *
     * @return True if executing
     */
    public final boolean isExecuting() {
        return isExecuting;
    }

    /**
     * Returns true if this monitor has a watchdog timer
     * @return True if watchdog cinfigured
     */
    public boolean hasWatchdog() {
        return hasWatchdog;
    }

    /**
     * Maximum size of the consumer pool
     * @return Max size it can grow to
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Scaling factor being used to increase the size of the pool
     * @return Scaling factor
     */
    public int getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Returns a size of the consumer thread pool
     * @return Size of the thread pool
     */
    public int getNoOfConsumers() {
        return consumers!=null?consumers.size():-1;
    }

    /**
     * Returns the length of the process queue
     * @return Number of jobs queued
     */
    public int getQueueLength() {
        return processorQueue!=null?processorQueue.size():-1;
    }

    /**
     * Returns the duration in milliseconds of the last run
     *
     * @return Milliseconds
     */
    public final int getLastRunDuration() {
        return lastRunDuration;
    }

    /**
     * Returns the maximum time in milliseconds that a background run has taken
     *
     * @return Milliseconds
     */
    public final int getMaxRunDuration() {
        return maxRunDuration;
    }

    /**
     * Returns the date when the maximum time occurred
     *
     * @return Milliseconds
     */
    public final Date getMaxRunDate() {
        return maxRunDate;
    }

    /**
     * Returns true if the logging level changes depending upon time
     * taken to run the tasks
     * @return True if logging is dynamic
     */
    public boolean isDynamicLoggingDisabled() {
        return isDynamicLoggingDisabled;
    }

    /**
     * Sets if the monitor logging should be dynamic i.e. if the task
     * takes a long time, should the logging switch to debug
     * @param isDynamicLoggingDisabled True if dynamic
     */
    public void setDynamicLoggingDisabled(boolean isDynamicLoggingDisabled) {
        this.isDynamicLoggingDisabled = isDynamicLoggingDisabled;
    }

    /**
     * Calculates the rolling average of the run durations
     *
     * @return Real number milliseconds
     */
    public final double getAverageRunDuration() {
        double returnValue = 0.0;
        if (!Common.isBlank(averages)) {
            synchronized (averages) {
                for (Integer value : averages) {
                    returnValue += value;
                }
                returnValue /= averages.size();
            }
        }
        return returnValue;
    }

    /**
     * Return the timestamp of the last time the thread ran
     *
     * @return Date
     */
    public final Date getLastRunDate() {
        return lastMonitorRun;
    }

    /**
     * This method must be overridden by classes
     */
    abstract public void runTask();

    /**
     * Attempts to gently stop the consumer (if that doesn't work it kills it)
     * and removes it from the consumer pool
     * @param consumer Consumer to remove
     */
    private void removeConsumer(ConsumerThread consumer) {
        if (consumer!=null && !Common.isBlank(consumers)) {

            // Remove it from the pool

            consumers.remove(consumer);

            // If it's alive, then signal it to stop

            if (consumer.isAlive()) {
                long start = System.currentTimeMillis();
                consumer.setRunning(false);
                if (!consumer.isExecuting()) {
                    consumer.interrupt();
                }

                // Wait for it to stop of it's own free will

                while (consumer.isAlive() && (System.currentTimeMillis() - start) < (STOP_WAIT_TIMEOUT_SEC * 1000)) {
                    Common.sleep(SHUTDOWN_WAIT_SEC * 1000);
                }

                // If it hasn't stopped, then kill it

                if (consumer.isAlive()) {
                    Common.stopThread(consumer);
                }
            }
        }
    }

    /**
     * Attempts to gently stop the consumer (if that doesn't work it kills it)
     * and removes it from the consumer pool
     */
    private void closeConsumerPool() {
        if (!Common.isBlank(consumers)) {

            // Ask the consumers to stop nicely

            for (ConsumerThread consumer : consumers) {
                if (consumer.isAlive()) {
                    consumer.setRunning(false);
                    if (!consumer.isExecuting()) {
                        consumer.interrupt();
                    }
                }
            }
            Common.sleep(SHUTDOWN_WAIT_SEC * 1000);

            // Wait for them all to stop and if they don't within a reasonable time,
            // kill them all

            boolean stopped;
            long start = System.currentTimeMillis();
            do {
                stopped = true;
                for (ConsumerThread consumer : consumers) {
                    if (consumer.isAlive()) {
                        stopped = false;
                    }
                }
                if (!stopped) {
                    Common.sleep(SHUTDOWN_WAIT_SEC * 1000);
                }
            } while (!stopped && (System.currentTimeMillis() - start) < (STOP_WAIT_TIMEOUT_SEC * 1000));

            // Kill all the naughty boys that have not shutdown by themselves

            for (ConsumerThread consumer : consumers) {
                if (consumer.isAlive()) {
                    Common.stopThread(consumer);
                }
            }
            consumers.clear();
        }
    }

    /**
     * Shuts down the monitor thread - tries it nicely first and then
     * forcibly if that doesn't work
     */
    private void closeMonitor() {
        if (monitor !=null && monitor.isAlive()) {
            logger.info("Stopping %s", monitorName);
            isRunning = false;
            if (!isExecuting) {
                monitor.interrupt();
            }

            // We need to wait until the monitor has completed

            try {
                long lStart = System.currentTimeMillis();
                while (!isShutdownComplete) {
                    Common.sleep(SHUTDOWN_WAIT_SEC * 1000);
                    if (System.currentTimeMillis() - lStart > STOP_WAIT_TIMEOUT_SEC * 1000) {
                        isShutdownComplete = true;
                        logger.error("Timed out [%d secs] waiting for shutdown of [%s] - killed the thread", STOP_WAIT_TIMEOUT_SEC, monitorName);
                        Common.stopThread(monitor);
                    }
                }
            }
            catch (Exception e) {
                logger.debug("Exception in the %s Monitor - %s", monitorName, PivotalException.getErrorMessage(e));
            }
            logger.info("Stopped %s", monitorName);
        }
    }

    /**
     * Adjusts the consumer pool to try and keep up with demand
     */
    private void adjustConsumerPool() {

        // Only do something if we are configured to manage a thread pool

        if (processorQueue!=null) {

            // Check for any consumers that are taking a long time or are actually dead

            Set<ConsumerThread> test = new HashSet<>();
            test.addAll(consumers);
            Date deadDate = Common.addDate(new Date(), Calendar.SECOND, -monitorDeadPeriod);
            for (ConsumerThread thread : test) {

                // If the thread is dead then get rid of it from the pool

                if (!thread.isAlive()) {
                    logger.error("Found a dead consumer [%s] in monitor [%s] - killing", thread.getName(), getMonitorName());
                    removeConsumer(thread);
                }

                // If the thread is stuck, then kill it and remove it from the pool
                // Need to have this in a try/catch because timing may cause one of the methods to fail

                try {
                    if (thread.getLastRun() != null && thread.isExecuting() && thread.getLastRun().before(deadDate)) {
                        logger.error("Found a consumer [%s] in monitor [%s] that has been running job [%s] since [%tc] - killing", thread.getName(), getMonitorName(), thread.getRunningJobName(), thread.getLastRun());
                        removeConsumer(thread);
                    }
                }
                catch (Exception e) {
                    logger.debug("Problem checking and/or removing thread - %s", PivotalException.getErrorMessage(e));
                }
            }

            // If there is a ratio of more than scale factor between controls and consumers, then
            // add more consumers but only if we haven't already maxed out

            while (consumers.size() < maxPoolSize && ((consumers.size() < (processorQueue.size() / scaleFactor)) || consumers.size() == 0)) {
                try {
                    Constructor consumerConstructor = threadClass.getConstructor(new Class[]{});
                    ConsumerThread consumer = (ConsumerThread) consumerConstructor.newInstance();
                    consumer.setProcessorQueue(processorQueue);
                    consumer.setMonitor(this);
                    consumer.setIndex(consumers.size() + 1);
                    consumers.add(consumer);
                    consumer.start();
                    logger.debug("Increased the consumer pool to %d", consumers.size());
                }
                catch (Exception e) {
                    logger.error("Cannot create consumer %s - %s", threadClass, PivotalException.getErrorMessage(e));
                }
            }
        }
    }

    /**
     * Adds the object to the process queue so that it can be consumed by a pooled thread
     * This uses the objects toString method to make sure that an object of this instance
     * isn't already on the queue. This prevents the queue from building up with lots
     * of long running or 'stuck' objects
     * @param object Object to work on
     */
    protected void addObjectToProcessQueue(ConsumerObject object) {
        if (object!=null) {
            boolean addJob = true;

            // Check if it is already queued

            for (ConsumerObject tmp : processorQueue) {
                if (Common.doStringsMatch(tmp.getName(), object.getName())) {
                    addJob = false;
                    logger.warn("Cannot add job [%s] to queue because it is already queued - queued %tc", object.getName(), object.getStartTime());
                    break;
                }
            }

            // Now check to see if it is currently running

            if (addJob) {
                for (ConsumerThread tmp : consumers) {
                    if (tmp.isExecuting() && Common.doStringsMatch(tmp.getRunningJobName(), object.getName())) {
                        addJob = false;
                        logger.warn("Cannot add job [%s] to queue because it is still running - started %tc", object.getName(), object.getStartTime());
                        break;
                    }
                }
            }
            if (addJob) {
                logger.debug("Added consumer task %s", object.getFullName());
                processorQueue.add(object);
            }
        }
    }

    /**
     * Autonomous thread for periodically checking the event queue table
     * for tasks that need to be run
     */
    class MonitorThread extends Thread {

        private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MonitorThread.class);
//        private Level defaultLevel = logger.getLevel();
        private int overrunCount;
        private int OVERRUN_THRESHOLD = 5;

        /**
         * This method is called by the thread manager to begin the thread
         * Loop round indefinitely checking the status of the monitor
         */
        public void run() {

            // Loop round whilst we're not interrupted and we should be running

            setName(monitorName);
            setPriority(MIN_PRIORITY);
            lastMonitorRun = new Date();
            Progress localProgress = new Progress();
            while (isRunning) {
                try {

                    // If we have not been triggered by someone, then sleep for a while
                    // This allows for another go round the loop when we were triggered during an
                    // actual cycle and not during the sleep period

                    if (!isTriggered()) {
                        try {
                            while (Common.diffDate(lastMonitorRun, new Date(), Calendar.SECOND) < monitorPeriod) {
                                sleep(500);
                            }
                        }
                        catch (InterruptedException e) {
                            logger.debug("Sleep has been interrupted");
                        }
                    }

                    // Set some flags to indicate that we're doing something

                    isExecuting = true;
                    lastMonitorRun = new Date();
                    localProgress.resetStartTime();
                    progress.resetStartTime();
                    isTriggered = false;

                    // If we haven't been told to stop

                    if (isRunning) {
                        logger.debug("%s Monitor tick started", monitorName);

                        // Run the registered tasks

                        runTask();

                        // Check to see if the critical parts of this thread are running slowly

                        changeLogLevelIfRunningSlowly();
                        logger.debug("%s Monitor tick completed in [%d] seconds", monitorName, progress.getSecondsElapsed(true));

                        // Adjust the consumer thread pool if we have one

                        adjustConsumerPool();
                    }
                }
                catch (Throwable e) {
                    logger.error("Problem in the Monitor [%s] - %s", monitorName, PivotalException.getErrorMessage(e));

                    // We don't want an error here to completely fill up the log file because of a
                    // persistent fault that doesn't reset the timer - so reset it so that at least
                    // the error wouldn't happen hundreds of times per second

                    lastMonitorRun = new Date();
                }
                finally {

                    // Close the current Hibernate cache otherwise changes in other sessions
                    // notably the UI are not going to be visible here

                    HibernateUtils.closeSession();
                    isExecuting = false;
                }
            }
            isShutdownComplete = true;
        }

        /**
         * Dynamically adjusts the logging level for the background process so that we can
         * get logging information out of the process whenever it starts to run slowly.  If the situation
         * repairs itself after X successful runs then reset the logging back to the default
         */
        private void changeLogLevelIfRunningSlowly() {

            // Record the running stats

            lastRunDuration = progress.getMilliSecondsElapsed();
            if (lastRunDuration > maxRunDuration) {
                maxRunDuration = lastRunDuration;
                maxRunDate = new Date();
            }
            synchronized (averages) {
                averages.add(lastRunDuration);
                while (averages.size() > 10)
                    averages.remove(0);
            }

            // If the thread is taking longer than the allowed time for the whole loop then
            // change the log level to debug

            if (!Constants.inIde() && !isDynamicLoggingDisabled && getPeriod() > 0) {
                if (progress.getSecondsElapsed() > monitorPeriod && !logger.isDebugEnabled()) {

                    // Set the log level to debug and make sure it cant be reset until we've been through another
                    // OVERRUN_THRESHOLD times

                    logger.warn("Switching %s logging to DEBUG because the background task is taking a long time - [%d] seconds", monitorName, progress.getSecondsElapsed());
//                    defaultLevel = logger.getLevel();
//                    logger.setLevel(Level.DEBUG);
                    overrunCount = OVERRUN_THRESHOLD;
                }

                // If the monitor is OK then make sure the logging level is set back to what it was before
                // if we have managed to go through a few times

                else {
                    if (overrunCount > 0) overrunCount--;
//                    if (overrunCount == 0 && logger.getLevel() != defaultLevel && logger.getLevel() != null) {
//                        logger.setLevel(defaultLevel);
                        logger.warn("Switching %s logging back to default because the background task is back in time - [%d] seconds", monitorName, progress.getSecondsElapsed());
//                    }
                }
            }
        }
    }

    /**
     * Belt and braces thread task that check to make sure the
     * main monitor thread hasn't mysteriously died
     */
    class Watchdog extends TimerTask {

        @Override
        public void run() {

            /**
             * Check to see if the monitor has been dead for too long - it uses this as a means
             * of determining if the monitor has been killed or just died and actually needs
             * automatically restarting
             */
            try {
                if (isRunning && (monitor==null || !monitor.isAlive())) {
                    logger.warn("%s appears to be dead - checking to see if it is just busy", monitorName);
                    if (Common.getTimeDifference(lastMonitorRun) > monitorDeadPeriod * 1000) {

                        // Something horrible has happened and the thread has died
                        // so start it up again

                        logger.warn("Something happened and caused the %s Monitor thread to die - restarting it", monitorName);
                        startMonitor();
                    }
                }
            }
            catch (Exception e) {
                logger.error("Problem during checkMonitorHealth - %s", PivotalException.getErrorMessage(e));
            }

            // Release the session, just in case

            HibernateUtils.closeSession();
        }
    }

}
