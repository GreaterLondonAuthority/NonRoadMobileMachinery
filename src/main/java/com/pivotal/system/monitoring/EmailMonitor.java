/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.monitoring;

import com.pivotal.system.hibernate.entities.EmailQueueEntity;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.Progress;
import com.pivotal.web.email.Email;
import com.pivotal.web.email.EmailManager;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.pivotal.web.email.EmailManager.getEmailSession;

/**
 * Provides the main system email monitoring functionality
 * Sends queued emails so the user doesn't have to wait.
 */
public class EmailMonitor extends Monitor {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailMonitor.class);

    private static List<Email> emailQueue=new ArrayList<>();

    private static Date lastTruncateLogTable;
    private static EmailMonitor instance;

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
    public static EmailMonitor init(String name, int period, int deadPeriod) {
        if (instance!=null) {
            instance.stopMonitor();
        }
        instance = new EmailMonitor();
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
    public static EmailMonitor getInstance() {
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

        if (isRunning) {

            // Get a list of emails to work on

            Progress localProgress = new Progress();
            List<Email> emails = getEmailQueue(false);

            if (emails.size() > 0) {

                if (false) {
                    try {
                        Session emailSession = getEmailSession();
                        for (Email email : emails)
                            EmailManager.sendEmailSession(email, emailSession);

                        emailQueue.clear();
                    }
                    catch (Exception e) {
                        logger.error("Unable to create email session {}", PivotalException.getErrorMessage(e));
                    }
                }
                else {
                    // Get sender to speed up sending multiple emails
                    JavaMailSenderImpl sender = EmailManager.connectEmailServer();

                    // Process any emails
                    for (Email email : emails)
                        EmailManager.sendEmail(email, sender);

                    emailQueue.clear();
                }
            }
        }
    }

    /**
     * Adds an email to the queue
     *
     * @param email Email object
     */
    public synchronized static void addEmail(Email email) {
        emailQueue.add(email);
        EmailQueueEntity.saveEmail(email);
    }

    /**
     * Returns a copy of the event queue
     *
     * @return Copy of the queue
     */
    public static List<Email> getEmailQueue() {
        return getEmailQueue(false);
    }

    /**
     * Returns a copy of the event queue and optionally clears it afterwards
     *
     * @param clear True if the queue should be cleared
     * @return Copy of the queue
     */
    protected synchronized static List<Email> getEmailQueue(boolean clear) {
        List<Email> emails = new ArrayList<>();
        emails.addAll(emailQueue);
        if (clear) emailQueue.clear();
        return emails;
    }
}
