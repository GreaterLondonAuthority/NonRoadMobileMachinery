/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.RoleTypeEntity;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manages the administration of role types
 */
@Authorise
@Controller
@RequestMapping("/admin/role_type")
public class RoleTypeController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RoleTypeController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return RoleTypeEntity.class;
    }
}
