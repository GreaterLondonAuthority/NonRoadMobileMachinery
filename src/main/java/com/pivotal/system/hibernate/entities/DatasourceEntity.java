/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.hibernate.utils.AppDataSource;
import com.pivotal.utils.PivotalException;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Maps the datasource table to the DatasourceEntity lass
 */
@Table(name = "datasource")
@Entity
public class DatasourceEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasourceEntity.class);
    private static final long serialVersionUID = -7982792780321913867L;
    /** Constant <code>SYSTEM_DATASOURCE_INTERNAL="system.datasource.internal"</code> */
    public static final String SYSTEM_DATASOURCE_INTERNAL = "system.datasource.internal";

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

    @NotBlank(message = "You must enter a name for this datasource")
    private String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "name", nullable = false, length = 255, precision = 0)
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

    @NotBlank(message = "You must enter a driver class name e.g. org.mariadb.jdbc.Driver")
    private String driver;

    /**
     * <p>Getter for the field <code>driver</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "driver", nullable = false, length = 255, precision = 0)
    @Basic
    public String getDriver() {
        return driver;
    }

    /**
     * <p>Setter for the field <code>driver</code>.</p>
     *
     * @param driver a {@link java.lang.String} object.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    @NotBlank(message = "You must enter a database URL e.g. jdbc:mysql://localhost:3306/dashboard")
    private String databaseUrl;

    /**
     * <p>Getter for the field <code>databaseUrl</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "database_url", nullable = false, length = 255, precision = 0)
    @Basic
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * <p>Setter for the field <code>databaseUrl</code>.</p>
     *
     * @param databaseUrl a {@link java.lang.String} object.
     */
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    private String username;

    /**
     * <p>Getter for the field <code>username</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "username", nullable = false, length = 100, precision = 0)
    @Basic
    public String getUsername() {
        return username;
    }

    /**
     * <p>Setter for the field <code>username</code>.</p>
     *
     * @param username a {@link java.lang.String} object.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    private String password;

    /**
     * <p>Getter for the field <code>password</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "password", length = 100, precision = 0)
    @Basic
    public String getPassword() {
        return password;
    }

    /**
     * <p>Setter for the field <code>password</code>.</p>
     *
     * @param password a {@link java.lang.String} object.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    private String initSql;

    /**
     * <p>Getter for the field <code>initSql</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Basic
    @Column(name = "init_sql", length = 65535, precision = 0)
    public String getInitSql() {
        return initSql;
    }

    /**
     * <p>Setter for the field <code>initSql</code>.</p>
     *
     * @param initSql a {@link java.lang.String} object.
     */
    public void setInitSql(String initSql) {
        this.initSql = initSql;
    }

    private ReportEntity cacheTriggerReport;

    /**
     * <p>Getter for the field <code>cacheTriggerReport</code>.</p>
     *
     * @return a {@link ReportEntity} object.
     */
    @ManyToOne
    @JoinColumn(name = "cache_trigger_report_id", referencedColumnName = "id")
    public ReportEntity getCacheTriggerReport() {
        return cacheTriggerReport;
    }
    /**
     * <p>Setter for the field <code>cacheTriggerReport</code>.</p>
     *
     * @param report a {@link ReportEntity} object.
     */
    public void setCacheTriggerReport(ReportEntity report) {
        cacheTriggerReport = report;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DatasourceEntity)) return false;
        final DatasourceEntity other = (DatasourceEntity) obj;
        return Objects.equals(this.cacheTimeout, other.cacheTimeout) &&
               Objects.equals(this.getCacheTriggerReport(), other.getCacheTriggerReport()) &&
               Objects.equals(this.databaseUrl, other.databaseUrl) &&
               Objects.equals(this.description, other.description) &&
               Objects.equals(this.driver, other.driver) &&
               Objects.equals(this.initSql, other.initSql) &&
               Objects.equals(this.initialSize, other.initialSize) &&
               Objects.equals(this.maxActive, other.maxActive) &&
               Objects.equals(this.maxIdle, other.maxIdle) &&
               Objects.equals(this.maxWait, other.maxWait) &&
               Objects.equals(this.minIdle, other.minIdle) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.password, other.password) &&
               Objects.equals(this.removeAbandoned, other.removeAbandoned) &&
               Objects.equals(this.removeAbandonedTimeout, other.removeAbandonedTimeout) &&
               Objects.equals(this.useCache, other.useCache) &&
               Objects.equals(this.useConnectionPool, other.useConnectionPool) &&
               Objects.equals(this.username, other.username) &&
               Objects.equals(this.validationQuery, other.validationQuery);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DatasourceEntity{" +
               "cacheTimeout='" + cacheTimeout + '\'' +
               ", cacheTriggerReport=" + getCacheTriggerReport() +
               ", databaseUrl='" + databaseUrl + '\'' +
               ", description='" + description + '\'' +
               ", driver='" + driver + '\'' +
               ", id='" + id + '\'' +
               ", initSql='" + initSql + '\'' +
               ", initialSize='" + initialSize + '\'' +
               ", maxActive='" + maxActive + '\'' +
               ", maxIdle='" + maxIdle + '\'' +
               ", maxWait='" + maxWait + '\'' +
               ", minIdle='" + minIdle + '\'' +
               ", name='" + name + '\'' +
               ", password='" + password + '\'' +
               ", removeAbandoned='" + removeAbandoned + '\'' +
               ", removeAbandonedTimeout='" + removeAbandonedTimeout + '\'' +
               ", useCache='" + useCache + '\'' +
               ", useConnectionPool='" + useConnectionPool + '\'' +
               ", username='" + username + '\'' +
               ", validationQuery='" + validationQuery + '\'' +
               '}';
    }

    private Collection<ScheduledTaskEntity> scheduledTasks;

    /**
     * <p>Getter for the field <code>scheduledTasks</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks() {
        return scheduledTasks;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks</code>.</p>
     *
     * @param scheduledTasks a {@link java.util.Collection} object.
     */
    public void setScheduledTasks(Collection<ScheduledTaskEntity> scheduledTasks) {
        this.scheduledTasks = scheduledTasks;
    }

    private Collection<ScheduledTaskEntity> scheduledTasks1;

    /**
     * <p>Getter for the field <code>scheduledTasks1</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource1", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks1() {
        return scheduledTasks1;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks1</code>.</p>
     *
     * @param scheduledTasks1 a {@link java.util.Collection} object.
     */
    public void setScheduledTasks1(Collection<ScheduledTaskEntity> scheduledTasks1) {
        this.scheduledTasks1 = scheduledTasks1;
    }

    private Collection<ScheduledTaskEntity> scheduledTasks2;

    /**
     * <p>Getter for the field <code>scheduledTasks2</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource2", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks2() {
        return scheduledTasks2;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks2</code>.</p>
     *
     * @param scheduledTasks2 a {@link java.util.Collection} object.
     */
    public void setScheduledTasks2(Collection<ScheduledTaskEntity> scheduledTasks2) {
        this.scheduledTasks2 = scheduledTasks2;
    }

    private Collection<ScheduledTaskEntity> scheduledTasks3;

    /**
     * <p>Getter for the field <code>scheduledTasks3</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource3", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks3() {
        return scheduledTasks3;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks3</code>.</p>
     *
     * @param scheduledTasks3 a {@link java.util.Collection} object.
     */
    public void setScheduledTasks3(Collection<ScheduledTaskEntity> scheduledTasks3) {
        this.scheduledTasks3 = scheduledTasks3;
    }

    private Collection<ScheduledTaskEntity> scheduledTasks4;

    /**
     * <p>Getter for the field <code>scheduledTasks4</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource4", cascade = CascadeType.ALL)
    public Collection<ScheduledTaskEntity> getScheduledTasks4() {
        return scheduledTasks4;
    }

    /**
     * <p>Setter for the field <code>scheduledTasks4</code>.</p>
     *
     * @param scheduledTasks4 a {@link java.util.Collection} object.
     */
    public void setScheduledTasks4(Collection<ScheduledTaskEntity> scheduledTasks4) {
        this.scheduledTasks4 = scheduledTasks4;
    }

    private Collection<DistributionListEntity> distributionLists;

    /**
     * <p>Getter for the field <code>distributionLists</code>.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    @OneToMany(mappedBy = "datasource", cascade = CascadeType.ALL)
    public Collection<DistributionListEntity> getDistributionLists() {
        return distributionLists;
    }

    /**
     * <p>Setter for the field <code>distributionLists</code>.</p>
     *
     * @param distributionLists a {@link java.util.Collection} object.
     */
    public void setDistributionLists(Collection<DistributionListEntity> distributionLists) {
        this.distributionLists = distributionLists;
    }

    private boolean useConnectionPool;
    /**
     * <p>isUseConnectionPool.</p>
     *
     * @return a boolean.
     */
    @Column(name = "use_connection_pool", nullable = false, length = 0, precision = 0)
    @Basic
    public boolean isUseConnectionPool() {
        return useConnectionPool;
    }
    /**
     * <p>Setter for the field <code>useConnectionPool</code>.</p>
     *
     * @param useConnectionPool a boolean.
     */
    public void setUseConnectionPool(boolean useConnectionPool) {
        this.useConnectionPool = useConnectionPool;
    }

    private boolean removeAbandoned;
    /**
     * <p>isRemoveAbandoned.</p>
     *
     * @return a boolean.
     */
    @Column(name = "remove_abandoned", nullable = false, length = 0, precision = 0)
    @Basic
    public boolean isRemoveAbandoned() {
        return removeAbandoned;
    }
    /**
     * <p>Setter for the field <code>removeAbandoned</code>.</p>
     *
     * @param removeAbandoned a boolean.
     */
    public void setRemoveAbandoned(boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
    }


    private String validationQuery;

    /**
     * <p>Getter for the field <code>validationQuery</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Column(name = "validation_query", length = 255, precision = 0)
    @Basic
    public String getValidationQuery() {
        return validationQuery;
    }
    /**
     * <p>Setter for the field <code>validationQuery</code>.</p>
     *
     * @param validationQuery a {@link java.lang.String} object.
     */
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    private Integer maxActive;
    /**
     * <p>Getter for the field <code>maxActive</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "max_active", length = 5, precision = 0)
    @Basic
    public Integer getMaxActive() {
        return maxActive;
    }
    /**
     * <p>Setter for the field <code>maxActive</code>.</p>
     *
     * @param maxActive a {@link java.lang.Integer} object.
     */
    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    private Integer maxIdle;
    /**
     * <p>Getter for the field <code>maxIdle</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "max_idle", length = 5, precision = 0)
    @Basic
    public Integer getMaxIdle() {
        return maxIdle;
    }

    /**
     * <p>Setter for the field <code>maxIdle</code>.</p>
     *
     * @param maxIdle a {@link java.lang.Integer} object.
     */
    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }


    private Integer minIdle;
    /**
     * <p>Getter for the field <code>minIdle</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "min_idle", length = 5, precision = 0)
    @Basic
    public Integer getMinIdle() {
        return minIdle;
    }

    /**
     * <p>Setter for the field <code>minIdle</code>.</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setMinIdle(Integer id) {
        minIdle = id;
    }


    private Integer initialSize;
    /**
     * <p>Getter for the field <code>initialSize</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "initial_size", length = 5, precision = 0)
    @Basic
    public Integer getInitialSize() {
        return initialSize;
    }

    /**
     * <p>Setter for the field <code>initialSize</code>.</p>
     *
     * @param initialSize a {@link java.lang.Integer} object.
     */
    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }


    private Integer maxWait;
    /**
     * <p>Getter for the field <code>maxWait</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "max_wait", length = 15, precision = 0)
    @Basic
    public Integer getMaxWait() {
        return maxWait;
    }

    /**
     * <p>Setter for the field <code>maxWait</code>.</p>
     *
     * @param maxWait a {@link java.lang.Integer} object.
     */
    public void setMaxWait(Integer maxWait) {
        this.maxWait = maxWait;
    }


    private Integer removeAbandonedTimeout;
    /**
     * <p>Getter for the field <code>removeAbandonedTimeout</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "remove_abandoned_timeout", length = 5, precision = 0)
    @Basic
    public Integer getRemoveAbandonedTimeout() {
        return removeAbandonedTimeout;
    }

    /**
     * <p>Setter for the field <code>removeAbandonedTimeout</code>.</p>
     *
     * @param removeAbandonedTimeout a {@link java.lang.Integer} object.
     */
    public void setRemoveAbandonedTimeout(Integer removeAbandonedTimeout) {
        this.removeAbandonedTimeout = removeAbandonedTimeout;
    }

    private boolean useCache;
    /**
     * <p>isUseCache.</p>
     *
     * @return a boolean.
     */
    @Column(name = "use_cache", nullable = false, length = 0, precision = 0)
    @Basic
    public boolean isUseCache() {
        return useCache;
    }
    /**
     * <p>Setter for the field <code>useCache</code>.</p>
     *
     * @param useCache a boolean.
     */
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    private Integer cacheTimeout;
    /**
     * <p>Getter for the field <code>cacheTimeout</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Column(name = "cache_timeout", length = 5, precision = 0)
    @Basic
    public Integer getCacheTimeout() {
        return cacheTimeout;
    }
    /**
     * <p>Setter for the field <code>cacheTimeout</code>.</p>
     *
     * @param cacheTimeout a {@link java.lang.Integer} object.
     */
    public void setCacheTimeout(Integer cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    /**
     * <p>createSystemDatasource.</p>
     */
    public static void createSystemDatasource() {

        // Check to see if the data source already exists

        DatasourceEntity ds = HibernateUtils.getEntity(DatasourceEntity.class, SYSTEM_DATASOURCE_INTERNAL);
        if (ds==null)
            ds = new DatasourceEntity();

        // Populate it with all the stuff from the context
        // We are updating it

        try {
            AppDataSource dsInfo = HibernateUtils.getDataSource();
            ds.setName(SYSTEM_DATASOURCE_INTERNAL);
            ds.setDriver(dsInfo.getDriverClassName());
            ds.setCacheTimeout(600);
            ds.setUseCache(true);
            ds.setUseConnectionPool(true);
            ds.setDatabaseUrl(dsInfo.getUrl());
            ds.setInitialSize(dsInfo.getInitialSize());
            ds.setMaxActive(dsInfo.getMaxActive());
            ds.setMaxIdle(dsInfo.getMaxIdle());
            ds.setMaxWait(dsInfo.getMaxWait());
            ds.setMinIdle(dsInfo.getMinIdle());
            ds.setPassword(dsInfo.getPassword());
            ds.setUsername(dsInfo.getUsername());
            ds.setValidationQuery(dsInfo.getValidationQuery());
            HibernateUtils.save(ds);
        }
        catch (Exception e) {
            logger.error("Cannot access the Hibernate sources - {}", PivotalException.getErrorMessage(e));
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(cacheTimeout, getCacheTriggerReport(), databaseUrl, description, driver, initSql, initialSize, maxActive, maxIdle, maxWait, minIdle, name, password, removeAbandoned, removeAbandonedTimeout, useCache, useConnectionPool, username, validationQuery);
    }
}
