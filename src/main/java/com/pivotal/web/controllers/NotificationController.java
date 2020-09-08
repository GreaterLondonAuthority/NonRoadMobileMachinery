/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Manages the administration of systems
 */
@Authorise
@Controller
@RequestMapping("/notification")
public class NotificationController extends AbstractController {

    /**
     * Input
     *
     * @param message a {@link java.lang.String} object.
     * @param level   a {@link java.lang.String} object.
     * @param group   a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @ResponseBody
    @RequestMapping(value = "/addnotification", method = RequestMethod.GET)
    public String addNotification(@RequestParam(value = "message") String message, @RequestParam(value = "level", required = false) String level, @RequestParam(value = "group", required = false) String group) {
        Notification.NotificationGroup notGroup = Notification.NotificationGroup.Individual;
        if (group != null) {
            if (group.equalsIgnoreCase("all")) {
                notGroup = Notification.NotificationGroup.All;
            }
            else if (group.equalsIgnoreCase("admin")) {
                notGroup = Notification.NotificationGroup.Admin;
            }
            else {
                notGroup = Notification.NotificationGroup.Individual;
            }
        }

        Notification.NotificationLevel notLevel = Notification.NotificationLevel.Info;
        if (level != null) {
            if (level.equalsIgnoreCase("error")) {
                notLevel = Notification.NotificationLevel.Error;
            }
            else if (level.equalsIgnoreCase("warning")) {
                notLevel = Notification.NotificationLevel.Warning;
            }
            else {
                notLevel = Notification.NotificationLevel.Info;
            }
        }

        NotificationManager.addNotification(message, notLevel, notGroup, Notification.NotificationType.Application);
        return String.format("{\"success\":\"true\"}");
    }
}
