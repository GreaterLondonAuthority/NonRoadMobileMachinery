/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.nrmm.service.notification.Notification;
import com.pivotal.reporting.reports.Report;
import com.pivotal.reporting.reports.ReportFactory;
import com.pivotal.reporting.scheduler.ScheduleMonitor;
import com.pivotal.system.hibernate.entities.DatasourceEntity;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.Privileges;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.GridResults;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.notifications.NotificationManager;
import org.hibernate.jdbc.Work;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Handles requests for storing and managing tasks
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/task")
public class TaskController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return ScheduledTaskEntity.class;
    }

    /**
     * Serves the report information for a task
     *
     * @param model Model to populate
     * @param id ID of the report
     */
    @RequestMapping(value={"/report"}, method= RequestMethod.GET)
    public void getAddress(Model model, @RequestParam(value = "id") Integer id) {
        Report report= ReportFactory.getReport(HibernateUtils.getEntity(ReportEntity.class, id));
        model.addAttribute("Report", report);
        report.close();
    }

    /** {@inheritDoc} */
    @Override
    protected void addAttributesToModel(Model model, Object entity, Integer id, EditStates editState) {
        ScheduledTaskEntity task = (ScheduledTaskEntity)entity;
        if (task.getReport()!=null) {
            Report report= ReportFactory.getReport(task.getReport());
            model.addAttribute("Report", report);
            report.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeValidation(HttpSession session, HttpServletRequest request, Object entity) {

        // Make sure the task is connected to the system datasource

        ScheduledTaskEntity task = (ScheduledTaskEntity)entity;
        task.setDatasource(HibernateUtils.getEntity(DatasourceEntity.class, DatasourceEntity.SYSTEM_DATASOURCE_INTERNAL));
    }

    /** {@inheritDoc} */
    @Override
    public GridResults getGridData() {

        // Interpolate the schedule type to it's string value

        GridResults results = super.getGridData();
        if (results.getFieldList().hasField("schedType")) {
            for (Map<String, Object> row :  results.getData()) {
                int type=(int)row.get("schedType");
                String value = type + "";
                if (type==ScheduledTaskEntity.SCHED_TYPE_MINUTE) {
                    value = "admin.scheduled_task.schedule.every.x.minutes";
                }
                else if (type==ScheduledTaskEntity.SCHED_TYPE_DAY) {
                    value = "admin.scheduled_task.schedule.every.day";
                }
                else if (type==ScheduledTaskEntity.SCHED_TYPE_WEEK) {
                    value = "admin.scheduled_task.schedule.every.week";
                }
                else if (type==ScheduledTaskEntity.SCHED_TYPE_MONTH) {
                    value = "admin.scheduled_task.schedule.every.month";
                }
                else if (type==ScheduledTaskEntity.SCHED_TYPE_NEVER) {
                    value = "admin.scheduled_task.schedule.never";
                }
                else if (type==ScheduledTaskEntity.SCHED_TYPE_ONCE) {
                    value = "admin.scheduled_task.schedule.once";
                }
                row.put("schedType", I18n.getString(value));
            }
        }

        return results;
    }


    /**
     * Launches the task
     *
     * @param id ID of the task
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @ResponseBody
    @RequestMapping(value = "launch", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonResponse runTask(@RequestParam(value = "id") int id) {
        JsonResponse response = new JsonResponse();
        ScheduledTaskEntity task = HibernateUtils.getEntity(ScheduledTaskEntity.class, id);
        try {
            logger.debug("Launching task {}", id);
            if (task!=null && lockTask(task)) {
                // Send notification
                NotificationManager.addNotification("admin.scheduled_task.launched", Notification.NotificationLevel.Info, Notification.NotificationGroup.Individual, Notification.NotificationType.Application, true);

                ScheduleMonitor.launchTask(task);
            }
        }
        catch (Exception e) {
            response.setError(I18n.getString("system.error.exception", id, PivotalException.getErrorMessage(e)));
            logger.error(response.getError());
        }
        return response;
    }

    /**
     * Attempts to lock the task so that it cannot overlap with other running
     * tasks, scheduled or manually run
     *
     * @param task Scheduled task to lock
     * @return True if the task was successfully locked
     */
    private static boolean lockTask(ScheduledTaskEntity task) {
        boolean returnValue = false;
        if (task!=null) {
            try {
                // We need to lock the table whilst we do this operation to prevent other NRMM servers
                // conflicting with us

                HibernateUtils.getCurrentSession().doWork(new Work() {
                    @Override
                    public void execute(Connection connection) throws SQLException {
                        connection.nativeSQL("lock tables scheduled_task write");
                    }
                });

                // Check to see if it isn't already locked

                List<ScheduledTaskEntity> list=HibernateUtils.selectEntitiesBypassCache("from ScheduledTaskEntity where id=? and locked=false", task.getId());
                if (!Common.isBlank(list)) {
                    task.updateScheduledTaskStatus(true, null, new Date());
                    returnValue = true;
                }
            }
            catch (Exception e) {
                logger.error("Problem locking task - {}", PivotalException.getErrorMessage(e));
            }
            finally {

                // Make sure we release the lock on the table

                try {
                    HibernateUtils.getCurrentSession().doWork(new Work() {
                        @Override
                        public void execute(Connection connection) throws SQLException {
                            connection.nativeSQL("unlock tables");
                        }
                    });
                }
                catch (Exception e) {
                    logger.error("Problem unlocking schedule table - {}", PivotalException.getErrorMessage(e));
                }
            }
        }
        return returnValue;
    }
}
