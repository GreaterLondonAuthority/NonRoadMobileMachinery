/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.workflow;

import com.pivotal.system.hibernate.entities.ActionEntity;
import com.pivotal.system.hibernate.entities.ActionTypeEntity;
import com.pivotal.system.hibernate.entities.WorkflowEntity;
import com.pivotal.system.hibernate.utils.AppDataSource;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VelocityUtils;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.controllers.utils.MappingManager;
import com.pivotal.web.email.EmailManager;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;

/**
 * Provides the main system event monitoring functionality
 * This mostly means checking for storage of events to the audit/log tables
 */
public class WorkflowHelper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorkflowHelper.class);

    /**
     * This method executes the script
     *
     * @param workflowJob Job to execute
     *
     * @return returns JsonResponse object with result of execution
     */
    public static JsonResponse executeWorkflow(WorkflowJob workflowJob) {
        return executeWorkflow(workflowJob, false);
    }

    /**
     * This method executes the script
     *
     * @param workflowJob Job to execute
     * @param returnOutput if true then the output from the script is put into the
     *                     information property
     *
     * @return returns JsonResponse object with result of execution
     */
    public static JsonResponse executeWorkflow(WorkflowJob workflowJob, boolean returnOutput) {
        JsonResponse workflowResult = new JsonResponse();

        if (workflowJob.getWorkflowEntity() == null) {
            logger.debug("Execution of workflow failed as workflow record not found");
            workflowResult.setError("Execution of workflow failed as workflow record not found");
        }
        else {
            workflowResult = executeScript(workflowJob.getWorkflowEntity().getScript(),
                                           workflowJob.getWorkflowEntity().getName(),
                                           workflowJob.getSettings(),
                                           returnOutput);
        }

        return workflowResult;
    }

    /**
     * Executes script, separate method allows other uses
     * @param script    Script to execute
     * @param jobName   Name of script
     * @param params    parameters to be used in script
     * @param returnOutput If true then information property is loaded with script output
     *
     * @return JsonResponse Object
     */
    public static JsonResponse executeScript(String script, String jobName, Map<String, Object>params, boolean returnOutput) {

        JsonResponse workflowResult = new JsonResponse();

        // Setup the environment
        try {
            logger.debug("Creating connection to database");
            AppDataSource dataSource = HibernateUtils.getDataSource();

            logger.debug("Creating velocity engine");
            VelocityEngine engine = VelocityUtils.getEngine();

            logger.debug("Creating context");
            Context context = VelocityUtils.getVelocityContext();

            // Output extra objects to context

            context.put("Source", dataSource);
            context.put("CaseManager", CaseManager.class);
            context.put("MappingManager", MappingManager.class);
            context.put("HibernateUtils", HibernateUtils.class);

            context.put("Engine", engine);

            // Add generic objects

            Map<String, Object> genericObjects = ServletHelper.getGenericObjects(ServletHelper.getRequest(), ServletHelper.getResponse(), true);
            for (String key : genericObjects.keySet())
                context.put(key, genericObjects.get(key));

            // Add workflow settings

            if (!isBlank(params))
                for (String key : params.keySet())
                    context.put(key, params.get(key));

            // Put the context
            context.put("context", context);
            context.put("Context", context);

            // Add the result class
            context.put("WorkflowResult", workflowResult);

            context.put("EmailManager", EmailManager.class);
            context.put("WorkflowHelper", WorkflowHelper.class);

            context.put("Logger", logger);
            context.put("logger", logger);

            context.put("NotificationManager", NotificationManager.class);

            try {
                StringWriter output = new StringWriter();
                logger.debug("Executing job {}", jobName);
                engine.evaluate(context, output, WorkflowEntity.class.getSimpleName(), script);
                logger.debug("Job output {}", output.toString().trim());
                if (returnOutput)
                    workflowResult.setInformation(output.toString());
            }
            catch (Exception e) {
                logger.info("Script execution for workflow name [{}], failed - {}", jobName, PivotalException.getErrorMessage(e));
            }
        }
        catch (Exception e) {
            logger.debug("Error setting up environment for Job {}", PivotalException.getErrorMessage(e));
        }
        logger.debug("Job complete");

        return workflowResult;
    }

    /**
     * Executes workflow by code
     *
     * @param code code of workflow to run
     *
     * @return JsonResponse
     */
    public static JsonResponse executeWorkflow(String code) {

        return new WorkflowJob(code).execute();
    }

    /**
     * Executes workflow by code
     *
     * @param code          code of workflow to run
     * @param settings      Map of settings to be passed to the script
     * @param returnOutput  if true then output is returned in information property
     *
     * @return JsonResponse
     */
    public static JsonResponse executeWorkflow(String code, Map<String, Object>settings, boolean returnOutput) {

        WorkflowJob workflowJob = new WorkflowJob(code);
        workflowJob.putSetting(settings);
        return workflowJob.execute(returnOutput);
    }

    /**
     * Executes workflow by entity
     *
     * @param workflowEntity workflow to run
     *
     * @return JsonResponse
     */
    public static JsonResponse executeWorkflow(WorkflowEntity workflowEntity) {

        return new WorkflowJob(workflowEntity).execute();
    }

    /**
     * Creates an action to be processed when the URL is clicked
     * @param name Name of action to be run
     * @param days Number of days action expires in
     * @param hours Number of hours action expires in
     * @param minutes Number of minutes action expires in
     * @param seconds Number of seconds action expires in
     * @param settings settings to be passed to action
     *
     * @return full URL to action
     */
    public static String createAction(String name, String tag, Integer days, Integer hours, Integer minutes, Integer seconds, String settings) {

        Timestamp expires = Common.getTimestamp();

        if (!isBlank(days))
            expires = Common.addTimestamp(expires, Calendar.DAY_OF_WEEK, days);

        if (!isBlank(hours))
            expires = Common.addTimestamp(expires, Calendar.HOUR_OF_DAY, hours);

        if (!isBlank(minutes))
            expires = Common.addTimestamp(expires, Calendar.MINUTE, minutes);

        if (!isBlank(seconds))
            expires = Common.addTimestamp(expires, Calendar.SECOND, seconds);

        return createAction(name, tag, expires, settings);

    }

    /**
     * Creates an action to be processed when the URL is clicked
     * @param name Name of action to be run
     * @param tag field for any use
     * @param expires time/date the action link expires
     * @param settings settings to be passed to action
     *
     * @return full URL to action
     */
    public static String createAction(String name, String tag, Timestamp expires, String settings) {

        String returnValue = null;

        ActionTypeEntity actionTypeEntity = HibernateUtils.getEntity(ActionTypeEntity.class, name);

        if (actionTypeEntity == null) {
            logger.error("Invalid action type specified {}", name);
        }
        else {
            ActionEntity actionEntity = HibernateUtils.getEntity(ActionEntity.class);
            actionEntity.setType(actionTypeEntity);
            actionEntity.setUsed(false);
            if (!isBlank(expires))
                actionEntity.setExpiry(expires);
            actionEntity.setGuid(Common.generateGUID());
            if (!isBlank(settings))
                actionEntity.setSettings(settings);

            actionEntity.setTag(tag);

            if (HibernateUtils.save(actionEntity))
                returnValue = ServletHelper.getFullAppPath() + "/action/" + actionEntity.getGuid();
            else
                logger.debug("Error saving action");
        }

        return returnValue;
    }
}
