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
import com.pivotal.system.hibernate.entities.SettingsEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import com.pivotal.web.utils.ThemeManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles all the themes
 */
@Authorise
@Controller
@RequestMapping(value = {"/admin/theme"})
public class ThemeController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ThemeController.class);

    /**
     * Returns the list of themes
     *
     * @param model   Context to fill
     * @return a {@link String} object.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public String close(Model model) {
        model.addAttribute("CloseWindow", true);
        return "admin";
    }

    /**
     * Returns the list of themes
     *
     * @param model   Context to fill
     * @return a {@link String} object.
     */
    @RequestMapping(value = {"/", "/list"}, method = RequestMethod.GET)
    public String showThemes(Model model) {
        model.addAttribute("CustomThemes", ThemeManager.getManagedThemes());
        return "admin";
    }

    /**
     * Removes the specified theme
     *
     * @param name    Name of the theme to remove
     * @return a {@link String} object.
     */
    @RequestMapping(value = "/remove", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse removeTheme(@RequestParam(value = "name") String name) {
        ThemeManager.deleteTheme(name);

        // If we have just deleted the current default theme, then reset it to to the system default

        SettingsEntity defaultTheme = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_THEME);
        if (defaultTheme.getValue()!=null && Common.doStringsMatch(defaultTheme.getValue(), name)) {
            HibernateUtils.delete(defaultTheme);
            HibernateUtils.commit();
        }
        return new JsonResponse();
    }

    /**
     * Sets the default theme
     *
     * @param name    Name of the theme to remove
     * @return a {@link String} object.
     */
    @RequestMapping(value = "/set", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse setTheme(@RequestParam(value = "name") String name) {
        SettingsEntity defaultTheme = HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_THEME);
        defaultTheme.setValue(name);
        HibernateUtils.save(defaultTheme);
        return new JsonResponse();
    }

    /**
     * Returns the form for entering settings values
     *
     * @param model   Context to fill
     * @param name    Name of the theme to edit
     * @return a {@link String} object.
     */
    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String addTheme(Model model, @RequestParam(value = "name", required = false) String name) {

        // Work out the themes

        List<Map<String, String>> themes = new ArrayList<>();
        List<MediaEntity> list = ThemeManager.getManagedThemes();
        if (!Common.isBlank(list)) {
            for (MediaEntity tmp : list) {
                themes.add(Common.getMapFromPairs("text", tmp.getName(), "value", tmp.getName()));
            }
        }
        model.addAttribute("Themes", themes);
        model.addAttribute("SelectedTheme", name);

        // Tell the view we are editing

        model.addAttribute(AbstractAdminController.EDIT_STATE, AbstractAdminController.EditStates.ADDING);
        return "admin";
    }

    /**
     * Loads the theme
     * We can't use Jackson to do the JSON conversion here because the file upload will
     * cause a "save this file" action on IE so the data has to be returned as content
     * type text
     *
     * @param model       Model to use
     * @param request     Web request
     * @param name        Name to give the theme
     * @param description Description of the theme
     * @return a {@link String} object.
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String updateTheme(Model model, DefaultMultipartHttpServletRequest request, @RequestParam(value = "name") String name, @RequestParam(value = "description", required = false) String description) {

        // Check for buffoonary

        MultipartFile filePart = request.getFile("file");
        if (filePart.isEmpty()) {
            logger.error("Theme file is empty");
        }

        // Copy the uploaded stream to a temporary file

        else {
            File tmpFile = new File(Common.getTemporaryDirectory() + File.separator + filePart.getOriginalFilename());
            try {
                filePart.transferTo(tmpFile);

                // Now load it into the database

                ThemeManager.updateTheme(tmpFile, name, description);
            }
            catch (Exception e) {
                logger.error("Problem uploading theme file - {}", PivotalException.getErrorMessage(e));
                model.addAttribute("Error", "");
            }
            finally {
                Common.deleteDir(tmpFile);
            }
        }

        // Respond to the pinned indicator

        model.addAttribute(AbstractAdminController.ATTRIBUTE_PINNED, ServletHelper.parameterExists(AbstractAdminController.ATTRIBUTE_PINNED));
        if (!request.getParameterMap().containsKey(AbstractAdminController.ATTRIBUTE_PINNED)) {
            model.addAttribute("CloseWindow", true);
            return "admin";
        }
        else {
            return "redirect:" + request.getRequestURL().toString();
        }
    }

    /**
     * Streams the default theme back to the user
     * @param response     Web response
     *
     * @return Streams theme or null if problem
     */
    @RequestMapping(value = "/default", method = RequestMethod.GET)
    @ResponseBody
    public String getDefaultTheme(HttpServletResponse response) {

        try {
            File tmpFile = ThemeManager.getDeployedTheme(HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT);
            String filename = HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT + ".zip";
            response.setContentType(ServletHelper.getServletContext().getMimeType(filename));
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            response.setHeader("Content-Description", filename);
            Common.pipeInputToOutputStream(tmpFile, response.getOutputStream());
            Common.deleteDir(tmpFile);
        }
        catch (Exception e) {
            logger.error("Problem getting default theme - {}", PivotalException.getErrorMessage(e));
        }
        return null;
    }

}
