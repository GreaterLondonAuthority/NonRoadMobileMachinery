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
import com.pivotal.system.security.Privileges;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.utils.JsonMapper;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.util.List;
import java.util.Map;

import static com.pivotal.system.hibernate.utils.HibernateUtils.getEntity;
import static com.pivotal.system.security.CaseManager.safeGet;
import static com.pivotal.utils.Common.isBlank;

@Authorise
@Controller
@RequestMapping(value = "/machinery")
public class MachineryController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MachineryController.class);

    /**
     * This is the entry point for adding a machine
     *
     * @param model  Model to populate
     * @param siteId Site machinery is being added to
     * @param params Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/edit/{siteId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String show(Model model, @PathVariable(value = "siteId") Integer siteId, @RequestParam Map<String, Object> params) {
        return show(model, siteId, null, params);
    }

    /**
     * This is the entry point for editing a machine
     *
     * @param model       Model to populate
     * @param siteId      Site machinery is being added to
     * @param machineryId Machinery being edited
     * @param params      Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/edit/{siteId}/{machineryId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String show(Model model, @PathVariable(value = "siteId") Integer siteId, @PathVariable(value = "machineryId") Integer machineryId, @RequestParam Map<String, Object> params) {

        // Add the current user to the context

        model.addAttribute(UserManager.CURRENT_USER, UserManager.getCurrentUser());

        MachineryEntity machineryEntity = null;
        AbstractAdminController.EditStates editState = null;

        if (ServletHelper.hasErrors(model)) {
            if (params.containsKey("machineryentity"))
                machineryEntity = (MachineryEntity) params.get("machineryentity");
            editState = AbstractAdminController.EditStates.EDITING;
        }
        else {
            if (isBlank(machineryId)) {
                machineryEntity = getEntity(MachineryEntity.class);
                editState = AbstractAdminController.EditStates.ADDING;
            }
            else if (params != null && params.containsKey(AutoSaveEntity.RESTORE_PARAMETER)) {
                machineryEntity = AutoSaveController.restoreAutoSave(safeGet(params, AutoSaveEntity.RESTORE_PARAMETER, ""), machineryId, MachineryEntity.class);
            }
            else {
                machineryEntity = getEntity(MachineryEntity.class, machineryId);
            }
        }

        if (editState == null)
            editState = AbstractAdminController.EditStates.EDITING;

        if (isBlank(machineryId) || (machineryEntity != null && machineryEntity.getSite() != null && machineryEntity.getSite().getId().equals(siteId))) {

            if (!machineryEntity.checkUserAccess()) {
                logger.error("User doesn't have access to edit the machinery id [{}]", machineryId);
            }
            else {
                SiteEntity siteEntity = getSiteFromMachinery(machineryEntity, siteId);
                model.addAttribute("siteentity", siteEntity);
                model.addAttribute("machineryentity", machineryEntity);
                model.addAttribute("CaseManager", CaseManager.class);
                model.addAttribute(AbstractAdminController.EDIT_STATE, editState);
                return "machinery/edit";
            }
        }
        else {
            if (isBlank(machineryEntity))
                logger.error("Requested site [{}] id doesn't match missing machinery ", siteId);
            else
                logger.error("Requested site [{}] id doesn't match machinery site id [{}]", siteId, machineryEntity.getSite().getId());
        }
        return "redirect:/dashboard";

    }

    /**
     * This is the entry point for machinery tabs
     *
     * @param model        Model to populate
     * @param templateName Name of tab to show
     * @param siteId       Site machinery is being added to
     * @param params       Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/tab/{templateName}/{siteId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showTab(Model model, @PathVariable(value = "templateName") String templateName, @PathVariable(value = "siteId") Integer siteId, @RequestParam Map<String, Object> params) {
        return showTab(model, templateName, siteId, null, params);
    }

    /**
     * This is the entry point for machinery tabs
     *
     * @param model        Model to populate
     * @param templateName Name of tab to show
     * @param siteId       Site machinery is being added to
     * @param machineryId  Machinery that is being edited
     * @param params       Map of parameters
     * @return View to use
     */
    @RequestMapping(value = "/tab/{templateName}/{siteId}/{machineryId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showTab(Model model, @PathVariable(value = "templateName") String templateName,
                          @PathVariable(value = "siteId") Integer siteId, @PathVariable(value = "machineryId") Integer machineryId, @RequestParam Map<String, Object> params) {


        MachineryEntity machineryEntity = HibernateUtils.getEntity(MachineryEntity.class, machineryId);
        if (machineryEntity == null)
            machineryEntity = HibernateUtils.getEntity(MachineryEntity.class);

        model.addAttribute("machineryentity", machineryEntity);

        SiteEntity siteEntity = getSiteFromMachinery(machineryEntity, siteId);
        if (siteEntity == null)
            siteEntity = HibernateUtils.getEntity(SiteEntity.class);

        model.addAttribute("siteentity", siteEntity);

        model.addAttribute(UserManager.CURRENT_USER, UserManager.getCurrentUser());
        model.addAttribute("CaseManager", CaseManager.class);
        model.addAttribute(AbstractAdminController.EDIT_STATE, (machineryEntity == null || isBlank(machineryEntity.getId())) ? AbstractAdminController.EditStates.ADDING : AbstractAdminController.EditStates.EDITING);

        if ("machinery_list".equals(templateName)) {
            String extraWhere = "";
            if (UserManager.getCurrentUser().getBorough() != null)
                extraWhere = String.format(" and site.borough.id = %d", UserManager.getCurrentUser().getBorough().getId());

            if (UserManager.getCurrentUser().hasAccess(siteEntity, Privileges.VIEW_ALL_MACHINERY))
                model.addAttribute("MachineryList", HibernateUtils.selectEntities("From MachineryEntity where site = ? " + extraWhere + " order by contractor", siteEntity));
            else
                model.addAttribute("MachineryList", HibernateUtils.selectEntities("From MachineryEntity where site = ? " + extraWhere + " and adminUser = ? order by contractor", siteEntity, UserManager.getCurrentUser()));
        }

        logger.debug("Opening tab " + templateName);
        return "machinery/tab/" + templateName;
    }

    /**
     * Save a machine
     *
     * @param request         Current request
     * @param model           Model
     * @param machineryEntity machine to be saved
     * @param params          Request parameter map
     * @return view
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse save(HttpServletRequest request, Model model
            , @ModelAttribute("machinerySaveForm") @Valid MachineryEntity machineryEntity
            , BindingResult result
            , @RequestParam Map<String, Object> params

    ) {

        JsonResponse returnValue = new JsonResponse();

        Integer siteId = Common.parseInt(safeGet(params, "siteId"));
        Integer machineryId = Common.parseInt(safeGet(params, "machineryId"));

        if (result.hasErrors()) {

            for (ObjectError error : result.getAllErrors())
                ServletHelper.addError(model, error.toString());
        }
        else {
            AutoSaveController.autoSaveRemove(getAutoSaveTemplate(), machineryEntity);

            SiteEntity siteEntity = HibernateUtils.getEntity(SiteEntity.class, siteId);

            if (siteEntity == null) {
                returnValue.setError("Unable to save machinery record as existing site was not specified");
                logger.debug(returnValue.getError());
                ServletHelper.addError(model, returnValue.getError());
            }
            else {

                // Set defaults for a new record

                MachineryEntity updatedMachinery = null;
                String oldMachinery = null;

                if (isBlank(machineryId) || machineryId == 0) {

                    // Make sure user has access to site

                    if (!siteEntity.checkUserAccess(false)) {
                        returnValue.setError("Update failed as you do not have access to the site");
                        logger.error("User {} doesn't have access to site {}", UserManager.getCurrentUser().getEmail(), siteId);
                    }
                    else {
                        updatedMachinery = getEntity(MachineryEntity.class);
                        updatedMachinery.setAddedBy(UserManager.getCurrentUser());
                        updatedMachinery.setTimeAddedNow();
                        updatedMachinery.setAdminUser(UserManager.getCurrentUser());
                    }
                }
                else {
                    updatedMachinery = getEntity(MachineryEntity.class, machineryId);

                    // Make sure user has access to machinery item

                    if (!updatedMachinery.checkUserAccess()) {
                        returnValue.setError("Update failed as you do not have access to the machinery item");
                        logger.error("User {} doesn't have access to machinery item {}", UserManager.getCurrentUser().getEmail(), machineryId);
                        updatedMachinery = null;
                    }
                    else
                        oldMachinery = HibernateUtils.serializeEntity(updatedMachinery);
                }

                if (updatedMachinery != null) {
                    updatedMachinery.setTimeModifiedNow();
                    updatedMachinery.setModifiedBy(UserManager.getCurrentUser());
                    updatedMachinery.setSite(siteEntity);

                    // Merge changes from page into existing entity
                    if (updateValue(params, "type")) updatedMachinery.setType(getLookup(machineryEntity.getType(), params, "type", "machinerytype"));
                    if (updateValue(params, "typeOther")) updatedMachinery.setTypeOther(machineryEntity.getTypeOther());
                    if (updateValue(params, "contractor")) updatedMachinery.setContractor(machineryEntity.getContractor());
                    if (updateValue(params, "startDate")) updatedMachinery.setStartDate(Common.getTimestamp(Common.parseDate(params.get("startDate").toString())));
                    if (updateValue(params, "endDate")) updatedMachinery.setEndDate(Common.getTimestamp(Common.parseDate(params.get("endDate").toString())));
                    if (updateValue(params, "machineId")) updatedMachinery.setMachineId(machineryEntity.getMachineId());
                    if (updateValue(params, "supplier")) updatedMachinery.setSupplier(machineryEntity.getSupplier());
                    if (updateValue(params, "engineManufacturer_input")) updatedMachinery.setEngineManufacturer(safeGet(params, "engineManufacturer_input"));
                    if (updateValue(params, "machineryManufacturer_input")) updatedMachinery.setMachineryManufacturer(safeGet(params, "machineryManufacturer_input"));
                    if (updateValue(params, "powerRating")) updatedMachinery.setPowerRating(machineryEntity.getPowerRating());
                    if (updateValue(params, "typeApprovalNumber")) updatedMachinery.setTypeApprovalNumber(machineryEntity.getTypeApprovalNumber());
                    if (updateValue(params, "euStage")) updatedMachinery.setEuStage(machineryEntity.getEuStage());
                    if (updateValue(params, "retrofitModel"))
                        updatedMachinery.setRetrofitModel(getLookup(machineryEntity.getRetrofitModel(), params, "retrofitModel", "retrofitmodel"));
                    if (updateValue(params, "retrofitModelOther")) updatedMachinery.setRetrofitModelOther(machineryEntity.getRetrofitModelOther());
                    if (updateValue(params, "retrofitId")) updatedMachinery.setRetrofitId(machineryEntity.getRetrofitId());
                    if (updateValue(params, "exemptionReason")) updatedMachinery.setExemptionReason(machineryEntity.getExemptionReason());
                    if (updateValue(params, "exemptionReasonText")) updatedMachinery.setExemptionReasonText(machineryEntity.getExemptionReasonText());
                    if (updateValue(params, "exemptionStatus")) updatedMachinery.setExemptionStatus(machineryEntity.getExemptionStatus());
                    if (updateValue(params, "exemptionId")) updatedMachinery.setExemptionId(machineryEntity.getExemptionId());

                    //                machineryEntity = updatedMachinery;

                    HibernateUtils.getCurrentSession().getTransaction().rollback();

                    try {
                        CaseManager.updateDatabase(request, model, updatedMachinery, null);
                    }
                    catch (Exception e) {
                        logger.debug("Error saving machinery - {}", PivotalException.getErrorMessage(e));
                    }

                    // Check to see if they uploaded files and then decided use them
                    if (Common.isYes(safeGet(params, "UseFiles")))
                        processUploadedFiles(safeGet(params, "machineryfiles_data"), updatedMachinery);

                    WorkflowJob workflowJob = new WorkflowJob("MACHINERY_REGISTRATION");
                    workflowJob.putSetting("Machinery", updatedMachinery);
                    workflowJob.putSetting("oldMachinery", HibernateUtils.deserializeToMap(oldMachinery));
                    workflowJob.putSetting("Site", siteEntity);
                    WorkflowHelper.executeWorkflow(workflowJob);
                    returnValue.putDataItem("MachineryId", updatedMachinery.getId());
                }
            }
        }

        return returnValue;
    }

    private static void processUploadedFiles(String fileDetails, MachineryEntity machineryEntity) {

        String returnValue = "";

        if (!isBlank(fileDetails)) {
            List<Object> asyncList = JsonMapper.deserializeItem("[" + fileDetails.toString() + "]", List.class);
            String externalStorage = HibernateUtils.getUploadedFileLocation();
            for (Object thisFile : asyncList) {

                String fileName = (String) ((Map) thisFile).get("OriginalFilename");
                String tempFileName = (String) ((Map) thisFile).get("TempFilename");
                Integer fileSize = (Integer) ((Map) thisFile).get("Size");

                if (!isBlank(tempFileName)) {
                    File tempFile = new File(tempFileName);
                    if (tempFile.exists()) {

                        // Move tempFileName to external storage location

                        try {

                            String newFilename = externalStorage + "MID-" + machineryEntity.getId() + "-" + Common.getFilenameBody(tempFileName) + "." + Common.getFilenameExtension(fileName);

                            Common.copyFile(tempFileName, newFilename);

                            // Now create database entry

                            MachineryMediaEntity machineryMediaEntity = getEntity(MachineryMediaEntity.class);
                            machineryMediaEntity.setMachinery(machineryEntity);
                            machineryMediaEntity.setMedia(getEntity(MediaEntity.class));

                            machineryMediaEntity.getMedia().setFilename(Common.getFilename(newFilename));
                            machineryMediaEntity.getMedia().setName(Common.getFilenameBody(fileName));
                            machineryMediaEntity.getMedia().setFileSize((int) fileSize);
                            machineryMediaEntity.getMedia().setExtension(Common.getFilenameExtension(fileName));
                            machineryMediaEntity.getMedia().setTimeModifiedNow();
                            machineryMediaEntity.getMedia().setType("registration");

                            // Update MediaEntity

                            if (!HibernateUtils.save(machineryMediaEntity.getMedia()))
                                returnValue += (isBlank(returnValue) ? "\r\n" : "") + "Unable to save media entity";

                            else if (!HibernateUtils.save(machineryMediaEntity))
                                returnValue += (isBlank(returnValue) ? "\r\n" : "") + "Unable to save machinery data entity";

                            else
                                Common.addFileForDeletion(tempFile);

                        }
                        catch (Exception e) {
                            returnValue += (isBlank(returnValue) ? "\r\n" : "") + PivotalException.getErrorMessage(e);
                        }
                    }
                    else {
                        returnValue += (isBlank(returnValue) ? "\r\n" : "") + String.format(I18n.getString("system.error.uploaded_file_missing"), fileName);
                    }
                }
            }
        }

    }

    /**
     * Gets site entity from the machinery record or if not available then
     * uses the site Id
     *
     * @param machineryEntity Machinery record to get site from
     * @param siteId          SiteId to use if can't get from machinery
     * @return SiteEntity
     */
    private SiteEntity getSiteFromMachinery(MachineryEntity machineryEntity, Integer siteId) {

        SiteEntity siteEntity;
        if (machineryEntity != null && !isBlank(machineryEntity.getSite()) && !isBlank(machineryEntity.getSite().getId())) {
            siteEntity = machineryEntity.getSite();
        }
        else
            siteEntity = HibernateUtils.getEntity(SiteEntity.class, siteId);

        return siteEntity;
    }

    /**
     * Save a machine
     *
     * @param model           Model
     * @param machineryEntity machine to be edited
     * @param action          Action (reject or accept)
     * @return view
     */
    @RequestMapping(value = "/exemption/{machineryId}/{action}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String showExemption(Model model, @PathVariable(value = "machineryId") MachineryEntity machineryEntity, @PathVariable(value = "action") String action) {

        model.addAttribute("machineryentity", machineryEntity);
        model.addAttribute("action", action);
        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.EDITING);

        return "machinery/exemption";
    }

    /**
     * Save a machine
     *
     * @param request         Current request
     * @param model           Model
     * @param params          Request parameter map
     * @return view
     */
    @RequestMapping(value = "/exemption", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse saveExemption(HttpServletRequest request, Model model, @RequestParam Map<String, Object> params) {

        JsonResponse returnValue = new JsonResponse();

        params = Common.cleanUserData(params);
        Integer machineryId = Common.parseInt(safeGet(params, "machineryId"));
        MachineryEntity machineryEntity = HibernateUtils.getEntity(MachineryEntity.class, machineryId);
        String action = safeGet(params, "action");
        if (!machineryEntity.getSite().checkUserAccess(true)) {
            logger.error("User {} doesn't have access to site {}", UserManager.getCurrentUser().getEmail(), machineryEntity.getSite().getId());
            returnValue.setError("Update failed as you do not have access to the site");
        }
        else {
            String lookupType = safeGet(params, "lookuptype");

            if (("accept".equalsIgnoreCase(action) && UserManager.getCurrentUser().hasAccess(Privileges.ACCEPT_PENDING_MACHINERY)) ||
                ("reject".equalsIgnoreCase(action) && UserManager.getCurrentUser().hasAccess(Privileges.REJECT_PENDING_MACHINERY))) {
                // Get fields from params and update
                if (updateValue(params, "exemptionStatus")) machineryEntity.setExemptionStatus(safeGet(params, "exemptionStatus"));
                if (updateValue(params, "exemptionStatusReason")) machineryEntity.setExemptionStatusReason(getLookup(params, "exemptionStatusReason", lookupType));
                if (updateValue(params, "exemptionStatusCode")) machineryEntity.setExemptionStatusCode(getLookup(params, "exemptionStatusCode", "exemptioncode"));
                if (updateValue(params, "exemptionStatusDate"))
                    machineryEntity.setExemptionStatusDate(Common.getTimestamp(Common.parseDate(params.get("exemptionStatusDate").toString())));
                if (updateValue(params, "exemptionStatusExpiryDate"))
                    machineryEntity.setExemptionStatusExpiryDate(Common.getTimestamp(Common.parseDate(params.get("exemptionStatusExpiryDate").toString())));
                if (updateValue(params, "exemptionId")) machineryEntity.setExemptionId(safeGet(params, "exemptionId"));

                HibernateUtils.save(machineryEntity);
            }
            else {
                returnValue.setError("Update failed as you do not have the required access to the machinery item");
                logger.debug("User doesn't have access to the action {} for machinery id {}", action, machineryId);
            }
        }
       return returnValue;

    }

    /**
     * Removes the specified element from the system
     * Expects that this is called via an AJAX JSON call
     *
     * @param model Model to use
     * @param id ID of the record to delete
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse delete(Model model, @PathVariable("id") Integer id) {

        JsonResponse returnValue = new JsonResponse();
        try {
            MachineryEntity entity = HibernateUtils.getEntity(MachineryEntity.class, id);
            if (entity == null)
                returnValue.setWarning(I18n.getString("system.error.entity_not_exists"));
            else {

                // Delete associated images
                HibernateUtils.delete(HibernateUtils.selectEntities("select mm.media from MachineryMediaEntity mm where mm.machinery = ?", entity));
                HibernateUtils.executeSQL("delete from machinery_media mm where mm.machinery_id = " + entity.getId());

                HibernateUtils.delete(entity);
                HibernateUtils.commit();
            }
        }
        catch (Exception e) {
            returnValue.setError(PivotalException.getErrorMessage(e));
        }

        return returnValue;
    }

    /**
     * Removes the specified element from the system
     * Expects that this is called via an AJAX JSON call
     *
     * @param model Model to use
     * @param id ID of the record to delete
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value = "/offsite/{id}", method = RequestMethod.POST)
    @ResponseBody
    public JsonResponse offiste(Model model, @PathVariable("id") Integer id) {

        JsonResponse returnValue = new JsonResponse();
        try {
            MachineryEntity entity = HibernateUtils.getEntity(MachineryEntity.class, id);
            if (entity == null)
                returnValue.setWarning(I18n.getString("system.error.entity_not_exists"));
            else {
                if (!entity.getSite().checkUserAccess(false)) {
                    logger.error("User {} doesn't have access to site {}", UserManager.getCurrentUser().getEmail(), entity.getSite().getId());
                    returnValue.setError("Update failed as you do not have access to the site");
                }
                else {
                    if (UserManager.getCurrentUser().hasAccess(Privileges.OFFSITE_MACHINERY)) {
                        entity.setEndDate(Common.getTimestamp());
                        HibernateUtils.commit();
                    }
                }
            }
        }
        catch (Exception e) {
            returnValue.setError(PivotalException.getErrorMessage(e));
        }

        return returnValue;
    }
}
