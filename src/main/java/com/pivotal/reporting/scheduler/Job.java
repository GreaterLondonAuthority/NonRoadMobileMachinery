/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.scheduler;

import com.pivotal.reporting.publishing.Publisher;
import com.pivotal.reporting.publishing.PublisherFactory;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.RuntimeParameter;
import com.pivotal.reporting.reports.VelocityReport;
import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.ExecutionResults;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.ServletHelper;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This is the base class for all jobs that run within NRMM and provides
 * some common methods for status message handling etc
 * It is responsible for updating the task status within the database
 * which acts as the cross-thread communication mechanism - a bit clumsy
 * perhaps but does mean that tasks not running on this machine can
 * be monitored
 */
abstract public class Job extends Thread {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Job.class);

    int scheduledTaskID;
    boolean running;
    Thread runningThread;
    ScheduledTaskEntity scheduledTask;
    Date startDate;
    List<String> runtimeErrors=new ArrayList<>();


    /**
     *
     * Constructs a new entry of the task queue. It won't be associated with any task
     */
    protected Job() {
    }

    /**
     *
     * Constructs a new entry of the task queue
     *
     * @param objEntry The task entry to execute
     */
    public Job(ScheduledTaskEntity objEntry) {
        scheduledTask=objEntry;
        scheduledTaskID=objEntry.getId();
    }

    /**
     * Returns the task ID of this task
     *
     * @return ID
     */
    public int getTaskId() {
        return scheduledTaskID;
    }

    /**
     * Returns true if the task has not been told to stop
     *
     * @return True if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Instructs the task to stop at it's convenience
     * This sets a flag to tell the task to stop what it is doing
     * and complete normally - therefore, it could take some
     * time for the task to actually come to a close
     */
    public void stopTask() {
        logger.info("Scheduled task [{}] is being asked to stop by system/user", scheduledTask);
        running=false;

        // If the thread exists and is waiting then interrupt it

        if (runningThread!=null && runningThread.getState().equals(Thread.State.TIMED_WAITING))
            runningThread.interrupt();
    }

    /**
     * Sets the current status message for this task
     *
     * @param message Message to display
     */
    public void setStatusMessage(String message) {
        scheduledTask.setSchedLastError(Common.dateFormat(new Date(),"yyyy-MM-dd h:mm:ss a ") + message);
        updateTaskStatus();
    }

    /**
     * Convenience method for adding a message to the current status
     * message.  The new message is appended with a line break
     *
     * @param message Text to append
     */
    @SuppressWarnings("unused")
    public void appendStatusMessage(String message) {
        if (Common.isBlank(scheduledTask.getSchedLastError()))
            scheduledTask.setSchedLastError(message);
        else
            scheduledTask.setSchedLastError(scheduledTask.getSchedLastError().split("\n")[0] + '\n' + message);
        updateTaskStatus();
    }

    /**
     * Returns the current status message for this job
     *
     * @return String that may contain line breaks
     */
    @SuppressWarnings("unused")
    public String getStatusMessage() {
        return scheduledTask.getSchedLastError();
    }

    /**
     *
     * This method is called by the thread manager to begin the thread
     */
    public void run() {

        // Add a log entry to say we have started the job

        if(scheduledTask != null) {
            runningThread = currentThread();
            setPriority(MIN_PRIORITY);
            setName(scheduledTask.threadName());
            logger.debug("Started scheduled task for {}", scheduledTask.getName());
            LogEntity.addLogEntry(LogEntity.STATUS_STARTED_TASK, scheduledTask);
        }
        // We call the run implementation and make sure that catch
        // absolutely every possible error - that way we are guaranteed
        // to have a clean exit point where we can remove ourselves
        // from the running list

        ExecutionResults executionResults=null;
        try {
            running=true;
            startDate=new Date();
            executionResults = runTask();
        }
        catch (Throwable e) {
            logger.error("Task {} did not close nicely - {}", getName(), PivotalException.getErrorMessage(e));
        }

        // Remove ourselves from the running list

        running=false;
        ScheduleMonitor.removeTaskFromRunningList(this);

        // We have completed so clear the lock and lock_by fields

        if (scheduledTask!=null) {

            // Add a log entry to say we have completed the job

            if (Common.isBlank(runtimeErrors)) {
                LogEntity.addLogEntry(LogEntity.STATUS_FINISHED_TASK, scheduledTask, startDate);
                publishNotification(scheduledTask, this, executionResults);
            }
            else {
                LogEntity.addLogEntry(LogEntity.STATUS_SYSTEM_ERROR, scheduledTask, Common.join(runtimeErrors, "\n"), Common.diffDate(startDate, new Date(), Calendar.SECOND));
                publishNotification(scheduledTask, runtimeErrors, this, executionResults);
            }

            scheduledTask.updateScheduledTaskStatus(false, Common.join(runtimeErrors, "\n"), null);
            logger.debug("Completed scheduled task {}", scheduledTask.getName());
        }
        else
            logger.debug("Completed scheduled task [unknown]");

        // Indicate to the outside world that the thread has gone

        runningThread=null;

        // Close the Hibernate session and commit all changes

        HibernateUtils.closeSession();
    }

    /**
     *
     * This method is called by the thread manager to begin the thread
     *
     * @return String representing the result of the execution
     */
    protected abstract ExecutionResults runTask();

    /**
     * Local method for updating the task record with the status
     */
    private void updateTaskStatus() {

        try {
            //if task id is 0 it means it's an internal execution of a transitory task.
            if(scheduledTask!=null && scheduledTaskID > 0) {
                HibernateUtils.save(scheduledTask);
                scheduledTask = HibernateUtils.getEntity(ScheduledTaskEntity.class, scheduledTaskID);
            }
        }
        catch (Exception e) {
            logger.error("Problem saving task entry - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the number of milliseconds that this job has been running for
     *
     * @return milliseconds
     */
    public long getDuration() {
        return new Date().getTime() - startDate.getTime();
    }

    /**
     * Returns the thread associated with this job
     *
     * @return Running thread - can be null if it isn't actually running
     */
    public Thread getThread() {
        return runningThread;
    }

    /**
     * Sends a notification message to the distribution list as defined in the task
     *
     * @param task Task that has failed
     * @param job Possible job that this notification is associated with - can be null
     * @param executionResults Object representing the result of the transformation
     */
    public static void publishNotification(ScheduledTaskEntity task, Job job, ExecutionResults executionResults) {
        publishNotification(task, null, false, job, executionResults);
    }

    /**
     * Sends an error notification message to the distribution list as defined in the task
     *
     * @param task Task that has failed
     * @param errors List of errors
     * @param job Possible job that this notification is associated with - can be null
     * @param executionResults Object representing the result of the transformation
     */
    public static void publishNotification(ScheduledTaskEntity task, List<String> errors, Job job, ExecutionResults executionResults) {
        publishNotification(task, errors, true, job, executionResults);
    }

    /**
     * Sends a notification message to the distribution list as defined in the task
     * and by the useError flag
     *
     * @param task Task that has failed
     * @param errors List of errors
     * @param useError True if we are publishing errors
     * @param job Possible job that this notification is associated with - can be null
     * @param executionResults Object representing the result of the transformation
     */
    private static void publishNotification(ScheduledTaskEntity task, List<String> errors, boolean useError, Job job, ExecutionResults executionResults) {

        // Check to make sure we have been sent useful stuff

        DistributionListEntity list=useError?task.getErrorDistributionList():task.getNotifyDistributionList();
        if (list!=null) {

            // Get a reference to the web context that we are running within

            logger.debug("Getting servlet context to use to find the template");
            ServletContext servlet= ServletHelper.getServletContext();
            if (servlet !=null) {

                // Ok, we have something to actually do
                // Get the error template to fill in

                try {
                    String templateName =useError? Constants.ERROR_NOTIFICATION_TEMPLATE:Constants.COMPLETION_NOTIFICATION_TEMPLATE;
                    logger.debug("Reading tempate from [{}]", templateName);
                    String template=Common.readTextFile(ServletHelper.getRealPath(servlet, templateName));

                    // Get a publisher to use

                    logger.debug("Getting publisher for {}", task.getName());
                    Publisher publisher= PublisherFactory.getPublisher(task, list);

                    // Create the report

                    logger.debug("Creating a velocity report for the template");
                    Report report=new VelocityReport(template);

                    // Sets the connection properties

                    logger.debug("Setting connection properties {}", task.getDatasource().getName());
                    report.setDatasource(task.getDatasource(), task.getDatasource1(), task.getDatasource2(), task.getDatasource3(), task.getDatasource4());

                    // If we have some error values then add them as runtime parameters

                    List<RuntimeParameter> params=new ArrayList<>();
                    params.add(new RuntimeParameter("Errors", errors));
                    params.add(new RuntimeParameter("Task", task));
                    params.add(new RuntimeParameter("Recipients", publisher.getRecipients()));
                    params.add(new RuntimeParameter("ResultMessage", executionResults.getMessage()));
                    params.add(new RuntimeParameter("ExecutionResults", executionResults));
                    params.add(new RuntimeParameter("Job", job));
                    report.setParameters(params);

                    // We now need to loop through all the groups of recipients of the
                    // publisher channel

                    logger.debug("Looping through {} recipients for {}", publisher.getRecipients().size(), task.getName());
                    for (List<Recipient> recipients : publisher.getRecipients()) {
                        logger.debug("Publishing report to {}", list.toString());
                        publisher.publish(report, recipients, job);
                    }
                }
                catch (Exception e) {
                    logger.error("Problem trying to publish a notification - {}", PivotalException.getErrorMessage(e));
                }
            }
        }
    }
}
