/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.nrmm.service.notification;

/**
 * <p>This class handles all notifications that are raised within the system.
 * They are placed on a queue for processing by a background thread.
 * The exception to this is if the Urgent flag is sent - in this case, the
 * notification is processed immediately</p>
 *
 */
public interface NotificationManager {

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     */
    void addNotificationMessage(String message);

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     */
    void addNotificationMessage(String message, Notification.NotificationLevel level);

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     */
    void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group);

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type    Notification Type (Accepts Application) - @link Notification.NotificationType
     */
    void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type);

    /**
     * Add new Message to the notifications queue
     *
     * @param message Message text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type    Notification Type (Accepts Application) - @link Notification.NotificationType
     * @param urgent  True if the message has to be processed immediately
     */
    void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent);

    /**
     * Add new Message to the notifications queue
     *
     * @param message  Message text
     * @param level    Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group    Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type     Notification Type (Accepts Application) - @link Notification.NotificationType
     * @param urgent   True if the message has to be processed immediately
     * @param mediaUrl An optional link to the media to play when the notification is displayed
     */
    void addNotificationMessage(String message, Notification.NotificationLevel level, Notification.NotificationGroup group, Notification.NotificationType type, boolean urgent, String mediaUrl);
}
