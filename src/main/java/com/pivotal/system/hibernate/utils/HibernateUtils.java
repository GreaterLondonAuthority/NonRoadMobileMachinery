/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.hibernate.utils;

import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.ChangeLogEntity;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.entities.SettingsEntity;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.web.Constants;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.postgresql.util.Base64;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyValue;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.w3c.dom.Document;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.servlet.ServletContextEvent;
import javax.validation.ConstraintViolation;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.*;
import java.util.Comparator;
import java.util.Date;
import java.util.*;

import static com.pivotal.utils.ClassUtils.setPropertyValue;
import static com.pivotal.utils.Common.isBlank;

/**
 * Hibernate initialisation
 * We're using our own management of sessions here so that we
 * can safely work in a truly multi-threaded environment
 * We use a ThreadLocal variable (a value bound to the current thread)
 * to store the current session
 */
public class HibernateUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HibernateUtils.class);

    // Save the current Hibernate session
    private static ThreadLocal<Session> threadSession = new ThreadLocal<>();

    /**
     * Constant <code>JDBC_NRMM="jdbc/app"</code>
     */
    public static final String JDBC_APP = "jdbc/app";
    public static final String MAIL_SETTINGS = "mail/Session";
    public static final String UPLOADED_FILE_LOCATION = "uploadedFileLocation";
    public static final String MAPPING_FILE_LIST = "mappingFileList";
    public static final String MAPPING_KEY = "mappingKey";

    private static SessionFactory sessionFactory;

    //** Email address to send error reports
    /**
     * Constant <code>SETTING_ERROR_EMAIL="system.setting.error.email"</code>
     */
    public static final String SETTING_ERROR_EMAIL = "system.setting.error.email";

    //** The maximum number of recipients that a report can be sent to individually
    /**
     * Constant <code>SETTING_MAX_RECIPIENTS="system.setting.max.recipients"</code>
     */
    public static final String SETTING_MAX_RECIPIENTS = "system.setting.max.recipients";
    /**
     * Constant <code>SETTING_MAX_RECIPIENTS_DEFAULT</code>
     */
    public static final Integer SETTING_MAX_RECIPIENTS_DEFAULT = 5000;

    //** Flag to indicate that the system shouldn't actually send reports
    /**
     * Constant <code>SETTING_PUBLISHING_ENABLED="system.setting.publishing.enabled"</code>
     */
    public static final String SETTING_PUBLISHING_ENABLED = "system.setting.publishing.enabled";
    /**
     * Constant <code>SETTING_PUBLISHING_ENABLED_DEFAULT=false</code>
     */
    public static final boolean SETTING_PUBLISHING_ENABLED_DEFAULT = true;

    /**
     **  Publishing server to send reports
     */
    public static final String SETTING_PUBLISHING_SERVER = "system.setting.publishing.server";

    /**
     * Constant <code>SETTING_AUTHENTICATION_TYPE="system.setting.authentication.type"</code>
     */
    public static final String SETTING_AUTHENTICATION_TYPE = "system.setting.authentication.type";
    public static final String SETTING_AUTHENTICATION_TYPE_DEFAULT = "none";

    public static final String SETTING_LOGINASOTHERUSER_ENABLED = "system.setting.authentication.loginasuserenabled";
    public static final Boolean SETTING_LOGINASOTHERUSER_ENABLED_DEFAULT = false;

    //** Address of the LDAP server used for authentication
    /**
     * Constant <code>SETTING_LDAP_HOST="system.setting.ldap.host"</code>
     */
    public static final String SETTING_LDAP_HOST = "system.setting.ldap.host";
    /**
     * Constant <code>SETTING_LDAP_PRINCIPLE_DN="system.setting.ldap.principle.dn"</code>
     */
    public static final String SETTING_LDAP_PRINCIPLE_DN = "system.setting.ldap.principle.dn";
    /**
     * Constant <code>SETTING_LDAP_PRINCIPLE_PASSWORD="system.setting.ldap.principle.password"</code>
     */
    public static final String SETTING_LDAP_PRINCIPLE_PASSWORD = "system.setting.ldap.principle.password";
    /**
     * Constant <code>SETTING_LDAP_USER_SEARCH="system.setting.ldap.user.search"</code>
     */
    public static final String SETTING_LDAP_USER_SEARCH = "system.setting.ldap.user.search";

    //** SAML settings
    /**
     * Constant <code>SETTING_SAML_IDP="system.setting.saml.idp"</code>
     */
    public static final String SETTING_SAML_IDP = "system.setting.saml.idp";
    /**
     * Constant <code>SETTING_SAML_TYPE="system.setting.saml.type"</code>
     */
    public static final String SETTING_SAML_TYPE = "system.setting.saml.type";
    /**
     * Constant <code>SETTING_SAML_TYPE_DEFAULT=false</code>
     */
    public static final boolean SETTING_SAML_TYPE_DEFAULT = false;
    /**
     * Constant <code>SETTING_SAML_CERTIFICATE="system.setting.saml.certificate"</code>
     */
    public static final String SETTING_SAML_CERTIFICATE = "system.setting.saml.certificate";
    /**
     * Constant <code>SETTING_SAML_USERNAME_TAG="system.setting.saml.username.tag"</code>
     */
    public static final String SETTING_SAML_USERNAME_TAG = "system.setting.saml.username.tag";
    /**
     * Constant <code>SETTING_SAML_EMAIL_TAG="system.setting.saml.email.tag"</code>
     */
    public static final String SETTING_SAML_EMAIL_TAG = "system.setting.saml.email.tag";
    /**
     * Constant <code>SETTING_SAML_PROVIDER_NAME="system.setting.saml.provider.name"</code>
     */
    public static final String SETTING_SAML_PROVIDER_NAME = "system.setting.saml.provider.name";
    /**
     * Constant <code>SETTING_SAML_AUTH_ID="system.setting.saml.auth.id"</code>
     */
    public static final String SETTING_SAML_AUTH_ID = "system.setting.saml.auth.id";

    /**
     * Constant <code>SETTING_EMAIL_SERVER_HOST="system.setting.email.server.host"</code>
     */
    public static final String SETTING_EMAIL_SERVER_HOST = "system.setting.email.server.host";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_USERNAME="system.setting.email.server.username"</code>
     */
    public static final String SETTING_EMAIL_SERVER_USERNAME = "system.setting.email.server.username";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_PASSWORD="system.setting.email.server.password"</code>
     */
    public static final String SETTING_EMAIL_SERVER_PASSWORD = "system.setting.email.server.password";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_PORT="system.setting.email.server.port"</code>
     */
    public static final String SETTING_EMAIL_SERVER_PORT = "system.setting.email.server.port";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_FROM="system.setting.email.server.from"</code>
     */
    public static final String SETTING_EMAIL_SERVER_FROM = "system.setting.email.server.from";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_SSL="system.setting.email.server.ssl"</code>
     */
    public static final String SETTING_EMAIL_SERVER_SSL = "system.setting.email.server.ssl";
    /**
     * Constant <code>SETTING_EMAIL_SERVER_DEBUG="system.setting.email.server.debug"</code>
     */
    public static final String SETTING_EMAIL_SERVER_DEBUG = "system.setting.email.server.debug";

    //** When publishing is turned off, the address to publish (email) all reports
    /**
     * Constant <code>SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS="system.setting.default.email.publisher."{trunked}</code>
     */
    public static final String SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS = "system.setting.default.email.publisher.address";
    /**
     * Constant <code>SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS_DEFAULT=""</code>
     */
    public static final String SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS_DEFAULT = "";

    //** When publishing is turned off, the address to publish (VFS) all reports
    /**
     * Constant <code>SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS="system.setting.default.vfs.publisher.ad"{trunked}</code>
     */
    public static final String SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS = "system.setting.default.vfs.publisher.address";
    /**
     * Constant <code>SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS_DEFAULT="c:\\tmp.pdf"</code>
     */
    public static final String SETTING_DEFAULT_VFS_PUBLISHER_ADDRESS_DEFAULT = "c:\\tmp.pdf";

    //** The default theme used
    /**
     * Constant <code>SETTING_DEFAULT_THEME="system.setting.default.theme"</code>
     */
    public static final String SETTING_DEFAULT_THEME = "system.setting.default.theme";
    /**
     * Constant <code>SETTING_DEFAULT_THEME_DEFAULT="default"</code>
     */
    public static final String SETTING_DEFAULT_THEME_DEFAULT = "default";

    // ** Default email host
    /**
     * Constant <code>SETTING_DEFAULT_EMAIL_HOST="mail.pivotal-solutions.co.uk"</code>
     */
    public static final String SETTING_DEFAULT_EMAIL_HOST = "mail.pivotal-solutions.co.uk";

    // ** Default email sender
    /**
     * Constant <code>SETTING_DEFAULT_EMAIL_SENDER="do-not-reply@pivotal-solutions.co.uk"</code>
     */
    public final static String SETTING_DEFAULT_EMAIL_SENDER = "do-not-reply@pivotal-solutions.co.uk";

    //** When publishing is turned off, the address to publish (VFS) all reports
    /**
     * Constant <code>SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS="system.setting.default.printer.publishe"{trunked}</code>
     */
    public static final String SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS = "system.setting.default.printer.publisher.address";
    /**
     * Constant <code>SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS_DEFAULT="Gnostice Print2eDoc"</code>
     */
    public static final String SETTING_DEFAULT_PRINTER_PUBLISHER_ADDRESS_DEFAULT = "Gnostice Print2eDoc";

    //** Flag to indicate that the scheduler is switched off
    /**
     * Constant <code>SETTING_SCHEDULING_ENABLED="system.setting.scheduling.enabled"</code>
     */
    public static final String SETTING_SCHEDULING_ENABLED = "system.setting.scheduling.enabled";
    /**
     * Constant <code>SETTING_SCHEDULING_ENABLED_DEFAULT=false</code>
     */
    public static final boolean SETTING_SCHEDULING_ENABLED_DEFAULT = false;

    //** Number of rows to display in a page
    /**
     * Constant <code>SETTING_PAGE_LENGTH="system.setting.page.length"</code>
     */
    public static final String SETTING_PAGE_LENGTH = "system.setting.page.length";
    /**
     * Constant <code>SETTING_PAGE_LENGTH_DEFAULT</code>
     */
    public static final Integer SETTING_PAGE_LENGTH_DEFAULT = 50;

    //** Number of months of entries to keep in the log table
    /**
     * Constant <code>SETTING_LOG_MONTHS="system.setting.log.months"</code>
     */
    public static final String SETTING_LOG_MONTHS = "system.setting.log.months";
    /**
     * Constant <code>SETTING_LOG_MONTHS_DEFAULT</code>
     */
    public static final Integer SETTING_LOG_MONTHS_DEFAULT = 12;

    //** Flag to indicate which which is the selected database cache
    /**
     * Constant <code>SETTING_CACHE_ENGINE="system.setting.cache.engine"</code>
     */
    public static final String SETTING_CACHE_ENGINE = "system.setting.cache.engine";
    /**
     * Constant <code>SETTING_CACHE_ENGINE_DEFAULT="ehcache"</code>
     */
    public static final String SETTING_CACHE_ENGINE_DEFAULT = "ehcache";

    //** Flag to indicate the cache max size if selected cache is inside JVM
    /**
     * Constant <code>SETTING_CACHE_MAX_SIZE="system.setting.cache.max.size"</code>
     */
    public static final String SETTING_CACHE_MAX_SIZE = "system.setting.cache.max.size";
    /**
     * Constant <code>SETTING_CACHE_MAX_SIZE_DEFAULT</code>
     */
    public static final Integer SETTING_CACHE_MAX_SIZE_DEFAULT = 24;

    //** Integer that tells the system the maximum time a Dashboard user session should
    //** be kept alive after the last recorded activity in minutes
    /**
     * Constant <code>SETTING_SESSION_TIMEOUT="system.setting.session.timeout"</code>
     */
    public static final String SETTING_SESSION_TIMEOUT = "system.setting.session.timeout";
    /**
     * Constant <code>SETTING_SESSION_TIMEOUT_DEFAULT</code>
     */
    public static final Integer SETTING_SESSION_TIMEOUT_DEFAULT = 30;

    //** Integer that tells the system the maximum time a Dashboard user session should
    //** be kept alive after the last recorded activity in minutes
    /**
     * Constant <code>SETTING_SESSION_TIMEOUT_WARNING="system.setting.session.timeout_warning"</code>
     */
    public static final String SETTING_SESSION_TIMEOUT_WARNING = "system.setting.session.timeout_warning";
    /**
     * Constant <code>SETTING_SESSION_TIMEOUT_WARNING_DEFAULT</code>
     */
    public static final Integer SETTING_SESSION_TIMEOUT_WARNING_DEFAULT = 25;

    //** Integer that tells the system the maximum time in minutes an uploaded file will be retained
    //** until it is either claimed by a process or deleted
    /**
     * Constant <code>SETTING_UPLOAD_FILE_RETENTION="system.setting.session.upload_file_retention"</code>
     */
    public static final String SETTING_SESSION_UPLOAD_FILE_RETENTION = "system.setting.session.upload_file_retention";
    /**
     * Constant <code>SETTING_SESSION_UPLOAD_FILE_RETENTION_DEFAULT</code>
     */
    public static final Integer SETTING_SESSION_UPLOAD_FILE_RETENTION_DEFAULT = 240;

    //** list of memcached servers used if selected cache is memcached
    /**
     * Constant <code>SETTING_CACHE_MEMCACHED_SERVERS="system.setting.cache.memcached.servers"</code>
     */
    public static final String SETTING_CACHE_MEMCACHED_SERVERS = "system.setting.cache.memcached.servers";
    /**
     * Constant <code>SETTING_CACHE_MEMCACHED_SERVERS_DEFAULT=""</code>
     */
    public static final String SETTING_CACHE_MEMCACHED_SERVERS_DEFAULT = "";

    //** The expiration time of an cached object
    /**
     * Constant <code>SETTING_CACHE_EXPIRATION="system.setting.cache.expiration"</code>
     */
    public static final String SETTING_CACHE_EXPIRATION = "system.setting.cache.expiration";
    /**
     * Constant <code>SETTING_CACHE_EXPIRATION_DEFAULT</code>
     */
    public static final Integer SETTING_CACHE_EXPIRATION_DEFAULT = 300;

    //** The period of the background monitor thread
    /**
     * Constant <code>SETTING_MONITOR_PERIOD="system.setting.monitor.period"</code>
     */
    public static final String SETTING_MONITOR_PERIOD = "system.setting.monitor.period";
    /**
     * Constant <code>SETTING_MONITOR_PERIOD_DEFAULT</code>
     */
    public static final Integer SETTING_MONITOR_PERIOD_DEFAULT = 60;
    /**
     * Constant <code>SETTING_MONITOR_PERIOD_DEAD="system.setting.monitor.period.dead"</code>
     */
    public static final String SETTING_MONITOR_PERIOD_DEAD = "system.setting.monitor.period.dead";
    /**
     * Constant <code>SETTING_MONITOR_PERIOD_DEAD_DEFAULT</code>
     */
    public static final Integer SETTING_MONITOR_PERIOD_DEAD_DEFAULT = 360;

    //** Turns off the recording of timing/counting of requests
    /**
     * Constant <code>SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS="system.setting.monitor.bypass.dispatche"{trunked}</code>
     */
    public static final String SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS = "system.setting.monitor.bypass.dispatcher.events";
    /**
     * Constant <code>SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS_DEFAULT=false</code>
     */
    public static final boolean SETTING_MONITOR_BYPASS_DISPATCHER_EVENTS_DEFAULT = false;

    //** Turns on/off auto-start of search manager
    /**
     * Constant <code>SETTING_SEARCH_AUTOSTART="system.setting.search.manager.autostart"</code>
     */
    public static final String SETTING_SEARCH_AUTOSTART = "system.setting.search.manager.autostart";
    /**
     * Constant <code>SETTING_SEARCH_AUTOSTART_DEFAULT=true</code>
     */
    public static final boolean SETTING_SEARCH_AUTOSTART_DEFAULT = true;

    //** Sets the template for the search
    /**
     * Constant <code>SETTING_SEARCH_CRITERIA_TEMPLATE="system.setting.search.manager.criteria_template"</code>
     */
    public static final String SETTING_SEARCH_CRITERIA_TEMPLATE = "system.setting.search.manager.criteria_template";
    /**
     * Constant <code>SETTING_SEARCH_CRITERIA_TEMPLATE_DEFAULT=$Criteria</code>
     */
    public static final String SETTING_SEARCH_CRITERIA_TEMPLATE_DEFAULT = "$Criteria";

    /**
     * Constant <code>SETTING_APP_GENERAL_AUTOSAVE_PERIOD="system.setting.app.general.autosave_period"</code>
     */
    public static final String SETTING_APP_GENERAL_AUTOSAVE_PERIOD="system.setting.app.general.autosave_period";
    /**
     * Constant <code>SETTING_APP_GENERAL_AUTOSAVE_PERIOD_DEFAULT="30"</code>
     */
    public static final Integer SETTING_APP_GENERAL_AUTOSAVE_PERIOD_DEFAULT=30;

    /**
     * Constant <code>SETTING_APP_GENERAL_FILE_UPLOAD_TYPES="system.setting.app.general.file_upload_types"</code>
     */
    public static final String SETTING_APP_GENERAL_FILE_UPLOAD_TYPES="system.setting.app.general.file_upload_types";

    /**
     * Constant <code>SETTING_APP_GENERAL_FILE_UPLOAD_TYPES_DEFAULT="system.setting.app.general.file_upload_types_default"</code>
     */
    public static final String SETTING_APP_GENERAL_FILE_UPLOAD_TYPES_DEFAULT="jpg,jpeg,png,tiff,bmp,gif,pdf,doc,docx,odt,xls,xlsx";

    /**
     * Constant <code>SETTING_APP_GENERAL_FILE_UPLOAD_MAXSIZE="system.setting.app.general.file_upload_maxsize"</code>
     */
    public static final String SETTING_APP_GENERAL_FILE_UPLOAD_MAXSIZE="system.setting.app.general.file_upload_maxsize";

    /**
     * Constant <code>SETTING_APP_GENERAL_FILE_UPLOAD_MAXSIZE_default="system.setting.app.general.file_upload_maxsize_default"</code>
     */
    public static final Integer SETTING_APP_GENERAL_FILE_UPLOAD_MAXSIZE_DEFAULT=10;

    /**
     * Constant <code>SETTING_APP_GENERAL_TAN_GUIDE_LINK="system.setting.app.general.tan_guide_link"</code>
     */
    public static final String SETTING_APP_GENERAL_TAN_GUIDE_LINK="system.setting.app.general.tan_guide_link";

    /**
     * Constant <code>SETTING_MEETING_WIDGET_STANDARD_TITLES="widget.meeting.name"</code>
     */
    public static final String SETTING_MEETING_WIDGET_STANDARD_TITLES="widget.meeting.name";
    /**
     * Constant <code>SETTING_CASE_WIDGET_STANDARD_TITLES="widget.case.name"</code>
     */
    public static final String SETTING_CASE_WIDGET_STANDARD_TITLES="widget.case.name";

    /**
     * Constant <code>SETTING_APP_MEETING_STANDARD_TITLES="system.setting.app.meeting.standard.titles"</code>
     */
    public static final String SETTING_APP_MEETING_STANDARD_TITLES="system.setting.app.meeting.standard.titles";

    /**
     * Constant <code>SETTING_WIDGET_RECORD_LIMIT="system.setting.app.widget.record_limit"</code>
     */
    public static final String SETTING_WIDGET_RECORD_LIMIT="system.setting.app.widget.record_limit";

    /**
     * Constant <code>SETTING_WIDGET_NOTE_TAG_IGNORE="system.setting.app.widget.note.tag.ignore"</code>
     */
    public static final String SETTING_WIDGET_NOTE_TAG_IGNORE="system.setting.app.widget.note.tag.ignore";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_DAYS="system.setting.app.meeting.preapp.days"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_DAYS="system.setting.app.meeting.preapp.days";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_DAYS_DEFAULT=10</code>
     */
    public static final Integer SETTING_APP_MEETING_PREAPP_MEETING_DAYS_DEFAULT=10;

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_VENUE="system.setting.app.meeting.preapp.venue"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_VENUE="system.setting.app.meeting.preapp.venue";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_VENUE_DEFAULT=1</code>
     */
    public static final Integer SETTING_APP_MEETING_PREAPP_MEETING_VENUE_DEFAULT=1;

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_TITLE="system.setting.app.meeting.preapp.title"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_TITLE="system.setting.app.meeting.preapp.title";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_TITLE_DEFAULT="system.setting.app.meeting.preapp.title.default"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_TITLE_DEFAULT="system.setting.app.meeting.preapp.title.default";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_STATUS="system.setting.app.meeting.preapp.status"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_STATUS="system.setting.app.meeting.preapp.status";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_STATUS_DEFAULT=1</code>
     */
    public static final Integer SETTING_APP_MEETING_PREAPP_MEETING_STATUS_DEFAULT=1;

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_TYPE="system.setting.app.meeting.preapp.type"</code>
     */
    public static final String SETTING_APP_MEETING_PREAPP_MEETING_TYPE="system.setting.app.meeting.preapp.type";

    /**
     * Constant <code>SETTING_APP_MEETING_PREAPP_MEETING_TYPE_DEFAULT=1</code>
     */
    public static final Integer SETTING_APP_MEETING_PREAPP_MEETING_TYPE_DEFAULT=1;

    /**
     * Constant <code>SETTING_APP_MEETING_STAGE_MEETING_TYPE="system.setting.app.meeting.stage.type"</code>
     */
    public static final String SETTING_APP_MEETING_STAGE_MEETING_TYPE="system.setting.app.meeting.stage.type";

    /**
     * Constant <code>SETTING_APP_MEETING_STAGE_MEETING_TYPE_DEFAULT=2</code>
     */
    public static final Integer SETTING_APP_MEETING_STAGE_MEETING_TYPE_DEFAULT=2;

    /**
     * Constant <code>SETTING_APP_HOLIDAYS="system.setting.app.holidays"</code>
     */
    public static final String SETTING_APP_HOLIDAYS="system.setting.app.holidays";

    /**
     * Constant <code>SETTING_APP_RESTRICTED_HOLIDAYS="system.setting.app.restricted_holidays"</code>
     */
    public static final String SETTING_APP_RESTRICTED_HOLIDAYS="system.setting.app.restricted_holidays";

    /**
     * Constant <code>SETTING_APP_HOLIDAY_URL="system.setting.app.holiday_url"</code>
     */
    public static final String SETTING_APP_HOLIDAY_URL="system.setting.app.holiday_url";

    /**
     * Constant <code>SETTING_APP_CASE_RESOLUTION_PERIOD="system.setting.app.case.resolution_period"</code>
     */
    public static final String SETTING_APP_CASE_RESOLUTION_PERIOD="system.setting.app.case.resolution_period";

    /**
     * Constant <code>SETTING_APP_CASE_RESOLUTION_PERIOD_DEFAULT=42</code>
     */
    public static final Integer SETTING_APP_CASE_RESOLUTION_PERIOD_DEFAULT=42;

    /**
     * Constant <code>SETTING_APP_CASE_SHOW_EMBEDDED_IMAGES_IN_DOCUMENTS="system.setting.app.case.show_embeded_images_in_documents"</code>
     */
    public static final String SETTING_APP_CASE_SHOW_EMBEDDED_IMAGES_IN_DOCUMENTS="system.setting.app.case.show_embedded_images_in_documents";

    /**
     * Constant <code>SETTING_APP_NEXT_CASE_NUMBER="system.setting.app.next_case_number"</code>
     */
    public static final String SETTING_APP_NEXT_CASE_NUMBER="system.setting.app.next_case_number";

    /**
     * Constant <code>SETTING_APP_NEXT_CASE_NUMBER_DEFAULT="4000"</code>
     */
    public static final String SETTING_APP_NEXT_CASE_NUMBER_DEFAULT="4000";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_STATUS="system.setting.app.case.default.status"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_STATUS="system.setting.app.case.default.status";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_STAGE="system.setting.app.case.default.stage"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_STAGE="system.setting.app.case.default.stage";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_AGENT_STATUS="system.setting.app.case.default_agent.status"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_AGENT_STATUS="system.setting.app.case.default_agent.status";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_AGENT_STAGE="system.setting.app.case.default_agent.stage"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_AGENT_STAGE="system.setting.app.case.default_agent.stage";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_NOTE_TYPE="system.setting.app.case.default.note.type"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_NOTE_TYPE="system.setting.app.case.default.note.type";

    /**
     * Constant <code>SETTING_APP_CASE_AGENT_DOCUMENT_INFORMATION="system.setting.app.case.agent_document_information"</code>
     */
    public static final String SETTING_APP_CASE_AGENT_DOCUMENT_INFORMATION="system.setting.app.case.agent_document_information";

    /**
     * Constant <code>SETTING_APP_CASE_OFFICER_ROLE="system.setting.app.case.officer.role"</code>
     */
    public static final String SETTING_APP_CASE_OFFICER_ROLE="system.setting.app.case.officer.role";

    /**
     * Constant <code>SETTING_APP_CASE_COPY_OFFICERS="system.setting.app.case.copy.officers"</code>
     */
    public static final String SETTING_APP_CASE_COPY_OFFICERS="system.setting.app.case.copy.officers";

    /**
     * Constant <code>SETTING_APP_CASE_COPY_OFFICERS_DEFAULT=""</code>
     */
    public static final String SETTING_APP_CASE_COPY_OFFICERS_DEFAULT="";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_FOLDERS="system.setting.app.case.default.folders"</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_FOLDERS="system.setting.app.case.default.folders";

    /**
     * Constant <code>SETTING_APP_CASE_DEFAULT_FOLDERS_DEFAULT=""</code>
     */
    public static final String SETTING_APP_CASE_DEFAULT_FOLDERS_DEFAULT="";

    /**
     * Constant <code>SETTING_APP_MEETING_DEFAULT_FOLDERS="system.setting.app.meeting.default.folders"</code>
     */
    public static final String SETTING_APP_MEETING_DEFAULT_FOLDERS="system.setting.app.meeting.default.folders";

    /**
     * Constant <code>SETTING_APP_MEETING_DEFAULT_FOLDERS_DEFAULT=""</code>
     */
    public static final String SETTING_APP_MEETING_DEFAULT_FOLDERS_DEFAULT="";

    /**
     * Constant <code>SETTING_BACKUP_WORKFLOW_SETTINGS="system.settings.backup.workflow.settings"</code>
     */
    public static final String SETTING_BACKUP_WORKFLOW_SETTINGS="system.settings.backup.workflow.settings";

    //** The date of the latest change to the NRMM database
    /**
     * Constant <code>lastUpdate</code>
     */
    public static Date lastUpdate;

    // The datasources used by the application
    private static Map<String, AppDataSource> dsMap = new HashMap<>();

    static {
        sessionFactory = null;
    }

    /**
     * Prevent instantiation of this static class
     */
    private HibernateUtils() {
    }

    /**
     * Returns true if Hibernate has been initialised
     *
     * @return True/False
     */
    public static boolean isInitialised() {
        return sessionFactory != null;
    }

    /**
     * This method is called to initialise the hibernate infrastructure
     * It can be called multiple times to re-init but this should be done
     * with extreme care.  If hibernate is re-homed to a different database
     * whilst NRMM is running tasks, then there is no doubt that the tasks
     * will fail and may even cause the NRMM settings to become unusable
     *
     * @param servletContextEvent Context event
     * @param config              Optional config file to use otherwise the default
     *                            hibernate.cfg.xml.hibernate.cfg.xml is used
     */
    public static void initHibernate(ServletContextEvent servletContextEvent, Document config) {

        if (!isInitialised()) {
            // We have to sort out a problem when running on OSX that causes
            // EHCache to throw a nasty thread dump

            if (ManagementFactory.getOperatingSystemMXBean().getName().matches("(?i)mac +os *x")) {
                System.setProperty("net.sf.ehcache.pool.sizeof.AgentSizeOf.bypass", "true");
            }

            try {
                Configuration configuration = new Configuration();
                if (config == null) {
                    // If no config file is passed in then look for instances of the default file in the classpath
                    ClassLoader ldr = Thread.currentThread().getContextClassLoader();
                    Enumeration<URL> en = ldr.getResources("hibernate.cfg.xml");
                    // Add each file found into the configuration
                    while (en.hasMoreElements()) {
                        URL thisOne = en.nextElement();
                        logger.info("Processing hibernate mapping file {}", thisOne.toString());
                        configuration.configure(thisOne);
                    }
                }
                else {
                    // Use the supplied config file
                    configuration.configure(config);
                }

                // Set the dialect from the default MySQL if necessary

                configuration.setProperty("hibernate.dialect", getDataSource().getDialect());

                // Build the factory

                sessionFactory = configuration.buildSessionFactory();
                logger.info("Initial SessionFactory created successfully");
            }
            catch (Throwable e) {

                // Make sure you log the exception, as it might be swallowed

                logger.error("Initial SessionFactory creation failed. This is almost certainly a mis-configuration in the context.xml - please check the username/password/URL");
                logger.debug("Initial SessionFactory creation failed - {}", e);
                throw new PivotalException(e);
            }
        }
    }

    /**
     * Returns the statically created session factory
     *
     * @return Session factory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Returns a session from the session context. If there is no session
     * in the context it opens a session, stores it in the context and returns it.
     *
     * This would return the current open session or if this does not exist,
     * will create a new session
     *
     * @return the session
     */
    synchronized public static Session getCurrentSession() {
        Session session = threadSession.get();
        if (session==null) {
            if (sessionFactory != null) {
                session = sessionFactory.openSession();
                try {
                    session.beginTransaction();
                    threadSession.set(session);

                    try {
                        AppDataSource dsInfo = getDataSource();
                        if (dsInfo.isPostgreSQL())
                            session.createSQLQuery("set search_path=" + dsInfo.getSchema() + ",public").executeUpdate();
                    }
                    catch (Exception e) {
                        logger.info("Unable to set search path as there was an error getting datasource " +PivotalException.getErrorMessage(e));
                    }
                }
                catch (HibernateException ex) {
                    logger.debug("Problem beginning session {}", ex);
                    session.close();
                    throw new ExceptionInInitializerError(ex);
                }
            }
            else {
                logger.debug("Session factory is shutdown");
            }
        }

        // Make sure we always have a transaction

        else if (!session.getTransaction().isActive()) {
            try {
                session.beginTransaction();
            }
            catch (HibernateException ex) {
                logger.debug("Problem beginning transaction {}", ex);
                session.close();
                throw new ExceptionInInitializerError(ex);
            }
        }
        return session;
    }

    /**
     * Closes the current session if it is open
     */
    synchronized public static void closeSession() {
        Session session = threadSession.get();
        if (session!=null) {

            // Clean out all the session objects and make sure the database is up to date

            try {
                try {
                    if (session.isOpen() && session.getTransaction().isActive()) {
                        session.getTransaction().rollback();
                        session.clear();
                    }
                }
                catch (Exception e) {
                    logger.debug("Rolling back - flush has failed - {}", PivotalException.getErrorMessage(e));
                }
                session.close();
            }
            catch (Exception e) {
                logger.debug("Close session failed - {}", PivotalException.getErrorMessage(e));
            }
            threadSession.remove();
        }
    }

    /**
     * Closes the session factory
     * This is a "hard" stop and any open session hanging around
     * are going to find that their underlying JDBC connection is unreliable
     * Use with caution!!
     */
    public static void close() {
        closeSession();
        if (sessionFactory != null) {
            try {
                sessionFactory.close();
                sessionFactory = null;
            }
            catch (Exception e) {
                logger.error("Problem closing session factory - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query Query to execute
     * @return The query object
     */
    public static Query createQuery(StringBuilder query) {
        return getCurrentSession().createQuery(query.toString());
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query Query to execute
     * @return The query object
     */
    public static Query createQuery(String query) {
        return getCurrentSession().createQuery(query);
    }

    /**
     * Safe way of creating SQL queries without worrying about transactions
     *
     * @param query Query to execute
     * @return The query object
     */
    public static Query createSQLQuery(String query) {
        SQLQuery result = getCurrentSession().createSQLQuery(query);

        return result;
    }

    /**
     * Runs the query and returns the first entry in the list or null
     * if the query didn't find anything
     *
     * @param query HQL Query to execute
     * @param <E>   a E object.
     * @return List of entities
     */
    public static <E> E selectFirstEntity(String query) {
        return selectFirstEntity(query, (Object[]) null);
    }

    /**
     * Runs the paramaterised query and returns the first entry in the list or null
     * if the query didn't find anything
     *
     * @param query  HQL Query to execute
     * @param values Map of positional parameters
     * @param <E>    a E object.
     * @return List of entities
     */
    public static <E> E selectFirstEntity(String query, Object... values) {
        E returnValue = null;
        List<E> tmp = selectEntities(query, false, 1, values);
        if (!isBlank(tmp)) {
            returnValue = tmp.get(0);
        }
        return returnValue;
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with named parameters,
     * and provides a quick way of filling in the parameters without creating a Map,
     * by taking pairs of the parameters
     *
     * @param query  HQL Query to execute
     * @param values Named parameters - the first value is the name of the first parameter, followed by it's value, and so on. The parameter names must be strings. E.g: {"parameter1", value1, "parameter2", value2}
     * @param <E>    a E object.
     *
     * @return List of entities
     */
    public static <E> List<E> selectEntitiesNamedParameters(String query, Object... values) {

        Map<String, Object> parameters = new HashMap<>();

        for (int i = 0; i < values.length; i += 2) {
            parameters.put((String) values[i], values[i + 1]);
        }

        return selectEntities(query, parameters);

    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query HQL Query to execute
     * @param <E>   a E object.
     * @return List of entities
     */
    public static <E> List<E> selectEntities(String query) {
        return selectEntities(query, false);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with named parameters
     *
     * @param query  HQL Query to execute
     * @param values Map of named parameters
     * @param <E>    a E object.
     * @return List of entities
     */
    public static <E> List<E> selectEntities(String query, Map<String, Object> values) {
        return selectEntities(query, false, values);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with numbered parameters
     *
     * @param query  HQL Query to execute
     * @param values Map of positional parameters
     * @param <E>    a E object.
     * @return List of entities
     */
    public static <E> List<E> selectEntities(String query, Object... values) {
        return selectEntities(query, false, values);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with numbered parameters
     *
     * @param query  HQL Query to execute
     * @param values Map of positional parameters
     * @param <E>    a E object.
     * @return List of entities
     */
    public static <E> List<E> selectEntitiesBypassCache(String query, Object... values) {
        return selectEntities(query, false, values);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query   HQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param <E>     a E object.
     * @return List of entities
     */
    public static <E> List<E> selectEntities(String query, boolean noCache) {
        return selectEntities(query, noCache, (Map<String, Object>) null);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with named parameters
     *
     * @param query   HQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param values  Map of named parameters
     * @param <E>     a E object.
     * @return List of entities
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> selectEntities(String query, boolean noCache, Map<String, Object> values) {
        return selectEntities(query, noCache, values, 0);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with named parameters
     *
     * @param query   HQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param values  Map of named parameters
     * @param maxResults    Maximum number of results to return
     * @param <E>    a E object.
     * @return List of entities
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> selectEntities(String query, boolean noCache, Map<String, Object> values, int maxResults) {
        List<E> returnValue = new ArrayList<>();
        if (getCurrentSession() == null) {
            logger.warn("Cannot select values for Hibernate query [{}] - no session available", query);
        }
        else {
            try {
                Query queryObj = getCurrentSession().createQuery(query);
                queryObj.setCacheable(!noCache);
                if (maxResults>0) {
                    queryObj.setMaxResults(maxResults);
                }
                if (!isBlank(values)) {
                    for (Map.Entry entry : values.entrySet()) {
                        if (Collection.class.isAssignableFrom(entry.getValue().getClass()))
                            queryObj.setParameterList((String) entry.getKey(), (Collection) entry.getValue());
                        else
                            queryObj.setParameter((String) entry.getKey(), entry.getValue());
                    }
                }
                returnValue = (List<E>) queryObj.list();
            }
            catch (Exception e) {
                logger.error("Cannot select values for Hibernate query [{}] - {}", query, PivotalException.getErrorMessage(e));
            }
        }
        return returnValue;
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with numbered parameters
     *
     * @param query   HQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param values  List of positional parameters
     * @param <E>     a E object.
     * @return List of entities
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> selectEntities(String query, boolean noCache, Object... values) {
        return selectEntities(query, noCache, 0, values);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     * This method expects to receive a parameterised query with numbered parameters
     *
     * @param query   HQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param maxResults Maximum number of rows to return
     * @param values  List of positional parameters
     * @param <E>    a E object.
     * @return List of entities
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> selectEntities(String query, boolean noCache, int maxResults, Object... values) {
        List<E> returnValue = new ArrayList<>();
        if (getCurrentSession() == null) {
            logger.warn("Cannot select values for Hibernate query [{}] - no session available", query);
        }
        else {
            try {
                Query queryObj = getCurrentSession().createQuery(query);
                queryObj.setCacheable(!noCache);
                if (maxResults>0) {
                    queryObj.setMaxResults(maxResults);
                }
                if (!isBlank(values)) {
                    for (int i = 0; i < values.length; i++) {
                        queryObj.setParameter(i, values[i]);
                    }
                }
                returnValue = queryObj.list();
            }
            catch (Exception e) {
                logger.warn("Cannot select values for Hibernate query [{}] - {}", query, PivotalException.getErrorMessage(e));
            }
        }
        return returnValue;
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query SQL Query to execute
     * @param <E>   a E object.
     * @return The query object
     */
    public static <E> List<E> selectSQLEntities(String query) {
        return selectSQLEntities(query, true);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query  SQL Query to execute
     * @param values List of positional parameters
     * @param <E>    a E object.
     * @return The query object
     */
    public static <E> List<E> selectSQLEntities(String query, Object... values) {
        return selectSQLEntities(query, true, values);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query   SQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param <E>     a E object.
     * @return The query object
     */
    public static <E> List<E> selectSQLEntities(String query, boolean noCache) {
        return selectSQLEntities(query, noCache, (Object[]) null);
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param query   SQL Query to execute
     * @param noCache If true, the query results is not cached by Hibernate
     * @param values  List of positional parameters
     * @param <E>     a E object.
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> selectSQLEntities(String query, boolean noCache, Object... values) {
        List<E> returnValue = new ArrayList<>();
        try {
            Query queryObj = getCurrentSession().createSQLQuery(query);
            queryObj.setCacheable(!noCache);
            if (!isBlank(values)) {
                for (int i = 0; i < values.length; i++) {
                    queryObj.setParameter(i, values[i]);
                }
            }
            returnValue = queryObj.list();
        }
        catch (Exception e) {
            logger.warn("Cannot enumerate values for SQL query - {} - {}", query, PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Creates a detached entity given the specified name
     *
     * @param entityName Name of the entity to instantiate
     * @param <T>        a T object.
     * @return The entity object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEntity(String entityName) {
        T entity = null;
        try {
            Class<?> entityClass = null;
            for (Class pubClass : ClassUtils.getClasses(ChangeLogEntity.class.getPackage().getName())) {
                if (Common.doStringsMatch(pubClass.getSimpleName(), entityName)) {
                    entityClass = pubClass;
                    break;
                }
            }

            // If we've got a suitable class to use then construct it

            entity = (T) getEntity(entityClass);
        }
        catch (Exception e) {
            logger.error("Cannot instantiate entity class for [{}] - {}", entityName, PivotalException.getErrorMessage(e));
        }
        return entity;
    }

    /**
     * Creates a detached entity given the specified class
     *
     * @param entityClass Class of the entity to instantiate
     * @param <T>         a T object.
     * @return The entity object
     */
    public static <T> T getEntity(Class<T> entityClass) {
        T entity = null;
        if (entityClass != null) {
            try {
                // If we've got a suitable class to use then construct it

                Class[] paramTypes = {};
                Object[] paramValues = {};
                Constructor<T> objCon = entityClass.getConstructor(paramTypes);
                entity = objCon.newInstance(paramValues);
            }
            catch (Exception e) {
                logger.error("Cannot instantiate entity class for [{}] - {}", entityClass, PivotalException.getErrorMessage(e));
            }
        }
        return entity;
    }

    /**
     * Gets the hibernate entity for the given entity name and id
     *
     * @param entityName Name of the entity to get
     * @param Id         Unique ID of the row
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static Object getEntity(String entityName, Integer Id) {
        if (sessionFactory==null) {
            return null;
        }
        else {
            Class entityClass = null;
            Map x = getSessionFactory().getAllClassMetadata();
            for (Iterator i = x.values().iterator(); i.hasNext() && entityClass == null; ) {
                SingleTableEntityPersister y = (SingleTableEntityPersister) i.next();
                if (Common.doStringsMatch(entityName, y.getEntityType().getReturnedClass().getName()) ||
                    Common.doStringsMatch(entityName, y.getEntityType().getReturnedClass().getSimpleName())) {
                    entityClass = y.getEntityType().getReturnedClass();
                }
            }
            if (entityClass != null) {
                return getEntity(entityClass, Id);
            }
            else {
                return null;
            }
        }
    }

    /**
     * Gets the hibernate entity for the given table and primary key
     *
     * @param tableName Name of the entity to get
     * @param Id        Unique ID of the row
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static Object getEntityByTable(String tableName, Integer Id) {
        if (sessionFactory==null)
            return null;
        else
            return getEntity(getEntityClassByTable(tableName), Id);
    }

    /**
     * Gets the hibernate entity for the given table
     *
     * @param tableName Name of the entity to get
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static Class getEntityClassByTable(String tableName) {
        if (sessionFactory==null) {
            return null;
        }
        else {
            Class entityClass = null;
            Map x = sessionFactory.getAllClassMetadata();
            for (Iterator i = x.values().iterator(); i.hasNext() && entityClass == null; ) {
                SingleTableEntityPersister y = (SingleTableEntityPersister) i.next();
                if (Common.doStringsMatch(tableName, y.getTableName())) {
                    entityClass = y.getEntityType().getReturnedClass();
                }
            }
            return entityClass;
        }
    }

    /**
     * Gets the hibernate entity for the given table and primary key
     *
     * @param entity Class of the entity to get
     * @param Id     Unique ID of the row
     * @param <T>    a T object.
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEntity(Class<T> entity, Integer Id) {
        if(Id != null && getCurrentSession() != null)
            return (T) getCurrentSession().get(entity.getName(), Id);
        return null;
    }

    /**
     * Gets the hibernate entity for the given table and primary key
     *
     * @param entity Class of the entity to get
     * @param name   Unique name of the row
     * @param <T>    a T object.
     * @return The query object
     */
    @SuppressWarnings("unchecked")
    public static <T> T getEntity(Class<T> entity, String name) {
        if (entity != null)
            return (T) getEntity(entity.getName(), name);
        else
            return null;
    }

    /**
     * Creates or updates an entity using the values passed in the
     * map as setter names
     *
     * @param clazz  Class of the entity to get
     * @param values Map of values to apply to the setters
     * @param <T>    a T object.
     * @return The newly created entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T createEntity(Class<?> clazz, Map<String, Object> values) {
        if (clazz != null) {
            T entity = null;
            if (!isBlank(values)) {
                String name = (String) values.get("name");
                Object type = values.get("type");
                if (type != null && type instanceof String)
                    type = getLookupIdValue(type);

                if (type != null && type instanceof Integer && (Integer) type <= Short.MAX_VALUE && (Integer) type >= Short.MIN_VALUE)
                    type = ((Integer) type).shortValue();

                entity = (T) HibernateUtils.getEntityOrNew(clazz, name, type);
                for (String key : values.keySet()) {
                    logger.debug("Setting {} of {}", key, entity.getClass().getSimpleName());
                    Object value = values.get(key);

                    // Look out for the getFile method

                    if (value instanceof String && ((String) value).matches("(?is) *getFile *\\(.+\\) *")) {
                        String filename = Common.getItem(((String) value), "[()]", 1);
                        File file = new File(ServletHelper.getRealPath(filename.replaceAll("['\"]", "")));
                        if (file.exists())
                            value = Common.readTextFile(file);
                        else
                            logger.error("File doesn't exist [{}]", file);
                    }

                    // Look out for the lookup entity ID method

                    else if (value instanceof String && ((String) value).matches("(?is) *lookupId *\\([a-z]+[ ,]+.+\\) *")) {
                        value = getLookupIdValue(value);
                    }

                    // Look out for the lookup entity method

                    else if (value instanceof String && ((String) value).matches("(?is) *lookup *\\([a-z]+[ ,]+.+\\) *")) {
                        value = getLookupIdEntity(value);
                    }
                    setPropertyValue(entity, key, value);
                }
                if (!HibernateUtils.save(entity))
                    entity = null;
            }
            return entity;
        }
        else
            return null;
    }

    /**
     * Restores an entity using the map as setter names
     * Does not save it to the database
     *
     * @param clazz  Class of the entity to get
     * @param values Map of values to apply to the setters
     * @param <T>    a T object.
     * @return The newly created entity
     */
    @SuppressWarnings("unchecked")
    public static <T> T restoreEntity(Class<?> clazz, Map<String, List> values) {

        T entity = null;
        if (clazz != null) {
            Integer entityId = null;
            if (values.containsKey("id")) {

                try {
                    if (!isBlank(String.valueOf(values.get("id").get(0))))
                        entityId = Common.parseInt(String.valueOf(values.get("id").get(0)));
                }
                catch(Exception e) {
                    logger.debug("Unable to get entity " + PivotalException.getErrorMessage(e));
                    entityId = null;
                }
            }

            if (entityId == null)
                entity = (T) HibernateUtils.getEntity(clazz);
            else
                entity = (T) HibernateUtils.getEntity(clazz, entityId);
            if (!isBlank(values)) {
                String keyToUse;
                List valueToUse = null;
                for (String key : values.keySet()) {
                    if (key.endsWith("_input")) {

                        // Work out which one to get
                        // If form control is id/value drop down type then use the field
                        // Otherwise use the _input field
                        keyToUse = key.substring(0, key.length() - 6);
                        if (values.containsKey(keyToUse)) {
                            valueToUse = values.get(keyToUse);
                            if (values.size() == 0 || isBlank(valueToUse.get(0)))
                                valueToUse = values.get(key);
                        }
                    }
                    else if (values.containsKey(key + "_input")) {
                        keyToUse = null;
                    }
                    else {
                        keyToUse = key;
                        valueToUse = values.get(key);
                    }

                    if (!isBlank(keyToUse))
                        setPropertyValueByType(entity, keyToUse, valueToUse);
                }
            }
        }

        return entity;
    }

    /**
     * Exercises the setter method to set the bean value
     *
     * @param entity    Entity to update
     * @param name      Method name (nested is allowed)
     * @param valueList Value to apply to the property
     *
     */
    public static void setPropertyValueByType(Object entity, String name, List<Object> valueList) {

        try {
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(entity);

            Class propertyType = beanWrapper.getPropertyType(name);
            if (propertyType != null) {

                boolean useSet = false;
                if (propertyType == Set.class) {
                    useSet = true;
                    propertyType = beanWrapper.getPropertyTypeDescriptor(name).getElementTypeDescriptor().getType();
                }
                Set<Object> newValues = new LinkedHashSet<>();
                Object newValue = null;

                for(Object value : valueList) {

                    if (!isBlank(value) && !((String)value).isEmpty()) {
                        newValue = null;
                        // Check type

                        if (value.getClass() == propertyType) {
                            newValue = value;
                        }
                        else if (propertyType == Boolean.class || propertyType == boolean.class) {
                            // Look for boolean, it comes back from the form with a visible tag and a true/false tag
                            // so we just capture the true/false one which may not have been the first one in the array
                            if (!value.equals("visible"))
                                newValue = Common.isYes(value);
                        }
                        else {
                            logger.debug("Class differences [beanType = {}] [valueType = {}] [name = {}] [value = {}]", beanWrapper.getPropertyType(name).toString(), value.getClass().toString(), name, value.toString());
                            try {
                                logger.debug("Trying integer key");
                                newValue = HibernateUtils.getEntity(propertyType, Common.parseInt((String) value));
                            }
                            catch (Exception e1) {
                                logger.debug("Integer value assignment failed");
                            }

                            if (newValue == null) {
                                logger.debug("Trying string key");
                                try {
                                    newValue = HibernateUtils.getEntity(propertyType, (String) value);
                                }
                                catch (Exception e2) {
                                    logger.debug("String Value assignment failed");
                                }
                            }

                            if (newValue == null) {
                                try {
                                    logger.debug("Trying to force it");
                                    newValue = value;
                                }
                                catch (Exception e3) {
                                    logger.debug("Unable to assign value");
                                }
                            }
                        }

                        if (!isBlank(newValue))
                            newValues.add(newValue);
                    }
                }

                // See if we are storing a set of data

                if (useSet && !isBlank(newValues))
                    beanWrapper.setPropertyValue(new PropertyValue(name, newValues));

                else if (propertyType == boolean.class) {
                    newValue = false;
                    if (newValues.size() > 0 && !isBlank(newValues.toArray()[0]))
                        newValue = Common.isYes(newValues.toArray()[0]);

                    beanWrapper.setPropertyValue(new PropertyValue(name, newValue));
                }
                // Get first found value

                else if (newValues.size() > 0 && !isBlank(newValues.toArray()[0]))
                    beanWrapper.setPropertyValue(new PropertyValue(name, newValues.toArray()[0]));
            }
            else {
                logger.debug("not a field " + name);
            }

        }
        catch (Exception e) {
            logger.debug("Field [{}] has produced a null value", name);
        }
    }

    /**
     * Looks up the id for the lookupId(&lt;class&gt;, name)
     * method used by Archetypes
     *
     * @param value lookup declaration
     *
     * @return if ok then id else value
     */
    public static Object getLookupIdValue(Object value) {

        Object lookupEntity = getLookupIdEntity(value);

        Object retValue = value;
        if (lookupEntity != null && !(lookupEntity instanceof String))
            retValue = ClassUtils.getPropertyValue(lookupEntity, "id");

        return retValue;
    }

    /**
     * Looks up the id for the lookupId(&lt;class&gt;, name)
     * method used by Archetypes
     *
     * @param value lookup declaration
     *
     * @return if ok then id else value
     */
    public static Object getLookupIdEntity(Object value) {

        Object retValue = value;
        if (retValue instanceof String && (((String) retValue).matches("(?is) *lookup *\\([a-z]+[ ,]+.+\\) *") || ((String) retValue).matches("(?is) *lookupId *\\([a-z]+[ ,]+.+\\) *"))) {
            String itemName = Common.getItem(((String) retValue), "[()]", 1);
            String className = Common.getItem(itemName, " *,", 0);
            String typeName = Common.getItem(itemName, " *, *", 2);
            itemName = Common.getItem(itemName, " *, *", 1).replaceAll("['\"]", "");
            if (!isBlank(typeName))
                typeName = typeName.replaceAll("['\"]", "");
            retValue = HibernateUtils.getEntity(className, itemName, typeName);

            // If we didn't find the entity, it may be because it's named a little
            // differently or the name we have been sent is a longer identifier

            if (retValue == null && itemName.contains(".name"))
                retValue = HibernateUtils.getEntity(className, itemName.replaceAll("\\.name$", "").replaceAll("^.+\\.", ""), typeName);
        }

        return retValue;
    }

    /**
     * Gets id from passed entity and name value
     * @param entityName    Entity to get data from
     * @param value         Name value to lookup
     *
     * @return              Id of record
     */
    public static Integer getLookupId(String entityName, String value) {

        Integer returnValue = null;

        if (!isBlank(entityName) && !isBlank(value)) {

            try {
                Object entity = getEntity(entityName, value);
                returnValue = ClassUtils.invokeMethod(entity, "getId");
            }
            catch(Exception e) {
                logger.error("Unable to get id for {}, {} - {}", entityName, value, PivotalException.getErrorMessage(e));
            }
        }

        return returnValue;
    }

    /**
     * Gets the hibernate entity for the given entity, name and optionally type
     * If it doesn't exist, it creates a new one
     *
     * @param entity Class of the entity to get
     * @param name   Name of the row
     * @param type   Type value of the row
     * @param <T>    a T object.
     * @return The query object
     */
    public static <T> T getEntityOrNew(Class<T> entity, String name, Object type) {
        T returnValue = null;
        if (entity != null) {
            returnValue = getEntity(entity.getName(), name, type);
            if (returnValue == null) returnValue = getEntity(entity);
        }
        return returnValue;
    }

    /**
     * Safe way of creating queries without worrying about transactions
     *
     * @param entityName Name of the entity to get
     * @param name       Unique name of the row
     * @param <T>        a T object.
     * @return The query object
     */
    public static <T> T getEntity(String entityName, String name) {
        return getEntity(entityName, name, null);
    }

    /**
     * Returns the first entity where the column "name" matches the name
     * If type is supplied, this is expected to match a column called "type"
     *
     * @param entityName Name of the entity to get
     * @param name       Name of the row
     * @param type       Type value of the row
     * @param <T>        a T object.
     * @return The query object
     */
    public static <T> T getEntity(String entityName, String name, Object type) {
        if (!isBlank(name)) {
            List<T> entityList;
            if (isBlank(type))
                entityList = selectEntities("from " + entityName + " where lower(name)=?", name.toLowerCase());
            else
                entityList = selectEntities("from " + entityName + " where lower(name)=? and type=?", name.toLowerCase(), type);
            if (!isBlank(entityList))
                return entityList.get(0);
            else
                return null;
        }
        else
            return null;
    }

    /**
     * Parses the content replacing specific system variables with their
     * runtime values using Velocity
     *
     * @param content String to parse
     * @return parse content
     */
    public static String parseSystemVariables(String content) {
        return parseSystemVariables(content, null);
    }

    /**
     * Parses the content replacing specific system variables with their
     * runtime values using Velocity
     *
     * @param content String to parse
     * @param task    Scheduled task associated with this
     * @return parse content
     */
    public static String parseSystemVariables(String content, ScheduledTaskEntity task) {
        return parseSystemVariables(content, task, null);
    }

    /**
     * Parses the content replacing specific system variables with their
     * runtime values using Velocity
     *
     * @param content               String to parse
     * @param task                  Scheduled task associated with this
     * @param extraContextVariables Map of additional context values
     * @return parse content
     */
    public static String parseSystemVariables(String content, ScheduledTaskEntity task, Map<String, Object> extraContextVariables) {
        if (!isBlank(content)) {

            // Parse out all the system variables

            Writer output = new StringWriter();
            VelocityEngine engine;
            try {
                engine = VelocityUtils.getEngine();

                // Now add the useful stuff to the context

                logger.debug("Creating velocity context");
                Context context = VelocityUtils.getVelocityContext();
                context.put("Task", task);

                // Add on any extras we might have

                if (!isBlank(extraContextVariables)) {
                    for (Map.Entry entry : extraContextVariables.entrySet())
                        context.put((String) entry.getKey(), entry.getValue());
                }

                // Carry out the transformation using the script in the database

                engine.evaluate(context, output, HibernateUtils.class.getSimpleName(), content);
                logger.debug("Parsed string for system variables\n    {}\n    {}", content, output.toString());
                content = output.toString();
            }
            catch (Exception e) {
                logger.error("Problem parsing system variables\n    {} - ", content, PivotalException.getErrorMessage(e));
            }
        }
        return content;
    }

    /**
     * Parses the template replacing specific system variables with their
     * runtime values using Velocity
     *
     * @param template              Template to process
     * @param extraContextVariables Map of additional context values
     * @return parse content
     */
    public static String parseTemplate(Template template, Map<String, Object> extraContextVariables) {

        String content = null;
        if (!isBlank(template)) {

            // Parse out all the system variables

            VelocityEngine engine;
            try {
                engine = VelocityUtils.getEngine();

                // Now add the useful stuff to the context

                logger.debug("Creating velocity context");
                Context context = VelocityUtils.getVelocityContext();

                // Add on any extras we might have

                if (!isBlank(extraContextVariables)) {
                    for (Map.Entry entry : extraContextVariables.entrySet())
                        context.put((String) entry.getKey(), entry.getValue());
                }

                context.put("Context", context);
                context.put("context", context);
                context.put("Engine", engine);
                context.put("engine", engine);

                // Carry out the transformation
                // Execute the menu generator

                Writer templateOutput = new StringWriter();
                template.merge(context, templateOutput);

                content = templateOutput.toString();
                logger.debug("Parsed template\n    {}\n    {}", content);
            }
            catch (Exception e) {
                logger.error("Problem parsing template \n{}", PivotalException.getErrorMessage(e));
            }
        }
        return content;
    }

    /**
     * Parses the content replacing specific system variables with their
     * runtime values using Velocity
     *
     * @param content               String to parse
     * @param extraContextVariables Map of additional context values
     * @return parse content
     */
    public static String parseTemplate(String content, Map<String, Object> extraContextVariables) {
        if (!isBlank(content)) {

            // Parse out all the system variables

            Writer output = new StringWriter();
            VelocityEngine engine;
            try {
                engine = VelocityUtils.getEngine();

                // Now add the useful stuff to the context

                logger.debug("Creating velocity context");
                Context context = VelocityUtils.getVelocityContext();

                // Add on any extras we might have

                if (!isBlank(extraContextVariables)) {
                    for (Map.Entry entry : extraContextVariables.entrySet())
                        context.put((String) entry.getKey(), entry.getValue());
                }

                context.put("Context", context);
                context.put("context", context);
                context.put("Engine", engine);
                context.put("engine", engine);

                // Carry out the transformation using the script in the database

                engine.evaluate(context, output, HibernateUtils.class.getSimpleName(), content);
                logger.debug("Parsed template\n    {}\n    {}", content, output.toString());
                content = output.toString();
            }
            catch (Exception e) {
                logger.error("Problem parsing template \n    {} - {}", content, PivotalException.getErrorMessage(e));
            }
        }
        return content;
    }

    /**
     * Executes a query on the underlying database directly
     *
     * @param SQL Action query to execute
     * @return True if it worked without error
     */
    public static boolean executeSQL(String SQL) {
        Database tmp = new DatabaseHibernate();
        tmp.execute(SQL);
        tmp.close();
        return !tmp.isInError();
    }

    /**
     * Parses the content replacing specific recipient markers with values from
     * the passed recipient column map
     *
     * @param content   String to parse
     * @param recipient Recipient to get values from
     * @return parse content
     */
    public static String parseRecipientVariables(String content, Recipient recipient) {
        if (!isBlank(content) && recipient != null && !isBlank(recipient.getValues())) {

            // Parse out all the system variables

            Writer output = new StringWriter();
            VelocityEngine engine;
            try {
                engine = VelocityUtils.getEngine();

                // Now add the useful stuff to the context

                logger.debug("Creating velocity context");
                Context context = VelocityUtils.getVelocityContext();

                // Now add the recipient information and the column values
                // from the recipient select

                context.put("Recipient", recipient);
                context.put("ColumnList", new ArrayList<>(recipient.getValues().values()));
                Map<String, String> tmp = recipient.getValues();
                context.put("ColumnValues", new CaseInsensitiveMap(tmp));
                for (Map.Entry entry : tmp.entrySet()) {
                    context.put((String) entry.getKey(), entry.getValue());
                    context.put(((String) entry.getKey()).toLowerCase(), entry.getValue());
                }

                // Carry out the transformation using the script in the database

                engine.evaluate(context, output, HibernateUtils.class.getSimpleName(), content);
                logger.debug("Parsed string for recipient variables\n    {}\n    {}", content, output.toString());
                content = output.toString();
            }
            catch (Exception e) {
                logger.error("Problem parsing recipient variables\n    {} - {}", content, PivotalException.getErrorMessage(e));
            }
        }
        return content;
    }

    /**
     * Parses the content replacing specific system and recipient markers
     * with values from the passed recipient column map
     *
     * @param content   String to parse
     * @param task      Scheduled task asscociated with this
     * @param recipient Recipient to get values from
     * @return parse content
     */
    public static String parseVariables(String content, ScheduledTaskEntity task, Recipient recipient) {
        return parseSystemVariables(parseRecipientVariables(content, recipient), task);
    }

    /**
     * Parses the content replacing specific system and recipient markers
     * with values from the passed recipient column map
     *
     * @param content               String to parse
     * @param task                  Scheduled task asscociated with this
     * @param recipient             Recipient to get values from
     * @param extraContextVariables Map of additional context values
     * @return parse content
     */
    public static String parseVariables(String content, ScheduledTaskEntity task, Recipient recipient, Map<String, Object> extraContextVariables) {
        return parseSystemVariables(parseRecipientVariables(content, recipient), task, extraContextVariables);
    }

    /**
     * This method will returns a value from the system settings table
     * for the specified name.  If the value doesn't exist, then it will
     * return the default value
     *
     * @param name         Name of the setting
     * @param defaultValue Value to use if the setting doesn't exist
     * @return Int value
     */
    public static Integer getSystemSetting(String name, int defaultValue) {
        SettingsEntity setting = getSystemSettingEntity(name);
        if (setting != null)
            return setting.getValueNumeric();
        else
            return defaultValue;
    }

    /**
     * This method will returns a value from the system settings table
     * for the specified name.  If the value doesn't exist, then it will
     * return the default value
     *
     * @param name         Name of the setting
     * @param defaultValue Value to use if the setting doesn't exist
     * @return Int value
     */
    public static boolean getSystemSetting(String name, boolean defaultValue) {
        SettingsEntity setting = getSystemSettingEntity(name);
        if (setting != null)
            return Common.isYes(setting.getValueNumeric());
        else
            return defaultValue;
    }

    /**
     * This method will returns a value from the system settings table
     * for the specified name.  If the value doesn't exist, then it will
     * return the default value
     *
     * @param name         Name of the setting
     * @param defaultValue Value to use if the setting doesn't exist
     * @return String value
     */
    public static String getSystemSetting(String name, String defaultValue) {
        SettingsEntity setting = getSystemSettingEntity(name);
        if (setting != null && !isBlank(setting.getValue()))
            return setting.getValue();
        else
            return defaultValue;
    }

    /**
     * This method will returns a value from the system settings table
     * for the specified name.  If the value doesn't exist, then it will
     * return the default value
     *
     * @param name         Name of the setting
     * @param defaultValue Value to use if the setting doesn't exist
     * @return String value
     */
    public static String getSystemSettingText(String name, String defaultValue) {
        SettingsEntity setting = getSystemSettingEntity(name);
        if (setting != null && !isBlank(setting.getValueText()))
            return setting.getValueText();
        else
            return defaultValue;
    }

    /**
     * This method will returns a setting object for the given name
     *
     * @param name Name of the setting
     * @return SettingsEntity
     */
    private static SettingsEntity getSystemSettingEntity(String name) {
        SettingsEntity setting = null;
        try {
            // Get the current thread session

            setting = (SettingsEntity) getCurrentSession().get(SettingsEntity.class, name);
        }
        catch (Exception e) {
            logger.error("Problem getting system value - {}", PivotalException.getErrorMessage(e));
        }
        return setting;
    }

    /**
     * This method will returns a setting object for the given name
     *
     * @param name Name of the setting
     * @return SettingsEntity
     */
    public static SettingsEntity getSystemSetting(String name) {
        SettingsEntity setting = getSystemSettingEntity(name);
        if (setting == null) {
            setting = new SettingsEntity();
            setting.setName(name);
        }
        return setting;
    }

    /**
     * Deletes an entity from the database
     *
     * @param entity Entity to delete
     */
    public static void delete(Object entity) {
        getCurrentSession().delete(entity);
    }

    /**
     * Deletes the list of entities from the database
     *
     * @param entities Entities to delete
     */
    public static void delete(List<Object> entities) {
        if (!isBlank(entities)) {
            for (Object entity : entities) {
                HibernateUtils.delete(entity);
            }
        }
    }

    /**
     * Evicts an entity from the session
     *
     * @param entity Entity to evict
     */
    public static void evict(Object entity) {
        try {
            getCurrentSession().evict(entity);
            getSessionFactory().getCache().evictEntityRegion(entity.getClass());
        }
        catch (Exception e) {
            logger.error("Problem evicting object - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Saves an entity to the database
     *
     * @param entity Entity to add
     * @return True if the save worked OK
     */
    public static boolean save(Object entity) {
        return save(null, entity);
    }

    /**
     * Saves an entity to the database
     * This method calls a commit so if a user wants to wrap a few saves in a transaction
     * they need to call the save() method on the session object themselves and
     * not use
     *
     * @param entity Entity to add
     * @param commit True if the save should commit the transaction
     * @return True if the save worked OK
     */
    public static boolean save(Object entity, boolean commit) {
        return save(null, entity, commit, null);
    }

    /**
     * Saves an entity to the database
     * This method calls a commit so if a user wants to wrap a few saves in a transaction
     * they need to call the save() method on the session object themselves or use
     * the
     *
     * @param model  Model to update with any errors
     * @param entity Entity to add
     * @return True if the save worked OK
     */
    public static boolean save(Model model, Object entity) {
        return save(model, entity, true);
    }

    /**
     * Saves an entity to the database
     * This method calls a commit so if a user wants to wrap a few saves in a transaction
     * they need to call the save() method on the session object themselves or use
     * the
     *
     * @param model  Model to update with any errors
     * @param entity Entity to add
     * @param result Binding result to save errors into
     * @return True if the save worked OK
     */
    public static boolean save(Model model, Object entity, BindingResult result) {
        return save(model, entity, true, result);
    }

    /**
     * Saves an entity to the database
     * This method calls a commit so if a user wants to wrap a few saves in a transaction
     * they need to call the save() method on the session object themselves and
     * not use
     *
     * @param model  Model to update with any errors
     * @param entity Entity to add
     * @param commit True if the save should commit the transaction
     * @return True if the save worked OK
     */
    public static boolean save(Model model, Object entity, boolean commit) {
        return save(model, entity, commit, null);
    }

    /**
     * Saves an entity to the database
     * This method calls a commit so if a user wants to wrap a few saves in a transaction
     * they need to call the save() method on the session object themselves and
     * not use
     *
     * @param model  Model to update with any errors
     * @param entity Entity to add
     * @param commit True if the save should commit the transaction
     * @param result Binding result to save errors into
     * @return True if the save worked OK
     */
    public static boolean save(Model model, Object entity, boolean commit, BindingResult result) {
        boolean returnValue = false;
        Session session = getCurrentSession();
        try {
            try {
                session.saveOrUpdate(entity);
            }

            // If the save fails because there is already a version of this entity in the session,
            // then merge them together and attempt the save again

            catch (NonUniqueObjectException e) {
                entity = session.merge(entity);
                session.saveOrUpdate(entity);
            }

            // Commit the transaction so that we can catch any problems and
            // display them to the user immediately

            if (commit && session.getTransaction().isActive()) {
                session.getTransaction().commit();
                session.beginTransaction();
                session.refresh(entity);
            }

            // If we have been passed a model then this must be a user initiated
            // update so make sure we force the cache to cough up a new object
            // next time it's called

            if (model != null) {
                evict(entity);
            }
            returnValue = true;
        }
        catch (javax.validation.ConstraintViolationException e) {
            List<String> errs = new ArrayList<>();
            for (ConstraintViolation violation : e.getConstraintViolations()) {
                errs.add(violation.getMessage() + " [" + violation.getRootBean().getClass().getName().replaceAll(".*\\.", "") + " - " + violation.getPropertyPath() + ']');
                if (result != null)
                    result.addError(new FieldError(violation.getRootBeanClass().getSimpleName(), violation.getPropertyPath().toString(), violation.getMessage()));
            }
            logger.error(ServletHelper.addError(model, Common.join(errs)));
            session.getTransaction().rollback();
        }
        catch (org.hibernate.exception.ConstraintViolationException e) {

            String error;
            if (result == null) {
                error = e.getLocalizedMessage() + '\n' + e.getCause().getMessage().replaceAll("^[^:]+:", "");

                if (error.contains("Call getNextException")) {
                    try {
                        String newError = e.getSQLException().getNextException().getMessage();
                        if (!isBlank(newError))
                            error = newError;
                    }
                    catch(Exception e1) {
                        logger.debug("Unable to get next error {}", error);
                    }
                }
                logger.error(ServletHelper.addError(model, error));

            }
            else {
                error = ServletHelper.simplifyErrorMessage(e);

                // Look out for some errors we can attribute to fields

                String path = null;
                if (error.contains("Duplicate entry")) {
                    path = Common.getItem(error, "'", 3);
                    if (path != null) {
                        path = path.replaceAll("^.+_", "");
                        error = Common.getItem(error, " for key", 0);
                    }
                }
                else if (error.contains("\"")) {
                    path = error.replaceAll("^[^\"]*\"","").replaceAll("\"[^/]*$", "");
                    if (path.endsWith("_id"))
                        path = path.replaceAll("_id$","");
                }

                logger.error(ServletHelper.addError(model, error));

                try {
                    result.rejectValue(path, ServletHelper.FORM_ERRORS, error);
                }
                catch(Exception e1) {
                    try {
                        result.rejectValue("", ServletHelper.FORM_ERRORS, error);
                    }
                    catch(Exception e2) {
                        logger.debug("rejectValue failed for field {}, {}", path, PivotalException.getErrorMessage(e2));
                    }
                }
            }
            session.getTransaction().rollback();
        }
        catch (Exception e) {
            logger.error(ServletHelper.addError(model, "Problem saving record %s - [%s]", PivotalException.getErrorMessage(e), entity));
            if (result != null)
                result.addError(new ObjectError(entity.getClass().getSimpleName().toLowerCase(), PivotalException.getErrorMessage(e)));
            session.getTransaction().rollback();
        }

        // Make sure we have a transaction, even if this is just continuing and existing one

        session.beginTransaction();
        return returnValue;
    }

    /**
     * Flushes any pending edits to the database
     */
    public static void flush() {
        try {
            getCurrentSession().flush();
        }
        catch (Exception e) {
            logger.error("Problem flushing changes to the database - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Executes the command
     *
     * @param model   Model to update with any errors
     * @param command Command to execute
     */
    public static void update(Model model, String command) {
        try {
            getCurrentSession().createQuery(command).executeUpdate();
        }
        catch (org.hibernate.exception.ConstraintViolationException e) {
            logger.error(ServletHelper.addError(model, e.getCause().getMessage().replaceAll("^[^:]+:", "")));
        }
        catch (Exception e) {
            logger.error(ServletHelper.addError(model, "Problem updating [%s] - %s", command, PivotalException.getErrorMessage(e)));
        }
    }

    /**
     * Update an entity
     *
     * @param model  Model to update with any errors
     * @param entity Entity update
     */
    public static void update(Model model, Object entity) {

        try {
            getCurrentSession().update(entity);
            commit();
        }
        catch (Exception e) {
            logger.error(ServletHelper.addError(model, "Problem updating [%s] - %s", entity, PivotalException.getErrorMessage(e)));
        }
    }

    /**
     * Serialises the current database values for the row that underpins the
     * hibernate entity into a human readable string that will also allow
     * robotic reloading
     *
     * @param entity Hibernate entity that supports getId()
     * @return Serialised values
     */
    public static String serializeEntity(Object entity) {

        String returnValue = null;

        try {
            if (entity != null) {
                // Get the ID
                Method method;
                try {
                    method = entity.getClass().getDeclaredMethod("getId", new Class[]{});
                }
                catch (NoSuchMethodException e) {
                    logger.debug("Trying super class getId() method");
                    method = entity.getClass().getSuperclass().getDeclaredMethod("getId", new Class[]{});
                }

                Object tmp = method.invoke(entity);

                // Turn the entity into a serialized string of values

                if (tmp != null) {
                    String table=null;
                    try {
                        table = entity.getClass().getAnnotation(javax.persistence.Table.class).name();
                    }
                    catch(Exception e) {
                        logger.warn("Error getting table name {}", PivotalException.getErrorMessage(e));
                    }
                    if (isBlank(table))
                        table = entity.getClass().getSimpleName().replaceAll("Entity", "").replaceAll("[A-Z]", "_$0").toLowerCase().replaceAll("^_", "").replaceAll("_\\$\\$_javassist_.*$","");

                    if (Common.doStringsMatch(table, "report_blob"))
                        table = "report";
                    else if (Common.doStringsMatch(table, "user"))
                        table = "users";

                    returnValue = serializeRow(tmp.toString(), table);
                }
                else
                    returnValue = null;
            }
            else {
                logger.debug("Received Null Entity");
            }
        }
        catch (NoSuchMethodException e) {
            logger.error("Entity does not have a getId() method");
        }
        catch (Exception e) {
            logger.error("Problem reading values from entity - {}", PivotalException.getErrorMessage(e));
        }
        return returnValue;
    }

    /**
     * Turns the specified row of values into a values suitable for storing
     * in the change_log
     *
     * @param Id    Id of the row
     * @param table Table to get the row from
     * @return String of values
     */
    public static String serializeRow(String Id, String table) {
        String returnValue = null;
        Database database = new DatabaseHibernate();
        List<Map<String, Object>> results = database.find("select * from " + table + " where id=" + Id);
        if (!isBlank(results)) {

            // Serialize each column value

            String blobColumn = null;
            Object blob = null;
            Map<String, Object> row = results.get(0);
            for (Map.Entry entry : row.entrySet()) {
                Object value = entry.getValue();

                // If the value isn't a BLOB then store it inline

                if (value instanceof byte[]) {
                    blobColumn = (String) entry.getKey();
                    blob = value;
                }
                else
                    returnValue = (returnValue == null ? "" : returnValue + '\n') + '@' + entry.getKey() + ": " + value;
            }

            // Save the BLOB as a base64 encode stream at the bottom

            if (blob != null) {
                returnValue += "\n@" + blobColumn + ": ";
                if (((byte[]) blob).length > 0) returnValue += Base64.encodeBytes((byte[]) blob);
            }
        }
        database.close();
        return returnValue;
    }

    /**
     * Turns the specified row of values into a values suitable for storing
     * in the change_log
     *
     * @param Id    Id of the row
     * @param table Table to get the row from
     * @return String of values
     */
    public static String serializeRowJSON(String Id, String table) {
        Database database = new DatabaseHibernate();
        List<Map<String, Object>> results = database.find("select * from " + table + " where id=" + Id);
        Map<String, Object> row=null;
        if (!isBlank(results)) {
            row = results.get(0);
        }
        database.close();

        return JsonMapper.serializeItem(row);
    }

    public static Map<String, Object> deserializeToMap(String serialisedValue) {

        Map<String, Object> valueMap = new LinkedHashMap<>();

        if (!Common.isBlank(serialisedValue)) {
            List<String> fields = Common.splitToList(serialisedValue, "(^@)|(\n@)");

            for (int i = 1; i < fields.size(); i++) {
                String[] parts = fields.get(i).split(": *", 2);
                String fieldName = parts[0];
                if (!fieldName.startsWith("[")) {
                    String fieldValue = null;
                    if (parts.length > 1) fieldValue = parts[1].replaceAll("'", "''");

                    // Check to see if we need to decode the data

//                    if (isReport && Common.doStringsMatch(fieldName, "file")) {
//                        try {
//                            fieldValue = new String(com.pivotal.utils.Base64.decode(fieldValue), "UTF-8");
//                        }
//                        catch (Exception e) {
//                            logger.error("Problem decoding change value - {}", PivotalException.getErrorMessage(e));
//                        }
//                    }

                    Object val;
                    //is it boolean
                    if (Common.doStringsMatch(fieldName, "disabled") || Common.doStringsMatch(fieldName, "internal")) {
                        Boolean tmp = Common.parseBoolean(fieldValue);
                        if (tmp) {
                            fieldValue = "1";
                        }
                        else {
                            fieldValue = "0";
                        }
                    }

                    valueMap.put(fieldName, fieldValue);
                }
            }
        }

        return valueMap;
    }

    /**
     * Adds a new entry in the database for the specified entity
     *
     * @param entity      Hibernate entity that supports getId()
     * @param changes     String of changes
     * @param addOrUpdate True if it is add, false if it is an update
     */
    public static void addChangeLog(Object entity, String changes, boolean addOrUpdate) {
        addChangeLog(entity, changes, addOrUpdate ? ChangeLogEntity.ChangeTypes.ADDED : ChangeLogEntity.ChangeTypes.EDITED);
    }

    /**
     * Adds a new entry in the database for the specified entity
     *
     * @param entity     Hibernate entity that supports getId()
     * @param changes    String of changes
     * @param changeType Type of change to record
     */
    public static void addChangeLog(Object entity, String changes, ChangeLogEntity.ChangeTypes changeType) {
        addChangeLog(entity, changes, changeType, null, null);
    }

    /**
     * Adds a new entry in the database for the specified entity
     *
     * @param entity     Hibernate entity that supports getId()
     * @param changes    String of changes
     * @param changeType Type of change to record
     * @param parentRow  Key to link actions on child records back to parent
     */
    public static void addChangeLog(Object entity, String changes, ChangeLogEntity.ChangeTypes changeType, String parentRow) {
        addChangeLog(entity, changes, changeType, null, parentRow);
    }

    /**
     * Adds a new entry in the database for the specified entity
     *
     * @param entity     Hibernate entity that supports getId()
     * @param changes    String of changes
     * @param changeType Type of change to record
     * @param model      Current model to store errors in
     */
    public static void addChangeLog(Object entity, String changes, ChangeLogEntity.ChangeTypes changeType, Model model) {
        addChangeLog(entity, changes, changeType, model, null);
    }

    /**
     * Adds a new entry in the database for the specified entity
     *
     * @param entity     Hibernate entity that supports getId()
     * @param changes    String of changes
     * @param changeType Type of change to record
     * @param model      Current model to store errors in
     * @param parentRow  Key to link actions on child records back to parent
     */
    public static void addChangeLog(Object entity, String changes, ChangeLogEntity.ChangeTypes changeType, Model model, String parentRow) {

        ChangeLogEntity change = new ChangeLogEntity();
        change.setChangeType(changeType.getDescription());
        change.setPreviousValues(changes);
        change.setUserFullName(UserManager.getCurrentUserName());
        change.setTimeAdded(new Timestamp(new Date().getTime()));

        // Get the ID

        try {
            if (entity instanceof String)
                change.setTableAffected((String) entity);
            else {
                Method method = null;

                try {
                    method = entity.getClass().getDeclaredMethod("getId", new Class[]{});
                }
                catch(NoSuchMethodException nsme) {
                    // Try from super class

                    method = entity.getClass().getSuperclass().getDeclaredMethod("getId", new Class[]{});
                }
                Object tmp = method.invoke(entity);
                String table=null;
                try {
                    table = entity.getClass().getAnnotation(javax.persistence.Table.class).name();
                }
                catch(Exception e) {
                    logger.debug("Error getting table name {}", PivotalException.getErrorMessage(e));
                }
                if (isBlank(table))
                    table = entity.getClass().getSimpleName().replaceAll("Entity", "").replaceAll("[A-Z]", "_$0").toLowerCase().replaceAll("^_", "").replaceAll("_\\$\\$_javassist_.*$","");

                if (Common.doStringsMatch(table, "report_blob")) table = "report";
                change.setRowAffected((Integer) tmp);
                if (isBlank(parentRow))
                    change.setParentRow(change.getTableAffected() + ":" + change.getRowAffected());
                else
                    change.setParentRow(parentRow);

                change.setTableAffected(table);
            }

            // Save the new record

            ChangeLogEntity.addLogEntry(model, change);

            // Update the last change

            lastUpdate = change.getTimeAdded();
        }
        catch (Exception e) {
            logger.error("Problem getting identity of entity - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Reads the hibernate xml file to get the connection details of the local
     * NRMM database.  It then checks that it is accessible, connects to it and
     * makes sure that the database exists
     * If it doesn't, then it will create it using the app.sql file
     *
     * @param servletContextEvent Context event
     * @return True if a new database was created
     */
    public static boolean setupAppDatabase(ServletContextEvent servletContextEvent) {

        boolean newDatabase = false;
        boolean newSchemaOnly = false;
        String error = null;
        try {

            // Make all H2 databases use case-insensitive table/column names

            System.setProperty("h2.identifiersToUpper", "false");

            // Retrieve the useful values from the servlet context

            logger.debug("Reading hibernate parameters");
            AppDataSource ds = getDataSource();

            // Allocate and use a connection directly

            if (ds != null) {

                // Set the schema name to be the same as the database name if postgreSQL and not in context

                if (ds.isPostgreSQL() && isBlank(ds.getSchema()))
                    ds.setSchema(ds.getDatabase());

                Connection db;
                logger.debug("Connecting to [{}]", ds.getUrl());
                try {
                    db = DataSourceUtils.createNewConnection(ds.getUrl(), ds.getDriverClassName(), ds.getUsername(), ds.getPassword());
                    DatabaseMetaData md = db.getMetaData();
                    ResultSet rs = md.getTables(null, ds.getSchema(), "patch", null);
                    if (!rs.next()) {
                        rs = md.getTables(null, ds.getSchema(), "PATCH", null);
                        newDatabase = !rs.next();
                    }
                    db.close();
                    if (!newDatabase)
                        logger.info("{} database is present and available [{}]", Common.getAplicationName(), ds.getUrl().split("[?]")[0]);
                    else
                        // DB exists but schema doesn't
                        newSchemaOnly = true;
                }
                catch (SQLException e) {
                    if (PivotalException.getErrorMessage(e) == null || !PivotalException.getErrorMessage(e).matches("(?is)(.+unknown database.+)|(.+database .+ does not exist)"))
                        throw new PivotalException("Cannot open the %s database - %s", Common.getAplicationName(), PivotalException.getErrorMessage(e));
                    else
                        newDatabase = true;
                }

                // If the database is not present then we need to create it

                if (newDatabase) {
                    createDatabase(servletContextEvent, ds, newSchemaOnly);
                }

                // Upgrade the database using all the changes files

                patchDatabase(servletContextEvent, ds);

                // Start the connection pool just in case it has been stopped
                // This can happen if the system has been shutdown to apply database changes etc.

                logger.debug("Reading hibernate parameters");
                ds.createPool();
            }
        }
        catch (Exception e) {
            error = String.format("Problem verifying %s database - %s", Common.getAplicationName(), PivotalException.getErrorMessage(e));
            if (error.contains("Call getNextException")) {
                try {
                    error += " " + ((SQLException)e).getNextException().getMessage();
                }
                catch(Exception e1) {
                    logger.debug("Unable to get next error {}", error);
                }
            }
        }

        // Check if we have encountered an error

        if (!isBlank(error)) {
            throw new PivotalException(error);
        }
        return newDatabase;
    }

    /**
     * Creates the local NRMM database
     *
     * @param servletContextEvent Servlet context for this app
     * @param dsInfo              Data source information
     * @param newSchemaOnly       If true then db exists but schema doesn't (postgres only?)
     * @throws Exception If an error occurs
     */
    private static void createDatabase(ServletContextEvent servletContextEvent, AppDataSource dsInfo, boolean newSchemaOnly) throws Exception {

        // Check that the database definition exists

        File sql = new File(ServletHelper.getRealPath(servletContextEvent.getServletContext(), Constants.APP_SQL_FILE));
        if (!sql.exists())
            throw new Exception(String.format("The %s SQL file [%s] doesn't exist", Common.getAplicationName(), Constants.APP_SQL_FILE));

        if (dsInfo.isH2()) {
            updateH2Database(dsInfo, sql, true);
        }
        else if (dsInfo.isPostgreSQL()) {
            updatePostgreSQLDatabase(dsInfo, sql, true, newSchemaOnly);
        }
        else {
            updateMySQLDatabase(dsInfo, sql, true);
        }
    }

    /**
     * Creates the local NRMM database
     *
     * @param dsInfo Data source information
     * @param sql    File containing SQL statements
     * @param create True if the database is to be created from scratch
     * @throws Exception If an error occurs
     */
    private static void updateH2Database(AppDataSource dsInfo, File sql, boolean create) throws Exception {

        // Check that the database definition exists

        Connection db;
        Statement statement = null;
        if (create)
            logger.info("Creating {} H2 database for [{}]", Common.getAplicationName(), dsInfo.getDatabase());
        else
            logger.info("Applying {} H2 patch [{}]", Common.getAplicationName(), sql.getName());
        db = DriverManager.getConnection(dsInfo.getUrl(), dsInfo.getUsername(), dsInfo.getPassword());

        // OK, we have a database connection so let's create the database

        try {
            db.setAutoCommit(false);
            statement = db.createStatement();
            String commands = Common.readTextFile(sql.getAbsolutePath());
            for (String command : commands.split("(?ims)(;\\s+)|(\\*/;*\\s*)")) {
                if (!isBlank(command) && !command.trim().startsWith("/*")) {

                    // Remove the MySQL character set stuff

                    command = command.replaceFirst("(?mis)\\) *DEFAULT *CHARSET *= *utf8.*$", ");");
                    if (command.matches("(?mis)^\\s*create +index.+")) {
                        command = command.replaceAll("\\([0-9]+\\)", "");
                    }
                    List<String> extraCommands = new ArrayList<>();

                    // Check to see if there are any update timestamps in here

                    int pos;
                    while ((pos = command.toUpperCase().indexOf(" ON UPDATE CURRENT_TIMESTAMP")) > -1) {
                        String table = command.split("(?mis)create +table +", 2)[1].split("\\W", 2)[0];
                        int line = command.lastIndexOf('\n', pos);
                        String column = command.substring(line, pos).trim().split("\\W", 2)[0];
                        extraCommands.add(String.format("alter table %s alter column %s timestamp as now();", table, column));
                        command = command.replaceAll("(?mis) ON UPDATE CURRENT_TIMESTAMP", "");
                    }
                    statement.addBatch(command);
                    for (String extraCommand : extraCommands) {
                        statement.addBatch(extraCommand);
                    }
                }
            }
            statement.executeBatch();

            // Add the patch file if patching

            if (create)
                logger.info("The {} H2 database has been successfully created", Common.getAplicationName());
            else {
                statement.execute("replace patch (name,checksum) values ('" + sql.getName().replaceAll("'", "''") + "','" + Common.getMD5String(commands) + "');");
                logger.info("The {} H2 database has been successfully patched", Common.getAplicationName());
            }
        }
        catch (Exception e) {
            db.rollback();
            logger.debug("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            Common.close(statement);
            if (db != null) try {
                db.commit();
                db.setAutoCommit(true);
                db.close();
            }
            catch (Exception e) {
                logger.debug("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Creates the local NRMM database
     *
     * @param dsInfo Data source information
     * @param sql    File containing SQL statements
     * @param create True if the database is to be created from scratch
     * @param newSchemaOnly True if db exists but schema does not
     * @throws Exception If an error occurs
     */
    private static void updatePostgreSQLDatabase(AppDataSource dsInfo, File sql, boolean create, boolean newSchemaOnly) throws Exception {

        // Check that the database definition exists

        Connection db;
        Statement statement = null;
        if (create) {
            logger.info("Creating {} PostgreSQL database for [{}.{}]", Common.getAplicationName(), dsInfo.getDatabase(), dsInfo.getSchema());

            // Connect using default user db

            db = DriverManager.getConnection(dsInfo.getServerAddress(), isBlank(dsInfo.getDefaultUsername())?dsInfo.getUsername():dsInfo.getDefaultUsername(), isBlank(dsInfo.getDefaultPassword())?dsInfo.getPassword():dsInfo.getDefaultPassword());
        }
        else {
            logger.info("Applying {} PostgreSQL patch [{}]", Common.getAplicationName(), sql.getName());
            db = DriverManager.getConnection(dsInfo.getUrl(), dsInfo.getUsername(), dsInfo.getPassword());
        }

        // OK, we have a database connection so let's create the database

        try {
            db.setAutoCommit(true);
            statement = db.createStatement();
            if (create) {

                // If not only new Schema then create db
                if (!newSchemaOnly) {

                    // Create the database

                    statement.execute(String.format("CREATE DATABASE %s ENCODING 'UTF8' TEMPLATE template0;", dsInfo.getDatabase()));

                    // Close it

                    Common.close(statement, db);
                }

                // Open it

                db = DriverManager.getConnection(dsInfo.getUrl(), dsInfo.getUsername(), dsInfo.getPassword());

                try {
                    // Now create a schema with the passed in name

                    statement = db.createStatement();
                    statement.execute(String.format("CREATE SCHEMA %s AUTHORIZATION %s;", dsInfo.getSchema(), dsInfo.getUsername()));

                }
                catch (Exception e) {
                    logger.info("Error creating schema - continuing to attempt creation of tables - {}", PivotalException.getErrorMessage(e));
                }

                // Recreate the statement object ready to receive the batch of commands
                statement = db.createStatement();
            }

            // Make sure we are using the correct schema

            statement.addBatch("set schema '" + dsInfo.getSchema() + "';");

            // Loop through all the commands

            db.setAutoCommit(false);
            String commands = Common.readTextFile(sql.getAbsolutePath());

            // If patching, make sure we haven't already patched it

            boolean okToRun = create;
            if (!create) {
                try {
                    if (statement.execute("select count(name) from " + dsInfo.getSchema() + ".patch where name = '" + sql.getName().replaceAll("'", "''") + "' and checksum = '" + Common.getMD5String(commands) + "';")) {
                        ResultSet patchResult = statement.getResultSet();
                        if (patchResult.next())
                            okToRun = patchResult.getInt(1)==0;
                    }
                }
                catch(Exception e) {
                    logger.info("Error checking applied patch files in database " + sql.getName() + " " + PivotalException.getErrorMessage(e));
                }
            }

            if (okToRun) {

                if (false) {
                    String[] commandArray;
                    if (commands.contains("DO $$"))
                        commandArray = commands.split("\0");
                    else
                        commandArray = commands.split("(?ims)(;\\s+)|(\\*/;*\\s*)");

                    for (String command : commandArray) {
                        if (!isBlank(command) && !command.trim().startsWith("/*")) {


                            // Remove the MySQL character set stuff

                            command = command.replaceFirst("(?mis)\\) *DEFAULT *CHARSET *= *utf8.*$", ");");
                            if (command.matches("(?mis)^\\s*create +index.+")) {
                                command = command.replaceAll("\\([0-9]+\\)", "");
                            }
                            List<String> extraCommands = new ArrayList<>();

                            // Swap the easy types

                            command = command.replaceFirst("(?mis)INTEGER NOT NULL AUTO_INCREMENT", "SERIAL");
                            command = command.replaceAll("(?mis) LONGBLOB", " BYTEA");
                            command = command.replaceAll("(?mis) BLOB", " BYTEA");
                            command = command.replaceAll("(?mis) LONGTEXT", " TEXT");
                            command = command.replaceAll("(?mis) DATETIME", " TIMESTAMP");
                            command = command.replaceAll("(?mis) DOUBLE", " DOUBLE PRECISION");
                            command = command.replaceAll("(?mis) COMMENT +'[^']+'", "");

                            command = command.replaceFirst("(?mis)CREATE +TABLE +user ", "CREATE TABLE \"user\" ");
                            command = command.replaceFirst("(?mis) ON +user ", " ON \"user\" ");
                            command = command.replaceFirst("(?mis) REFERENCES +user ", " REFERENCES \"user\" ");
                            command = command.replaceFirst("(?mis)ALTER TABLE +user ", "ALTER TABLE \"user\" ");


                            // Make all constraints deferrable

                            if (command.matches("(?mis)ALTER TABLE .+ADD +CONSTRAINT .+")) {
                                command = command + " DEFERRABLE INITIALLY IMMEDIATE";
                            }

                            // Check to see if there are any update timestamps in here

                            int pos;
                            while ((pos = command.toUpperCase().indexOf(" ON UPDATE CURRENT_TIMESTAMP")) > -1) {
                                String table;
                                String column;
                                if (command.split("(?mis)create table +", 2).length > 1) {
                                    table = command.split("(?mis)create +table +", 2)[1].split("[ (]", 2)[0];
                                    int line = command.lastIndexOf('\n', pos);
                                    column = command.substring(line, pos).trim().split("\\W", 2)[0];
                                }
                                else {
                                    table = command.split("(?mis)alter +table +", 2)[1].split("[ (]", 2)[0];
                                    column = command.split("(?mis) column")[1].split("\\W")[1];
                                }

                                String simpleTable = (table.contains(".") ? table.split("\\.")[1] : table);
                                extraCommands.add(String.format("CREATE OR REPLACE FUNCTION %s_%s_update_timestamp()\n" +
                                        "RETURNS TRIGGER AS $$\n" +
                                        "BEGIN\n" +
                                        "   NEW.%s = now(); \n" +
                                        "   RETURN NEW;\n" +
                                        "END;\n" +
                                        "$$ language 'plpgsql';", table, column, column));
                                extraCommands.add(String.format("CREATE TRIGGER %s_%s_update_timestamp BEFORE UPDATE\n" +
                                        "    ON %s FOR EACH ROW EXECUTE PROCEDURE \n" +
                                        "    %s_%s_update_timestamp();", simpleTable, column, table, table, column));
                                command = command.replaceAll("(?mis) ON UPDATE CURRENT_TIMESTAMP", "");
                            }
                            while ((pos = command.toUpperCase().indexOf(" ON INSERT CURRENT_TIMESTAMP")) > -1) {
                                String table;
                                String column;
                                if (command.split("(?mis)create table +", 2).length > 1) {
                                    table = command.split("(?mis)create +table +", 2)[1].split("[ (]", 2)[0];
                                    int line = command.lastIndexOf('\n', pos);
                                    column = command.substring(line, pos).trim().split("\\W", 2)[0];
                                }
                                else {
                                    table = command.split("(?mis)alter +table +", 2)[1].split("[ (]", 2)[0];
                                    column = command.split("(?mis) column")[1].split("\\W")[1];
                                }

                                String simpleTable = (table.contains(".") ? table.split("\\.")[1] : table);
                                extraCommands.add(String.format("CREATE OR REPLACE FUNCTION %s_%s_insert_timestamp()\n" +
                                        "RETURNS TRIGGER AS $$\n" +
                                        "BEGIN\n" +
                                        "   NEW.%s = now(); \n" +
                                        "   RETURN NEW;\n" +
                                        "END;\n" +
                                        "$$ language 'plpgsql';", table, column, column));
                                extraCommands.add(String.format("CREATE TRIGGER %s_%s_insert_timestamp BEFORE INSERT\n" +
                                        "    ON %s FOR EACH ROW EXECUTE PROCEDURE \n" +
                                        "    %s_%s_insert_timestamp();", simpleTable, column, table, table, column));
                                command = command.replaceAll("(?mis) ON INSERT CURRENT_TIMESTAMP", "");
                            }

                            statement.addBatch(command);
                            for (String extraCommand : extraCommands) {
                                statement.addBatch(extraCommand);
                            }
                        }
                    }
                }
                else {
                    statement.addBatch(commands);
                }
                statement.executeBatch();


                if (create)
                    logger.info("The {} PostgreSQL database has been successfully created", Common.getAplicationName());
                else {
                    // Add the patch file hash to db so we don't run it again
                    statement.execute("delete from " + dsInfo.getSchema() + ".patch where name = '" + sql.getName().replaceAll("'", "''") + "';");
                    statement.execute("insert into " + dsInfo.getSchema() + ".patch (name,checksum) values ('" + sql.getName().replaceAll("'", "''") + "','" + Common.getMD5String(commands) + "');");
                    logger.info("The {} PostgreSQL database has been successfully patched", Common.getAplicationName());
                }
            }
            else
                logger.info("Skipping previously applied patch [" + sql.getName() + "]");
        }
        catch (Exception e) {
            db.rollback();
            logger.error("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            Common.close(statement);
            if (db != null) try {
                db.commit();
                db.setAutoCommit(true);
                db.close();
            }
            catch (Exception e) {
                logger.error("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Creates the local NRMM database
     *
     * @param dsInfo Data source information
     * @param sql    File containing SQL statements
     * @param create True if the database is to be created from scratch
     * @throws Exception If an error occurs
     */
    private static void updateMySQLDatabase(AppDataSource dsInfo, File sql, boolean create) throws Exception {

        // Check that the database definition exists

        Connection db;
        Statement statement;
        if (create)
            logger.info("Creating {} MySQL database for [{}]", Common.getAplicationName(), dsInfo.getDatabase());
        else
            logger.info("Applying {} MySQL patch [{}]", Common.getAplicationName(), sql.getName());
        db = DriverManager.getConnection(dsInfo.getServerAddress(), dsInfo.getUsername(), dsInfo.getPassword());

        // OK, we have a database connection so let's create the database

        String currentCommand = null;
        String commands = Common.readTextFile(sql.getAbsolutePath());
        String[] commandList = Common.readTextFile(sql.getAbsolutePath()).split("(?ims)(;\\s+)|(\\*/;*\\s*)");
        statement = db.createStatement();
        try {
            db.setAutoCommit(false);
            if (create) {
                statement.execute(String.format("create database %s DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;", dsInfo.getDatabase()));
                statement.execute(String.format("use %s;", dsInfo.getDatabase()));
                statement.execute("SET foreign_key_checks=0;");
                for (String command : commandList) {
                    if (!isBlank(command) && !command.trim().startsWith("/*")) {
                        currentCommand = command;
                        statement.execute(command);
                    }
                }
            }
            else {
                lockAllTables(dsInfo.getDatabase(), statement);
                statement.execute(String.format("use %s", dsInfo.getDatabase()));
                statement.addBatch("SET foreign_key_checks=0;");
                for (String command : commandList) {
                    if (!isBlank(command) && !command.trim().startsWith("/*"))
                        statement.addBatch(command);
                }
                statement.addBatch("SET foreign_key_checks=1;");
                statement.executeBatch();
            }
            statement.execute("SET foreign_key_checks=1;");

            // Add the patch file if patching

            if (create)
                logger.info("The {} MySQL database has been successfully created", Common.getAplicationName());
            else {
                statement.execute("replace patch (name,checksum) values ('" + sql.getName().replaceAll("'", "''") + "','" + Common.getMD5String(commands) + "');");
                logger.info("The {} MySQL database has been successfully patched", Common.getAplicationName());
            }
        }
        catch (SQLException e) {
            db.rollback();
            if (create) {
                logger.error("Cannot execute the DDL command [{}] - {}", currentCommand, PivotalException.getErrorMessage(e));
                statement.execute(String.format("drop database %s;", dsInfo.getDatabase()));
            }
            else
                logger.debug("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            throw e;
        }
        catch (Exception e) {
            db.rollback();
            logger.debug("Cannot execute the DDL commands - {}", PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            if (statement != null)
                try {
                    statement.execute("UNLOCK TABLES;");
                    statement.close();
                }
                catch (Exception e) {
                    logger.debug("Problem with unlock - {}", PivotalException.getErrorMessage(e));
                }
            if (db != null) try {
                db.commit();
                db.setAutoCommit(true);
                db.close();
            }
            catch (Exception e) {
                logger.debug("Problem with commit/close - {}", PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     * Upgrades the local NRMM database
     *
     * @param servletContextEvent Servlet context for this app
     * @param dsInfo              Data source information
     * @throws Exception If an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void patchDatabase(ServletContextEvent servletContextEvent, AppDataSource dsInfo) throws Exception {

        // Get a list of all the change files

        logger.debug("Checking for change files in [{}]", ServletHelper.getRealPath(servletContextEvent.getServletContext(), Constants.APP_SQL_FILE));
        File sql = new File(ServletHelper.getRealPath(servletContextEvent.getServletContext(), Constants.APP_SQL_FILE));
        List<File> changeFiles = Common.listFiles(sql.getParentFile(), "app_patch-.+\\.sql");
        if (!isBlank(changeFiles)) {

            // We need to sort these files by their number

            Collections.sort(changeFiles, new Comparator() {
                public int compare(Object o1, Object o2) {
                    String s1 = ((File) o1).getName().replaceAll("[^0-9]", "");
                    String s2 = ((File) o2).getName().replaceAll("[^0-9]", "");
                    int a = Common.parseInt(s1);
                    int b = Common.parseInt(s2);
                    return a < b ? -1 : a == b ? 0 : 1;
                }
            });

            // Apply each file

            for (File changeFile : changeFiles) {
                if (dsInfo.isH2()) {
                    updateH2Database(dsInfo, changeFile, false);
                }
                else if (dsInfo.isPostgreSQL()) {
                    updatePostgreSQLDatabase(dsInfo, changeFile, false, false);
                }
                else {
                    updateMySQLDatabase(dsInfo, changeFile, false);
                }
            }
        }
    }

    /**
     * Attempts to lock all the tables of the database
     *
     * @param database  Database to lock
     * @param statement Statement to use
     * @throws SQLException Problems
     */
    private static void lockAllTables(String database, Statement statement) throws SQLException {
        ResultSet tables = null;
        try {
            tables = statement.executeQuery("select distinct table_name from information_schema.tables where table_schema='" + database + '\'');
            if (tables != null) {
                List<String> tableNames = new ArrayList<>();
                while (tables.next()) {
                    tableNames.add(tables.getString(1));
                }
                tables.close();
                tables = null;
                statement.execute("lock tables " + Common.join(tableNames, " write,") + " write;");
            }
        }
        finally {
            Common.close(tables);
        }
    }

    /**
     * Clears the 2nd level cache in Hibernate
     */
    public static void clearCache() {
        try {
            if (sessionFactory != null && sessionFactory.getCache() != null) {
                sessionFactory.getCache().evictEntityRegions();
                sessionFactory.getCache().evictCollectionRegions();
                sessionFactory.getCache().evictDefaultQueryRegion();
                sessionFactory.getCache().evictQueryRegions();
            }
        }
        catch (Exception e) {
            logger.debug("Problem clearing the cache - {}", PivotalException.getErrorMessage(e));
        }
    }

    /**
     * Adds the data source to be used by the application. Used to override the context lookup.
     *
     * @param datasourceName The name of the data source
     * @param ds             The data source
     */
    public static void addDataSource(String datasourceName, AppDataSource ds) {
        dsMap.put(datasourceName, ds);
    }

    /**
     * Gets the datasource for the Hibernate connection
     *
     * @return Datasource info
     *
     * @throws java.lang.Exception Error
     */
    public static AppDataSource getDataSource() throws Exception {
        javax.naming.Context initCtx = null;
        try {
            initCtx = new InitialContext();
            javax.naming.Context envCtx = (javax.naming.Context) initCtx.lookup("java:comp/env");
            return (AppDataSource) envCtx.lookup(HibernateUtils.JDBC_APP);
        }
        finally {
            Common.close(initCtx);
        }
    }

    /**
     * Gets the named datasource for the Hibernate connection. If the data source can not be found then default back to the main data source.
     *
     * @param datasourceName The data source to lookup
     * @return Datasource info
     *
     * @throws java.lang.Exception Error
     */
    public static AppDataSource getDataSource(String datasourceName) throws Exception {
        AppDataSource retval = dsMap.get(datasourceName);
        if (retval == null) {
            javax.naming.Context initCtx = null;
            try {
                initCtx = new InitialContext();
                javax.naming.Context envCtx = (javax.naming.Context) initCtx.lookup("java:comp/env");
                try {
                    retval = (AppDataSource) envCtx.lookup(datasourceName);
                }
                catch (NameNotFoundException e) {
                    logger.debug("Could not find resource {} - continuing", datasourceName);
                }
                if ((retval == null) && (!Common.doStringsMatch(JDBC_APP, datasourceName))) {
                    retval = (AppDataSource) envCtx.lookup(JDBC_APP);
                }
                dsMap.put(datasourceName, retval);
            }
            finally {
                Common.close(initCtx);
            }
        }
        return retval;
    }

   public static String getUploadedFileLocation() {
       String returnValue = ServletHelper.getServletContext().getInitParameter(UPLOADED_FILE_LOCATION);
       if (!isBlank(returnValue) && !returnValue.endsWith(File.separator))
           returnValue += File.separator;
       return returnValue;
    }

    public static String getMappingFileList() {
        return ServletHelper.getServletContext().getInitParameter(MAPPING_FILE_LIST);
    }


    public static String getMappingKey() {
        return ServletHelper.getServletContext().getInitParameter(MAPPING_KEY);
    }

   /**
     * Convenience method for committing a transaction
     *
     * @throws org.hibernate.HibernateException If Tx cannot be started
     */
    public static void commit() throws org.hibernate.HibernateException {
        getCurrentSession().getTransaction().commit();
        getCurrentSession().beginTransaction();
    }

    /**
     * Convenience method for starting a transaction
     *
     * @throws org.hibernate.HibernateException If Tx cannot be started
     */
    public static void rollback() throws org.hibernate.HibernateException {
        getCurrentSession().getTransaction().rollback();
        getCurrentSession().beginTransaction();
    }

    public static String getPropertyFromFieldName(Class clazz, String fieldName) {

        String returnValue = null;

        if (!isBlank(fieldName)) {
            BeanWrapperImpl beanWrapper = new BeanWrapperImpl(clazz);
            for (PropertyDescriptor prop : beanWrapper.getPropertyDescriptors()) {
                if (prop.getReadMethod() != null) {
                    String name = null;
                    if (prop.getReadMethod().getAnnotation(Column.class) != null)
                        name = prop.getReadMethod().getAnnotation(Column.class).name();
                    else if (prop.getReadMethod().getAnnotation(JoinColumn.class) != null)
                        name = prop.getReadMethod().getAnnotation(JoinColumn.class).name();

                    if (fieldName.equals(name) || fieldName.equals(prop.getName()))
                        returnValue = prop.getName();
                }
            }
        }
        return returnValue;
    }

    public static TempModel getTempModel() {
        return new TempModel();
    }

    /**
     * A simple implementation of the Model so we can capture results
     */
    public static class TempModel implements Model {

        Map<String, Object>storage;

        public TempModel() {
            storage = new HashMap<>();
        }

        @Override
        public Model addAttribute(String attributeName, Object attributeValue) {
            if (attributeName != null)
                storage.put(attributeName, attributeValue);

            return this;
        }

        @Override
        public Model addAttribute(Object attributeValue) {
            return addAttribute(Common.generateGUID(), attributeValue);
        }

        @Override
        public Model addAllAttributes(Collection<?> attributeValues) {

            if (attributeValues != null)
                for (Object object : attributeValues)
                    addAttribute(object);

            return this;
        }

        @Override
        public Model addAllAttributes(Map<String, ?> attributes) {
            if (attributes != null)
                storage.putAll(attributes);

            return this;
        }

        @Override
        public Model mergeAttributes(Map<String, ?> attributes) {

            if (attributes != null) {
                for(String key : attributes.keySet())
                    if (!storage.containsKey(key))
                        storage.put(key, attributes.get(key));
            }
            return this;
        }

        @Override
        public boolean containsAttribute(String attributeName) {
            return storage.containsKey(attributeName);
        }

        @Override
        public Map<String, Object> asMap() {
            return storage;
        }
    }
}
