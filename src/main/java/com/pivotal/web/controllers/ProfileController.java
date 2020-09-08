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
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.EncryptionUtils;
import com.pivotal.utils.I18n;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.utils.ThemeManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for managing a User's profile.
 */
@Authorise
@Controller
@RequestMapping(value = {"/profile"})
public class ProfileController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProfileController.class);

    /**
     * Shows the form containing the User's profile, allowing him to edit it.
     *
     * @param  model The model that takes data from the controller and makes it accessible in the view.
     * @return name of the view to display
     */
    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String editForm(Model model) {

        // Get the id and then the User instance from the database so it is fully initialized:
        UserEntity currentUser = UserManager.getCurrentUser();
        UserEntity userEntity = HibernateUtils.getEntity(UserEntity.class, currentUser.getId());

        // Put it where velocity can get to it:
        model.addAttribute("profileUser", userEntity);
        model.addAttribute("Themes", ThemeManager.getThemes());
        model.addAttribute("PersonalTheme", currentUser.getPreferences().isUsingDefault(HibernateUtils.SETTING_DEFAULT_THEME)?null:currentUser.getPreferences().get(HibernateUtils.SETTING_DEFAULT_THEME));
        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.EDITING);

        // Return the view name:
        return "profile";
    }


    /**
     * Handles the Parameters sent from the edit-profile form, updates the User Entity and saves it.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param firstname     Name of the user.
     * @param lastname      Last name of the user.
     * @param telephone     Telephone number.
     * @param email         Email address
     * @param password      new password
     * @param theme         theme to use
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @ResponseBody
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public JsonResponse update(HttpServletRequest request,
           @RequestParam(value = "firstname", required = true) String firstname,
           @RequestParam(value = "lastname", required = true) String lastname,
           @RequestParam(value = "telephone", required = true) String telephone,
           @RequestParam(value = "email", required = true) String email,
           @RequestParam(value = "password", required = false) String password,
           @RequestParam(value = "theme", required = false) String theme) {

        JsonResponse returnValue = new JsonResponse();

        try {


            // Get the id and then the User instance from the database so it is fully initialized:
            UserEntity currentUser = UserManager.getCurrentUser();
            UserEntity userEntity = HibernateUtils.getEntity(UserEntity.class, currentUser.getId());

            if (!userEntity.getEmail().equals(email)) {
                // check for duplicate email
                UserEntity checkUser = HibernateUtils.selectFirstEntity("From UserEntity where email = ?", email);
                if (checkUser != null && checkUser.getId() != null) {
                    returnValue.setError(I18n.translate("profile.edit.error.duplicate_email"));
                }
            }

            if (!returnValue.getInError()) {
                // Set the preference for the theme
                currentUser.getPreferences().put(HibernateUtils.SETTING_DEFAULT_THEME, Common.doStringsMatch(theme, HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT) ? null : theme);

                // Update from request:
                userEntity.setFirstname(Common.cleanUserData(firstname));
                userEntity.setLastname(Common.cleanUserData(lastname));
                userEntity.setPhoneNumber(Common.cleanUserData(telephone));
                userEntity.setEmail(Common.cleanUserData(email));

                // Don't update password unless set.
                if (!Common.isBlank(password)) {
                    password = EncryptionUtils.decryptPassword(password, request.getRequestedSessionId());
                    userEntity.setPassword(Common.getMD5String(password));
                }

                logger.debug("Saving userEntity: {}", userEntity);

                if (!HibernateUtils.save(userEntity)) {
                    returnValue.setError("Unable to save the updated user record.");
                }
            }
        }
        catch (Exception e) {
            logger.error("Error updating profile - {}", PivotalException.getErrorMessage(e));
            JsonResponse jsonResponse = new JsonResponse();
            jsonResponse.setError("Error updating the profile");
        }

        return returnValue;
    }
}
