/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.entities.ReportTextEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VelocityUtils;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the administration of Report Text/paragraphs
 *
 */
@Authorise
@Controller
@RequestMapping("/admin/report_text")
public class ReportTextController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportTextController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        logger.debug("Getting class");
        return ReportTextEntity.class;
    }

    /**
     * This is the entry point for imagebrowser actions
     *
     * @param model       Model to populate
     * @param request     Request being processed
     * @param action      action to perform
     * @param params      Map of QueryString parameters
     *
     * @return View to use
     */
    @RequestMapping(value = "/imagebrowser/{action}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object processImageBrowserAction(Model model, HttpServletRequest request
            , @PathVariable(value = "action") String action
            , @RequestParam Map<String, Object> params
    ) {

        if ("read".equalsIgnoreCase(action)) {

            List<Map<String, String>> returnValue = new ArrayList<>();

            Map<String, Object> values = new HashMap<>();
            values.put("fileExtensions", Common.splitToList("bmp,gif,jpg,jpeg,png", ","));

            List<MediaEntity> mediaEntityList = HibernateUtils.selectEntities("from MediaEntity where lower(type) = '" + MediaEntity.TYPE_REPORTTEXT_FILE + "' and lower(extension) in (:fileExtensions)", values);

            if (!Common.isBlank(mediaEntityList) && mediaEntityList.size() > 0) {
                for (MediaEntity mediaEntity : mediaEntityList) {
                    Map<String, String> imageValues = new HashMap<>();
                    imageValues.put("url", ServletHelper.getFullAppPath() + "/image/" + mediaEntity.getId());
                    imageValues.put("name", mediaEntity.getName());
                    imageValues.put("id", String.valueOf(mediaEntity.getId()));
                    imageValues.put("tag", "Media");

                    returnValue.add(imageValues);
                }
            }

            return returnValue;
        }
        else if ("delete".equalsIgnoreCase(action)) {
            String[] asTmp = CaseManager.safeGet(params, "src").split("/");
            if (asTmp.length > 1) {

                // Get the mediaId (last element in src path)
                Integer mediaId = Common.parseInt(asTmp[asTmp.length - 1]);
                HibernateUtils.delete(HibernateUtils.getEntity(MediaEntity.class, mediaId));
                HibernateUtils.commit();
            }
        }

        return null;
    }

    /**
     * This is the entry point for imagebrowser file upload
     *
     * @param model       Model to populate
     * @param request     Request being processed
     * @param files       Files to upload
     * @param params      Map of QueryString parameters
     *
     * @return View to use
     */
    @RequestMapping(value = "/imagebrowser/upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Object processImageBrowserUpload(Model model, HttpServletRequest request
            , @RequestParam(value = "file", required = false) List<MultipartFile> files
            , @RequestParam Map<String, Object> params
    ) {

        Map<String, String> returnValue = new HashMap<>();

        if (files.size() > 0) {

            for (MultipartFile thisFile : files) {

                if (thisFile != null) {

                    try {
                        logger.debug("Adding file " + thisFile.getOriginalFilename());

                        MediaEntity mediaEntity = HibernateUtils.getEntity(MediaEntity.class);
                        mediaEntity.setFilename(thisFile.getOriginalFilename());
                        if (Common.isBlank(mediaEntity.getName()))
                            mediaEntity.setName(Common.getFilenameBody(thisFile.getOriginalFilename()));

                        mediaEntity.setFileSize((int) thisFile.getSize());
                        mediaEntity.setExtension(Common.getFilenameExtension(thisFile.getOriginalFilename()));
                        mediaEntity.setTimeModifiedNow();
                        mediaEntity.setTimeAddedNow();
                        mediaEntity.setType(MediaEntity.TYPE_REPORTTEXT_FILE);

                        if (HibernateUtils.save(model, mediaEntity))
                            if (CaseManager.addMediaFile(mediaEntity, thisFile))
                                HibernateUtils.commit();

                        if (returnValue.size() == 0)
                            returnValue.put("link", String.format("%s/image/%d", request.getContextPath(), mediaEntity.getId()));
                    }
                    catch (Exception e) {
                        logger.debug("Error uploading file {} - {}", thisFile.getName(), PivotalException.getErrorMessage(e));
                    }
                }
            }
        }
        else
            logger.info("Files not uploaded as files list is empty");

        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public void beforeSave(HttpSession session, HttpServletRequest request, Model model, Object entityObject, Integer id, EditStates editState, BindingResult result) {

        ReportTextEntity entity = (ReportTextEntity)entityObject;

        // Make sure the report text is correct

        // Replace %24 with $

        entity.setText(entity.getText().replaceAll("\\%24","\\$"));

//        if (entity.getCaseStages().size() == 1 && entity.getCaseStages().toArray()[0] == null)
//            entity.setCaseStages(null);
    }


    /**
     * Adds any controller specific objects to the model
     * It should be overridden for this to happen
     *
     * @param model     Model to add to
     * @param entity    Entity currently under edit
     * @param id        The ID of the entity under edit
     * @param editState The type of edit
     */
    protected void addAttributesToModel(Model model, Object entity, Integer id, EditStates editState) {
        // add CaseManager to allow getting of lookup lists

        model.addAttribute("CaseManager", CaseManager.class);
        VelocityUtils.addConstants(model, ReportTextEntity.class);
    }
}
