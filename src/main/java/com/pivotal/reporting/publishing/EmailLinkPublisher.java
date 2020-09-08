/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.reporting.publishing;

import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
/**
 * Manages the publishing of information to email recipients as links
 * The code actually takes place in in EmailPublisher, this class simply acts
 * as a placeholder for the factory reflection mechanism
 */
public class EmailLinkPublisher extends EmailPublisher {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailLinkPublisher.class);

    /**
     * Creates an instance of the this publishing channel for
     * sending emails with links in them
     *
     * @param task Associated scheduled task
     * @param list Distribution list from which to get connection details
     */
    public EmailLinkPublisher(ScheduledTaskEntity task, DistributionListEntity list) {
        super(task,list);
    }

}
