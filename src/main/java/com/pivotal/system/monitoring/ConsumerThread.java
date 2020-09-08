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
import com.pivotal.utils.PivotalException;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

/**
 * An abstract class that must be extended to provide pooled consumer execution services
 * from the monitor
 */
public abstract class ConsumerThread extends Thread {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConsumerThread.class);

    private BlockingQueue<ConsumerObject> processorQueue;
    private Monitor monitor;
    private boolean running = true;
    private boolean executing = false;
    private int index;
    private Date lastRun;
    private ConsumerObject runningJob;
    private String runningJobName;

    /**
     * Default constructor
     */
    protected ConsumerThread() {}

    /**
     * This method is called whenever some work is required to be done
     * This happens whenever there a consumer object has been taken from the
     * queue to work on
     * and needs working on by a consumer
     *
     * @param object consumer object
     */
    abstract public void runTask(ConsumerObject object);

    /**
     * This method can be overridden by extenders to add features to their instance
     */
    public void init() {}

    /**
     * This method can be overridden by extenders to remove features to their instance
     */
    public void finish() {}

    @Override
    public void run() {

        // Set the name to something sensible

        setName(monitor.getMonitorName() + String.format(" (consumer %d)", index));
        setPriority(MIN_PRIORITY);
        logger.info("{} started", getName());

        // Call any consumer specific initialisation

        init();

        // Loop until told to stop

        while (running) {
            try {
                // Wait for something in the queue and pass it on to be processed

                runningJob = processorQueue.take();

                // Set the last successful run timestamp

                lastRun = new Date();
                runningJobName = runningJob.getName();
                executing = true;

                // Work on the task

                runTask(runningJob);

                // Close any session used

                HibernateUtils.closeSession();
            }
            catch (InterruptedException | IllegalMonitorStateException e) {
                logger.debug("Consumer thread [{}] has been interrupted", getName());
            }
            catch (PivotalException e) {
                // Don't do anything - it will have been recorded upstream
            }
            catch (Throwable e) {
                logger.error("Problem has occurred in the consumer thread [{}] - {}", getName(), PivotalException.getErrorMessage(e));
            }
            finally {
                executing = false;
                runningJob = null;
                runningJobName = null;
                lastRun = null;
            }
        }

        // Call any code that has to be run when the thread stops

        finish();

        // Release the session just in case

        HibernateUtils.closeSession();
    }

    /**
     * Sets the process queue used by this thread
     * By providing the process queue reference, we don't have to call back to the
     * parent monitor to get access to the queue
     * @param processorQueue Process queue to get tasks from
     */
    public void setProcessorQueue(BlockingQueue<ConsumerObject> processorQueue) {
        this.processorQueue = processorQueue;
    }

    /**
     * Sets the monitor to use for the basename of the thread
     * This is used to prefix the name given to the thread when it starts
     * so that consumer threads can be grouped with their parent monitor in the display
     * @param monitor Monitor that this consumer belongs to
     */
    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Get the monitor that this consumer belongs to
     * @return Monitor parent
     */
    public Monitor getMonitor() {
        return monitor;
    }

    /**
     * Sets the flag to say if the process should remain in the running state
     * Tasks should periodically check this flag to determine if they should continue running
     * @param running Process is still running
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Sets the index of this thread in the pool
     * This is just a simple way of delineating threads in the pool from each other
     * and only has any value from a display point of view
     * @param index Index of thread
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns the timestamp of the last time this thread ran
     * @return Last run timestamp or null if it hasn't run yet
     */
    public Date getLastRun() {
        return lastRun;
    }

    /**
     * Returns true if the thread is executing the task
     * @return True if executing
     */
    public boolean isExecuting() {
        return executing;
    }

    /**
     * Returns the name of the currently running job
     * @return Running job
     */
    public ConsumerObject getRunningJob() {
        return runningJob;
    }

    /**
     * Gets the full name of the running job
     * @return Full name of the job
     */
    public String getRunningJobName() {
        return runningJobName;
    }
}
