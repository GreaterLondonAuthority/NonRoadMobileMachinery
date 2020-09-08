/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import com.google.common.collect.Maps;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a one stop shop for translating strings within the application
 * using I18N bundles
 * It attempts to use the most appropriate locale depending upon how it
 * has been invoked and also allows for bundles to be non-cached for
 * development purposes
 * This class is exposed as a Velocity Directive #I18N(message, params)
 * using the method translate(locale, XXXX, YYYY)
 */
public class I18n {

    // Get access to a logger for when we cannot load the resolver
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(I18n.class);

    // The actual I18n resolver

    /**
     * Gets the locale from the current request attached to this thread
     *
     * @return Locale or default if not defined in the request
     */
    public static Locale getLocale() {
            return I18nImplemenation.getLocale();
    }

    /**
     * Gets the locale string from the current request attached to this thread
     *
     * @return Locale string or default if not defined in the request
     */
    @SuppressWarnings("unused")
    public static String getLocaleString() {

        String localeString;

        localeString = I18nImplemenation.getLocale().toString();

        return localeString.replaceAll("_", "-");
    }

    /**
     * Gets the locale using the current context
     *
     * @param context Context to use
     * @return Locale
     */
    public static Locale getLocale(Context context) {

            // Convert the context to the map
            Map<String, Object> map = Maps.newHashMap();
            for (Object key : context.getKeys()) {
                map.put((String) key, context.get((String) key));
            }

            return I18nImplemenation.getLocale(map);
    }

    /**
     * Gets the locale using the specified request object
     * It will try the session first and then the actual request
     * This allows the user to specify the locale rather than using
     * the locale defined by the browser which may or may not be
     * accurate
     * The method will always return a locale even if it has to fall
     * back on whatever the server locale for the JVM is
     *
     * @param request Request to use
     * @return Locale
     */
    @SuppressWarnings("unused")
    public static Locale getLocale(HttpServletRequest request) {
            return I18nImplemenation.getLocale(request);
    }

    /**
     * Translates the string using the locale attache3d to this thread and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @return Translated string
     */
    public static String getString(String stringToTranslate) {
        return translate(stringToTranslate);
    }

    /**
     * Translates the string using the locale attache3d to this thread and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @param nullIfNotFound    True if null should be returned if not found
     * @return Translated string
     */
    public static String getString(String stringToTranslate, boolean nullIfNotFound) {
        return translate(stringToTranslate, nullIfNotFound);
    }

    /**
     * Translates the string using the locale attache3d to this thread and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @param parameters        Optional array of parameters to use
     * @return Translated string
     */
    public static String getString(String stringToTranslate, Object... parameters) {
            return I18nImplemenation.getString(stringToTranslate, parameters);
    }

    /**
     * Translates the string using the locale and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @param parameters        Optional array of parameters to use
     * @param locale            a {@link java.util.Locale} object.
     * @return Translated string
     */
    public static String getString(Locale locale, String stringToTranslate, Object... parameters) {
            return I18nImplemenation.getString(locale, stringToTranslate, parameters);
    }

    /**
     * Translates the string using the locale and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param locale            The locale to use
     * @param stringToTranslate String to getString
     * @param nullIfNotFound    True if null should be returned if not found
     * @return Translated string
     */
    public static String getString(Locale locale, String stringToTranslate, boolean nullIfNotFound) {
        return getString(locale, stringToTranslate, nullIfNotFound, new Object[]{});
    }

    /**
     * Translates the string using the locale and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param locale            The locale to use
     * @param stringToTranslate String to getString
     * @param nullIfNotFound    True if null should be returned if not found
     * @param parameters        Optional array of parameters to use
     * @return Translated string
     */
    public static String getString(Locale locale, String stringToTranslate, boolean nullIfNotFound, Object... parameters) {
            return I18nImplemenation.getString(locale, stringToTranslate, nullIfNotFound, parameters);
    }

    /**
     * Translates the string using the locale attache3d to this thread and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @return Translated string
     */
    public static String translate(String stringToTranslate) {
            return I18nImplemenation.translate(stringToTranslate);
    }

    /**
     * Translates the string using the locale attache3d to this thread and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * @param stringToTranslate String to getString
     * @param nullIfNotFound    True if null should be returned if not found
     * @return Translated string or null if it doesn't exist
     */
    public static String translate(String stringToTranslate, boolean nullIfNotFound) {
            return I18nImplemenation.translate(stringToTranslate, nullIfNotFound);
    }

    /**
     * Return a nicely formatted of how long ago a given date is, in days, hours, minutes and seconds.
     *
     * @param startDate First date to compare. Must be in the past.
     * @return The string
     */
    public static String diffDateWords(Date startDate) {
        int[] unitTypes = new int[]{Calendar.DATE, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND};
        return diffDateWords(startDate, new Date(), unitTypes, false);
    }

