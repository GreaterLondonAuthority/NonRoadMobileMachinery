/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.RuntimeParameter;
import com.pivotal.system.security.UserManager;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.HttpUtils;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;

import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Maps the log table to the LogEntity lass
 */
@Table(name = "log")
@Entity
public class LogEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LogEntity.class);

    /** Constant <code>STATUS_SERVER_STARTED="StartedServer"</code> */
    public static final String STATUS_SERVER_STARTED = "StartedServer";
    /** Constant <code>STATUS_SERVER_STOPPED="StoppedServer"</code> */
    public static final String STATUS_SERVER_STOPPED = "StoppedServer";
    /** Constant <code>STATUS_STARTED_TASK="StartedTask"</code> */
    public static final String STATUS_STARTED_TASK = "StartedTask";
    /** Constant <code>STATUS_REPORT_PUBLISHED="ReportPublished"</code> */
    public static final String STATUS_REPORT_PUBLISHED = "ReportPublished";
    /** Constant <code>STATUS_FINISHED_TASK="FinishedTask"</code> */
    public static final String STATUS_FINISHED_TASK = "FinishedTask";
    /** Constant <code>STATUS_SYSTEM_ERROR="SystemError"</code> */
    public static final String STATUS_SYSTEM_ERROR = "SystemError";
    /** Constant <code>STATUS_TRUNCATED_LOGS="TruncatedLogs"</code> */
    public static final String STATUS_TRUNCATED_LOGS = "TruncatedLogs";
    private static final long serialVersionUID = -6300100873232526605L;

    private Integer id;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    private Integer taskId;

    /**
     * <p>Getter for the field <code>taskId</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "task_id", length = 10, precision = 0)
    @Basic
    public Integer getTaskId() {
        return taskId;
    }

    /**
     * <p>Setter for the field <code>taskId</code>.</p>
     *
     * @param taskId a {@link java.lang.Integer} object.
     */
    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    private Timestamp dateAdded;

    /**
     * <p>Getter for the field <code>dateAdded</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Column(name = "date_added", nullable = false, length = 19, precision = 0)
    @Basic
    public Timestamp getDateAdded() {
        return dateAdded;
    }

    /**
     * <p>Setter for the field <code>dateAdded</code>.</p>
     *
     * @param date_added a {@link java.sql.Timestamp} object.
     */
    public void setDateAdded(Timestamp date_added) {
        this.dateAdded = date_added;
    }

    private String status;

    /**
     * <p>Getter for the field <code>status</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "status", nullable = false, length = 50, precision = 0)
    @Basic
    public String getStatus() {
        return status;
    }

    /**
     * <p>Setter for the field <code>status</code>.</p>
     *
     * @param status a {@link java.lang.String} object.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    private String message;

    /**
     * <p>Getter for the field <code>message</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "message", length = 65535, precision = 0)
    @Basic
    public String getMessage() {
        return message;
    }

    /**
     * <p>Setter for the field <code>message</code>.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    private Long duration;

    /**
     * <p>Getter for the field <code>duration</code>.</p>
     *
     * @return a {@link java.lang.Long} object.
     */
    @Column(name = "duration", length = 20, precision = 0)
    @Basic
    public Long getDuration() {
        return duration;
    }

    /**
     * <p>Setter for the field <code>duration</code>.</p>
     *
     * @param duration a {@link java.lang.Long} object.
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }

    private Long total;

    /**
     * <p>Getter for the field <code>total</code>.</p>
     *
     * @return a {@link java.lang.Long} object.
     */
    @Column(name = "total", length = 20, precision = 0)
    @Basic
    public Long getTotal() {
        return total;
    }

    /**
     * <p>Setter for the field <code>total</code>.</p>
     *
     * @param total a {@link java.lang.Long} object.
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    private String reportName;

    /**
     * <p>Getter for the field <code>reportName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "report_name", length = 65535, precision = 0)
    @Basic
    public String getReportName() {
        return reportName;
    }

    /**
     * <p>Setter for the field <code>reportName</code>.</p>
     *
     * @param reportName a {@link java.lang.String} object.
     */
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    private String parameterValues;

    /**
     * <p>Getter for the field <code>parameterValues</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "parameter_values", length = 65535, precision = 0)
    @Basic
    public String getParameterValues() {
        return parameterValues;
    }

    /**
     * <p>Setter for the field <code>parameterValues</code>.</p>
     *
     * @param parameterValues a {@link java.lang.String} object.
     */
    public void setParameterValues(String parameterValues) {
        this.parameterValues = parameterValues;
    }

    private String recipients;

    /**
     * <p>Getter for the field <code>recipients</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "recipients", length = 65535, precision = 0)
    @Basic
    public String getRecipients() {
        return recipients;
    }

    /**
     * <p>Setter for the field <code>recipients</code>.</p>
     *
     * @param recipients a {@link java.lang.String} object.
     */
    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    private String serverId;

    /**
     * <p>Getter for the field <code>serverId</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "server_id", nullable = false, length = 100, precision = 0)
    @Basic
    public String getServerId() {
        return serverId;
    }

    /**
     * <p>Setter for the field <code>serverId</code>.</p>
     *
     * @param serverId a {@link java.lang.String} object.
     */
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    private String taskName;

    /**
     * <p>Getter for the field <code>taskName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "task_name", length = 255, precision = 0)
    @Basic
    public String getTaskName() {
        return taskName;
    }

    /**
     * <p>Setter for the field <code>taskName</code>.</p>
     *
     * @param taskName a {@link java.lang.String} object.
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    private String userFullName;

    /**
     * <p>Getter for the field <code>userFullName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "user_full_name", length = 100, precision = 0)
    @Basic
    public String getUserFullName() {
        return userFullName;
    }

    /**
     * <p>Setter for the field <code>userFullName</code>.</p>
     *
     * @param user_full_name a {@link java.lang.String} object.
     */
    public void setUserFullName(String user_full_name) {
        this.userFullName = user_full_name;
    }

    private String userLocation;

    /**
     * <p>Getter for the field <code>userLocation</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "user_location", length = 100, precision = 0)
    @Basic
    public String getUserLocation() {
        return userLocation;
    }

    /**
     * <p>Setter for the field <code>userLocation</code>.</p>
     *
     * @param user_location a {@link java.lang.String} object.
     */
    public void setUserLocation(String user_location) {
        this.userLocation = user_location;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LogEntity)) return false;
        final LogEntity other = (LogEntity) obj;
        return Objects.equals(this.id, other.id) &&
               Objects.equals(this.dateAdded, other.dateAdded) &&
               Objects.equals(this.duration, other.duration) &&
               Objects.equals(this.message, other.message) &&
               Objects.equals(this.parameterValues, other.parameterValues) &&
               Objects.equals(this.recipients, other.recipients) &&
               Objects.equals(this.reportName, other.reportName) &&
               Objects.equals(this.serverId, other.serverId) &&
               Objects.equals(this.status, other.status) &&
               Objects.equals(this.taskId, other.taskId) &&
               Objects.equals(this.taskName, other.taskName) &&
               Objects.equals(this.total, other.total) &&
               Objects.equals(this.userFullName, other.userFullName) &&
               Objects.equals(this.userLocation, other.userLocation);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "LogEntity{" +
               "dateAdded='" + dateAdded + '\'' +
               ", duration='" + duration + '\'' +
               ", id='" + id + '\'' +
               ", message='" + message + '\'' +
               ", parameterValues='" + parameterValues + '\'' +
               ", recipients='" + recipients + '\'' +
               ", reportName='" + reportName + '\'' +
               ", serverId='" + serverId + '\'' +
               ", status='" + status + '\'' +
               ", taskId='" + taskId + '\'' +
               ", taskName='" + taskName + '\'' +
               ", total='" + total + '\'' +
               ", userFullName='" + userFullName + '\'' +
               ", userLocation='" + userLocation + '\'' +
               '}';
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task) {
        addLogEntry(status, task, task.getName());
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param message Text to use
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task, String message) {
        addLogEntry(status, task, message, 0);
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param start Start time to measure dureation from
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task, Date start) {
        addLogEntry(status, task, task.getName(), Common.getTimeDifference(start));
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param duration Time in seconds that task took
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task, int duration) {
        addLogEntry(status, task, task.getName(), duration);
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param message Message to add to log
     * @param start Start time of the duration
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task, String message, Date start) {
        addLogEntry(addLogEntryPrivate(status, task, message, Common.getTimeDifference(start)));
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param message Message to add to log
     * @param duration Time in seconds that task took
     */
    public static void addLogEntry(String status, ScheduledTaskEntity task, String message, long duration) {
        addLogEntry(addLogEntryPrivate(status, task, message, duration));
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param name Name of the originator
     * @param start Start from which to measure duration
     * @param task Task associated with this status
     * @param report Report being published
     * @param recipients List of the recipients it was being sent to
     */
    public static void addLogEntry(String status, String name, Date start, ScheduledTaskEntity task, Report report, List<Recipient> recipients) {
        addLogEntry(status, name, Common.getTimeDifference(start), task, report, recipients);
    }

    /**
     * Adds a log message to the log table
     *
     * @param status Status type to use
     * @param name Name of the originator
     * @param duration Duration to record
     * @param task Task associated with this status
     * @param report Report being published
     * @param recipients List of the recipients it was being sent to
     */
    public static void addLogEntry(String status, String name, int duration, ScheduledTaskEntity task, Report report, List<Recipient> recipients) {
        LogEntity log= addLogEntryPrivate(status, task, name, duration);

        // Get a list of the recipients

        if (!Common.isBlank(recipients)) {
            List<String> recips=new ArrayList<>();
            for (Recipient recip : recipients) {
                if (Common.isBlank(recip.getDescriptiveName()))
                    recips.add(recip.getName());
                else
                    recips.add(recip.getDescriptiveName() + " <" + recip.getName() + '>');
            }
            log.setRecipients(Common.join(recips, "; "));
        }

        // Now get a list of the parameters used

        if (report!=null) {
            if (!Common.isBlank(report.getParameters())) {
                List<String> params=new ArrayList<>();
                for (RuntimeParameter param : report.getParameters()) {
                    params.add(param.getName() + ':' + param.getValue());
                }
                log.setParameterValues(Common.join(params,", "));
            }
        }
        addLogEntry(log);
    }

    /**
     * Adds a log message to the log table
     *
     * @param log Log entity to save
     */
    public static void addLogEntry(LogEntity log) {
        try {
            HibernateUtils.save(log);
        }
        catch (Exception e) {
            logger.error("Problem saving log entry - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns a log entity filled with the default information
     *
     * @param status Status type to use
     * @param task Task associated with this status
     * @param message Message to add to log
     * @param duration Time in seconds that task took
     *
     * @return Returns a log entity
     */
    private static LogEntity addLogEntryPrivate(String status, ScheduledTaskEntity task, String message, long duration) {
        LogEntity log=new LogEntity();
        log.setServerId(ServletHelper.getAppIdentity());
        log.setStatus(status);
        log.setDateAdded(new Timestamp(new Date().getTime()));
        log.setUserFullName(UserManager.getCurrentUserName());
        if(task!=null){
            log.setTaskId(task.getId());
            log.setTaskName(task.getName());
            if(task.getReport() != null) {
                log.setReportName(task.getReport().getName());
            }
        }
        log.setMessage(message);
        log.setDuration(duration);
        return log;
    }

    /**
     * Adds a log entry into the audit log
     *
     * @param status Status type to use
     * @param message Message to add to log
     * @return Returns a log entity
     */
    public static LogEntity addLogEntry(String status, String message) {
        return addLogEntry(status, message, 0);
    }

    /**
     * Adds a log entry into the audit log
     *
     * @param status Status type to use
     * @param message Message to add to log
     * @param duration Time in seconds that task took
     * @return Returns a log entity
     */
    public static LogEntity addLogEntry(String status, String message, long duration) {
        LogEntity log=new LogEntity();
        log.setServerId(ServletHelper.getAppIdentity());
        log.setStatus(status);
        log.setDateAdded(new Timestamp(new Date().getTime()));
        log.setMessage(message);
        log.setDuration(duration);
        log.setUserFullName(UserManager.getCurrentUserName());
        addLogEntry(log);
        return log;
    }

    /**
     * Adds a log entry into the audit log that includes the remote address
     *
     * @param status Status type to use
     * @param message Message to add to log
     * @param start Date of the begining of the activity
     * @param request Request containing the remote address
     * @return Returns a log entity
     */
    public static LogEntity addLogEntry(String status, String message, Date start, HttpServletRequest request) {
        return addLogEntry(status, message, Common.getTimeDifference(start), request);
    }

    /**
     * Adds a log entry into the audit log that includes the remote address
     *
     * @param status Status type to use
     * @param message Message to add to log
     * @param duration Time in seconds that task took
     * @param request Request containing the remote address
     * @return Returns a log entity
     */
    public static LogEntity addLogEntry(String status, String message, long duration, HttpServletRequest request) {
        LogEntity log=new LogEntity();
        log.setServerId(ServletHelper.getAppIdentity());
        log.setStatus(status);
        log.setDateAdded(new Timestamp(new Date().getTime()));
        log.setMessage(message);
        log.setDuration(duration);
        log.setUserFullName(UserManager.getCurrentUserName());
        if (request!=null) {
            log.setUserLocation(request.getParameter("client"));
            if (Common.isBlank(log.getUserLocation())) log.setUserLocation(HttpUtils.getAddressFromRequest(request));
        }
        addLogEntry(log);
        return log;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(dateAdded, duration, message, parameterValues, recipients, reportName, serverId, status, taskId, taskName, total, userFullName, userLocation);
    }
}
