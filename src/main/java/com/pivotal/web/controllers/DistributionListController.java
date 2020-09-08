/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.monitoring.utils.Definition;
import com.pivotal.system.hibernate.entities.DistributionListEntity;
import com.pivotal.utils.Common;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Handles requests for storing and managing distribution lists
 */
@Authorise
@Controller
@RequestMapping(value = {"/admin/distribution_list", "/alarm_control/distribution_list"})
public class DistributionListController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DistributionListController.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Class getEntityClass() {
        return DistributionListEntity.class;
    }

    /**
     * Serves the address input stuff for a data transducer type
     *
     * @param session Session associated with this user
     * @param model   Model to populate
     * @param type    Name of the distribution list type
     */
    @RequestMapping(value = {"/type", "/definition", "/definition_row"}, method = RequestMethod.GET)
    public void getDefinition(HttpSession session, Model model, @RequestParam("typename") String type) {
        try {
            model.addAttribute("Definition", new Definition(DistributionListEntity.getDefinitionXML(type)));
        }
        catch (Exception e) {
            logger.error("Error getting definition");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeValidation(HttpSession session, HttpServletRequest request, Object entity) {
        DistributionListEntity dist = (DistributionListEntity) entity;
        if (Common.doStringsMatch("deadend", dist.getType())) {
            dist.setContent("empty");
        }
    }
}
