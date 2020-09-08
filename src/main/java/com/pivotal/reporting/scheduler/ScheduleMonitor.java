/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.scheduler;

import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.Monitor;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pivotal.utils.PivotalException;
import org.hibernate.jdbc.Work;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * This class is the main scheduler manager that is responsible
 * for searching through the list of schedulable tasks and running
 * them
 */
public class ScheduleMonitor extends Monitor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduleMonitor.class);

    private static ScheduleMonitor instance;

    /** The scheduler map of currently running tasks */
    private static Map<Integer,Job> taskList;

    /**
     * Initialise the system
     *
     * @param name Name of the monitor
     * @return a {@link ScheduleMonitor} object.
     */
    public static ScheduleMonitor init(String name) {
        if (instance!=null) {
            instance.stopMonitor();
        }
        instance = new ScheduleMonitor();
        instance.setMonitorName(name);
        instance.setPeriod(10);
        return instance;
    }

    /**
     * Returns the running instance
     *
     * @return Instance of the monitor
     */
    public static ScheduleMonitor getInstance() {
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

    /**
     * Adds the task to the map of currently running jobs
     *
     * @param task Job to add
     */
    synchronized public static void addTaskToRunningList(Job task) {
        if (taskList==null) taskList=new LinkedHashMap<>();
        taskList.put(task.getTaskId(), task);
    }

    /**
     * Removes the task from the map of currently running jobs
     *
     * @param task Job to remove
     */
    synchronized public static void removeTaskFromRunningList(Job task) {
        removeTaskFromRunningList(task.getTaskId());
    }

    /**
     * Removes the task from the map of currently running jobs
     *
     * @param taskId Id of the task to remove
     */
    synchronized public static void removeTaskFromRunningList(int taskId) {
        if (!Common.isBlank(taskList)) {
            taskList.remove(taskId);
        }
    }

    /**
     * Returns a clone of the running task list
     *
     * @return Set of tasks
     */
    synchronized public static Map<Integer,Job> getRunningTaskList() {
        return taskList==null?null:new LinkedHashMap<>(taskList);
    }

    /**
     * Launches the specified task as a new job
     *
     * @param task Task to launch
     */
    public static void launchTask(ScheduledTaskEntity task) {
        Job newTask = new ReportJob(task);
        NotificationManager.addNotification(I18n.getString("system.scheduled.report.started", task.getName()), Notification.NotificationLevel.Info, Notification.NotificationGroup.Admin, Notification.NotificationType.Application, true);

        // Start the task

        newTask.start();

        // Add the task to our running list

        addTaskToRunningList(newTask);
    }

    /** {@inheritDoc} */
    @Override
    public void runTask() {

        if (isRunning) {
            if (HibernateUtils.getSystemSetting(HibernateUtils.SETTING_SCHEDULING_ENABLED, HibernateUtils.SETTING_SCHEDULING_ENABLED_DEFAULT)) {
                logger.debug("Checking schedule");
                checkScheduledTasks();
            }
            else
                logger.debug("Scheduling is disabled");
        }
        else {

            // The scheduler has been interrupted and told to shut down
            // We need to loop through and tell any running tasks to die

            logger.debug("Stopping running tasks");
            Map<Integer,Job> jobs=getRunningTaskList();
            if (!Common.isBlank(jobs)) {
                for(Job job : jobs.values()) {
                    try {
                        logger.debug("Requesting task stop for - {}", job.getName());
                        job.stopTask();
                    }
                    catch(Exception e) {
                        logger.error("Problem stopping task - {}", PivotalException.getErrorMessage(e));
                    }
                }
            }

            // Close the current Hibernate session - this will force a reload
            // of the session the next time around.  This follows the "session per request"
            // pattern

            try {
                HibernateUtils.closeSession();
            }
            catch (Exception e) {
                logger.warn("Problem closing the Hibernate session - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Reads the schedule tasks table and looks for any non-locked
     * tasks that are ready to run, then runs them
     */
    private static void checkScheduledTasks() {

        try {
            // We need to lock the table whilst we do this operation to prevent other NRMM servers
            // conflicting with us

            if (HibernateUtils.getDataSource().isMySQL()) {
                HibernateUtils.getCurrentSession().doWork(new Work() {
                    @Override
                    public void execute(Connection connection) throws SQLException {
                        connection.nativeSQL("lock tables scheduled_task write");
                    }
                });
            }

            // Get a list of the possible tasks to run

            List<ScheduledTaskEntity> runList=new ArrayList<>();
            List<ScheduledTaskEntity> list=HibernateUtils.selectEntities("from ScheduledTaskEntity where locked=false and disabled=false and taskType not in ('webservice')", true);
            if (!Common.isBlank(list)) {

                // Order the list by the oldest first

                // Loop through each of the tasks checking to see if they are
                // available to run

                for (ScheduledTaskEntity task : list) {
                    if (task.scheduledToRun()) {
                        task.updateScheduledTaskStatus(true, null, new Date());
                        runList.add(task);
                    }
                }
            }

            // OK, we now have a list of tasks to actually run
            // Create a job for each of these and launch them

            if (!Common.isBlank(runList)) {
                logger.debug("Found {} tasks to run", runList.size());
                for (ScheduledTaskEntity task : runList) {
                    logger.debug("Launching task {}", task.getName());

                    // Check the type of job to launch

                    launchTask(task);
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem retrieving list of tasks - {}", PivotalException.getErrorMessage(e));
        }
        finally {
            try {
                if (HibernateUtils.getDataSource().isMySQL()) {

                    // Make sure we release the lock on the table

                    try {
                        HibernateUtils.getCurrentSession().doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                connection.nativeSQL("unlock tables");
                            }
                        });
                    }
                    catch (Exception e) {
                        logger.error("Problem unlocking schedule table - {}", PivotalException.getErrorMessage(e));
                    }
                }
            }
            catch (Exception e) {
                logger.error("Cannot determine source type - {}", PivotalException.getErrorMessage(e));
            }
        }
    }
}