    /**
     * Return a nicely formatted string of the difference between two dates, in days, hours, minutes and seconds.
     * The display is truncated after the first difference is found i.e. for most situations, the user
     * will not be interested what the hours are if it is greater than a day and similarly if the
     * difference is greater than an hour then they are probably not interested in the minutes
     *
     * @param startDate First date to compare. Must be before endDate.
     * @param endDate   Second date to compare. Must be after startDate.
     * @param unitTypes Array of units to display in descending order
     * @param showAll   If true all are shown otherwise first one
     * @return The string
     */
    public static String diffDateWords(Date startDate, Date endDate, int[] unitTypes, boolean showAll) {

        StringBuilder result = new StringBuilder();
        if (startDate != null && endDate != null && !Common.isBlank(unitTypes)) {
            boolean firstEntry = true;
            long diff;

            for (int unitType : unitTypes) {
                if ((diff = Common.diffDate(startDate, endDate, unitType)) > 0) {
                    // If it's not the first word, separate it with a comma and a space.
                    if (!firstEntry) {
                        result.append(", ");
                    }
                    else {
                        firstEntry = false;
                    }

                    // Find the right word to use for this date unit and append it to the string.
                    result.append(String.format("%d %s", diff, translate(Common.getDateUnit(unitType, diff != 1))));

                    // And subtract what we've just put on the string from the endDate;
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(endDate);
                    calendar.add(unitType, (int)-diff);
                    endDate = calendar.getTime();
                    if (!showAll) break;
                }
            }
        }
        if (result.length() == 0) {
            // If we haven't put out any text, it was less than a second.
            result.append(translate("date.less_than_a_second"));
        }
        return result.toString();
    }

    /**
     * Return a nicely formatted string of the difference between two dates, in days, hours, minutes and seconds.
     *
     * @param startDate First date to compare. Must be before endDate.
     * @param endDate   Second date to compare. Must be after startDate.
     * @param units     Maximum number of units to display - i.e. how much detail to show. Set to 0 to show all units.
     * @return The string
     */
    @SuppressWarnings("unused")
    public static String diffDateWords(Date startDate, Date endDate, int units) {

        int[] unitTypes = new int[]{Calendar.DATE, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND};
        return diffDateWords(startDate, endDate, unitTypes, true);

    }

    /**
     * Reads the UK to US translations file and creates a new US locale file
     * with the neccersary translations made automatically
     */
    public static void createUsTranslations() {

        // Get the word translations and defaults into properties

        OutputStream out = null;
        InputStream tx = I18n.class.getResourceAsStream("/i18n/uk-to-us.properties");
        InputStream source = I18n.class.getResourceAsStream("/i18n/app.properties");
        if (tx != null) {
            Properties txProps = new Properties();
            try {
                txProps.load(tx);

                // If we actually have any translations

                if (!txProps.isEmpty()) {
                    Properties translations = new Properties();
                    Properties newTranslations = new Properties();
                    translations.load(source);

                    // Loop though each of the default values

                    Enumeration e = translations.propertyNames();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = (String) translations.get(key);
                        boolean newValueMade = false;

                        // Loop through all the possible translations

                        Enumeration t = txProps.propertyNames();
                        while (t.hasMoreElements()) {
                            String lookup = (String) t.nextElement();
                            String replace = (String) txProps.get(lookup);

                            // If the value contains lookup

                            int start = 0;
                            StringBuilder newValue = new StringBuilder();
                            Matcher matcher = Pattern.compile("(?mis)([^\\w]|^)(" + lookup + ")([^\\w]|$)").matcher(value);
                            while (matcher.find()) {
                                newValue.append(value.substring(start, matcher.start()));
                                newValue.append(matcher.group(1));

                                // Check for capitalisation

                                if (Character.isUpperCase(matcher.group(2).codePointAt(0))) {
                                    newValue.append(replace.substring(0, 1).toUpperCase());
                                    newValue.append(replace.substring(1));
                                }
                                else {
                                    newValue.append(replace);
                                }
                                newValue.append(matcher.group(3));
                                start = matcher.end();
                            }

                            // If we replace anything

                            if (start > 0) {
                                if (start < value.length()) {
                                    newValue.append(value.substring(start));
                                }
                                newValueMade = true;
                                value = newValue.toString();
                            }
                        }
                        if (newValueMade) {
                            newTranslations.put(key, value);
                        }
                    }

                    // If we have some translations, write them out to the file

                    if (!newTranslations.isEmpty()) {
                        URL dest = I18n.class.getResource("/i18n/app.properties");
                        if (dest != null) {

                            // Just putting a fix in as it was also replacing the folder name as well (e.g. /app-2.0-SNAPSHOT/WEB-INF/..... etc

                            File f = new File(Common.decodeURL(dest.getFile()).replace("app.properties", "app_en_US.properties"));
                            out = new FileOutputStream(f);
                            newTranslations.store(out, "Automatically generated by App");
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Close everything

        Common.close(tx, source, out);
    }


    /**
     * Get a list of the locale names available on the system
     * @return List of local names
     */
    public static List<String> getSupportedLocales() {

        // Initialise the list with the UK locale

        List<String> locales = new ArrayList<>();
        locales.add("en-GB");

        // Now get the rest

        URL dest = I18n.class.getResource("/i18n/app.properties");
        if (dest!=null) {
            File defaultFile = new File(dest.getFile());
            String pattern = Common.getFilenameBody(defaultFile.getName());
            List<File> localeFiles = Common.listFiles(defaultFile.getParentFile(), pattern + "[^\\.]+\\.properties", false, false);
            if (!Common.isBlank(localeFiles)) {
                for (File localeFile : localeFiles) {
                    locales.add(localeFile.getName().replaceAll("(^[^_]+_)|(\\..+$)","").replaceAll("_","-"));
                }
            }
        }
        return locales;
    }

    /**
     * Gets the country name for the given locale
     * @param locale Locale expressed
     * @return Country name
     */
    public static String getCountryForLocale(Locale locale) {
        return getCountryForLocale(locale.toString());
    }

    /**
     * Gets the country name for the given locale
     * @param localeString Locale expressed as en-UK etc
     * @return Country name
     */
    public static String getCountryForLocale(String localeString) {
        String returnValue = null;
        if (!Common.isBlank(localeString)) {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (localeString.matches("(?i)^.+[-_]" + locale.getCountry())) {
                    returnValue = locale.getDisplayCountry();
                    break;
                }
            }
        }
        return returnValue;
    }
}

