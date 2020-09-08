/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.ChangeLogEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Handles requests viewing the audit log
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/audit")
public class AuditController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuditController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return ChangeLogEntity.class;
    }

    @Override
    public String edit(HttpSession session, Model model, @RequestParam(value = "id", required = false) Integer id) {

        // Find the next change if there is one

        if (id!=null) {
            ChangeLogEntity log = HibernateUtils.getEntity(ChangeLogEntity.class, id);
            if (log != null) {
                List<ChangeLogEntity> list = HibernateUtils.selectEntities("from ChangeLogEntity where tableAffected=? and rowAffected=? and timeAdded>?",
                                                                           log.getTableAffected(), log.getRowAffected(), log.getTimeAdded());
                if (!Common.isBlank(list)) {
                    model.addAttribute("NextLogId", list.get(0).getId());
                }
            }
        }
        return super.edit(session, model, id);
    }


}
