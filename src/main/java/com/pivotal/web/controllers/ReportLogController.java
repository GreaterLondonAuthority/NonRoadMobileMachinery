/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.GridResults;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;

/**
 * Provides the display the reports run history
 */

@Authorise
@Controller
@RequestMapping(value = "/admin/report_log")
public class ReportLogController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportLogController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return LogEntity.class;
    }

    /**
     * This will update the data Range in the session when filter is applied
     *
     * @param request HttpServletRequest request object
     * @return Json Response with if there is any error
     */
    @ResponseBody
    @RequestMapping(value = "/refreshoption", method = RequestMethod.GET)
    public JsonResponse changeVal(HttpServletRequest request) {
        JsonResponse returnValue = new JsonResponse();
        if (!Common.isBlank(request.getParameter("daterange")))
            ServletHelper.getSession().setAttribute("auditdaterange", Common.parseInt(request.getParameter("daterange")));
        else
            ServletHelper.getSession().removeAttribute("auditdaterange");
        return returnValue;

    }

    /** {@inheritDoc} */
    @Override
    public GridResults getGridData() {

        // Now sort out some criteria

        String where = "";
        if (!Common.isBlank(ServletHelper.getSession().getAttribute("auditdaterange"))) {
            int range = Common.parseInt(ServletHelper.getSession().getAttribute("auditdaterange").toString());
            String format = "yyyy-MM-dd HH:mm:ss";
            String tomorrow = Common.dateFormat(Common.addDate(Common.getDate(), Calendar.DATE, 1), format) + '\'';

            if (range == 1)
                where = " dateAdded between '" + Common.dateFormat(Common.getDate(), format) + "' and '" + tomorrow;
            else if (range == 2)
                where = "dateAdded between '" + Common.dateFormat(Common.addDate(Common.getDate(), Calendar.DATE, -7), format) + "' and '" + tomorrow;
            else if (range == 3)
                where = "dateAdded between '" + Common.dateFormat(Common.addDate(Common.getDate(), Calendar.MONTH, -1), format) + "' and '" + tomorrow;
            else if (range == 4)
                where = "dateAdded between '" + Common.dateFormat(Common.addDate(Common.getDate(), Calendar.YEAR, -1), format) + "' and '" + tomorrow;
        }

        // only show the ReportPublished and  SystemError in the log

        where += (Common.isBlank(where) ? "" : " and ") + "(status='ReportPublished' or status='SystemError')";

        GridResults gridResults = null;
        try {
            gridResults = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(getNamespace()), where);
            gridResults.executeQuery();
            gridResults.savePreferences();
        }
        catch (Exception e) {
            logger.error("Problem getting Report Log grid results - {}", PivotalException.getErrorMessage(e));
        }
        return gridResults;
    }


}
