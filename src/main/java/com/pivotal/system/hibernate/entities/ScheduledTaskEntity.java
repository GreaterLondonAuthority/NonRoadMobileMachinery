/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.monitoring.utils.DefinitionSettings;
import com.pivotal.reporting.reports.Report;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Maps the schedule_task table to the ScheduleTaskEntity lass
 */
@Table(name = "scheduled_task")
@Entity
public class ScheduledTaskEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduledTaskEntity.class);
    private static final long serialVersionUID = -8004458698175899683L;

    /** Minute type schedule **/
    public static int SCHED_TYPE_MINUTE = 0;

    /** Daily type schedule **/
    public static int SCHED_TYPE_DAY = 1;

    /** Weekly type schedule **/
    public static int SCHED_TYPE_WEEK = 2;

    /** Monthly type schedule **/
    public static int SCHED_TYPE_MONTH = 3;

    /** Monthly type schedule **/
    public static int SCHED_TYPE_NEVER = 4;

    /** Once type schedule **/
    public static int SCHED_TYPE_ONCE = 5;

    /** Report task **/
    public static String TASK_TYPE_REPORT = "report";

    /** Report task **/
    public static String TASK_TYPE_INGRESS = "ingress";

    /** Report task **/
    public static String TASK_TYPE_WEBSERVICE = "webservice";

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

    @NotBlank(message = "You must specify a name for this task")
    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", nullable = false, length = 100, precision = 0)
    @Basic
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    private String description;

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "description", length = 255, precision = 0)
    @Basic
    public String getDescription() {
        return description;
    }

    /**
     * <p>Setter for the field <code>description</code>.</p>
     *
     * @param description a {@link java.lang.String} object.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    private boolean locked;

    /**
     * <p>isLocked.</p>
     *
     * @return a boolean.
     */
    @Column(name = "locked", length = 0, precision = 0)
    @Basic
    public boolean isLocked() {
        return locked;
    }

    /**
     * <p>Setter for the field <code>locked</code>.</p>
     *
     * @param locked a boolean.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    private String lockedBy;

    /**
     * <p>Getter for the field <code>lockedBy</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "locked_by", length = 255, precision = 0)
    @Basic
    public String getLockedBy() {
        return lockedBy;
    }

    /**
     * <p>Setter for the field <code>lockedBy</code>.</p>
     *
     * @param lockedBy a {@link java.lang.String} object.
     */
    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    private String outputType;

    /**
     * <p>Getter for the field <code>outputType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "output_type", length = 20, precision = 0)
    @Basic
    public String getOutputType() {
        return outputType;
    }

    /**
     * <p>Setter for the field <code>outputType</code>.</p>
     *
     * @param outputType a {@link java.lang.String} object.
     */
    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    /**
     * <p>getExportFormat.</p>
     *
     * @return a {@link com.pivotal.reporting.reports.Report.ExportFormat} object.
     */
    @SuppressWarnings("unused")
    @Transient
    public Report.ExportFormat getExportFormat() {
        return Report.ExportFormat.getType(outputType);
    }

    @NotNull(message = "You must select a suitable schedule to run the task")
    private Integer schedType;

    /**
     * <p>Getter for the field <code>schedType</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "sched_type", nullable = false, length = 10, precision = 0)
    @Basic
    public Integer getSchedType() {
        return schedType;
    }

    /**
     * <p>Setter for the field <code>schedType</code>.</p>
     *
     * @param schedType a {@link java.lang.Integer} object.
     */
    public void setSchedType(Integer schedType) {
        this.schedType = schedType;
    }

    /**
     * <p>getSchedTypeText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @SuppressWarnings("unused")
    @Transient
    public String getSchedTypeText() {
        String value = null;
        if (schedType==SCHED_TYPE_MINUTE) {
            value = "admin.scheduled_task.schedule.every.x.minutes";
        }
        else if (schedType==SCHED_TYPE_DAY) {
            value = "admin.scheduled_task.schedule.every.day";
        }
        else if (schedType==SCHED_TYPE_WEEK) {
            value = "admin.scheduled_task.schedule.every.week";
        }
        else if (schedType==SCHED_TYPE_MONTH) {
            value = "admin.scheduled_task.schedule.every.month";
        }
        else if (schedType==SCHED_TYPE_NEVER) {
            value = "admin.scheduled_task.schedule.never";
        }
        else if (schedType==SCHED_TYPE_ONCE) {
            value = "admin.scheduled_task.schedule.once";
        }
        return I18n.getString(value);
    }

    private Integer schedRunEveryMinutes;

    /**
     * <p>Getter for the field <code>schedRunEveryMinutes</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "sched_run_every_minutes", length = 10, precision = 0)
    @Basic
    public Integer getSchedRunEveryMinutes() {
        return schedRunEveryMinutes;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryMinutes</code>.</p>
     *
     * @param schedRunEveryMinutes a {@link java.lang.Integer} object.
     */
    public void setSchedRunEveryMinutes(Integer schedRunEveryMinutes) {
        this.schedRunEveryMinutes = schedRunEveryMinutes;
    }

    private String schedRunEveryMinutesFrom;

    /**
     * <p>Getter for the field <code>schedRunEveryMinutesFrom</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_minutes_from", length = 10, precision = 0)
    @Basic
    public String getSchedRunEveryMinutesFrom() {
        return schedRunEveryMinutesFrom;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryMinutesFrom</code>.</p>
     *
     * @param schedRunEveryMinutesFrom a {@link java.lang.String} object.
     */
    public void setSchedRunEveryMinutesFrom(String schedRunEveryMinutesFrom) {
        this.schedRunEveryMinutesFrom = schedRunEveryMinutesFrom;
    }

    private String schedRunEveryMinutesTo;

    /**
     * <p>Getter for the field <code>schedRunEveryMinutesTo</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_minutes_to", length = 10, precision = 0)
    @Basic
    public String getSchedRunEveryMinutesTo() {
        return schedRunEveryMinutesTo;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryMinutesTo</code>.</p>
     *
     * @param schedRunEveryMinutesTo a {@link java.lang.String} object.
     */
    public void setSchedRunEveryMinutesTo(String schedRunEveryMinutesTo) {
        this.schedRunEveryMinutesTo = schedRunEveryMinutesTo;
    }



    private String schedRunEveryDayAt;

    /**
     * <p>Getter for the field <code>schedRunEveryDayAt</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_day_at", length = 10, precision = 0)
    @Basic
    public String getSchedRunEveryDayAt() {
        return schedRunEveryDayAt;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryDayAt</code>.</p>
     *
     * @param schedRunEveryDayAt a {@link java.lang.String} object.
     */
    public void setSchedRunEveryDayAt(String schedRunEveryDayAt) {
        this.schedRunEveryDayAt = schedRunEveryDayAt;
    }

    private boolean schedRunEveryDayExcludeWeekends;

    /**
     * <p>isSchedRunEveryDayExcludeWeekends.</p>
     *
     * @return a boolean.
     */
    @Column(name = "sched_run_every_day_exclude_weekends", length = 0, precision = 0)
    @Basic
    public boolean isSchedRunEveryDayExcludeWeekends() {
        return schedRunEveryDayExcludeWeekends;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryDayExcludeWeekends</code>.</p>
     *
     * @param schedRunEveryDayExcludeWeekends a boolean.
     */
    public void setSchedRunEveryDayExcludeWeekends(boolean schedRunEveryDayExcludeWeekends) {
        this.schedRunEveryDayExcludeWeekends = schedRunEveryDayExcludeWeekends;
    }

    private boolean schedRunEveryDayExcludeHolidays;

    /**
     * <p>isSchedRunEveryDayExcludeHolidays.</p>
     *
     * @return a boolean.
     */
    @Column(name = "sched_run_every_day_exclude_holidays", length = 0, precision = 0)
    @Basic
    public boolean isSchedRunEveryDayExcludeHolidays() {
        return schedRunEveryDayExcludeHolidays;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryDayExcludeHolidays</code>.</p>
     *
     * @param schedRunEveryDayExcludeHolidays a boolean.
     */
    public void setSchedRunEveryDayExcludeHolidays(boolean schedRunEveryDayExcludeHolidays) {
        this.schedRunEveryDayExcludeHolidays = schedRunEveryDayExcludeHolidays;
    }

    private String schedRunEveryWeekDays;

    /**
     * <p>Getter for the field <code>schedRunEveryWeekDays</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_week_days", length = 255, precision = 0)
    @Basic
    public String getSchedRunEveryWeekDays() {
        return schedRunEveryWeekDays;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryWeekDays</code>.</p>
     *
     * @param schedRunEveryWeekDays a {@link java.lang.String} object.
     */
    public void setSchedRunEveryWeekDays(String schedRunEveryWeekDays) {
        this.schedRunEveryWeekDays = schedRunEveryWeekDays;
    }

    private String schedRunEveryWeekDaysAt;

    /**
     * <p>Getter for the field <code>schedRunEveryWeekDaysAt</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_week_days_at", length = 10, precision = 0)
    @Basic
    public String getSchedRunEveryWeekDaysAt() {
        return schedRunEveryWeekDaysAt;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryWeekDaysAt</code>.</p>
     *
     * @param schedRunEveryWeekDaysAt a {@link java.lang.String} object.
     */
    public void setSchedRunEveryWeekDaysAt(String schedRunEveryWeekDaysAt) {
        this.schedRunEveryWeekDaysAt = schedRunEveryWeekDaysAt;
    }

    private Integer schedRunEveryMonthOn;

    /**
     * <p>Getter for the field <code>schedRunEveryMonthOn</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "sched_run_every_month_on", length = 10, precision = 0)
    @Basic
    public Integer getSchedRunEveryMonthOn() {
        return schedRunEveryMonthOn;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryMonthOn</code>.</p>
     *
     * @param schedRunEveryMonthOn a {@link java.lang.Integer} object.
     */
    public void setSchedRunEveryMonthOn(Integer schedRunEveryMonthOn) {
        this.schedRunEveryMonthOn = schedRunEveryMonthOn;
    }

    private String schedRunEveryMonthAt;

    /**
     * <p>Getter for the field <code>schedRunEveryMonthAt</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_run_every_month_at", length = 10, precision = 0)
    @Basic
    public String getSchedRunEveryMonthAt() {
        return schedRunEveryMonthAt;
    }

    /**
     * <p>Setter for the field <code>schedRunEveryMonthAt</code>.</p>
     *
     * @param schedRunEveryMonthAt a {@link java.lang.String} object.
     */
    public void setSchedRunEveryMonthAt(String schedRunEveryMonthAt) {
        this.schedRunEveryMonthAt = schedRunEveryMonthAt;
    }

    private Timestamp schedRunOnceOn;

    /**
     * <p>Getter for the field <code>schedRunOnceOn</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Column(name = "sched_run_once_on")
    @Basic
    public Timestamp getSchedRunOnceOn() {
        return schedRunOnceOn;
    }

    /**
     * <p>Setter for the field <code>schedRunOnceOn</code>.</p>
     *
     * @param schedRunOnceOn a {@link java.sql.Timestamp} object.
     */
    public void setSchedRunOnceOn(Timestamp schedRunOnceOn) {
        this.schedRunOnceOn = schedRunOnceOn;
    }


    private Timestamp schedLastRun;

    /**
     * <p>Getter for the field <code>schedLastRun</code>.</p>
     *
     * @return a {@link java.sql.Timestamp} object.
     */
    @Column(name = "sched_last_run", length = 19, precision = 0)
    @Basic
    public Timestamp getSchedLastRun() {
        return schedLastRun;
    }

    /**
     * <p>Setter for the field <code>schedLastRun</code>.</p>
     *
     * @param schedLastRun a {@link java.sql.Timestamp} object.
     */
    public void setSchedLastRun(Timestamp schedLastRun) {
        this.schedLastRun = schedLastRun;
    }

    private String schedLastError;

    /**
     * <p>Getter for the field <code>schedLastError</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "sched_last_error", length = 65535, precision = 0)
    @Basic
    public String getSchedLastError() {
        return schedLastError;
    }

    /**
     * <p>Setter for the field <code>schedLastError</code>.</p>
     *
     * @param schedLastError a {@link java.lang.String} object.
     */
    public void setSchedLastError(String schedLastError) {
        this.schedLastError = schedLastError;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ScheduledTaskEntity)) return false;
        final ScheduledTaskEntity other = (ScheduledTaskEntity) obj;
        return Objects.equals(this.id, other.id) &&
               Objects.equals(this.getDatasource(), other.getDatasource()) &&
               Objects.equals(this.getDatasource1(), other.getDatasource1()) &&
               Objects.equals(this.getDatasource2(), other.getDatasource2()) &&
               Objects.equals(this.getDatasource3(), other.getDatasource3()) &&
               Objects.equals(this.getDatasource4(), other.getDatasource4()) &&
               Objects.equals(this.description, other.description) &&
               Objects.equals(this.disabled, other.disabled) &&
               Objects.equals(this.getDistributionList(), other.getDistributionList()) &&
               Objects.equals(this.endpoint, other.endpoint) &&
               Objects.equals(this.getErrorDistributionList(), other.getErrorDistributionList()) &&
               Objects.equals(this.locked, other.locked) &&
               Objects.equals(this.lockedBy, other.lockedBy) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.getNotifyDistributionList(), other.getNotifyDistributionList()) &&
               Objects.equals(this.outputType, other.outputType) &&
               Objects.equals(this.getReport(), other.getReport()) &&
               Objects.equals(this.schedLastError, other.schedLastError) &&
               Objects.equals(this.schedLastRun, other.schedLastRun) &&
               Objects.equals(this.schedRunEveryDayAt, other.schedRunEveryDayAt) &&
               Objects.equals(this.schedRunEveryDayExcludeWeekends, other.schedRunEveryDayExcludeWeekends) &&
               Objects.equals(this.schedRunEveryDayExcludeHolidays, other.schedRunEveryDayExcludeHolidays) &&
               Objects.equals(this.schedRunEveryMinutes, other.schedRunEveryMinutes) &&
               Objects.equals(this.schedRunEveryMinutesFrom, other.schedRunEveryMinutesFrom) &&
               Objects.equals(this.schedRunEveryMinutesTo, other.schedRunEveryMinutesTo) &&
               Objects.equals(this.schedRunEveryMonthAt, other.schedRunEveryMonthAt) &&
               Objects.equals(this.schedRunEveryMonthOn, other.schedRunEveryMonthOn) &&
               Objects.equals(this.schedRunEveryWeekDays, other.schedRunEveryWeekDays) &&
               Objects.equals(this.schedRunEveryWeekDaysAt, other.schedRunEveryWeekDaysAt) &&
               Objects.equals(this.schedRunOnceOn, other.schedRunOnceOn) &&
               Objects.equals(this.schedType, other.schedType) &&
               Objects.equals(this.settingsXML, other.settingsXML) &&
               Objects.equals(this.taskType, other.taskType);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ScheduledTaskEntity{" +
               "datasource=" + getDatasource() +
               ", datasource1=" + getDatasource1() +
               ", datasource2=" + getDatasource2() +
               ", datasource3=" + getDatasource3() +
               ", datasource4=" + getDatasource4() +
               ", description='" + description + '\'' +
               ", disabled='" + disabled + '\'' +
               ", distributionList=" + getDistributionList() +
               ", endpoint='" + endpoint + '\'' +
               ", errorDistributionList=" + getErrorDistributionList() +
               ", id='" + id + '\'' +
               ", locked='" + locked + '\'' +
               ", lockedBy='" + lockedBy + '\'' +
               ", name='" + name + '\'' +
               ", notifyDistributionList=" + getNotifyDistributionList() +
               ", outputType='" + outputType + '\'' +
               ", report=" + getReport() +
               ", schedLastError='" + schedLastError + '\'' +
               ", schedLastRun='" + schedLastRun + '\'' +
               ", schedRunEveryDayAt='" + schedRunEveryDayAt + '\'' +
               ", schedRunEveryDayExcludeWeekends='" + schedRunEveryDayExcludeWeekends + '\'' +
               ", schedRunEveryDayExcludeHolidays='" + schedRunEveryDayExcludeHolidays + '\'' +
               ", schedRunEveryMinutes='" + schedRunEveryMinutes + '\'' +
               ", schedRunEveryMinutesFrom='" + schedRunEveryMinutesFrom + '\'' +
               ", schedRunEveryMinutesTo='" + schedRunEveryMinutesTo + '\'' +
               ", schedRunEveryMonthAt='" + schedRunEveryMonthAt + '\'' +
               ", schedRunEveryMonthOn='" + schedRunEveryMonthOn + '\'' +
               ", schedRunEveryWeekDays='" + schedRunEveryWeekDays + '\'' +
               ", schedRunEveryWeekDaysAt='" + schedRunEveryWeekDaysAt + '\'' +
               ", schedRunOnceOn='" + schedRunOnceOn + '\'' +
               ", schedType='" + schedType + '\'' +
               ", settingsXML='" + settingsXML + '\'' +
               ", taskType='" + taskType + '\'' +
               '}';
    }

    /**
     * <p>getParameters.</p>
     *
     * @return a {@link com.pivotal.monitoring.utils.DefinitionSettings} object.
     * @throws java.lang.Exception if any.
     */
    @Transient
    public DefinitionSettings getParameters() throws Exception {
        return new DefinitionSettings(this);
    }

    private String settingsXML;

    /**
     * <p>Getter for the field <code>settingsXML</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "settings", length = 2147483647, precision = 0)
    @Basic
    public String getSettingsXML() {
        return settingsXML;
    }

    /**
     * <p>Setter for the field <code>settingsXML</code>.</p>
     *
     * @param settingsXML a {@link java.lang.String} object.
     */
    public void setSettingsXML(String settingsXML) {
        this.settingsXML = settingsXML;
    }


    private DefinitionSettings values;

    /**
     * Returns the definition values for this object
     *
     * @return DefinitionSettings object
     */
    @Transient
    public DefinitionSettings getSettings() {
        try {
            if (values==null) {
                values = new DefinitionSettings(this);
            }
        }
        catch (Exception e) {
            logger.error("Problem parsing settings - {}", PivotalException.getErrorMessage(e));
        }
        return values;
    }
    private DistributionListEntity errorDistributionList;

    /**
     * <p>Getter for the field <code>errorDistributionList</code>.</p>
     *
     * @return a {@link DistributionListEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "error_distribution_list_id", referencedColumnName = "id")
    public DistributionListEntity getErrorDistributionList() {
        return errorDistributionList;
    }

    /**
     * <p>Setter for the field <code>errorDistributionList</code>.</p>
     *
     * @param errorDistributionList a {@link DistributionListEntity} object.
     */
    public void setErrorDistributionList(DistributionListEntity errorDistributionList) {
        this.errorDistributionList = errorDistributionList;
    }

    private boolean disabled;

    /**
     * <p>isDisabled.</p>
     *
     * @return a boolean.
     */
    @Column(name = "disabled", length = 0, precision = 0)
    @Basic
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * <p>Setter for the field <code>disabled</code>.</p>
     *
     * @param disabled a boolean.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    private DistributionListEntity distributionList;

    /**
     * <p>Getter for the field <code>distributionList</code>.</p>
     *
     * @return a {@link DistributionListEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "distribution_list_id", referencedColumnName = "id")
    public DistributionListEntity getDistributionList() {
        return distributionList;
    }

    /**
     * <p>Setter for the field <code>distributionList</code>.</p>
     *
     * @param distributionList a {@link DistributionListEntity} object.
     */
    public void setDistributionList(DistributionListEntity distributionList) {
        this.distributionList = distributionList;
    }

    @NotNull(message = "You must select a datasource for the report to use")
    private DatasourceEntity datasource;

    /**
     * <p>Getter for the field <code>datasource</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "datasource_id", referencedColumnName = "id", nullable = false)
    public DatasourceEntity getDatasource() {
        return datasource;
    }

    /**
     * <p>Setter for the field <code>datasource</code>.</p>
     *
     * @param datasource a {@link DatasourceEntity} object.
     */
    public void setDatasource(DatasourceEntity datasource) {
        this.datasource = datasource;
    }

    private DatasourceEntity datasource1;

    /**
     * <p>Getter for the field <code>datasource1</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "datasource_id1", referencedColumnName = "id")
    public DatasourceEntity getDatasource1() {
        return datasource1;
    }

    /**
     * <p>Setter for the field <code>datasource1</code>.</p>
     *
     * @param datasource1 a {@link DatasourceEntity} object.
     */
    public void setDatasource1(DatasourceEntity datasource1) {
        this.datasource1 = datasource1;
    }

    private DatasourceEntity datasource2;

    /**
     * <p>Getter for the field <code>datasource2</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "datasource_id2", referencedColumnName = "id")
    public DatasourceEntity getDatasource2() {
        return datasource2;
    }

    /**
     * <p>Setter for the field <code>datasource2</code>.</p>
     *
     * @param datasource2 a {@link DatasourceEntity} object.
     */
    public void setDatasource2(DatasourceEntity datasource2) {
        this.datasource2 = datasource2;
    }

    private DatasourceEntity datasource3;

    /**
     * <p>Getter for the field <code>datasource3</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "datasource_id3", referencedColumnName = "id")
    public DatasourceEntity getDatasource3() {
        return datasource3;
    }

    /**
     * <p>Setter for the field <code>datasource3</code>.</p>
     *
     * @param datasource3 a {@link DatasourceEntity} object.
     */
    public void setDatasource3(DatasourceEntity datasource3) {
        this.datasource3 = datasource3;
    }

    private DatasourceEntity datasource4;

    /**
     * <p>Getter for the field <code>datasource4</code>.</p>
     *
     * @return a {@link DatasourceEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "datasource_id4", referencedColumnName = "id")
    public DatasourceEntity getDatasource4() {
        return datasource4;
    }

    /**
     * <p>Setter for the field <code>datasource4</code>.</p>
     *
     * @param datasource4 a {@link DatasourceEntity} object.
     */
    public void setDatasource4(DatasourceEntity datasource4) {
        this.datasource4 = datasource4;
    }

    private ReportEntity report;

    /**
     * <p>Getter for the field <code>report</code>.</p>
     *
     * @return a {@link ReportEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "report_id", referencedColumnName = "id")
    public ReportEntity getReport() {
        return report;
    }

    /**
     * <p>Setter for the field <code>report</code>.</p>
     *
     * @param report a {@link ReportEntity} object.
     */
    public void setReport(ReportEntity report) {
        this.report = report;
    }

    private String endpoint;

    /**
     * <p>Getter for the field <code>endpoint</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "endpoint", length = 100, precision = 0)
    @Basic
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * <p>Setter for the field <code>endpoint</code>.</p>
     *
     * @param endpoint a {@link java.lang.String} object.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @NotBlank(message = "You must specify a type for the task")
    private String taskType;

    /**
     * <p>Getter for the field <code>taskType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "task_type", nullable = false)
    @Basic
    public String getTaskType() {
        return taskType;
    }

    /**
     * <p>Setter for the field <code>taskType</code>.</p>
     *
     * @param taskType a {@link java.lang.String} object.
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    /**
     * Works out when this task should next run
     *
     * @return boolean
     */
    @Transient
    public Date getDueDate() {

        // Set up all the schedule information for the schedule

        Date dReturn=null;
        try {
            Calendar objNow = Calendar.getInstance();
            Calendar objTarget = Calendar.getInstance();
            if (getSchedLastRun()==null) {
                objTarget.setTime(new Date(0));
            }
            else {
                objTarget.setTime(getSchedLastRun());
            }
            Calendar objLastrun = Calendar.getInstance();
            objLastrun.setTime(objTarget.getTime());

            // *******************************************************************************************************
            // The due date and time is based upon the type of the schedule
            // Minute - this is run every X minutes after the last time it ran and may exclude weekends
            // *******************************************************************************************************
            if (getSchedType()== SCHED_TYPE_MINUTE) {
                if (getSchedLastRun()==null) {
                    objTarget.setTime(new Date());
                    objTarget.set(Calendar.SECOND, 0);
                }
                else {
                    objTarget.add(Calendar.MINUTE, Math.abs(getSchedRunEveryMinutes()));
                }

                int nextDayStartHour = 0;
                int nextDayStartMin = 0;

                if (!Common.isBlank(getSchedRunEveryMinutesFrom()) && !Common.isBlank(getSchedRunEveryMinutesTo())) {
                    if (objNow.after(objTarget)) {
                        objTarget.setTime(new Date());
                        objTarget.set(Calendar.SECOND, 0);
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(Common.parseTime(getSchedRunEveryMinutesFrom()));
                    int fromHour = calendar.get(Calendar.HOUR_OF_DAY);
                    int fromMin = calendar.get(Calendar.MINUTE);

                    calendar.setTime(Common.parseTime(getSchedRunEveryMinutesTo()));
                    int toHour = calendar.get(Calendar.HOUR_OF_DAY);
                    int toMin = calendar.get(Calendar.MINUTE);

                    if (getSchedRunEveryMinutesFrom().compareTo(getSchedRunEveryMinutesTo()) <= 0) {

                        nextDayStartHour = fromHour;
                        nextDayStartMin = fromMin;

                        if (objTarget.get(Calendar.HOUR_OF_DAY) < fromHour ||
                                (objTarget.get(Calendar.HOUR_OF_DAY) == fromHour && objTarget.get(Calendar.MINUTE) < fromMin) ) {
                            objTarget.set(Calendar.HOUR_OF_DAY, fromHour);
                            objTarget.set(Calendar.MINUTE, fromMin);
                            objTarget.set(Calendar.SECOND, 0);
                        }
                        if (objTarget.get(Calendar.HOUR_OF_DAY) > toHour ||
                                (objTarget.get(Calendar.HOUR_OF_DAY) == toHour && objTarget.get(Calendar.MINUTE) > toMin) ) {
                            objTarget.set(Calendar.HOUR_OF_DAY, fromHour);
                            objTarget.set(Calendar.MINUTE, fromMin);
                            objTarget.set(Calendar.SECOND, 0);
                            objTarget.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }
                    else {
                        if ( ( objTarget.get(Calendar.HOUR_OF_DAY) < fromHour ||
                                (objTarget.get(Calendar.HOUR_OF_DAY) == fromHour && objTarget.get(Calendar.MINUTE) < fromMin)) &&
                             (objTarget.get(Calendar.HOUR_OF_DAY) > toHour ||
                                (objTarget.get(Calendar.HOUR_OF_DAY) == toHour && objTarget.get(Calendar.MINUTE) > toMin)  )
                            ) {
                            objTarget.setTime(new Date());
                            objTarget.set(Calendar.SECOND, 0);
                            objTarget.set(Calendar.HOUR_OF_DAY, fromHour);
                            objTarget.set(Calendar.MINUTE, fromMin);
                        }
                    }
                }

                // If weekends or holidays are excluded, then skip forwards to Monday and
                // the first possible time on Monday
                boolean dateOk = false;
                while (!dateOk) {
                    if (isSchedRunEveryDayExcludeWeekends() &&
                            (objTarget.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                             objTarget.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
                        objTarget.set(Calendar.HOUR_OF_DAY, nextDayStartHour);
                        objTarget.set(Calendar.MINUTE, nextDayStartMin);
                        objTarget.set(Calendar.SECOND, 0);
                        while (objTarget.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                                objTarget.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                            objTarget.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }
                    else if (isSchedRunEveryDayExcludeHolidays() && CaseManager.isHoliday(objTarget.getTime())) {
                        objTarget.set(Calendar.HOUR_OF_DAY, nextDayStartHour);
                        objTarget.set(Calendar.MINUTE, nextDayStartMin);
                        objTarget.set(Calendar.SECOND, 0);
                        objTarget.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    else
                        dateOk = true;
                }
                dReturn=objTarget.getTime();
            }

            // *******************************************************************************************************
            // Day - this is run once a day at a particular time and may exclude weekends
            // *******************************************************************************************************
            else if (getSchedType()== SCHED_TYPE_DAY) {

                // If we have already run today, then move ahead til tomorrow

                if (getSchedLastRun()==null) {
                    objTarget.setTime(new Date());
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(Common.parseTime(getSchedRunEveryDayAt()));
                objTarget.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                objTarget.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                objTarget.set(Calendar.SECOND, 0);
                if (objNow.after(objTarget) && getSchedLastRun()!=null) {
                    objTarget.add(Calendar.DAY_OF_MONTH, 1);
                }

                // If the next run is an excluded weekend or holiday then move forwards to the Monday
                boolean dateOk = false;
                while (!dateOk) {

                    if  (isSchedRunEveryDayExcludeWeekends() &&
                        (objTarget.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY ||
                        objTarget.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)) {
                        objTarget.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    else if(isSchedRunEveryDayExcludeHolidays() && CaseManager.isHoliday(objTarget.getTime()))
                        objTarget.add(Calendar.DAY_OF_MONTH, 1);
                    else
                        dateOk = true;
                }
                dReturn=objTarget.getTime();
            }

            // *******************************************************************************************************
            // Week - this is run every week on particular days at a particular time
            // *******************************************************************************************************
            else if (getSchedType()== SCHED_TYPE_WEEK) {
                if (getSchedLastRun()==null) {
                    objTarget.setTime(new Date());
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(Common.parseTime(getSchedRunEveryWeekDaysAt()));
                objTarget.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                objTarget.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                objTarget.set(Calendar.SECOND, 0);

                // If we have already run today or today is not a day we are
                // allowed to run on, then move through the days to the next one we can run on

                if (getSchedRunEveryWeekDays()!=null) {

                    String[] tmp = schedRunEveryWeekDays.split(",");
                    String[] retValue = "0,0,0,0,0,0,0".split(",");
                    int position;
                    for(String pos : tmp) {
                        position = Common.parseInt(pos);
                        if (position >=0 && position < 7)
                            retValue[position] = "1";
                    }
                    String daysAllowedToRun=Common.join(retValue, "") + "0000000";

                    if (objNow.after(objTarget) && getSchedLastRun()!=null)
                        objTarget.add(Calendar.DAY_OF_MONTH, 1);
                    int iDay=0;
                    while (daysAllowedToRun.charAt(objTarget.get(Calendar.DAY_OF_WEEK)-1)!='1' && iDay<7) {
                        objTarget.add(Calendar.DAY_OF_MONTH, 1);
                        iDay++;
                    }
                    dReturn=objTarget.getTime();
                }
            }

            // *******************************************************************************************************
            // Month - this is run every month on a particular day at a particular time
            // *******************************************************************************************************
            else if (getSchedType()== SCHED_TYPE_MONTH) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(Common.parseTime(getSchedRunEveryMonthAt()));
                objTarget.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                objTarget.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
                objTarget.set(Calendar.SECOND, 0);
                objTarget.set(Calendar.DAY_OF_MONTH, getSchedRunEveryMonthOn());

                // If we have already run today or already run this month, then
                // add a month

                if (objLastrun.after(objTarget)) {
                    objTarget.add(Calendar.MONTH, 1);
                }
                dReturn=objTarget.getTime();
            }
            // *******************************************************************************************************
            // Once - this is run once
            // *******************************************************************************************************
            else
                if (getSchedType() == SCHED_TYPE_ONCE) {
                    objTarget.setTime(new Date(getSchedRunOnceOn().getTime()));
                    dReturn = objTarget.getTime();
                }
        }
        catch (Exception e) {
            logger.error("Problem determining schedule {}", PivotalException.getErrorMessage(e));
        }

        return dReturn;
    }

    /**
     * Determines if this task is ready to run by looking at the schedule
     * and figuring out whether the next due date is less than now
     *
     * @return boolean
     */
    public boolean scheduledToRun() {
        Date dueDate= getDueDate();
        Date now = new Date();
        boolean retValue = (dueDate!=null && now.after(dueDate) && (getSchedLastRun()==null || dueDate.after(getSchedLastRun())));
        return retValue;
    }

    /**
     * Convenience method for updating the status of a scheduled task
     *
     * @param lock True/false to lock/unlock a task
     */
    @SuppressWarnings("unused")
    public void updateScheduledTaskStatus(boolean lock) {
        updateScheduledTaskStatus(lock, null);
    }

    /**
     * Convenience method for updating the status of a scheduled task
     *
     * @param lock True/false to lock/unlock a task
     * @param lastRun Date of last run
     */
    public void updateScheduledTaskStatus(boolean lock, Date lastRun) {
        updateScheduledTaskStatus(lock, null, lastRun);
    }

    /**
     * Convenience method for updating the status of a scheduled task
     *
     * @param lock True/false to lock/unlock a task
     * @param error Error message to set
     * @param lastRun Date of last run
     */
    public void updateScheduledTaskStatus(boolean lock, String error, Date lastRun) {
        try {
            if(getId()!=null) {
                ScheduledTaskEntity scheduledTask = HibernateUtils.getEntity(ScheduledTaskEntity.class, getId());
                scheduledTask.setLocked(lock);
                scheduledTask.setLockedBy(lock ? ServletHelper.getAppIdentity() : null);
                if (Common.isBlank(error))
                    scheduledTask.setSchedLastError(null);
                else
                    scheduledTask.setSchedLastError(error);
                if (lastRun != null) scheduledTask.setSchedLastRun(new Timestamp(lastRun.getTime()));
                HibernateUtils.save(scheduledTask);
                HibernateUtils.flush();
            }
        }
        catch (Exception e) {
            logger.error("Problem resetting the status of the scheduled task - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Convenience method for clearing all tasks status for this server
     */
    public static void clearScheduledTaskStatus() {
        clearScheduledTaskStatus(false);
    }

    /**
     * Convenience method for clearing all tasks status for this server
     *
     * @param clearLastrun True if the last run should be cleared
     */
    public static void clearScheduledTaskStatus(boolean clearLastrun) {
        try {
            logger.debug("Clearing the locked status for all tasks owned by [{}]", ServletHelper.getAppIdentity());
            int rows=HibernateUtils.createQuery("update ScheduledTaskEntity set locked=false,lockedBy=null" + (clearLastrun?",schedLastRun=null":"") + " where " + (clearLastrun?"lockedBy is null or ":"") + "lockedBy='" + ServletHelper.getAppIdentity() + '\'').executeUpdate();
            if (rows>0) {
                HibernateUtils.commit();
                HibernateUtils.flush();
                logger.info("Cleared status for {} tasks belonging to [{}]", rows, ServletHelper.getAppIdentity());
            }
            else {
                HibernateUtils.rollback();
            }
        }
        catch (Exception e) {
            logger.error("Problem resetting the status of the tasks - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Returns the thread that this running task is attached to
     *
     * @return Thread object
     */
    public Thread runningThread() {
        Thread returnValue=null;
        Thread[] threads=new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread tmp : threads) {
            if (Common.doStringsMatch(tmp.getName(), threadName()))
                returnValue=tmp;
        }

        return returnValue;
    }

    /**
     * Returns an unambiguous name for the task
     *
     * @return String
     */
    public String threadName() {
        return getClass().getSimpleName() + ' ' + getName() + " (" + getId() + ')';
    }

    /**
     * <p>isReportType.</p>
     *
     * @return a boolean.
     */
    @Transient
    public boolean isReportType() {
        return Common.isBlank(taskType) || Common.doStringsMatch(TASK_TYPE_REPORT, taskType);
    }

    /**
     * <p>isWebServiceType.</p>
     *
     * @return a boolean.
     */
    @Transient
    public boolean isWebServiceType() {
        return Common.isBlank(taskType) || Common.doStringsMatch(TASK_TYPE_WEBSERVICE, taskType);
    }

    /**
     * <p>isTransformerType.</p>
     *
     * @return a boolean.
     */
    @SuppressWarnings("unused")
    @Transient
    public boolean isTransformerType() {
        return Common.isBlank(taskType) || Common.doStringsMatch(TASK_TYPE_INGRESS, taskType);
    }

    private DistributionListEntity notifyDistributionList;

    /**
     * <p>Getter for the field <code>notifyDistributionList</code>.</p>
     *
     * @return a {@link DistributionListEntity} object.
     */
    @ManyToOne
    public
    @JoinColumn(name = "notify_distribution_list_id", referencedColumnName = "id")
    DistributionListEntity getNotifyDistributionList() {
        return notifyDistributionList;
    }

    /**
     * <p>Setter for the field <code>notifyDistributionList</code>.</p>
     *
     * @param distributionList a {@link DistributionListEntity} object.
     */
    public void setNotifyDistributionList(DistributionListEntity distributionList) {
        notifyDistributionList = distributionList;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(getDatasource(), getDatasource1(), getDatasource2(), getDatasource3(), getDatasource4(), description, disabled, getDistributionList(), endpoint, getErrorDistributionList(), locked, lockedBy, name, getNotifyDistributionList(), outputType, getReport(), schedLastError, schedLastRun, schedRunEveryDayAt, schedRunEveryDayExcludeWeekends, schedRunEveryDayExcludeHolidays, schedRunEveryMinutes, schedRunEveryMinutesFrom, schedRunEveryMinutesTo, schedRunEveryMonthAt, schedRunEveryMonthOn, schedRunEveryWeekDays, schedRunEveryWeekDaysAt, schedRunOnceOn, schedType, settingsXML, taskType);
    }
}
