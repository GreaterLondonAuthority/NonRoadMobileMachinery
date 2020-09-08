/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Class that wraps any progress calculations and messaging for long running processes
 */
public class Progress {
    private Date startTime;
    private int count;
    private int total;
    private String message="";
    private boolean finished;
    private String error="";

    /**
     * Constructs a simple progress calculator
     */
    public Progress() {
        startTime = new Date();
    }

    /**
     * Constructs a simple progress calculator
     *
     * @param startTime Start time of the operation
     * @param total Total number we're heading towards
     * @param count Current position
     */
    public Progress(Date startTime, int total, int count) {
        this.startTime = startTime;
        this.total = total;
        this.count = count;
    }

    /**
     * Returns the current progress message
     *
     * @return Message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the current message text
     *
     * @param message Message text to display
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns true if the progress has completed
     *
     * @return True/False
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Sets the flag to indicate that this progress has completed
     *
     * @param finished True/False
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Returns the estimated end time
     *
     * @return End time
     */
    public Date getEndTime() {
        Date endTime;
        if (total>count && count>0)
            endTime=Common.addDate(startTime, Calendar.SECOND, getSecondsElapsed() * total / count);
        else
            endTime=startTime;
        return endTime;
    }

    /**
     * Returns the estimated end time as a string
     *
     * @return End time
     */
    public String getEndTimeString() {
        return getEndTimeString("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Returns the estimated end time as a string
     *
     * @param pattern String pattern to use
     *
     * @return End time
     */
    public String getEndTimeString(String pattern) {
        return Common.dateFormat(getEndTime(), pattern);
    }

    /**
     * Returns the percentage value
     *
     * @return Percent through the list
     */
    public int getPercent() {
        return total==0?0:count * 100 / total;
    }

    /**
     * How many seconds that have passed since the start
     *
     * @return Number of seconds elapsed
     */
    public int getSecondsElapsed() {
        return getSecondsElapsed(false);
    }

    /**
     * How many seconds that have passed since the start
     *
     * @param reset True if the start time should be reset
     *
     * @return Number of seconds elapsed
     */
    public int getSecondsElapsed(boolean reset) {
        int returnValue = (int)Common.diffDate(startTime, new Date(), Calendar.SECOND);
        if (reset) resetStartTime();
        return returnValue;
    }

    /**
     * How many milliseconds that have passed since the start
     *
     * @return Number of milliseconds elapsed
     */
    public int getMilliSecondsElapsed() {
        return getMilliSecondsElapsed(false);
    }

    /**
     * How many milliseconds that have passed since the start
     *
     * @param reset True if the start time should be reset
     *
     * @return Number of milliseconds elapsed
     */
    public int getMilliSecondsElapsed(boolean reset) {
        int returnValue = (int)Common.diffDate(startTime, new Date(), Calendar.MILLISECOND);
        if (reset) resetStartTime();
        return returnValue;
    }

    /**
     * How many seconds that have passed since the start
     *
     * @return Number of seconds elapsed
     */
    public int getSecondsRemaining() {
        return (int)Common.diffDate(new Date(), getEndTime(), Calendar.SECOND);
    }

    /**
     * Returns the count
     *
     * @return Number of actions
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the notional count
     *
     * @param count Count of actions
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Returns the total number of actions
     *
     * @return Total
     */
    public int getTotal() {
        return total;
    }

    /**
     * Sets the toal number of actions
     *
     * @param total Total
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * Returns the time that the actions started
     *
     * @return Start time Date
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Returns the error message associated with the progress
     *
     * @return String
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the current error message to display
     *
     * @param error Message
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns true if there is an error
     *
     * @return True/False
     */
    public boolean inError() {
        return !Common.isBlank(error);
    }

    /**
     * Resets the start time to now
     */
    public void resetStartTime() {
        startTime = new Date();
    }

}
