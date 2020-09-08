/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import com.pivotal.web.Constants;
import com.pivotal.web.servlet.ServletHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * <p>Implementation of the {@code I18nHostResolver} interface providing access to the i18n of the host application.</p>
 *
*/
public class I18nImplemenation  {

    // Get access to the logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(I18nImplemenation.class);
    public static final String I18N_REGEX = "[0-9a-zA-Z_-]{3,}+\\.[0-9a-zA-Z_.-]+";
    public static final String I18N_FUNCTION_REGEX = "(?is)i18n *\\(" + I18N_REGEX + " *\\)";
    public static final String LOCALE_SETTING = "Locale";

    // Ensure that when developing the bundles are not cached
    private final static ResourceBundle.Control bundleControl = new BundleControl();

    /**
     * {@inheritDoc}
     */
    public static Locale getLocale() {
        return getLocale(ServletHelper.getRequest());
    }

    /**
     * {@inheritDoc}
     */
    public static  Locale getLocale(Map<String, Object> context) {
        Locale locale = null;
        if (context != null) {
            locale = (Locale) context.get(LOCALE_SETTING);
            if (locale != null) {
                logger.debug("Retrieved locale from context object");
            }
            else {
                locale = getLocale((HttpServletRequest) context.get("Request"));
            }
        }

        // Now try the thread storage

        if (locale == null) {
            locale = getLocale();
        }
        return locale;
    }

    /**
     * {@inheritDoc}
     */
    public static  Locale getLocale(HttpServletRequest request) {
        Locale locale = null;
        if (request != null) {
            HttpSession session = request.getSession();
            if (session != null) {
                locale = (Locale) session.getAttribute(LOCALE_SETTING);
                if (locale != null) {
                    logger.debug("Retrieved locale from session object");
                }
            }
            if (locale == null) {
                locale = request.getLocale();
                if (locale != null) {
                    logger.debug("Retrieved locale from request object");
                }
            }
        }

        // Still no luck then try it again using the servlet helper

        if (locale == null && ServletHelper.getRequest() != null) {
            locale = getLocale(ServletHelper.getRequest());
        }

        if (locale == null) {
            locale = Locale.getDefault();
            logger.debug("Retrieved locale from default");
        }
        return locale;
    }

    /**
     * {@inheritDoc}
     */
    public static  String getString(String key) {
        return translate(key);
    }

    /**
     * {@inheritDoc}
     */
    public static  String getString(String key, Object... parameters) {
        return translate(getLocale(), key, parameters);
    }

    /**
     * {@inheritDoc}
     */
    public static  String getString(Locale locale, String key) {
        return translate(locale, key, (Object[]) null);
    }

    /**
     * {@inheritDoc}
     */
    public static  String getString(Locale locale, String key, Object... parameters) {
        return translate(locale, key, parameters);
    }

    /**
     * {@inheritDoc}
     */
    public static  String getString(Locale locale, String key, boolean nullIfNotFound, Object... parameters) {
        return translate(locale, key, nullIfNotFound, parameters);
    }

    /**
     * {@inheritDoc}
     */
    public static  String translate(String stringToTranslate) {
        return translate(getLocale(), stringToTranslate, (Object[]) null);
    }

    /**
     * {@inheritDoc}
     */
    public static  String translate(String stringToTranslate, boolean nullIfNotFound) {
        return translate(getLocale(), stringToTranslate, nullIfNotFound, (Object[]) null);
    }

    /**
     * Translates the string using the specified locale and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * The token selection uses the following rule to determine the actual message;
     *
     * a) If the stringToken doesn't contain a full stop then it is returned as the value<br>
     * b) If the stringToken contains any whitespace or non alpha-numeric characters it returns as the value<br>
     * b) If the stringToken is not in the bundle and it is mixed case, then it is returned as the value<br>
     * In all cases where a token is found, the token is recursively checked to see
     * if it is actually a token.  This allows the referencing of the same messages using
     * different tokens e.g.
     *
     * <pre>
     * general.validation.error = A general validation error occurred
     * users.role.add.validationerror = general.validation.error
     * users.add.validationerror = users.role.add.validationerror
     * transducers.test.error = general.validation.error
     * </pre>
     *
     * This method returns an error string if the token is either not found or creates
     * an endless loop
     *
     * @param locale      Locale to use
     * @param stringToken String token to get translation for
     * @param parameters  Optional array of parameters to use
     *
     * @return Translated string
     */
    protected static String translate(Locale locale, String stringToken, Object... parameters) {
        return translate(locale, stringToken, false, parameters);
    }

