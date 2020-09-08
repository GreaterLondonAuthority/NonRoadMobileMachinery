/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.workflow;

import com.pivotal.system.hibernate.entities.WorkflowEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.web.controllers.utils.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;

/**
 *
 */
public class WorkflowJob {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowJob.class);

    private WorkflowEntity workflowEntity = null;
    private Map<String, Object> settings;

    public WorkflowJob(String code) {

        if (!isBlank(code)) {
            workflowEntity = HibernateUtils.selectFirstEntity("From WorkflowEntity where lower(code) = lower(?)", code);
            if (workflowEntity == null)
                logger.debug("WorkflowJob not created as the code could not be found - {}", code);
        }
        else
            logger.error("WorkflowJob not created as a blank code was requested");

        settings = new HashMap<>();
    }

    public WorkflowJob(WorkflowEntity workflowEntity) {
        this.workflowEntity = workflowEntity;
        settings = new HashMap<>();

        if (this.workflowEntity == null)
            logger.error("WorkflowJob not created as a blank Workflow was specified");

    }

    public WorkflowJob putSetting(String name, Object value) {
        if (!isBlank(name))
            settings.put(name, value);

        return this;
    }

    public WorkflowJob putSetting(Map<String, Object>values) {
        if (!isBlank(values))
            settings.putAll(values);

        return this;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public WorkflowEntity getWorkflowEntity() {
        return workflowEntity;
    }

    /**
     * Executes the workflow script
     *
     * @return Workflow result
     */
    public JsonResponse execute() {
        return execute(false);
    }

    /**
     * Executes the workflow script
     *
     * @param returnOutput if true the the information property
     *                     will be loaded with script output
     *
     * @return Workflow result
     */
    public JsonResponse execute(boolean returnOutput) {
        JsonResponse returnValue = new JsonResponse();

        if (workflowEntity == null) {
            returnValue.setError("Unable to identify the workflow script to be executed");
            logger.error(returnValue.getError());
        }
        else {
            logger.debug("Runnning workflow {}", workflowEntity.getName());
            returnValue = WorkflowHelper.executeWorkflow(this, returnOutput);

            String workflowOutput = null;
            if (!isBlank(returnValue.getInformation()))
                workflowOutput = returnValue.getInformation().trim();

            logger.debug("Workflow result for {}. {} - Errors = {}, Result = {}", workflowEntity.getId(), workflowEntity.getName(), returnValue.getInError(), workflowOutput);
        }

        return returnValue;
    }
}
