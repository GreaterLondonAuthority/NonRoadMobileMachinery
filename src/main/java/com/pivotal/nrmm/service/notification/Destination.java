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
 * <p>Holder for user information needed for sending an HTTP message</p>
 *
 */
public interface Destination {

    /**
     * @return The application path
     */
    String getAppPath();

    /**
     * @return The user identifier
     */
    String getUserId();

    /**
     * @return The session identifier
     */
    String getSessionId();
}
