/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.WorkflowEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.workflow.WorkflowJob;
import com.pivotal.web.controllers.utils.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Manages the administration of workflows
 *
 */
@Controller
@RequestMapping(value = "/admin/workflow")
public class WorkflowController extends AbstractAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return WorkflowEntity.class;
    }

    /**
     * Returns the script for the specified case status using the status ID
     *
     * @param model Model to use to carry the report info
     * @param id ID of the case status
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "script", method = RequestMethod.GET)
    public String editScript(Model model, @RequestParam(value = "id") int id) {
        logger.debug("Getting script for workflow id {}", id);
        WorkflowEntity workflowEntity = HibernateUtils.getEntity(WorkflowEntity.class, id);
        if (workflowEntity!=null) {
            model.addAttribute("Content", workflowEntity.getScript());
            model.addAttribute("ContentId", workflowEntity.getId());
        }
        return "/media/editor";
    }

    /**
     * Allows workflows to be executed from javascript
     *
     * @param model Model to use to carry the report info
     * @param code Code of workflow to execute
     * @param params passed in parameters
     *
     * @return a json response object
     */
    @RequestMapping(value = "/execute", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse executeWorkflow(Model model, @RequestParam(value = "code") String code, @RequestParam Map<String, Object> params) {

        JsonResponse workflowResult = new JsonResponse();

        logger.debug("Executing workflow code {}", code);

        WorkflowJob workflowJob = new WorkflowJob(code);

        if (workflowJob.getWorkflowEntity() == null) {
            workflowResult.setError("Workflow not found " + code);
            logger.debug(workflowResult.getError());
        }
        else {
            // add in parameters to job

            workflowJob.putSetting("FormParams", params);

            // Execute Job

            workflowResult = workflowJob.execute(true);
        }

        return workflowResult;
    }
}