    /**
     * Translates the string using the specified locale and the parameters
     * The string is retrieved from the bundle and then passed through String.format
     * using the parameters array
     *
     * The token selection uses the following rule to determine the actual message;
     *
     * a) If the stringToken doesn't contain a full stop then it is returned as the value<br>
     * b) If the stringToken contains any whitespace or non alpha-numeric characters it returns as the value<br>
     * b) If the stringToken is not in the bundle and it is mixed case, then it is returned as the value<br>
     * In all cases where a token is found, the token is recursively checked to see
     * if it is actually a token.  This allows the referencing of the same messages using
     * different tokens e.g.
     *
     * <pre>
     * general.validation.error = A general validation error occurred
     * users.role.add.validationerror = general.validation.error
     * users.add.validationerror = users.role.add.validationerror
     * transducers.test.error = general.validation.error
     * </pre>
     *
     *
     * @param locale         Locale to use
     * @param stringToken    String token to get translation for
     * @param nullIfNotFound True if null should be returned if the string is not found
     * @param parameters     Optional array of parameters to use
     *
     * @return Translated string
     */
    protected static String translate(Locale locale, String stringToken, boolean nullIfNotFound, Object... parameters) {

        String returnValue = null;

        // If the string contains the 'magic' I18N function, then recursively resolve it

        if (stringToken != null && Common.findFirst(stringToken, I18N_FUNCTION_REGEX) != null) {
            returnValue = translateEmbedded(locale, stringToken, nullIfNotFound, parameters);
        }

        // If the token looks like it's a literal (contains whitespace, is numeric or doesn't look like a token) then return it

        else if (stringToken == null || !stringToken.contains(".") || stringToken.matches("(.*\\s.*)|(-?[0-9\\.]+)")
                || !stringToken.matches(I18N_REGEX)) {
            returnValue = stringToken;
        }

        else if (locale != null) {
            try {
                logger.debug("Using locale [{}]", locale.toString());
                ResourceBundle bundle = ResourceBundle.getBundle("i18n.app", locale, bundleControl);

                // Check to see if the token is available unmolested

                String token = stringToken;
                boolean found = bundle.containsKey(token);
                if (!found) {
                    found = bundle.containsKey(token.toLowerCase());
                    if (found) token = token.toLowerCase();
                }

                // Write out the translated string

                if (!found) {
                    if (!stringToken.endsWith(".column.extras"))
                        logger.error("ERROR no translation for [{}]", stringToken);
                    if (!nullIfNotFound) returnValue = "ERR[i18n]";
                }
                else {

                    // Check to see if this is actually a de-referenced property

                    logger.debug("Found token [{}] for target [{}]", token, stringToken);
                    int sanityCheck = 0;
                    while (bundle.containsKey(token) && token.matches("\\S+") && !Common.doStringsMatch(token, bundle.getString(token))) {
                        token = bundle.getString(token);
                        sanityCheck++;
                        int MAX_RECURSION = 50;
                        if (sanityCheck > MAX_RECURSION)
                            throw new PivotalException(String.format("The hunt for token [%s] causes an endless loop", stringToken));
                    }

                    // Translate any embedded values

                    if (!Common.isBlank(token)) {
                        token = translateEmbedded(locale, token, false);
                    }

                    // Run the format with/without the parameters

                    if (Common.isBlank(parameters)) {
                        returnValue = token;
                    }
                    else {
                        returnValue = String.format(token, parameters);
                    }
                }
            }
            catch (Exception e) {
                if (!nullIfNotFound) {
                    returnValue = "ERR[i18n]";
                }
                logger.error("ERROR translating [{}] - {}", stringToken, PivotalException.getErrorMessage(e));
            }
        }

        return returnValue;
    }

    /**
     * Replaces all the embedded I18N() function values with their translated values
     *
     * @param locale         Locale to use
     * @param stringToken    String token to get translation for
     * @param nullIfNotFound True if null should be returned if the string is not found
     * @param parameters     Optional array of parameters to use
     *
     * @return Translated string
     */
    private static String translateEmbedded(Locale locale, String stringToken, boolean nullIfNotFound, Object... parameters) {

        String returnValue = stringToken;
        if (!Common.isBlank(stringToken)) {

            // Get a list of the parts to replace

            List<String> parts = Common.find(stringToken, I18N_FUNCTION_REGEX);
            if (!Common.isBlank(parts)) {
                for (String part : parts) {
                    String token = Common.findFirst(part, I18N_REGEX);
                    returnValue = returnValue.replace(part, translate(locale, token, nullIfNotFound, parameters));
                }
            }
        }

        return returnValue;
    }

    /**
     * A class to enable us to manage the bundle cache
     */
    static class BundleControl extends ResourceBundle.Control {
        @Override
        public long getTimeToLive(String baseName, Locale locale) {

            // If we're running locally from within the IDE then set the TTL
            // to be non-caching

            if (Constants.inIde()) {
                return ResourceBundle.Control.TTL_DONT_CACHE;
            }
            else {
                return super.getTimeToLive(baseName, locale);
            }
        }
    }
}
