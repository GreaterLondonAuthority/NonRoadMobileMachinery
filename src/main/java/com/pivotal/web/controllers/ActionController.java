/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.ActionEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.utils.workflow.WorkflowJob;
import com.pivotal.web.controllers.utils.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.pivotal.utils.Common.isBlank;

/**
 * handles action requests
 */
@Controller
@RequestMapping(value = "/action")
public class ActionController {

    private static final Logger logger = LoggerFactory.getLogger(ActionController.class);

    /**
     * Process the action
     * @param guid the action guid
     *
     * @return Returns the html page to process
     */
    @RequestMapping(value = "/{guid}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String processActionHTML(Model model, @PathVariable String guid) {


        JsonResponse workflowResult = processAction(model, guid);

        model.addAttribute("WorkflowResult", workflowResult);

        return "action/default";
    }

    /**
     * Process the action and returns json
     *
     * @param guid the action guid
     */
    @RequestMapping(value = "/{guid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse processActionJSON(Model model, @PathVariable String guid) {

        return processAction(model, guid);
    }

    private static JsonResponse processAction(Model model, String guid) {

        JsonResponse workflowResult = new JsonResponse();

        if (!isBlank(guid)) {
            ActionEntity actionEntity = HibernateUtils.selectFirstEntity("From ActionEntity where guid = ?", guid);
            if (actionEntity != null) {
                if (actionEntity.getUsed()) {
                    logger.debug("A used Action is being requested {}", guid);
                    workflowResult.setError("Invalid action specified");
                }
                else {
                    actionEntity.setUsed(true);
                    HibernateUtils.save(actionEntity);

                    if (actionEntity.getExpiry().before(Common.getTimestamp())) {
                        logger.debug("An expired Action is being requested {}", guid);
                        workflowResult.setError("Invalid action specified");
                    }
                    else {
                        logger.debug("Processing action with guid {}", guid);

                        WorkflowJob workflowJob = new WorkflowJob(actionEntity.getType().getWorkflow());
                        workflowJob.putSetting("Action", actionEntity);
                        workflowResult = WorkflowHelper.executeWorkflow(workflowJob, true);
                        workflowResult.putDataItem("Action", actionEntity);
                        model.addAttribute("Action", actionEntity);

                        logger.debug("Workflow information = {}", workflowResult.getInformation());
                    }
                }
            }
            else {
                logger.debug("Unable to find action for {}", guid);
                workflowResult.setError("Invalid action specified");
            }
        }
        else {
            logger.debug("Action requested with blank guid");
            workflowResult.setError("Invalid action specified");
        }

        return workflowResult;
    }
}
