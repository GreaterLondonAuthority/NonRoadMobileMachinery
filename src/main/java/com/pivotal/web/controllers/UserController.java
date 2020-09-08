/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.EncryptionUtils;
import com.pivotal.utils.I18n;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.Initialisation;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;

/**
 * Handles requests for storing and managing preferences
 * For the most part, all the requests made to this controller will be
 * from an AJAX source and will not expect a return value
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/user")
public class UserController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return UserEntity.class;
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

        // Don't send the password to the client, it doesn't exist in a displayable form.
        UserEntity userEntity = (UserEntity) model.asMap().get(getEntityClass().getSimpleName());
        userEntity.setPassword(null);
        model.addAttribute("UserManager",UserManager.class);
        model.addAttribute("CaseManager",CaseManager.class);
    }

    /** {@inheritDoc} */
    @Override
    public void beforeSave(HttpSession session, HttpServletRequest request, Model model, Object entityObject, Integer id, EditStates editState, BindingResult result) {

        // If no errors, adjust the password if we have one

        UserEntity entity = (UserEntity)entityObject;
        if (!result.hasErrors()) {

            // Get hash of entered password

            String passwordFromForm = entity.getPassword();

            // These are the scenarios:
            // 1. empty password - adding: we md5 the empty string and set it on entity
            // 2. empty password - editing: find current password and set that on entity - user didn't replace.
            // 3. non-empty password - add/edit: decrypt, decode, set on entity.


            // If the user supplied password is empty then we need to
            if (isBlank(passwordFromForm)) {
                if (editState.equals(EditStates.ADDING)) {
                    logger.debug("Creating user {} with blank password", entity.getEmail());
                    String md5BlankPassword = Common.getMD5String(StringUtils.EMPTY);
                    entity.setPassword(md5BlankPassword);
                }
                else {
                    // editing? we need to put the existing password back on the entity:
                    Query query = HibernateUtils.createSQLQuery("select password from users where id = ?");
                    query.setInteger(0, id);
                    String existingPassword = (String) query.uniqueResult();
                    logger.debug("Updating user {} with existing password", entity.getEmail());
                    entity.setPassword(existingPassword);
                }
            }
            else {
                // non-empty password, scenario 3 - decrypt, decode, hash.
                String decodedDecryptedPassword = EncryptionUtils.decryptPassword(passwordFromForm, request.getRequestedSessionId());
                String hashOfEnteredPassword = Common.getMD5String(decodedDecryptedPassword);
                logger.debug("Updating user {} with new password {}", entity.getEmail(), decodedDecryptedPassword);
                entity.setPassword(hashOfEnteredPassword);
            }

            // Get access privileges from form

            Map<String, String>formPrivs = UserManager.extractPrivilegesFromForm(request);

//            entity.setPrivileges(Common.join(formPrivs.keySet(),","));
//            entity.setPrivilegeAccess(Common.join(formPrivs.values(),","));
//
//            logger.debug("Privileges set to {}", entity.getPrivileges());

        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean beforeDelete(Object entity, Integer id, JsonResponse jsonResponse) {
        boolean returnValue = true;
        if (id!=null && UserManager.getCurrentUser()!=null && UserManager.getCurrentUser().getId().intValue()==id.intValue()) {
            jsonResponse.setWarning(I18n.translate("user.error.cannot_delete_self"));
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterSave(Object entity, Integer id) {

        UserEntity ent = (UserEntity) entity;

        // check the user we are going to save is may be disabled or expired if that is the case if user is login
        // at the same time invalidate its session

        if (ent.isDisabled() || (!isBlank(ent.getExpires()) && (new Date()).after(ent.getExpires()))) {
            Map<String, HttpSession> sessions = Initialisation.getSessionMap();
            for (Map.Entry<String, HttpSession> session : sessions.entrySet()) {
                UserEntity usr = (UserEntity) session.getValue().getAttribute(UserManager.CURRENT_USER);
                if (!isBlank(usr)) {

                    // verify  both users are same in that case invalidate

                    if (usr.getId().equals(ent.getId())) {
                        String sId = session.getValue().getId();
                        if (sessions.containsKey(sId)) {
                            UserManager.logout(null, sId);
                            sessions.get(sId).invalidate();
                        }
                    }
                }

            }
        }
        else {

            // this will handle the case if user expire time set and he is  login
            // at that time session still have old  expire date that need to update here

            Map<String, HttpSession> sessions = Initialisation.getSessionMap();
            for (Map.Entry<String, HttpSession> session : sessions.entrySet()) {
                UserEntity usr = (UserEntity) session.getValue().getAttribute(UserManager.CURRENT_USER);
                if (!isBlank(usr)) {

                    // verify both users are same in that case

                    if (usr.getId().equals(ent.getId())) {
                        session.getValue().setAttribute(UserManager.CURRENT_USER, ent);
                    }
                }

            }
        }
        // Don't send the password to the client, it doesn't exist in a displayable form.
        ent.setPassword(null);
    }

    /**
     * Shows the users preferences
     *
     * @param model         Model
     * @param userEntity    User To get preferences for
     *
     * @return Name of template to process
     */
    @RequestMapping(value = "/preferences/{userId}", method = RequestMethod.GET)
    public String getPreferences(Model model, @PathVariable(value="userId") UserEntity userEntity) {

        model.addAttribute("userentity", userEntity);
        model.addAttribute("preferences", userEntity.getPreferences());

        return "admin/user/preferences";
    }

    /**
     * Removes user preference
     *
     * @param model         Model
     * @param userEntity    User To get preferences for
     * @param paramName     Parameter to delete
     *
     * @return JSON containing process result
     */
    @RequestMapping(value = "/removepreferences/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse removePreference(Model model, @PathVariable(value="userId") UserEntity userEntity, @RequestParam(value="name") String paramName) {

        JsonResponse returnValue = new JsonResponse();

        if (userEntity == null)
            returnValue.setError("No user specified");
        else if (isBlank(paramName))
            returnValue.setError("No parameter specified");
        else {
            userEntity.getPreferences().remove(paramName);
        }

        return returnValue;
    }
}
