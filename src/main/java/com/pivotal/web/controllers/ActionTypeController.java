/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.ActionTypeEntity;
import com.pivotal.web.controllers.utils.Authorise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manages the administration of action types
 *
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/action_type")
public class ActionTypeController extends AbstractAdminController {

    private static final Logger logger = LoggerFactory.getLogger(ActionTypeController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return ActionTypeEntity.class;
    }
}
