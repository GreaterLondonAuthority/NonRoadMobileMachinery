/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.notifications;

import com.pivotal.nrmm.service.notification.Destination;
import com.pivotal.nrmm.service.notification.DestinationManager;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.system.monitoring.Monitor;
import com.pivotal.utils.Common;
import com.pivotal.web.servlet.Initialisation;
import com.pivotal.web.servlet.ServletHelper;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class handles all notifications that are raised within the system.
 * They are placed on a queue for processing by a background thread.
 * The exception to this is if the Urgent flag is sent - in this case, the
 * notification is processed immediately
 *
 */
public class NotificationManager extends Monitor implements com.pivotal.nrmm.service.notification.NotificationManager {
    private static final int MONITOR_PERIOD = 10;

    /**
     * The scheduler map of currently running tasks
     */
    private static LinkedList<Notification> pendingNotifications;

    /**
     * Creates a monitor
     */
    NotificationManager(String name) {
        super();
        setMonitorName(name);
    }

    private static NotificationManager instance;

    /**
     * Initialise the monitor thread.
     *
     * @param name Name of the monitor.
     *
     * @return The new monitor.
     */
    public static NotificationManager init(String name) {
        if (instance != null) {
            instance.stopMonitor();
        }
        instance = new NotificationManager(name);
        instance.setPeriod(MONITOR_PERIOD);
        return instance;
    }

    /**
     * @return The current notification manager instance
     */
    public static NotificationManager getInstance() {
        return instance;
    }

    /**
     * Will shut down the manager
     */
    public static void shutdown() {
        if (instance != null) {
            instance.stopMonitor();
        }
    }

    /**
     * Adds the notification to the queue for later processing
     *
     * @param notification Notification to add
     */
    synchronized private static void addNotificationToQueue(Notification notification) {
        if (pendingNotifications == null) {
            pendingNotifications = new LinkedList<>();
        }
        pendingNotifications.add(notification);
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type    Notification Type (Accepts Application) - @link Notification.NotificationType
     * @param urgent  True if the message has to be processed immediately
     */
    public static void addNotification(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent) {
        addNotification(message, level, group, type, urgent, null);
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message  Message text
     * @param level    Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group    Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type     Notification Type (Accepts Application) - @link Notification.NotificationType
     * @param urgent   True if the message has to be processed immediately
     * @param mediaUrl Optional media file to play along with the notification
     */
    public static void addNotification(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent, String mediaUrl) {
        if (!Common.isBlank(instance)) {
            instance.addNotificationMessage(message, level, group, type, urgent, mediaUrl);
        }
    }

    @Override
    public void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent) {
        addNotificationMessage(message, level, group, type, urgent, null);
    }


    public void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent, String mediaUrl) {
        Notification.NotificationType tType = type;
        Notification.NotificationGroup tGroup = group;
        Notification.NotificationLevel tLevel = level;
        if (level == null) {
            tLevel = Notification.NotificationLevel.Info;
        }
        if (group == null) {
            tGroup = Notification.NotificationGroup.All;
        }
        if (type == null) {
            tType = Notification.NotificationType.Application;
        }
        NotificationImpl notification = new NotificationImpl(message, tLevel, tGroup, tType, mediaUrl);

        // If this is urgent, then it is sent directly and it jumps the queue

        if (urgent) {
            sendNotificationToSessions(notification);
        }
        else {
            addNotificationToQueue(notification);
        }
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type    Notification Type (Accepts Application) - @link Notification.NotificationType
     */
    public static void addNotification(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type) {
        if (!Common.isBlank(instance)) {
            instance.addNotificationMessage(message, level, group, type);
        }
    }

    @Override
    public void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type) {
        addNotification(message, level, group, type, false);
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     */
    @SuppressWarnings("unused")
    public static void addNotification(String message) {
        if (!Common.isBlank(instance)) {
            instance.addNotificationMessage(message);
        }
    }

    @Override
    public void addNotificationMessage(String message) {
        addNotification(message, null, null, null);
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     */
    @SuppressWarnings("unused")
    public static void addNotification(String message, Notification.NotificationLevel level) {
        if (!Common.isBlank(instance)) {
            instance.addNotificationMessage(message, level);
        }
    }

    @Override
    public void addNotificationMessage(String message, Notification.NotificationLevel level) {
        addNotification(message, level, null, null);
    }

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     */
    @SuppressWarnings("unused")
    public static void addNotification(String message, Notification.NotificationLevel level, Notification.NotificationGroup group) {
        if (!Common.isBlank(instance)) {
            instance.addNotificationMessage(message, level, group);
        }
    }

    @Override
    public void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group) {
        addNotification(message, level, group, null);
    }

    /**
     * Returns the next notification
     *
     * @return Next Notification to post
     */
    synchronized private static Notification dequeueNotification() {
        Notification notification = null;
        if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
            notification = pendingNotifications.poll();
        }
        return notification;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runTask() {

        // Check for notifications in the queue and send them to the logged in users

        if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
            Notification notification;
            while ((notification = dequeueNotification()) != null) {
                sendNotificationToSessions(notification);
            }
        }
    }

    /**
     * Sends a notification to the applicable sessions
     *
     * @param notification Notification to send
     */
    @SuppressWarnings("unchecked")
    private static void sendNotificationToSessions(Notification notification) {
        DestinationManager destinationManager = new NotificationImpl.DestinationManagerImpl();
        List<Destination> notifySessions = destinationManager.getNotifyUsersSessions(notification.getGroup());
        Map<String, HttpSession> sessions = Initialisation.getSessionMap();
        for (Destination destination : notifySessions) {

            // Get the session from the local store

            HttpSession session = sessions.get(destination.getSessionId());
            if (session != null) {
                List<Notification> notifications = (List<Notification>) session.getAttribute("notifications");
                if (notifications == null) {
                    notifications = new ArrayList<>();
                }
                notifications.add(notification);
                session.setAttribute("notifications", notifications);
            }
        }
    }

    /**
     * This will check the session for any pending notifications.
     *
     * @return Any notifications that are currently within the user's session
     */
    public static List<Notification> getSessionNotifications() {
        return getSessionNotifications(ServletHelper.getSession());
    }

    /**
     * This will check the session for any pending notifications.
     *
     * @param session The session to check for notifications. If null the current request session will be used.
     *
     * @return Any notifications that are currently within the user's session
     */
    @SuppressWarnings("unchecked")
    public static List<Notification> getSessionNotifications(HttpSession session) {
        List<Notification> currentNotifications = Lists.newArrayList();
        if (!Common.isBlank(session)) {
            final List<Notification> notifications = (List<Notification>) ServletHelper.getSession().getAttribute("notifications");
            if (!Common.isBlank(notifications)) {
                currentNotifications.addAll(notifications);
                session.removeAttribute("notifications");
            }
        }
        return currentNotifications;
    }
}
