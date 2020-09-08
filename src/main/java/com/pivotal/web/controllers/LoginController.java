/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.ActionEntity;
import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.monitoring.EventMonitor;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.*;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.utils.workflow.WorkflowJob;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.notifications.NotificationManager;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;

/**
 * Provides the display and actions associated with the user login process
 */
@Authorise
@Controller
@RequestMapping(value = {"/", "/login"})
public class LoginController extends AbstractController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoginController.class);

    /**
     * Logs out of the system
     *
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout() {
        logger.info("User {} has logged out", UserManager.getCurrentUserName());
        UserManager.logout();

        // If we got here then we are logging out
        // Redirect to the login page unless we are using SAML authentication or none at all

        if (UserManager.isNoAuthentication() || UserManager.isSAML()) {
            return "login/logout";
        }

        // Default Application need to redirect to login page

        else {
            return "redirect:/login";
        }
    }

    /**
     * loads the test page
     *
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String showTestPage() {
        logger.info("Running test page", UserManager.getCurrentUserName());
        return "/test";
    }

    /**
     * Returns the login form or if security is not enabled, redirects to the task page
     *
     * @param request Web request
     * @param model   Context to populate
     * @param session a {@link javax.servlet.http.HttpSession} object.
     * @return Returns the view to use
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(method = RequestMethod.GET)
    public String getLoginForm(HttpServletRequest request, HttpSession session, Model model) {

        if (UserManager.isUserLoggedIn()) {
            String setting = HibernateUtils.getSystemSetting("login.default.startpage", "dashboard");
            return "redirect:/" + setting;
        }

        // If we are at the root redirect to the login page

        else if (ServletHelper.getRequestInfo(request).isPageName("", "/")) {
            model.asMap().clear();
            return "redirect:/login";
        }

        else {
            String setting = HibernateUtils.getSystemSetting("login.default.startpage", "dashboard");
            ServletHelper.getRequest().getSession().setAttribute("initialpageoverride", setting);
            return "login";
        }
    }

    /**
     * Returns the path to the Register page
     *
     * @return a {@link java.lang.String} object.
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String getRegisterForm() {

        if (UserManager.isUserLoggedIn()) {
            String setting = HibernateUtils.getSystemSetting("login.default.startpage", "dashboard");
            return "redirect:/" + setting;
        }

        else {
            return "login/register";
        }
    }

    /**
     * Returns the login form or if security is not enabled, redirects to the task page
     *
     * @param model   Context to populate
     * @return Returns the view to use
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(value = "/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse register(Model model, @RequestParam Map<String, Object> params) {

        // We're attempting to register - we expect this to be an AJAX call

        JsonResponse returnValue  = new JsonResponse();
        params = Common.cleanUserData(params);

        if (UserManager.isUserLoggedIn()) {
            returnValue.setError(I18n.translate("login.register.error.already_logged_in"));

        }
        else if (UserManager.isSimple() || UserManager.isNoAuthentication()) {

            String password = EncryptionUtils.decryptPassword((String)params.get("input_password_encrypt"), ServletHelper.getRequest().getRequestedSessionId());

            String emailAddress = (String)params.get("email");
            if (!isBlank(emailAddress)) {

                // check for existing email

                if (HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = lower(?)", emailAddress) == null) {
                    UserEntity userEntity = HibernateUtils.getEntity(UserEntity.class);
                    userEntity.setEmail(emailAddress);
                    userEntity.setFirstname((String) params.get("firstname"));
                    userEntity.setLastname((String) params.get("lastname"));
                    userEntity.setPassword(Common.getMD5String(password));
                    userEntity.setPhoneNumber((String) params.get("phonenumber"));
                    if (!HibernateUtils.save(model, userEntity)) {
                        returnValue.setError(ServletHelper.getErrorsAsString(model));
                    }
                    else {
                        logger.debug("Registered user email = {}", userEntity.getEmail());
                        WorkflowJob workflowJob = new WorkflowJob("USER_REGISTRATION");
                        workflowJob.putSetting("User", userEntity);
                        WorkflowHelper.executeWorkflow(workflowJob);
                    }
                }
                else {
                    returnValue.setError(I18n.translate("login.register.error.email_exists"));
                    logger.debug("User attempted to register with existing email {}", emailAddress);
                }
            }
        }
        else {
            logger.error("Registration attempted when authentication not set to simple");
            returnValue.setError(I18n.translate("login.register.wrong.authentication"));
        }

        return returnValue;
    }

    /**
     * Returns the path to the password reset request page
     *
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/resetrequest", method = RequestMethod.GET)
    public String getResetRequest() {

        if (UserManager.isUserLoggedIn()) {
            String setting = HibernateUtils.getSystemSetting("login.default.startpage", "dashboard");
            return "redirect:/" + setting;
        }

        else {
            return "login/resetrequest";
        }
    }

    /**
     * Processes the password reset request page
     *
     * @param model   Context to populate
     * @return Returns the view to use
     */
    @RequestMapping(value = "/resetrequest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse resetRequestForm(Model model, @RequestParam Map<String, Object> params) {

        // We're attempting to register - we expect this to be an AJAX call

        JsonResponse returnValue  = new JsonResponse();
        params = Common.cleanUserData(params);
        if (UserManager.isUserLoggedIn()) {
            returnValue.setError(I18n.translate("login.register.error.already_logged_in"));

        }
        else if (UserManager.isSimple() || UserManager.isNoAuthentication()) {

            String emailAddress = (String)params.get("email");
            if (!isBlank(emailAddress)) {

                // check for existing email

                if (HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = lower(?)", emailAddress) != null) {
                    WorkflowHelper.executeWorkflow("PASSWORD_RESET_REQUEST");
                }
                else {
                    // Don't show error as will reveal which emails are registered and which aren't
                    // returnValue.setError(I18n.translate("login.register.error.email_doesnt_exist"));

                    logger.debug("User attempted to register with an email that doesn't exist {}", emailAddress);
                }
            }
        }
        else {
            logger.error("Registration attempted when authentication not set to simple");
            returnValue.setError(I18n.translate("login.register.wrong.authentication"));
        }

        return returnValue;
    }

    /**
     * Returns the path to the password reset page
     *
     * @return a {@link java.lang.String} object.
     */
    @RequestMapping(value = "/resetentry/{guid}", method = RequestMethod.GET)
    public String getResetEntryForm(Model model, @PathVariable("guid") String actionGuid) {

        if (UserManager.isUserLoggedIn()) {
            String setting = HibernateUtils.getSystemSetting("login.default.startpage", "dashboard");
            return "redirect:/" + setting;
        }

        else {
            if (!isBlank(actionGuid)) {
                model.addAttribute("actionGuid", actionGuid);
                return "login/resetentry";
            }
            else
                return "login";
        }
    }

    /**
     * Returns the login form or if security is not enabled, redirects to the task page
     *
     * @param model   Context to populate
     * @return Returns the view to use
     */
    @RequestMapping(value = "/resetentry", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse passwordReset(Model model, @RequestParam Map<String, Object> params) {

        // We're attempting to register - we expect this to be an AJAX call

        JsonResponse returnValue  = new JsonResponse();
        params = Common.cleanUserData(params);
        if (UserManager.isUserLoggedIn()) {
            returnValue.setError(I18n.translate("login.register.error.already_logged_in"));

        }
        else if (UserManager.isSimple() || UserManager.isNoAuthentication()) {

            ActionEntity actionEntity = null;

            // check the action entity is ok
            if (!isBlank(params.get("actionguid")))
                actionEntity = HibernateUtils.selectFirstEntity("From ActionEntity where guid = ?", params.get("actionguid"));

            if (actionEntity != null) {

                String emailAddress = actionEntity.getSettings();
                if (!isBlank(emailAddress)) {

                    // check for existing email

                    UserEntity userEntity = HibernateUtils.selectFirstEntity("From UserEntity where lower(email) = lower(?)", emailAddress);

                    if (userEntity != null) {
                        String password = Common.getMD5String(EncryptionUtils.decryptPassword((String)params.get("input_password_encrypt"), ServletHelper.getRequest().getRequestedSessionId()));

                        userEntity.setPassword(password);
                        userEntity.setPreviousPasswords(userEntity.getPreviousPasswords() + ";" + password);
                        if (HibernateUtils.save(userEntity)) {
                            WorkflowHelper.executeWorkflow(new WorkflowJob("USER_PASSWORD_RESET").putSetting("UserEntity", userEntity));
                            returnValue.setCompleted(true);
                        }
                    }
                    else {
                        logger.debug("User attempted to reset password with email that doesn't exist {}", emailAddress);
                    }
                }
            }
            else
                logger.error("Password reset attempted with invalid action guid - {}", params.get("actionguid"));
        }
        else {
            logger.error("Password reset attempted when authentication not set to simple");
        }

        return returnValue;
    }

    /**
     * Manages the posting of login credentials
     * Looks up the user in the LDAP server and test their password
     * against this
     *
     * @return A JSON response to a login request
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse login() {

        // We're attempting to login - we expect this to be an AJAX call

        JsonResponse returnValue = new JsonResponse();

        // Check if we are logging in using the sneak account

        try {
            String error = "";
            if (!UserManager.login()) {
                error = I18n.translate("login.invalid.username.password");
            }

            returnValue = WorkflowHelper.executeWorkflow("login");

            // If we have a login error but nothing from the workflow then use it
            // ie workflow error overrides standard error.
            if (!returnValue.getInError() && !isBlank(error))
                returnValue.setError(error);

        }
        catch (PivotalException e) {
            returnValue.setError(PivotalException.getErrorMessage(e));
        }
        catch (Exception e) {
            returnValue.setError(I18n.translate("login.error.failed.message"));
        }

        return returnValue;
    }

    /**
     * Allows a user to change their current locale
     *
     * @param session The current user session
     * @param locale the locale requested
     * @return A redirect to the login page
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(method = RequestMethod.POST)
    public String changeLocale(HttpSession session, @RequestParam("locale") String locale) {
        session.setAttribute(I18nImplemenation.LOCALE_SETTING, Locale.forLanguageTag(locale));
        return "redirect:/login";
    }

    /**
     * Serves to keep a session alive if the user has agreed to it
     *
     * @param request Request being processed
     * @param session Http session
     * @return Json with the notifications list
     */
    @ResponseBody
    @RequestMapping(value = "keepalive", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonResponse keepAlive(HttpSession session,HttpServletRequest request) {

        JsonResponse returnValue = new JsonResponse();
        EventMonitor.addEvent(EventMonitor.EVENT_TYPE_USER_SESSION_UPDATE, session.getId(), 0);
        returnValue.setCompleted(true);

        return returnValue;

    }

    /**
     * Serves to keep a session alive independently of the Servlet container timeout
     * It will also check for pending notifications in the session object
     *
     * @param request Request being processed
     * @param session Http session
     * @return Json with the notifications list
     */
    @ResponseBody
    @RequestMapping(value = "heartbeat", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public JsonResponse heartbeat(HttpSession session,HttpServletRequest request) {

        JsonResponse returnValue = new JsonResponse();

        // If not to be logged out then set expires

        boolean keepAlive = (request.getParameterMap().containsKey("nologout") && Common.isYes(request.getParameter("nologout")));

        // Check the login user is Expired if so invalidate session

        UserManager.checkCurrentUserExpires();

        // Let the user manager know the user is still alive
        if (UserManager.isUserLoggedIn()) {

            String userStatus = UserManager.heartbeat(keepAlive);

            // Now add on to the response any notifications

            returnValue.putDataItem("notifications", NotificationManager.getSessionNotifications(session));

            returnValue.putDataItem(UserManager.STATUS_LOGGED_OUT, userStatus.equalsIgnoreCase(UserManager.STATUS_LOGGED_OUT));
            returnValue.putDataItem(UserManager.STATUS_TIMEOUT_WARNING, userStatus.equalsIgnoreCase(UserManager.STATUS_TIMEOUT_WARNING));
        }
        else
            returnValue.putDataItem(UserManager.STATUS_LOGGED_OUT, true);

        return returnValue;
    }

    /**
     * Outputs images
     *
     * @param session HTTP Session
     * @param response HTTP Response
     * @param mediaId Passed in mediaId
     * @param params Map of QueryString parameters
     *
     */
    @Authorise(notLoggedIn = true)
    @RequestMapping(value = "/image/{mediaId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    //TODO This allows anyone to see an image if they know the URL, need some sort of security
    public void processImageBrowserAction(HttpSession session, HttpServletResponse response
                                            , @PathVariable(value = "mediaId") String mediaId
                                            , @RequestParam Map<String, Object> params
    ){

        if (!isBlank(mediaId))
              MediaController.writeToStream(session, response, Common.parseInt(mediaId), "");
    }
}
