/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.*;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.utils.workflow.WorkflowJob;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pivotal.system.hibernate.utils.HibernateUtils.getEntity;
import static com.pivotal.system.security.CaseManager.safeGet;
import static com.pivotal.utils.Common.isBlank;
import static com.pivotal.utils.Common.isYes;

@Authorise
@Controller
@RequestMapping("/site")
public class SiteController extends AbstractController implements HandlerExceptionResolver {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SiteController.class);

    private static final String HIDDEN_LOCKED_ID = "~|HIDDEN-LOCKED|~";


    /**
     * This is the entry point for editing a site
     *
     * @param model  Model to populate
     * @param params Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/register", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showRegisterSite(Model model, @RequestParam Map<String, Object> params) {

        // Add the current user to the context

        model.addAttribute(UserManager.CURRENT_USER, UserManager.getCurrentUser());

        // add CaseManager

        model.addAttribute("CaseManager", CaseManager.class);
        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.ADDING);
        model.addAttribute("siteentity", HibernateUtils.getEntity(SiteEntity.class));

        // Site id of last created site
        model.addAttribute("NewSiteId", safeGet(params, "NewSiteId"));

        return "site/register";
    }

    /**
     * This is the entry point for editing a site
     *
     * @param model  Model to populate
     * @param id     Id of record to show
     * @param params Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/edit/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showEditSite(Model model
            , @PathVariable(value = "id") Integer id
            , @RequestParam Map<String, Object> params
    ) {

        // Work out how to get Site Entity

        SiteEntity siteEntity = null;

        if (params != null && params.containsKey(AutoSaveEntity.RESTORE_PARAMETER)) {
            siteEntity = AutoSaveController.restoreAutoSave(safeGet(params, AutoSaveEntity.RESTORE_PARAMETER, ""), id, SiteEntity.class);
        }
        else if (isBlank(id)) {
            siteEntity = getEntity(SiteEntity.class);

        }
        else {

            // Key is site id

            siteEntity = getEntity(SiteEntity.class, id);
        }

        model.addAttribute("siteentity", siteEntity);

        // Add the current user to the context

        model.addAttribute(UserManager.CURRENT_USER, UserManager.getCurrentUser());
        model.addAttribute("NewSiteId", safeGet(params, "NewSiteId"));

        // add CaseManager

        model.addAttribute("CaseManager", CaseManager.class);
        model.addAttribute(AbstractAdminController.EDIT_STATE, isBlank(id) ? AbstractAdminController.EditStates.ADDING : AbstractAdminController.EditStates.EDITING);

        return "site/edit";
    }

    /**
     * This is the entry point for viewing a site
     *
     * @param model  Model to populate
     * @param id     Id of record to show
     * @param params Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/view/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String viewSite(Model model
            , @PathVariable(value = "id") Integer id
            , @RequestParam Map<String, Object> params
    ) {

        SiteEntity siteEntity = getEntity(SiteEntity.class, id);
        model.addAttribute("SiteEntity", siteEntity);
        model.addAttribute("Site", siteEntity);

        // Add the current user to the context

        model.addAttribute(UserManager.CURRENT_USER, UserManager.getCurrentUser());

        model.addAttribute("MachineToOpen", safeGet(params, "m"));

        // add CaseManager

        model.addAttribute("CaseManager", CaseManager.class);
        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.VIEWING);

        return "site/view";
    }

    /**
     * Save a site
     *
     * @param request    Current request
     * @param model      Model
     * @param siteEntity Site to be saved
     * @param params     Request parameter map
     * @return view
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse saveSiteRegister(HttpServletRequest request, Model model
            , @ModelAttribute("siteSaveForm") @Valid SiteEntity siteEntity
            , BindingResult result
            , @RequestParam Map<String, Object> params

    ) {

        JsonResponse returnValue = new JsonResponse();

        params = Common.cleanUserData(params);
        Integer siteId = null;
        if (result.hasErrors()) {

            for (ObjectError error : result.getAllErrors())
                ServletHelper.addError(model, error.toString());
        }
        else {

            AutoSaveController.autoSaveRemove(getAutoSaveTemplate(), siteEntity);

            boolean addingSite = false;

            // Set defaults for a new record

            SiteEntity updatedSite;

            if (isBlank(siteEntity.getId())) {

                addingSite = true;
                updatedSite = getEntity(SiteEntity.class);
                updatedSite.setAddedBy(UserManager.getCurrentUser());
            }
            else {
                updatedSite = getEntity(SiteEntity.class, siteEntity.getId());
            }

            // Merge changes from page into existing entity
            if (updateValue(params, "name")) updatedSite.setName(siteEntity.getName());
            if (updateValue(params, "description")) updatedSite.setDescription(siteEntity.getDescription());
            if (updateValue(params, "startDate")) updatedSite.setStartDate(Common.getTimestamp(Common.parseDate(params.get("startDate").toString())));
            if (updateValue(params, "endDate")) updatedSite.setEndDate(Common.getTimestamp(Common.parseDate(params.get("endDate").toString())));
            if (updateValue(params, "address")) updatedSite.setAddress(siteEntity.getAddress());
            if (updateValue(params, "postcode")) updatedSite.setPostcode(siteEntity.getPostcode());
            if (updateValue(params, "longitude")) updatedSite.setLongitude(siteEntity.getLongitude());
            if (updateValue(params, "latitude")) updatedSite.setLatitude(siteEntity.getLatitude());
            if (updateValue(params, "borough")) updatedSite.setBorough(siteEntity.getBorough());
            if (updateValue(params, "zone")) updatedSite.setZone(siteEntity.getZone());
            if (updateValue(params, "planningAppNumber")) updatedSite.setPlanningAppNumber(siteEntity.getPlanningAppNumber());
            if (updateValue(params, "contactFirstName")) updatedSite.setContactFirstName(siteEntity.getContactFirstName());
            if (updateValue(params, "contactLastName")) updatedSite.setContactLastName(siteEntity.getContactLastName());
            if (updateValue(params, "contactEmail")) updatedSite.setContactEmail(siteEntity.getContactEmail());
            if (updateValue(params, "contactPhoneNumber")) updatedSite.setContactPhoneNumber(siteEntity.getContactPhoneNumber());

            // Update site contact if not entered
            if (isBlank(updatedSite.getContactEmail()) && isBlank(updatedSite.getContactPhoneNumber())) {
                updatedSite.setContactEmail(UserManager.getCurrentUser().getEmail());
                updatedSite.setContactPhoneNumber(UserManager.getCurrentUser().getPhoneNumber());
                updatedSite.setContactFirstName(UserManager.getCurrentUser().getFirstname());
                updatedSite.setContactLastName(UserManager.getCurrentUser().getLastname());
            }

            if (updatedSite.getTimeAdded() == null)
                updatedSite.setTimeAddedNow();
            updatedSite.setTimeModifiedNow();
            updatedSite.setModifiedBy(UserManager.getCurrentUser());

            // Proceed to update auxiliary tables/fields

            siteEntity = updatedSite;

            CaseManager.updateDatabase(request, model,siteEntity, result);
            siteId = siteEntity.getId();

            // Add users
            Map<String, String>maUser = new HashMap<>();
            maUser.put("email", safeGet(params, "ma_contactEmail", ""));
            maUser.put("firstname", safeGet(params, "ma_contactFirstName", ""));
            maUser.put("lastname", safeGet(params, "ma_contactLastName", ""));

            Map<String, String>scUser = new HashMap<>();
            scUser.put("email", safeGet(params, "sc_contactEmail", ""));
            scUser.put("firstname", safeGet(params, "sc_contactFirstName", ""));
            scUser.put("lastname", safeGet(params, "sc_contactLastName", ""));

            WorkflowJob workflowJob = new WorkflowJob("SITE_REGISTRATION");
            workflowJob.putSetting("Site", siteEntity);
            if (!isBlank(maUser.get("email")))
                workflowJob.putSetting("maUser", maUser);
            if (!isBlank(scUser.get("email")))
                workflowJob.putSetting("scUser", scUser);
            WorkflowHelper.executeWorkflow(workflowJob);

            returnValue.putDataItem("SiteId", siteId);
        }

        return returnValue;
    }

    /**
     * Save a site
     *
     * @param request    Current request
     * @param model      Model
     * @param siteEntity Site to be saved
     * @param params     Request parameter map
     * @return view
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse saveSite(HttpServletRequest request, Model model
            , @ModelAttribute("siteSaveForm") @Valid SiteEntity siteEntity
            , BindingResult result
            , @RequestParam Map<String, Object> params

    ) {

        JsonResponse returnValue = new JsonResponse();

        Map<String, Object> newParams = new HashMap<>();
        params = Common.cleanUserData(params);
        Integer siteId = Common.parseInt(safeGet(params, "siteId"));
        if (result.hasErrors()) {

            for (ObjectError error : result.getAllErrors()) {
                if (!returnValue.getInError())
                    returnValue.setError(error.toString());
                ServletHelper.addError(model, error.toString());
            }
        }
        else {

            AutoSaveController.autoSaveRemove(getAutoSaveTemplate(), siteEntity);

            // Set defaults for a new record

            SiteEntity updatedSite;

            boolean okToGo = true;
            if (isBlank(siteId) || siteId == 0) {

                updatedSite = getEntity(SiteEntity.class);
                updatedSite.setAddedBy(UserManager.getCurrentUser());
                updatedSite.setTimeAddedNow();
            }
            else {
                updatedSite = getEntity(SiteEntity.class, siteId);
                // check user has access to the site

                if (!updatedSite.checkUserAccess(false)) {
                    returnValue.setError("Update failed as you do not have access to this site");
                    logger.error("User {} doesn't have access to site {}", UserManager.getCurrentUser().getEmail(), siteId);
                    okToGo = false;
                }
            }

            if (okToGo) {
                // Merge changes from page into existing entity
                if (updateValue(params, "name")) updatedSite.setName(siteEntity.getName());
                if (updateValue(params, "description")) updatedSite.setDescription(siteEntity.getDescription());
                if (updateValue(params, "startDate")) updatedSite.setStartDate(Common.getTimestamp(Common.parseDate(params.get("startDate").toString())));
                if (updateValue(params, "endDate")) updatedSite.setEndDate(Common.getTimestamp(Common.parseDate(params.get("endDate").toString())));
                if (updateValue(params, "address")) updatedSite.setAddress(siteEntity.getAddress());
                if (updateValue(params, "postcode")) updatedSite.setPostcode(siteEntity.getPostcode());
                if (updateValue(params, "longitude")) updatedSite.setLongitude(siteEntity.getLongitude());
                if (updateValue(params, "latitude")) updatedSite.setLatitude(siteEntity.getLatitude());
                if (updateValue(params, "borough")) updatedSite.setBorough(siteEntity.getBorough());
                if (updateValue(params, "zone")) updatedSite.setZone(siteEntity.getZone());
                if (updateValue(params, "planningAppNumber")) updatedSite.setPlanningAppNumber(siteEntity.getPlanningAppNumber());
                if (updateValue(params, "contactFirstName")) updatedSite.setContactFirstName(siteEntity.getContactFirstName());
                if (updateValue(params, "contactLastName")) updatedSite.setContactLastName(siteEntity.getContactLastName());
                if (updateValue(params, "contactEmail")) updatedSite.setContactEmail(siteEntity.getContactEmail());
                if (updateValue(params, "contactPhoneNumber")) updatedSite.setContactPhoneNumber(siteEntity.getContactPhoneNumber());

                updatedSite.setTimeModifiedNow();
                updatedSite.setModifiedBy(UserManager.getCurrentUser());

                // Proceed to update auxiliary tables/fields

                CaseManager.updateDatabase(request, model, updatedSite, result);
                siteId = updatedSite.getId();
                newParams.put("NewSiteId", siteId);

                WorkflowJob workflowJob = new WorkflowJob("SITE_UPDATE");
                workflowJob.putSetting("Site", updatedSite);
                WorkflowHelper.executeWorkflow(workflowJob);
            }
        }

        return returnValue;
    }

    /**
     * Adds the user to the database if new or gets existing user
     * based on email address
     *
     * @param userEntity User to be added
     *
     * @return Returns updated userEntity
     */
    private static UserEntity addUser(UserEntity userEntity) {

        UserEntity updatedUser = null;
        if (!isBlank(userEntity.getEmail())) {
            // Check if email is already used
            updatedUser = HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = lower(?)", userEntity.getEmail());

            if (updatedUser == null) {
                // Save what we have
                try {
                    HibernateUtils.save(userEntity);
                    updatedUser = userEntity;
                }
                catch (Exception e) {
                    logger.debug("Error saving site user {}", e.getMessage());
                }
            }
        }
        else
            logger.debug("Site user not saved as no email address was specified");

        return updatedUser;
    }


    /**
     * This is the entry point for editing a site contact
     *
     * @param model     Model to populate
     * @param siteId    site Id user is invited to
     * @param params    Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/contact/{siteId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showSiteUser(Model model
            , @PathVariable(value = "siteId") Integer siteId
            , @RequestParam Map<String, Object> params
    ) {

        model.addAttribute("Saved", isYes(safeGet(params, "Saved")));
        model.addAttribute("WorkflowResult", params.get("WorkflowResult"));

        SiteEntity siteEntity = getEntity(SiteEntity.class, siteId);
        model.addAttribute("SiteEntity", siteEntity);
        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.EDITING);

        return "site/contact";
    }

    /**
     * Save a site
     *
     * @param request    Current request
     * @param model      Model
     * @param params     Request parameter map
     * @return view
     */
    @RequestMapping(value = "/contact", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String saveSiteUser(HttpServletRequest request, Model model
            , @RequestParam Map<String, Object> params

    ) {

        String firstname = Common.cleanUserData(safeGet(params, "siteentity.contactFirstName", ""));
        String lastname = Common.cleanUserData(safeGet(params, "siteentity.contactLastName", ""));
        String email = Common.cleanUserData(safeGet(params, "siteentity.contactEmail", ""));
        String roleName = Common.cleanUserData(safeGet(params, "roleName"));
        Integer siteId = Common.parseInt(safeGet(params, "siteentity.id"));

        JsonResponse workflowResult = SiteEntity.sendInvitation(firstname, lastname, email, roleName, siteId);

        Map<String, Object> newParams = new HashMap<>();
        newParams.put("Saved", true);
        newParams.put("WorkflowResult", workflowResult);

        return showSiteUser(model, siteId, newParams);
    }

    /**
     * Removes the specified element from the system
     * Expects that this is called via an AJAX JSON call
     *
     * @param model Model to use
     * @param table table to be updated
     * @param id Id to be removed
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/delete/{table}/{id}", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse delete(Model model, @PathVariable("table") String table, @PathVariable("id") String id) {
        JsonResponse returnValue = new JsonResponse();
        try {
            if (!isBlank(table) && !isBlank(id)) {
                Object entity = null;

                if ("action".equals(table.toLowerCase())) {

                    entity = HibernateUtils.getEntity(ActionEntity.class, Common.parseInt(id));

                }
                else if ("siteuser".equals(table.toLowerCase())) {

                    List<String> ids = Common.splitToList(id, "_");

                    if (ids.size() == 3)
                        entity = HibernateUtils.selectFirstEntity(String.format("From SiteUsersEntity su where su.primaryKey.site = %d and su.primaryKey.user.id = %d and su.role.id = %d",
                                                                Common.parseInt(ids.get(0)),Common.parseInt(ids.get(1)),Common.parseInt(ids.get(2))));

                    // Need to update all machinery assigned to this user

                    HibernateUtils.executeSQL(String.format("update machinery set admin_user_id = %d where admin_user_id = %d", UserManager.getCurrentUser().getId(), ((SiteUsersEntity)entity).getUser().getId()));
                }

                if (entity != null) {
                    HibernateUtils.delete(entity);
                    HibernateUtils.commit();
                }
                else
                    logger.debug("Unknown entity requested to be deleted ? ?", table, id);
            }
        }
        catch(Exception e) {
            returnValue.setError(PivotalException.getErrorMessage(e));
        }

        return returnValue;
    }

    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
//
        Map<Object, Object> model = new HashMap<Object, Object>();
//        String errorMessage = "";
//
//        if (ex instanceof MaxUploadSizeExceededException) {
//            errorMessage = "File size should be less thqn " +
//                    ((MaxUploadSizeExceededException) ex).getMaxUploadSize() + " byte.";
//        }
//        else {
//
//            errorMessage = "Document load " + PivotalException.getErrorMessage(ex);
//        }
//
//        if (!isBlank(errorMessage)) {
//            model.put("errors", errorMessage);
//        }
//
        String pathToUse = ServletHelper.getPathInfo(request);
//
//        if (pathToUse.endsWith("/"))
//            pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
//
//        pathToUse += "_error";
//
//        if (pathToUse.startsWith("/"))
//            pathToUse = pathToUse.substring(1);
//
//        model.putAll(ServletHelper.getGenericObjects(request, response, false));
//
//        Context velocityContext = VelocityUtils.getVelocityContext();
//        for (Object contextKey : velocityContext.getKeys())
//            model.put(contextKey, velocityContext.get(contextKey.toString()));
//
        ModelAndView modelAndView = new ModelAndView(pathToUse, (Map) model);
//
        return modelAndView;
    }
}
