/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.nrmm.service.notification;

import java.util.List;

/**
 * <p>Handles gathering the destinations that the notification needs to be displayed.</p>
 *
 */
public interface DestinationManager {

    /**
     * Finds all the sessions ids required for the specified group
     *
     * @param notificationGroup The group that the notification is to be shown
     * @return Map with all the sessions ids required for the current group {sessionId : userId}
     */
    List<Destination> getNotifyUsersSessions(Notification.NotificationGroup notificationGroup);
}
