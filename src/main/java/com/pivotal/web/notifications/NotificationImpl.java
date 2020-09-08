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
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.web.servlet.ServletHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * A notification object that holds the type, severity and destination
 *
 */
public class NotificationImpl implements Notification, Serializable {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationImpl.class);
    private String message;
    private NotificationType type;
    private NotificationGroup group;
    private NotificationLevel level;
    private String mediaUrl = null;

    /**
     * Builds a notification
     *
     * @param message Notification text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type    Notification Type (Accepts Application) - @link Notification.NotificationType
     */
    public NotificationImpl(String message, NotificationLevel level, NotificationGroup group, NotificationType type) {
        this(message, level, group, type, null);
    }

    /**
     * Builds a notification
     *
     * @param message  Notification text
     * @param level    Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group    Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     * @param type     Notification Type (Accepts Application) - @link Notification.NotificationType
     * @param mediaUrl The url pointing to the media to play
     */
    public NotificationImpl(String message, NotificationLevel level, NotificationGroup group, NotificationType type, String mediaUrl) {
        setMessage(message);
        this.level = level;
        this.group = group;
        this.type = type;
        this.mediaUrl = mediaUrl;

        if (group == NotificationGroup.Individual && ServletHelper.getSession() != null) {
            group.setIndividualSessionId(new DestinationImpl(ServletHelper.getFullAppPath(), com.pivotal.system.security.UserManager.getCurrentUserName(), ServletHelper.getSession().getId()));
        }
        logger.debug("Built a notification - {}", this.toString());
    }

    /**
     * Builds a notification. Defaults additional parameters to : Level - Info ; Group - All ; Type - Application
     *
     * @param message Notification text
     */
    @SuppressWarnings("unused")
    public NotificationImpl(String message) {
        this(message, NotificationLevel.Info, NotificationGroup.All, NotificationType.Application);
    }

    /**
     * Builds a notification. Defaults additional parameters to : Group - All ; Type - Application
     *
     * @param message Notification text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     */
    @SuppressWarnings("unused")
    public NotificationImpl(String message, NotificationLevel level) {
        this(message, level, NotificationGroup.All, NotificationType.Application);
    }

    /**
     * Builds a notification. Defaults additional parameters to : Type - Application
     *
     * @param message Notification text
     * @param level   Notification Level (Error, Warning, Info) - @link Notification.NotificationLevel
     * @param group   Notification Group (Accepts All, Admin, Individual) - @link Notification.NotificationGroup
     */
    @SuppressWarnings("unused")
    public NotificationImpl(String message, NotificationLevel level, NotificationGroup group) {
        this(message, level, group, NotificationType.Application);
    }

    /**
     * Returns the message
     *
     * @return Message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message
     *
     * @param message Message
     */
    public void setMessage(String message) {
        this.message = I18n.getString(message);
    }

    /**
     * Gets the type of the notification
     *
     * @return Type of the notification
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * Sets the type of the notification
     *
     * @param type Notification type
     */
    public void setType(NotificationType type) {
        this.type = type;
    }

    /**
     * Gets the group to receive the notification
     *
     * @return Group
     */
    public NotificationGroup getGroup() {
        return group;
    }

    /**
     * Sets the group to receive this notification
     *
     * @param group Group
     */
    public void setGroup(NotificationGroup group) {
        this.group = group;
    }

    /**
     * Gets the severity level of the notification
     *
     * @return Severity level
     */
    public NotificationLevel getLevel() {
        return level;
    }

    /**
     * Sets the severity level of the notification
     *
     * @param level Severity level
     */
    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    /**
     * Gets the media url for the notification
     *
     * @return Media Url
     */
    public String getMediaUrl() {
        return mediaUrl;
    }

    /**
     * Sets the media url for the notification
     *
     * @param mediaUrl media url
     */
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Notification{" +
                "message='" + message + '\'' +
                ", type=" + type +
                ", group=" + group +
                ", level=" + level +
                '}';
    }

    /**
     * Holder for user information needed for sending an HTTP message
     */
    protected static class DestinationImpl implements Destination {
        String appPath;
        String userId;
        String sessionId;

        /**
         * Uses explicit values
         *
         * @param appPath   App path of the originator request
         * @param userId    User name of the user
         * @param sessionId Session ID of the request
         */
        private DestinationImpl(String appPath, String userId, String sessionId) {
            this.appPath = appPath;
            this.userId = userId;
            this.sessionId = sessionId;
        }

        /**
         * Uses the values returns from a query
         *
         * @param user Array of the user values
         */
        private DestinationImpl(Object[] user) {
            sessionId = (String) user[0];
            userId = (String) user[1];
            appPath = (String) user[2];
        }

        @Override
        public String toString() {
            return "NotificationUserAddress{" +
                    "appPath='" + appPath + '\'' +
                    ", userId='" + userId + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    '}';
        }

        @Override
        public String getAppPath() {
            return this.appPath;
        }

        @Override
        public String getUserId() {
            return this.userId;
        }

        @Override
        public String getSessionId() {
            return this.sessionId;
        }
    }

    /**
     * Available notification groups
     */
    protected static class DestinationManagerImpl implements DestinationManager {

        /**
         * Finds all the sessions ids required for the current group
         *
         * @return Map with all the sessions ids required for the current group {sessionId : userId}
         */
        public List<Destination> getNotifyUsersSessions(Notification.NotificationGroup notificationGroup) {
            List<Destination> notifyUsersSessionId = new ArrayList<>();
            switch (notificationGroup) {
                case Individual:
                    if (notificationGroup.getIndividualSessionId() != null) {
                        notifyUsersSessionId.add(notificationGroup.getIndividualSessionId());
                    }
                    break;
                case Admin:
                    notifyUsersSessionId.addAll(getLoggedInAdminsSessionIds());
                    break;
                case All:
                    notifyUsersSessionId.addAll(getLoggedInSessionIds());
                    break;
            }
            return notifyUsersSessionId;
        }

        /**
         * Finds all the users that are currently logged in to the system
         *
         * @return List of users logged in
         */
        private static List<Destination> getLoggedInSessionIds() {
            List<Destination> retVal = new ArrayList<>();
            List<Object[]> allLoggedUsers = HibernateUtils.selectSQLEntities("select u.sessionid,u.userid,u.app_path from user_status u join users on userid=email", true);
            if (!Common.isBlank(allLoggedUsers)) {
                for (Object[] user : allLoggedUsers) {
                    if (user[0] != null && user[1] != null) {
                        retVal.add(new DestinationImpl(user));
                    }
                }
            }
            return retVal;
        }

        /**
         * Finds all the administrators that are currently logged in to the system
         *
         * @return List of admin users
         */
        private static List<Destination> getLoggedInAdminsSessionIds() {
            List<Destination> retVal = new ArrayList<>();

//            // Get all logged in users
//            List<Object[]> allLoggedUsers = HibernateUtils.selectSQLEntities("select u.sessionid,u.userid,u.app_path,users.role from user_status u join users on userid=email", true);
//
//            // Check which of the logged in users are admins
//            for (Object[] user : allLoggedUsers) {
//
//                if (!Common.isBlank(user[3])) {
//                    // Check if one of the associated roles is the admin role
//                    List<BigInteger> adminRoleCount = HibernateUtils.selectSQLEntities("select count(*) from user_role_search urs join role r on urs.role_id = r.id where urs.users_id in (select id from users where name  = '" + user[1] + "') and r.administrator = true" );
//                    if (!Common.isBlank(adminRoleCount) && adminRoleCount.get(0).compareTo(new BigInteger("0")) > 0) {
//
//                        // This user is an admin. Add to return value
//                        if (user[0] != null && user[1] != null) {
//                            retVal.add(new DestinationImpl(user));
//                        }
//                    }
//                }
//            }
            // Current app doesn't use admin types

            return retVal;
        }
    }
}
