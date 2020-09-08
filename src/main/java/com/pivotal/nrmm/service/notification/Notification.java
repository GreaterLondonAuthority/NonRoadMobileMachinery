/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.nrmm.service.notification;

import java.io.Serializable;

/**
 * <p>A notification object that holds the type, severity and destination.</p>
 *
 */
public interface Notification extends Serializable {

    /**
     * <p>Available notification types</p>
     */
    enum NotificationType {
        Application
    }

    /**
     * <p>Available Levels for the notifications</p>
     */
    enum NotificationLevel {
        Info, Warning, Error
    }

    /**
     * <p>Available notification groups</p>
     */
    enum NotificationGroup {
        Individual, Admin, All;

        // The destination for this group
        private Destination individual;

        /**
         * For the individual group, we need the requester session id to be set
         *
         * @param individual Id of the requester
         */
        public void setIndividualSessionId(Destination individual) {
            this.individual = individual;
        }

        /**
         * @return The individual group
         */
        public Destination getIndividualSessionId() {
            return this.individual;
        }
    }

    /**
     * Returns the message
     *
     * @return Message
     */
    String getMessage();

    /**
     * Gets the type of the notification
     *
     * @return Type of the notification
     */
    NotificationType getType();

    /**
     * Gets the group to receive the notification
     *
     * @return Group
     */
    NotificationGroup getGroup();

    /**
     * Gets the severity level of the notification
     *
     * @return Severity level
     */
    NotificationLevel getLevel();

    /**
     * Gets the optional media url of the notification
     *
     * @return media url
     */
    String getMediaUrl();
}
