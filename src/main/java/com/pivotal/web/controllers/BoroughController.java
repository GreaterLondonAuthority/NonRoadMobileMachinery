/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.BoroughEntity;
import com.pivotal.system.security.CaseManager;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * Manages the administration of Boroughs
 *
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/borough")
public class BoroughController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BoroughController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        logger.debug("Getting entity class");
        return BoroughEntity.class;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String edit(HttpSession session, Model model, @RequestParam(value = "id", required = false) Integer id) {
        String edit = super.edit(session, model, id);

        // add CaseManager to allow getting of lookup lists

        model.addAttribute("CaseManager", CaseManager.class);

        return edit;
    }
}
