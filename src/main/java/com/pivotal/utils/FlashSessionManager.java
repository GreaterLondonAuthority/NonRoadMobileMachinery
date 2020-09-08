/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import com.pivotal.web.servlet.ServletHelper;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

/**
 * <p>Provides access to the flash scope within Spring MVC. This allows you to set attributes that will exist until the next request only.</p>
 *
 * <p>IMPORTANT: This is not just related to showing messages but transferring items between redirects (which is best practive for post requests). It was deleted
 * when the notification manager was used over the simple implementation I did using this manager and this class was deleted as well. This is used for more than
 * just notification messages, SO PLEASE DO NOT DELETE IT.</p>
 *
*/
@SuppressWarnings("unused")
public class FlashSessionManager {

    /**
     * This will set a success message to exist until the next page load. This is useful for using where a redirect
     * is used after some action. For example, if you perform a post and then redirect to the item after, you can
     * add a success message to the flash scope that will be shown to the user when they have been redirected.
     *
     * @param message The actual success message or the I18n key
     * @return True if the success message could be added to the flash scope
     */
    public static boolean setGlobalSuccessMessage(String message) {
        return setFlashMessage("globalUserMessage", true, message);
    }

    /**
     * This will set a failured message to exist until the next page load. This is useful for using where a redirect
     * is used after some action. For example, if you perform a post and then redirect to the item after, you can
     * add a failure message to the flash scope that will be shown to the user when they have been redirected.
     *
     * @param message The actual error message or the I18n key
     * @return True if the failure message could be added to the flash scope
     */
    public static boolean setGlobalFailureMessage(String message) {
        return setFlashMessage("globalUserMessage", false, message);
    }

    /**
     * This will add the message as an attribute in the flash session using the name provided.
     *
     * @param name    The name of the attribute
     * @param success True if this is a success message or false if an error
     * @param message The actual error message or the I18n key
     * @return True if the message could be added to the flash scope
     */
    public static boolean setMessage(String name, boolean success, String message) {
        return setFlashMessage(name, success, message);
    }

    /**
     * Returns any flash message that has been set from the last request.
     *
     * @return the Message or null if no message was set
     */
    public static Message getGlobalMessage() {
        return getFlashAttribute("globalUserMessage");
    }

    /**
     * Returns any flash message attribute that has been set from the last request.
     *
     * @param name The name of the attribute
     * @return the Message or null if no message was set
     */
    public static Message getMessage(String name) {
        return getFlashAttribute(name);
    }

    /**
     * This will set the message to exist until the next page load. This is useful for using where a redirect
     * is used after some action. For example, if you perform a post and then redirect to the item after you can
     * add a message to the flash scope that will be shown to the user when they have been redirected.
     *
     * @param name    The name of the attribute
     * @param success True if this is a success message or false if an error
     * @param message The message to display (This can also be an I18N key)
     * @return True if the message could be added to the flash scope
     */
    private static boolean setFlashMessage(String name, boolean success, String message) {

        // Create a new message to the flash scope

        return setFlashAttribute(name, new Message(success, message));
    }

    /**
     * Simple wrapper for storing a message that can be displayed to the user on page reload
     */
    public static class Message {

        // true if this is an success message
        private boolean success;

        // The actual message or I18N key
        private String text;

        /**
         * Creates a new message object
         *
         * @param success True if this is a success message or false if an error
         * @param text    The actual message text or I18N key
         */
        public Message(boolean success, String text) {
            this.success = success;
            this.text = text;
        }

        /**
         * @return True if this is an success message or false if an error message
         */
        public boolean getSuccess() {
            return success;
        }

        /**
         * @return The text of the message
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Adds the attribute to the flash scope. This means that it will only be available to the next request.
     *
     * @param name  The name of the attribute
     * @param value The value for the attribute
     * @param <T>   The type of the attribute
     * @return True if the attribute was added to the flash scope
     */
    public static <T> boolean setFlashAttribute(String name, T value) {

        // Get access to the outgoing flash map

        FlashMap outputFlashMap = RequestContextUtils.getOutputFlashMap(ServletHelper.getRequest());
        if (outputFlashMap != null) {
            outputFlashMap.put(name, value);
            return true;
        }
        return false;
    }

    /**
     * Will return the attribute added to the last request.
     *
     * @param name The name of the attribute to return
     * @param <T>  The type of the attribute
     * @return The attribute or null if no attribute with the name exists
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFlashAttribute(String name) {

        // Get access to the incoming flash map

        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(ServletHelper.getRequest());
        if (inputFlashMap != null) {
            return (T) inputFlashMap.get(name);
        }
        return null;
    }
}
