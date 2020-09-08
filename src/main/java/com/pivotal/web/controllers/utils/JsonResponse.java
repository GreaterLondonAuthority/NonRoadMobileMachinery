/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.pivotal.utils.Common;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a general purpose way of packaging up responses in JSON
 * The responses are all of the same shape so it's designed really for internal NRMM use
 */
public class JsonResponse {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonResponse.class);

    private String error;
    private String information;
    private String warning;
    private int length;
    private int count;
    private String id;
    private boolean completed;
    private Map<String,Object> data;

    /**
     * <p>Constructor for JsonResponse.</p>
     */
    public JsonResponse() {
        this.data = new HashMap<>();
    }

    /**
     * Adds an error message to the object
     *
     * @param error Error message
     */
    public JsonResponse(String error) {
        this.error = error;
    }

    /**
     * Adds an error and information message to the object
     *
     * @param error       Error message
     * @param information Information message
     */
    public JsonResponse(String error, String information) {
        this.error = error;
        this.information = information;
    }

    /**
     * Adds an error and information message to the object
     *
     * @param error       Error message
     * @param information Information message
     * @param count       A count of something
     */
    public JsonResponse(String error, String information, int count) {
        this.error = error;
        this.information = information;
        this.count = count;
    }

    /**
     * Returns the current error message
     *
     * @return Error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message
     *
     * @param error Error message
     */
    public void setError(String error) {
        if (!Common.isBlank(error))
            this.error = error;
    }

    /**
     * Sets te error message using String.format()
     *
     * @param error  Error message
     * @param values Array of objects to inject into message
     */
    public void setError(String error, Object... values) {
        this.error = String.format(error, values);
    }

    /**
     * Gets the current information message
     *
     * @return Information message
     */
    public String getInformation() {
        return information;
    }

    /**
     * Sets the current information message
     *
     * @param information Message
     */
    public void setInformation(String information) {
        this.information = information;
    }

    /**
     * Sets the current information message using String.format()
     *
     * @param information Message
     * @param values      Array of objects to inject into message
     */
    public void setInformation(String information, Object... values) {
        this.information = String.format(information, values);
    }

    /**
     * Gets the general purpose length property
     *
     * @return length value
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets the general purpose length property
     *
     * @param length length value
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Gets the current warning message
     *
     * @return Warning message
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Sets the warning message
     *
     * @param warning Warning message
     */
    public void setWarning(String warning) {
        this.warning = warning;
    }

    /**
     * Sets the warning message using String.format()
     *
     * @param warning Warning message
     * @param values  Array of objects to inject into message
     */
    public void setWarning(String warning, Object... values) {
        this.warning = String.format(warning, values);
    }

    /**
     * Returns the count value
     *
     * @return Count value
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count value
     *
     * @param count Count value
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Returns true if the error message is not empty
     *
     * @return True if in error
     */
    public boolean getInError() {
        return !Common.isBlank(error);
    }

    /**
     * Returns the ID associated with the object
     *
     * @return ID of the object
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the object
     *
     * @param id ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns true if the completed flag is set
     *
     * @return True if completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Set the completed flag
     *
     * @param completed Value to set
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Returns the current data map.
     *
     * @return data map.
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Gets the value of an entry in the data map
     *
     * @param key key to search for
     *
     * @return The value associated with the given key in the data map. null if nothing is found
     */
    public Object getDataItem(String key) {
        return this.data.get(key);
    }

    /**
     * Removes an entry from the data map
     *
     * @param key key to delete from the data map
     *
     * @return The value associated with the given key we are removing from the data map. null if nothing is found
     */
    public Object removeDataItem(String key) {
        return this.data.remove(key);
    }

    /**
     * Adds/Replaces an entry in the data map
     *
     * @param key      key to use in the data map
     * @param dataItem value to set
     */
    public void putDataItem(String key, Object dataItem) {

        if (!Common.isBlank(key) && !Common.isBlank(dataItem)) {
            this.data.put(key, dataItem);
        }
    }

    /**
     * Clears the data map
     */
    public void clearDataEntry() {
        this.data.clear();
    }

}
