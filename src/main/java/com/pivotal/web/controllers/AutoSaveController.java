/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.AutoSaveEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.pivotal.system.hibernate.utils.HibernateUtils.*;

/**
 * Manages auto save functions
 *
 */
@Authorise
@Controller
@RequestMapping(value = "/autosave")
public class AutoSaveController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AutoSaveController.class);

    /**
     * Saves a snapshot of the form to the database
     *
     * @param request  Request from client
     * @param template Template to save the data for
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/{template}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSave(HttpServletRequest request, @PathVariable(value = "template") String template) {
        return autoSave(request, template, null);
    }

    /**
     * Saves a snapshot of the form to the database
     *
     * @param request  Request from client
     * @param template Template to save the data for
     * @param id       Id of entity being auto saved
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/{template}/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSave(HttpServletRequest request, @PathVariable(value = "template") String template, @PathVariable(value = "id") Integer id) {

        logger.debug("Save");
        JsonResponse retValue = new JsonResponse();

        if (UserManager.getCurrentUser() != null && !Common.isBlank(template)) {

            if (id == null) id = -1;

            // Delete past auto saves for this object

            autoSaveRemove(template, id);

            // Add new one

            AutoSaveEntity autoSaveEntity = HibernateUtils.getEntity(AutoSaveEntity.class);
            autoSaveEntity.setReferenceId(id);
            autoSaveEntity.setReferenceType(template);
            autoSaveEntity.setUser(UserManager.getCurrentUser());
            autoSaveEntity.setSavedValues(JsonMapper.serializeItem(request.getParameterMap()));
            autoSaveEntity.setTimeAddedNow();
            HibernateUtils.save(autoSaveEntity);
        }

        return retValue;
    }

    /**
     * Initialises the auto save for the settings
     * Returns the refresh period in the information field and
     * indicates if an auto save record exists
     *
     * @param template Template to save the data for
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/init/{template}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSaveInit(@PathVariable(value = "template") String template) {
        return autoSaveInit(template, null);
    }

    /**
     * Initialises the auto save for the settings
     * Returns the refresh period in the information field and
     * indicates if an auto save record exists
     *
     * @param template  The unique page name for the autosave
     * @param id        The id of the record being auto saved
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/init/{template}/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSaveInit(@PathVariable(value = "template") String template, @PathVariable(value = "id") Integer id) {

        logger.debug("Init");

        JsonResponse retValue = new JsonResponse();

        retValue.setInformation(String.valueOf(getSystemSetting(SETTING_APP_GENERAL_AUTOSAVE_PERIOD, SETTING_APP_GENERAL_AUTOSAVE_PERIOD_DEFAULT)));

        if (!Common.isBlank(template)) {
            if (id == null) id = -1;

            AutoSaveEntity autoSaveEntity = HibernateUtils.selectFirstEntity("From AutoSaveEntity where user = ? and referenceType = ? and referenceId = ? order by timeAdded desc", UserManager.getCurrentUser(), template, id);

            if (!Common.isBlank(autoSaveEntity)) {

                // Found old autosave
                retValue.putDataItem("timeAdded", Common.dateFormat(autoSaveEntity.getTimeAdded(), "HH:mm:ss dd MMM yyyy"));
            }
        }
        else
            retValue.setError("No template specified, please contact system administrator");

        return retValue;
    }

    /**
     * Removes the auto save record for the specified record
     *
     * @param template  Template to save data for
     * @param entity    The object record being auto saved
     *
     * @return JsonResponse with outcome of save
     */
    public static JsonResponse autoSaveRemove(String template, Object entity) {

        Integer entityId = null;

        if (entity != null)
            entityId = ClassUtils.invokeMethod(entity, "getId");

        return autoSaveRemove(template, entityId);

    }


    /**
     * Removes the auto save record for a new record
     *
     * @param template  Template to save data for
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/remove/{template}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSaveRemove(@PathVariable(value = "template") String template) {
        return autoSaveRemove(template, null);
    }

    /**
     * Removes the auto save record for the specified record
     *
     * @param template  Template to save data for
     * @param id        The id of the record being auto saved
     * @return JsonResponse containing action status
     */
    @RequestMapping(value = "/remove/{template}/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @ResponseBody
    public static JsonResponse autoSaveRemove(@PathVariable(value = "template") String template, @PathVariable(value = "id") Integer id) {

        logger.debug("Remove");

        JsonResponse retValue = new JsonResponse();

        if (!Common.isBlank(template)) {
            try {
                if (id == null) id = -1;

                HibernateUtils.delete(HibernateUtils.selectEntities("From AutoSaveEntity where user = ? and referenceType = ? and referenceId = ?", UserManager.getCurrentUser(), template, id));
                HibernateUtils.commit();
            }
            catch(Exception e) {
                retValue.setError("Error removing autosave objects %s", PivotalException.getErrorMessage(e));
            }
        }
        else
            retValue.setError("No template specified, please contact system administrator");

        return retValue;
    }

    /**
     * Loads data from the auto save object to the entity
     *
     * @param id id of record to get
     *
     */
    static <T> T restoreAutoSave(String template, Integer id, Class<?> clazz) {

        T entity = null;

        if (!Common.isBlank(template)) {
            if (id==null) id = -1;

            AutoSaveEntity autoSaveEntity = HibernateUtils.selectFirstEntity("From AutoSaveEntity where user = ? and referenceType = ? and referenceId = ? order by timeAdded desc", UserManager.getCurrentUser(), template, id);

            // Remove auto save

            autoSaveRemove(template, id);

            if (!Common.isBlank(autoSaveEntity)) {

                Map<String, List> data = JsonMapper.deserializeIntoMap(autoSaveEntity.getSavedValues(), String.class, List.class);
                entity = HibernateUtils.restoreEntity(clazz, data);
            }
        }

        return entity;
    }
}
