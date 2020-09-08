/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

/**
 * Used to contain a result from calling validateXML
 */
public class XMLValidationResult {
    protected String error;

    /**
     * Returns the error from the call
     *
     * @return Error or null if not error
     */
    public String getError() {
        return error;
    }

    /**
     * Returns true if the call errored
     *
     * @return True if there is an error
     */
    public boolean isInError() {
        return !Common.isBlank(error);
    }
}
