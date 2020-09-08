/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.utils;

import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.Preferences;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.servlet.ServletHelper;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.velocity.context.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for handling themes in the system
 */
public class ThemeManager {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ThemeManager.class);

    /**
     * Returns a list of all the available themes in the database
     * @return List of them MediaEntities
     */
    public static List<MediaEntity> getManagedThemes() {
        return HibernateUtils.selectEntities("from MediaEntity where type=? order by name", MediaEntity.TYPE_CODE_MEDIA_THEME_TYPE);
    }

    /**
     * Returns a list of all the deployed theme names
     * @return List of theme names
     */
    public static List<String> getThemes() {

        List<String> themes = null;

        // Get a list of the folders and reduce it to just their names

        List<File> themeFolders = Common.listFiles(ServletHelper.getRealPath("/themes"), "[^.].*", true, false);
        if (!Common.isBlank(themeFolders)) {
            themes = new ArrayList<>();
            for (File file : themeFolders) {
                themes.add(file.getName());
            }
        }
        return themes;
    }

    /**
     * This is called during startup normally and is responsible for making sure that
     * the theme packages are in step with the deployed (exploded) versions
     */
    public static void initThemes() {

        // Get the real path to the themes folder and clear them all
        // out except for the default

        File path = new File(ServletHelper.getRealPath("/themes"));
        List<String> dirs = getThemes();
        if (!Common.isBlank(dirs)) {
            for (String dir : dirs) {
                if (!Common.doStringsMatch(dir, HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT)) {
                    Common.deleteDir(path + File.separator + dir);
                }
            }
        }

        // Copy all the themes from the media table into the themes folder

        List<MediaEntity> themes = getManagedThemes();
        if (!Common.isBlank(themes)) {
            for (MediaEntity thisTheme : themes) {

                MediaEntity theme = HibernateUtils.getEntity(MediaEntity.class, thisTheme.getId());

                // If the theme folder exists, delete it and re-create it

                File themeDir = new File(path.getAbsoluteFile() + File.separator + theme.getName());
                Common.deleteDir(themeDir);
                themeDir.mkdir();

                // Copy the zip file to the themes folder

                File tmpFile = CaseManager.getMediaFile(theme);
                try {
                    // Unzip it into the theme folder

                    ZipFile zip = new ZipFile(tmpFile);
                    zip.extractAll(themeDir.getAbsolutePath());
                }
                catch (Exception e) {
                    logger.error("Theme [{}] cannot be deployed - {}", theme.getName(), PivotalException.getErrorMessage(e));
                }
                finally {
                    if (tmpFile.exists()) {
                        tmpFile.delete();
                    }
                }
            }
        }
    }


    /**
     * Adds a theme to the system and deploys it
     * @param zipFile Zip file containing the theme files
     * @param name Name to give the theme
     * @param description Description to assign to the theme
     * @throws Exception Error if there is a problem with the theme
     */
    public static void updateTheme(File zipFile, String name, String description) throws Exception {

        if (Common.isBlank(zipFile)) {
            throw new PivotalException("No zip file specified or file is empty");
        }
        else if (Common.isBlank(name)) {
            throw new PivotalException("No name specified for the theme");
        }
        else {

            // Delete any existing theme with the same name

            name = name.toLowerCase().replaceAll("(?i)[^a-z0-9_]+", "");
            deleteTheme(name);
            MediaEntity theme = new MediaEntity();
            theme.setType(MediaEntity.TYPE_CODE_MEDIA_THEME_TYPE);
            theme.setDescription(Common.isBlank(description)?null:description.trim());
            theme.setExtension("zip");
            theme.setName(name);
            theme.setInternal(true);
            theme.setFilename(zipFile.getName());
            theme.setFileSize((int)zipFile.length());
            theme.setTimeModifiedNow();
            if (HibernateUtils.save(theme)) {
                CaseManager.addMediaFile(theme, zipFile);

                // Create the theme directory

                File path = new File(ServletHelper.getRealPath("/themes"));
                File themeDir = new File(path.getAbsoluteFile() + File.separator + theme.getName());
                Common.deleteDir(themeDir);
                themeDir.mkdir();

                // Now extract the theme

                ZipFile zip = new ZipFile(zipFile);
                zip.extractAll(themeDir.getAbsolutePath());
            }
            else {
                throw new PivotalException("Problem saving theme - check logs");
            }
        }
    }


    /**
     * Zips the content of a theme folder and returns it as a zip file
     * @param name Name to give the theme
     * @return zip file
     * @throws Exception Error if there is a problem with theme
     */
    public static File getDeployedTheme(String name) throws Exception {

        // Check for stupidity

        File returnValue = Common.getTemporaryFile("zip");
        if (Common.isBlank(name)) {
            throw new PivotalException("No name specified for the theme");
        }
        else {

            // Check the theme folder exists

            File path = new File(ServletHelper.getRealPath("/themes/" + name));
            if (!path.exists() || !path.isDirectory()) {
                throw new PivotalException("The theme doesn't exist or is not a theme folder");
            }
            else {

                // Now zip the contents of the folder

                ZipFile zip = new ZipFile(returnValue);
                ZipParameters params = new ZipParameters();
                params.setIncludeRootFolder(false);
                params.setReadHiddenFiles(false);
                zip.createZipFileFromFolder(path, params, false, 0);
            }
        }
        return returnValue;
    }

    /**
     * Deletes the theme from the database and removes the local file
     * @param name Name of the theme to remove
     */
    public static void deleteTheme(String name) {
        if (!Common.isBlank(name)) {
            HibernateUtils.delete(HibernateUtils.selectEntities("from MediaEntity where type=? and name=?", MediaEntity.TYPE_CODE_MEDIA_THEME_TYPE, name));
            HibernateUtils.commit();
            File path = new File(ServletHelper.getRealPath("/themes/" + name));
            Common.deleteDir(path);
        }
    }

    /**
     * Sets the theme to use for the current user
     * @param context Context to populate
     */
    public static void setTheThemeToUse(Context context) {
        String theme = HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT;
        if (UserManager.getCurrentUser()!=null) {
            Preferences<Object> preferences = UserManager.getCurrentUser().getPreferences();
            preferences.getDefaults().put(HibernateUtils.SETTING_DEFAULT_THEME, HibernateUtils.getSystemSetting(HibernateUtils.SETTING_DEFAULT_THEME, HibernateUtils.SETTING_DEFAULT_THEME_DEFAULT));

            // Check to see if the theme exists and if not, switch back to the default

            String tmp = (String)preferences.get(HibernateUtils.SETTING_DEFAULT_THEME);
            if (!Common.isBlank(tmp) && (new File(ServletHelper.getRealPath("/themes/" + tmp)).exists())) {
                theme = tmp;
            }
        }
        context.put(UserManager.CURRENT_USER_THEME, theme);
    }


}
