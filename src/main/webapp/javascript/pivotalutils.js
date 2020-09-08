/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
// Copy of common.js but only with PivotalUtils left
/*global $:false, jQuery:false */
"use strict";

/**
 * Global namespace functions
 */
var PivotalUtils = {

    isFormDirty: false,

    /**
     * Gives convenient access to the JQuery within the context of the top window
     */
    $: $,

    /**
         * Returns true if it thinks the browser is IE
         * @returns {boolean} True if IE
         */
        isMSIE: function() {
            var ua = window.navigator.userAgent;

            var msie = ua.indexOf('MSIE ');
            if (msie > 0) {
                // IE 10 or older => return version number
                return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
            }

            var trident = ua.indexOf('Trident/');
            if (trident > 0) {
                // IE 11 => return version number
                var rv = ua.indexOf('rv:');
                return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
            }

            var edge = ua.indexOf('Edge/');
            if (edge > 0) {
               // IE 12 (aka Edge) => return version number
               return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
            }

            // other browser
            return false;
        },

    /**
     * Intersects two arrays and returns an array with the common elements
     * @param a First Array
     * @param b Second Array
     * @returns {*} Array with the common elements between the two arrays
     */
    arrayIntersect : function(a,b){
        var res = null;
        if(a && b) {
            res = $.grep(a, function (i) {
                return $.inArray(i, b) > -1;
            });
        }
        return res;
    },

    /**
     * Checks if a given email address conforms to rfc822
     * @param email email to analyse
     * @returns {boolean} True if the given email conforms to rfc822, false otherwise
     */
    isEmail : function (email){

        var returnVal = false;

        if (email) {
            returnVal = /^([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22))*\x40([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d))*$/.test(email);
        }

        return returnVal;
    },

    /**
     * Process an array in multiple execution items. Each execution item is able to process items from the array for up to 50 milliseconds or up to a number of items.
     *
     * @param items Array of Items to process
     * @param process Function that will process the array item. It should receive the item as a parameter
     * @param callback Optional callback to be executed when ALL the items have been processed
     * @param delay Delay in miliseconds between execution items. IE, the time in miliseconds we are freeing the JS thread before processing another batch. Default is 25.
     * @param maxItems Maximum number items we're allowed to process in each execution before freeing the thread
     * @param timeWindow Time window in miliseconds we allow each execution to run for. Default is 50.
     * @param force True if we should keep executing even if ONE process function fails. If force is not enabled, if an exception is thrown in the process function the execution of the array will stop
     */
    timedArrayProcess : function (items, process, callback, delay, maxItems, timeWindow, force) {

        //validate mandatory inputs
        if(!items || !process || typeof process !== 'function') return;

        //Set defaults to additional parameters
        var n = items.length,
            execDelay = delay || 25,
            execMaxItems = maxItems || n,
            execTimeWindow = timeWindow || 50;

        //Init current index
        var i=0;

        // The first call to setTimeout() starts up array processing
        setTimeout(function timedExecution(){

            //Mark the start of the execution
            var start = +new Date(),
                j = i;

            do{
                try {
                    //Call the processing function. The current array item will be passed as a parameter.
                    //If any exception occurs the execution will stop unless the force flag is enabled
                    process.call(execDelay, items[i]);

                    //Increment the current index
                    i += 1;
                }
                catch(exception){
                    //failed to execute the processing function.
                    //If the force flag is on continue, otherwise terminate the execution
                    if(force !== true) {
                        return;
                    }
                }
            }
            // Keep execution until the maximum number of execution items has been reached OR until the max execution time has been reached
            while ( i < n && (i - j) < execMaxItems && (new Date() - start < execTimeWindow));

            //Do we still have items to process in the array
            if (i < n) {

                //Then let's start over with another timeout. It will give the JS thread some time to do other things
                setTimeout(timedExecution, delay);
            }

            //Nothing else left to process. We are done.
            //If we have a callback configured, call it with the input array
            else if(callback && typeof callback === 'function'){
                callback(items);
            }
        }, execDelay);
    },

    /**
     * Checks if the given position is in the bounds of the given element.
     * This was created to get around the IE hover problem. element.is(":hover") doesn't work properly in IE so we'll check if the give position is on top of an element or not.
     *
     * @param xPos X coordinate to test
     * @param yPos Y coordinate to test
     * @param elem Element to test against
     * @returns {boolean} True if the given position is in the bounds of the given element
     */
    isPositionInBounds: function (xPos, yPos, elem) {
        var res = false;
        if(xPos && yPos && elem){
            var offset = elem.offset(),
                w = elem.width(),
                h = elem.height(),
                marginLeft = parseInt(elem.css("margin-left")),
                marginTop = parseInt(elem.css("margin-top"));

            //Check if position is inside the bounding box. Account for the margins because the offsets will be shifted with them
            if(offset && w && h &&
                    xPos > (offset.left-marginLeft) && yPos > (offset.top-marginTop) &&
                    xPos < (offset.left + w) && yPos < (offset.top + h)
                    ){
                res = true;
            }

        }

        return res;
    },

    /**
     * The language bundle required for i18n
     */
    i18nBundle: {
        navigateAwayMessage: "You have unsaved changes\n\nAre you sure you wish to close this window?",
        autoSaveFoundMessage: "A previous auto save has been found for this record.\r\nClick Ok to restore it.\r\nDate of auto save"
    },

    /**
     * Returns the value of a css class property. If nothing is found, return null
     * @param prop Property to search for
     * @param fromClass CSS Class to search in
     * @returns {*} Css property value or null if nothing is found
     */
    getCssProperty : function (prop, fromClass) {
        var res = null;
        var inspector = $("<div>").css('display', 'none').addClass(fromClass);
        $("body").append(inspector); // add to DOM, in order to read the CSS property
        try {
            res = inspector.css(prop);
        } finally {
            inspector.remove(); // and remove from DOM
        }
        return res;
    },

    /**
    * Converts a color in RGB format to Hex
    * @param rgb value
    * @returns {string} Hex String equivalent to the given rgb value
    */
    rgb2hex : function (rgb){
        var res = null;
        if(rgb) {
            var tmp = rgb.match(/(\d+)/g);
            res =   "#" +
                    ("0" + parseInt(tmp[0], 10).toString(16)).slice(-2) +
                    ("0" + parseInt(tmp[1], 10).toString(16)).slice(-2) +
                    ("0" + parseInt(tmp[2], 10).toString(16)).slice(-2);
        }
        return res;
    },

    /**
     * Generates string with 4 random ascii chars number
     * @returns {string}
     */
    s4 : function () {
       return (((1+Math.random())*0x10000)|0).toString(16).substring(1);
    },

    /**
     * Generates a random guid
     * @returns {string} guid
     */
    guid : function () {
       return (PivotalUtils.s4()+PivotalUtils.s4()+"-"+PivotalUtils.s4()+"-"+PivotalUtils.s4()+"-"+PivotalUtils.s4()+"-"+PivotalUtils.s4()+PivotalUtils.s4()+PivotalUtils.s4());
    },

    /**
     * Returns the content for the given URL
     * @param sURL URL to retrieve content for
     * @param sParams Parameters to use
     * @param bPost True if this is a POST
     * @param bEvalScripts True if the response should be evaluated for scripts
     * @param iTimeout Timeout in milliseconds
     * @param acceptType Optional accept type
     * @param contentType Optional content type
     * @returns {*}
     */
    getContent: function(sURL, sParams, bPost, bEvalScripts, iTimeout, acceptType, contentType) {
        var returnValue = null;
        if (sParams) {
            if (sParams.charAt(0)==='?') {
                sParams=sParams.substring(1);
            }
        }
        else if (sURL.indexOf('?')!==-1) {
            sParams = sURL.split("?")[1];
            sURL = sURL.split("?")[0];
        }
        var headers = {};
        if (acceptType) {
            headers.Accept = acceptType;
        }
        if (contentType) {
            headers["Content-Type"] = contentType;
        }
        else if (bPost) {
            headers["Content-Type"] = "application/x-www-form-urlencoded";
        }
        else {
            headers["Content-Type"] = "text/plain; charset=utf-8";
        }
        $.ajax({
            type: bPost?'post':'get',
            url: sURL,
            headers: headers,
            contentType: headers?"":contentType?contentType:(bPost?"application/x-www-form-urlencoded":"text/plain"),
            data: sParams,
            async: false,
            cache: false,
            dataType: "text",
            timeout: iTimeout?iTimeout:0,
            success: function(data, textStatus, jqXHR) {
                returnValue = data;
                if (bEvalScripts) {
                    PivotalUtils.evaluateScripts(data);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (console) {
                    console.log('Error occurred in AJAX call (' + textStatus + ')');
                }
            }
        });
        return returnValue;
    },

    /**
     * Returns the content for the given URL
     * @param sURL URL to retrieve content for
     * @param sParams Parameters to use
     * @param bPost True if this is a POST
     * @returns {*}
     */
    getJsonPContent: function(sURL, sParams, bPost) {
        var returnValue = null;
        if (sParams) {
            if (sParams.charAt(0)==='?') {
                sParams=sParams.substring(1);
            }
        }
        else if (sURL.indexOf('?')!==-1) {
            sParams = sURL.split("?")[1];
            sURL = sURL.split("?")[0];
        }
        var headers = {};
        headers.Accept = "application/json; charset=utf-8";
        headers["Content-Type"] = headers.Accept;
        $.ajax({
            type: bPost?'post':'get',
            url: sURL,
            headers: headers,
            contentType: "",
            data: sParams,
            async: false,
            cache: false,
            dataType: "jsonp",
            timeout: 0,
            success: function(data, textStatus, jqXHR) {
                returnValue = data;
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (console) {
                    console.log('Error occurred in AJAX call (' + textStatus + ')');
                }
            }
        });
        return returnValue;
    },

    /**
     * Returns the content for the given URL
     * @param sURL URL to retrieve content for
     * @param sParams Parameters to use
     * @param bPost True if this is a POST
     * @returns {*}
     */
    getJsonPContentAsync: function(sURL, sParams, objCallback, bPost, iTimeout) {
        var returnValue = null;
        if (sParams) {
            if (sParams.charAt(0)==='?') {
                sParams=sParams.substring(1);
            }
        }
        else if (sURL.indexOf('?')!==-1) {
            sParams = sURL.split("?")[1];
            sURL = sURL.split("?")[0];
        }
        var headers = {};
        headers.Accept = "application/json; charset=utf-8";
        headers["Content-Type"] = headers.Accept;
        $.ajax({
            type: bPost?'post':'get',
            url: sURL,
            headers: headers,
            contentType: headers.Accept,
            data: sParams,
            async: true,
            cache: false,
            dataType: "jsonp",
            timeout: iTimeout?iTimeout:0,
            success: function(data, textStatus, jqXHR) {
                var request = data;
                if (objCallback) {
                    objCallback(request);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (console) {
                    console.log('Error occurred in AJAX call (' + textStatus + ')');
                }
            }
        });
        return returnValue;
    },

    /**
     * Finds all the script blocks in the content and evaluates each one
     * @param sBlock HTML content to evaluate
     */
    evaluateScripts: function(sBlock) {
        if (sBlock.toLowerCase().indexOf("<script")>-1) {
            var asScripts=sBlock.split(/<script[^>]*>/i);
            for (var iCnt=1; iCnt<asScripts.length; iCnt++) {
                try {
                    eval(asScripts[iCnt].split(/<\/script>/i)[0]);
                }
                catch(X) {
                    if (console) {
                        console.log(X);
                    }
                }
            }
        }
    },

    /**
     * Returns the content for the given URL and evaluates it
     * @param sURL URL to get content from
     * @param sParams Parameters to send
     * @param bPost True if POST
     * @returns {{}}
     */
    getJsonContent: function(sURL, sParams, bPost) {
        var objReturn={};
        try {
            var response = PivotalUtils.getContent(sURL,sParams,bPost,false,0,"application/json; charset=utf-8");
            if (response !== "") {
                objReturn = jQuery.parseJSON(response);
            }
        }
        catch (X) {
            if (console) {
                console.log("Problem retrieving JSON response from server for\n" + sURL + (sParams ? "?" + sParams : "") + "\n\n" + X);
            }
        }
        return objReturn;
    },

    /**
     * Returns the JSON content for the given URL as an evaluated JS object
     * @param sURL URL to get content from
     * @param sParams Parameters to send
     * @param objCallback Optional callback function
     * @param bPost True if POST True if this is a POSt
     * @param iTimeout Timeout in millieseconds to wait for response
     */
    getJsonContentAsync: function(sURL, sParams, objCallback, bPost, iTimeout) {
        PivotalUtils.getContentAsync(sURL, sParams, function(response) {
            if (objCallback && response && response.responseText !== "") {
                var object = false;
                try {
                    object = jQuery.parseJSON(response.responseText);
                }
                catch (X) {
                    if (console) {
                        console.log("Problem retrieving JSON response from server for\n" + sURL + (sParams ? "?" + sParams : "") + "\n\n" + response + "\n\n" + X);
                    }
                }
                if (object) {
                    objCallback(object);
                }
            }
        }, bPost, iTimeout, "application/json; charset=utf-8");
    },


    /**
     * Calls the server with the given URL asynchronously, issue a put method with some json content
     * and will optionally invoke the callback when data is returned
     * @param sURL URL to get data for
     * @param jsonData Parameters to use
     * @param objCallback Optional callback function
     * @param iTimeout Timeout in millieseconds to wait for response
     */
    putJsonContentAsync: function(sURL, jsonData, objCallback, iTimeout) {
        var headers = {};

        //Accept Json
        headers.Accept = "application/json";

        $.ajax({
            type: 'put',
            url: sURL,
            headers: headers,
            contentType: 'application/json',
            data: JSON.stringify(jsonData ? jsonData : {}),
            async: true,
            cache: false,
            dataType: "text",
            timeout: iTimeout?iTimeout:0,
            xhrFields: {
                "withCredentials": true
            },
            success: function(data, textStatus, jqXHR) {
                var request = {responseText: data};
                if (objCallback) {
                    objCallback(request);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (console) {
                    console.log('Error occurred in AJAX call (' + textStatus + ')');
                }
            }
        });

    },

    /**
     * Calls the server with the given URL asynchronously and will optionally
     * invoke the callback when data is returned
     * @param sURL URL to get data for
     * @param sParams Parameters to use
     * @param objCallback Optional callback function
     * @param bPost True if POST True if this is a POSt
     * @param iTimeout Timeout in millieseconds to wait for response
     * @param acceptType Optional accept type
     * @param contentType Optional content type
     */
    getContentAsync: function(sURL, sParams, objCallback, bPost, iTimeout, acceptType, contentType) {

        if (sParams && sParams.charAt) {
            if (sParams.charAt(0)==='?') {
                sParams=sParams.substring(1);
            }
        }
        else if (sURL.indexOf('?')!==-1) {
            sParams = sURL.split("?")[1];
            sURL = sURL.split("?")[0];
        }
        var headers = {};
        if (acceptType) {
            headers.Accept = acceptType;
        }
        if (contentType) {
            headers["Content-Type"] = contentType;
        }
        else if (bPost) {
            headers["Content-Type"] = "application/x-www-form-urlencoded";
        }
        else {
            headers["Content-Type"] = "text/plain; charset=utf-8";
        }
        $.ajax({
            type: (bPost)?'post':'get',
            url: sURL,
            headers: headers,
            contentType: headers?"":contentType?contentType:(bPost?"application/x-www-form-urlencoded":"text/plain; charset=utf-8"),
            data: sParams,
            async: true,
            cache: false,
            dataType: "text",
            timeout: iTimeout?iTimeout:0,
            xhrFields: {
                "withCredentials": true
            },
            success: function(data, textStatus, jqXHR) {
                var request = {responseText: data};
                if (objCallback) {
                    objCallback(request);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                if (console) {
                    console.log('Error occurred in AJAX call (' + textStatus + ')');
                }
            }
        });
    },

    /**
     * Updates the specified element with data from the server
     * @param sElementId ID of the element to update the content of
     * @param sURL The URL to get the content from
     * @param sParams Any parameters to add to the URL
     */
    updateContent: function(sElementId, sURL, sParams) {
        $('#' + sElementId).html(PivotalUtils.getContent(sURL, sParams));
    },

    /**
     * Updates the specified element with data from the server
     * @param sElementId ID of the element to update the content of
     * @param sURL The URL to get the content from
     * @param sParams Any parameters to add to the URL
     * @param objCallback Optional callback to notify when the content is retrieved
     * @param iTimeout Optional timeout (milliseconds)
     */
    updateContentAsync: function(sElementId, sURL, sParams, objCallback, iTimeout) {
        PivotalUtils.getContentAsync(sURL, sParams, function(objRequest) {
            $('#' + sElementId).html(objRequest.responseText);
            if (objCallback) {
                objCallback(objRequest);
            }
        },false,iTimeout);
    },


    /**
     * Get the kendoWindow object that this frame is inside
     *
     * @returns kendoWindow object we are inside, or null if we are not in a kendoWindow
     */
    getCurrentModalWindow: function () {
        var result = null;
        window.parent.$('iframe').each(function () {
            try {
                var modalWindow = $(this).parent();
                var idoc = 'contentDocument' in this ? this.contentDocument : this.contentWindow.document;
                if (idoc === document) {
                    result =  window.parent.$.data(modalWindow[0], "kendoWindow");
                }
            }
            catch (e) {
            }
        });
        return result;
    },

    /**
     * Shows a popup confirmation dialog using the kendo window providing the user with a choice of action. After the user has clicked either the
     * confirm or cancel button, the dialog will be closed automatically after any handlers have been called.
     *
     * @param id ID to associate with the window
     * @param title Title to display
     * @param options The other options for the popup
     *          messageText The message that is shown to the user
     *          confirmText The text shown for the confirm button
     *          cancelText The text shown for the cancel button
     *          confirmUrl The Url to load if the user clicks the confirm button
     *          confirmFunction The function to load if the user clicks the confirm button (is ignored if the confirmUrl is provided)
     *          hideCancelButton True if the cancel button should not be displayed
     *          cancelUrl The Url to load if the user clicks the cancel button
     *          cancelFunction The function to load if the user clicks the cancel button (is ignored if the cancelUrl is provided)
     *          width Width of the window
     *          height Height of the window
     * @returns The confirmation (KendoWindow) object
     */
    showConfirmationDialog: function(id, title, options) {

        // Create the template to use for the dialog
        var confirmText = options.confirmText?options.confirmText:PivotalUtils.i18nBundle.confimButtonDefaultConfirm;
        var cancelText = options.cancelText?options.cancelText:PivotalUtils.i18nBundle.confimButtonDefaultCancel;
        var thisTitle = title?title:PivotalUtils.i18nBundle.confirmationDefaultTitle;

        var template = "<p class='confirm-dialog-message'>" + options.messageText + "</p>" +
                       "<p class='confirm-dialog-buttons'><button class='k-button dialog-confirm-button k-button'><i class='fa fa-check'></i> " + confirmText + "</button>";
        if (!options.hideCancelButton) {
            template += "<button class='k-button dialog-cancel-button'><i class='fa fa-times'></i> " + cancelText + "</button>";
        }
        template += "</p>";
        var windowHeight = options.height?options.height:100;
        var windowWidth = options.width?options.width:350;
        var dialog = psApp.windowManager.open({title:thisTitle, width:windowWidth, height:windowHeight, customActions:[]});

        dialog.kendoWrapper.addClass("confirm-dialog");
        dialog.content(template);

        // Add a click handler to the confirm
        dialog.window.$(".dialog-confirm-button").click(function() {
            if (options && options.confirmUrl) {
                window.location.href = options.confirmUrl;
            }
            else if(options && options.confirmFunction) {
                setTimeout(options.confirmFunction, 50);
            }
            psApp.windowManager.close();
        }).end();

        // Add a click handler to the cancel
        dialog.window.$(".dialog-cancel-button").click(function() {
            if(options && options.cancelUrl) {
                window.location.href = options.cancelUrl;
            }
            else if (options && options.cancelFunction) {
                setTimeout(options.cancelFunction, 50);
            }
            psApp.windowManager.close();
        }).end();

        return dialog;
    },

    /**
     * Shows a modal 'screen' with an animated icon. The screen is removed after the given timeout if it isn't
     * hidden by code
     */
    showSimpleModalScreen: function() {
        PivotalUtils.showModalScreen();
    },

    /**
     * Shows a modal 'screen' with an animated icon over the specified element
     * with a message. The screen is removed after the given timeout if it isn't
     * hidden by code
     * @param message Optional message to show
     * @param overElement Optional element to place the screen over (default:document)
     * @param icon Optional animated icon to show (default:refresh)
     * @param timeout Optional number of milliseconds to wait before automatically closing (default:30000)
     * @param timeoutCallback Optional callback to call after the timeout occurs
     */
    showModalScreen: function(message, overElement, icon, timeout, timeoutCallback) {

        // We have to get the modal window or create a new one

        var body = $('body');
        icon = icon?icon:'refresh';
        timeout = timeout?timeout:60000;
        overElement = overElement?$(overElement):body;

        // Figure out what to display

        var modalContent = "<div><i class='fa fa-" + icon + " fa-spin'></i>";
        if (message) {
            modalContent+="<p class='modal-message'>" + message + "</p>";
        }
        modalContent+="</div>";

        // Create the 'screen'

        var modalContainer = $("#modal-screen");
        if (!modalContainer.length) {
            modalContainer = $("<div id='modal-screen' class='modal'>" + modalContent + "</div>");
            body.append(modalContainer);
        }
        else {
            modalContainer.html(modalContent);
        }

        // Keep the content position over the whole thing or only over
        // the element supplied

        body.data('modal-resizer', function() {
            modalContainer.offset(overElement.offset());
            modalContainer.height(overElement.height());
            modalContainer.width(overElement.width());
        });
        body.data('modal-resizer')();
        $(window).resize(function() {
            if (body.data('modal-resizer')) {
                body.data('modal-resizer')();
            }
        });

        // Start the loading overlay

        body.addClass("loading");

        // Create a timer to stop it going on forever

        body.data('loading-timeout', window.setTimeout(function() {

            // If we have a callback, check with them to find out what they want to do

            if (!timeoutCallback || !timeoutCallback()) {
               PivotalUtils.hideModalScreen();
           }
        }, timeout));
    },

    /**
     * Hides the modal 'screen' and removes the associated resize handler
     */
    hideModalScreen: function() {
        var body = $('body');
        body.removeClass('loading');
        window.clearTimeout(body.data('loading-timeout'));
        $(window).off("resize", body.data('modal-resizer'));
    },

    /**
     * Shows a notification on the current page
     * @param message Title for the image box
     * @param type Notification type
     * @param options contains
     * @returns The notification (KendoNotification) object
     */
    showNotification: function (message, type, options) {
        alert(message);
        return null;
    },

    /**
     * Shows a notification on the current page
     * @param message Title for the image box
     * @param type Notification type
     * @param options contains
     * @returns The notification (KendoNotification) object
     */
    showNotificationOld: function (message, type, options) {
        try {
            // Call the one at the top of the tree if there is one
            if (window.self !== top) {
                return top.PivotalUtils.showNotification(message, type, options);
            }
        }
        catch(e) {
            // This will only occur if we cannot access the parent (which is likely if we are curently within a cross
            // domain iframe) or if we can access the parent through an iframe (on same domain) but it is not the
            // NRMM application
        }

        var flashMessage;
        var flashSpan = $("#flash-message");
        //If the span has been added to the document already, reuse, if not create and add
        if (flashSpan.length) {
            flashMessage = $(flashSpan).data("kendoNotification");
        }
        else {
            //create the span and add to document
            flashSpan = document.createElement('span');
            flashSpan.id = "flash-message";
            $(flashSpan).css('display', 'none');
            document.body.appendChild(flashSpan);
            //init the kendo notification
            flashMessage = $(flashSpan).kendoNotification({
                position: {
                    pinned: true,
                    top: 10,
                    right: 10
                },
                stacking: "down",
                button: true,
                templates: [ {
                    type: "info",
                    template: '<table class="nrmmNotification"><tr><td><i class="fa fa-lightbulb-o"></i></td><td class="message">#= message #</td></tr></table>'
                }, {
                    type: "success",
                    template: '<table class="nrmmNotification"><tr><td><i class="fa fa-check-circle"></i></td><td class="message">#= message #</td></tr></table>'
                }, {
                    type: "warning",
                    template: '<table class="nrmmNotification"><tr><td><i class="fa fa-exclamation-circle"></i></td><td class="message">#= message #</td></tr></table>'
                }, {
                    type: "error",
                    template: '<table class="nrmmNotification"><tr><td><i class="fa fa-exclamation-triangle"></i></td><td class="message">#= message #</td></tr></table>'
                }]
            }).data('kendoNotification');

        }

        //Change configurable properties every time we add attempt to show a notification
        if (options) {
            flashMessage.setOptions({
                //default to 10 seconds
                autoHideAfter: options.hideDelay ? options.hideDelay : 1000000,
                width: options.width ? options.width : null,
                height: options.height ? options.height : null,
                position: options.position ? options.position : {pinned: true, top: 10, right: 10}
            });
        }

        //Show the notification
        if(flashMessage != undefined) {
            flashMessage.show(
                    { message: message ? message.replace('\n', '<br>') : '' },
                    type ? type.toLowerCase() : 'info'
            );
            return flashMessage;
        }
    },


    /**
     * Shows a Kendo tooltip with content read from the URL
     * @param elem Element to add to tooltip to
     * @param width Initial width of the tooltip (e.g. 400px or 70%)
     * @param height Initial height of the tooltip (e.g. 400px or 70%)
     * @param url URL to get source
     * @param useIFrame True if the content should be rendered inside an IFrame)
     * @param delay length of pause before showing tooltip
     * @returns The KendoTooltip object
     */
    showTooltip: function(elem, width, height, url, useIFrame, delay) {

        var kendoObj = elem.data("kendoTooltip");
        if(!kendoObj){
            var configs = {
                content:
                {
                    url: url
                },
                position: "top left",
                width : width,
                height : height,
                autoHide:true,
                iframe : useIFrame,
                hide: function() {
                    this.destroy();
                }
            };
            //default to 1 second
            configs.showAfter = delay ? delay : 1000;

            //Kendo Try
            elem.kendoTooltip(configs);

            elem.trigger('mouseenter');
            kendoObj = elem.data("kendoTooltip");
        }
        return kendoObj;

    },

    /**
     * Shows an image on an overlay
     * @param imgUrl URL to get image from
     * @param title Title for the image box
     * @param width Width of the image box (e.g. 400px or 70%)
     * @param height Height of the image box (e.g. 400px or 70%)
     * @param failedErrorMsg Display Message when the content fails to load
     * @returns The <a> element created to support the image box ( is removed on close )
     */
    showImagePreview: function(imgUrl, title, width, height, failedErrorMsg) {

        var ref = document.createElement('a');
        ref.id = "imgPreview";
        ref.href = imgUrl;
        ref.title = title;
        document.body.appendChild(ref);
        $(ref).colorbox({
            imgError:failedErrorMsg,
            width:width,
            height:height,
            photo:true,
            onClosed:function(){ this.remove();}.bind($(ref)),
            open:true
        });
    },

    /**
     * Adds Kendo tooltips to elements that have a 'tooltip' attribute within the scope
     * of a form or within the element given by the JQuery scope selector
     * @param scope Selector of the element(s) to limit the scope to
     * @param toolPos Position to place the tooltip
     */
    showFormTooltips: function(scope, toolPos) {
        var elements;
        if (scope) {
            elements = $(scope).find("[tooltip]");
        }
        else {
            elements = $("form").find("[tooltip]");
        }
        if (elements) {
            elements.kendoTooltip({
                position: toolPos?toolPos:"top",
                iframe: true,
                show: function (e) {
                    // fix the issue of multiple tooltip open after leaving mouse

                    var tooltips = $(".k-state-border-down");
                    if (tooltips.length > 1) {
                        tooltips.each(function (index) {
                            if (index === 0 && $(this).data("kendoTooltip")) {
                                $(this).data("kendoTooltip").hide();
                            }
                        });
                    }

                    // Fix an issue with the tooltips that are below their element

                    if (this.options.position==="bottom" && this.popup.element.parent().offset().top > this.element.offset().top) {
                        this.popup.element.parent().css("margin-top", "10px");
                    }
                    else {
                        // console.log("[target=" + $(e.target).attr("tooltip") + "] [popupElementHeight=" + this.popup.element.height() + "] [popupElementTop=" + this.popup.element.offset().top + "] [elementTop=" + this.element.offset().top + "]");
                        var check = (this.popup.element.height() + this.popup.element.offset().top) - this.element.offset().top;
                        if (check > 0) {
                            this.popup.element.css("top",-(check));
                        }
                        else
                            this.popup.element.parent().css("margin-bottom", "0px");
                    }

                    // tooltip issue not calling the hide event tooltip still on the page
                    // not ideal sol put time out after 5 sec

                    setTimeout(function (e) {
                        $(e).tooltip('hide');
                    }, 5000);
                },
                content: function (e) {
                    var target = e.target;
                    return $(target).attr("tooltip");
                }

            }).data("kendoTooltip");
        }
    },

    /**
     * Resize the sidebar to be the full height of the window (or document if vertical scroll is present)
     * NOTE:    This function is also called in the dashboard layout (\templates\layouts\advancedlayout.vm)
     *                for when users drag/drop/resize widgets and the vertical scroll increases
     */
    setSidebarHeight: function () {
        var bodyDiv = $('.body-padding');
        if (bodyDiv.length>0) {
            var windowHeight = $(window).height();
            bodyDiv.height(windowHeight - (bodyDiv.offset().top + parseInt(bodyDiv.css("padding-top")) + parseInt(bodyDiv.css("padding-bottom"))));
            $('aside').height(windowHeight);
        }
    },

    /**
     * Takes a hex colour and returns it as an rgb value along with it's opacity level.
     * @param colour Hex colour to convert to RGB. e.g. #FFFFFF
     * @param opacity Opacity value from 0.1 to 1
     * @returns The converted colour as an rgba string. Example: hex2rgba('#FFFFFF', .3) = rgba(255,255,255,.3)
     */
    hex2rgba: function (colour,opacity) {
        var r,g,b;
        if ( colour.charAt(0) == '#' ) {
            colour = colour.substr(1);
        }

        r = colour.charAt(0) + '' + colour.charAt(1);
        g = colour.charAt(2) + '' + colour.charAt(3);
        b = colour.charAt(4) + '' + colour.charAt(5);

        r = parseInt( r,16 );
        g = parseInt( g,16 );
        b = parseInt( b,16 );

        return "rgba(" + r + "," + g + "," + b + "," + opacity + ")";
    },

    keepSessionAlive: function(path, extra) {
        PivotalUtils.getJsonContentAsync(path + "/login/keepalive", extra, function(response){
            if (response && response.inError) {
                alert(response.error);
            }
        },true);
    },

    /**
     * Starts the heartbeat
     * @param path          Context path to use
     * @param logMessage    text to show in log to identify source of heartbeat tick
     * @param interval      Interval between heartbeats
     * @param extra         Extra parameters to be passed to the server
     */
    startHeartbeat: function(path, logMessage, interval, extra) {
        setInterval(function() {
            PivotalUtils.heartbeat(path, logMessage, extra);
        }, (interval?interval:60000));
    },

    /**
     * Calls the server to tell it we are still alive
     * It also send up useful information about the colour depth and resolution being used
     * The return value from the server may also contain notification messages
     * @param path Context path to use
     * @param logMessage text to show in log to identify source of heartbeat tick
     * @param extra         Extra parameters to be passed to the server
     */
    heartbeat: function(path, logMessage, extra) {

        PivotalUtils.log("Heartbeat - " + (logMessage?logMessage:path));
        PivotalUtils.getJsonContentAsync(path + "/login/heartbeat", extra,
                function(response){
                    if (response && response.data) {
                        try {

                            // If we get HTML back then we have been logged off and need to transfer
                            // to the login page
                            if (response.data.loggedOut) {
                                location.href = path + "/login/logout";
                            }
                            else if (response.data.timeoutWarning) {
                                $("#TimeoutWarning").modal();
                                $("#TimeoutButtonYes").focus();
                            }
                            else {

                                // Check for any notifications to display
                                var notifications = response.data.notifications;
                                for (var i = 0; i < notifications.length; i++) {
                                    var notif = notifications[i];
                                    if (notif.message !== "") {
                                        // PivotalUtils.showNotification(notif.message, notif.level);
                                        // if (notif.mediaUrl != "") {
                                        //     PivotalUtils.queueAudioFile(path, notif.mediaUrl);
                                        // }
                                    }
                                }
                            }
                        }
                        catch(X) {
                            PivotalUtils.log("Heartbeart fail " + response);
                            PivotalUtils.log(X);
                        }
                    }
                }, true);
    },

    /**
     * Recurses through this and any child iframe windows clearing the isFormDirty
     * flag so that the windows can be closed without invoking a warning
     */
    clearAllDirtyFlags: function() {
        PivotalUtils.isFormDirty = false;
        $("iframe").each(function() {
            try {
                var ssUtils = this.contentWindow.PivotalUtils;
                if (ssUtils) {
                    ssUtils.clearAllDirtyFlags();
                }
            }
            catch (X) {
                // Just ignore silently
            }
        });
    },

    /**
     * Calls the server to extend the current user login session
     *
     * @param path Context path to use
     */
    ping: function(path) {
        PivotalUtils.getContentAsync(path + "/login/ping", null, null, true);
    },

    audioQueue: [],
    audioObject: undefined,
    audioIsPlaying: false,
    audioIsMuted: false,
    loopingAudio: null,

    /**
     * Adds an audio file to be played when any other audio playback is finished
     *
     * @param path Context path to use
     * @param audioId ID of the audio file
     */
    queueAudioFile: function (path, audioId) {
        var topUtils = obtainTopPivotalUtils();
        topUtils.audioQueue.push({path: path, audioId: audioId});
        if (!topUtils.audioIsPlaying) {
            topUtils.checkAudioQueue();
        }
    },

    /**
     * Check if there are any audio files queued, and play them if there are.
     * This shouldn't be called directly.
     */
    checkAudioQueue: function () {
        var topUtils = obtainTopPivotalUtils();
        if (topUtils.audioObject === undefined) {

            topUtils.audioObject = new Audio();

            if (topUtils.audioIsMuted) {
                topUtils.audioObject.volume = 0;
            } else {
                topUtils.audioObject.volume = 1;
            }

            topUtils.audioObject.addEventListener("ended", PivotalUtils.checkAudioQueue);
            topUtils.audioObject.addEventListener("error", PivotalUtils.checkAudioQueue);
        }

        if (topUtils.audioQueue.length > 0) {
            topUtils.audioIsPlaying = true;
            var details = topUtils.audioQueue.shift();
            if (details.audioId) {
                topUtils.audioObject.src = details.path + "/media/stream/" + details.audioId;
                topUtils.audioObject.play();
            }

        } else if (topUtils.loopingAudio) {
            topUtils.audioIsPlaying = true;
            topUtils.audioObject.src = PivotalUtils.loopingAudio.path + "/media/stream/" + PivotalUtils.loopingAudio.audioId;
            topUtils.audioObject.play();

        } else {
            topUtils.audioIsPlaying = false;
        }
    },

    /**
     * Sets the audio to be played
     * @param path Context path
     * @param audioId ID of the audio to play
     */
    setLoopingAudio: function (path, audioId) {
        var topUtils = obtainTopPivotalUtils();
        topUtils.loopingAudio = {path: path, audioId: audioId};
        if (!topUtils.audioIsPlaying) {
            topUtils.checkAudioQueue();
        }
    },

    /**
     * Remove the current running audio
     */
    unsetLoopingAudio: function () {
        var topUtils = obtainTopPivotalUtils();
        topUtils.loopingAudio = null;
    },

    /**
     * Turns on/off the alarm audio and optionally saves the preference
     * @param path Path to the server
     * @param mute True if the audio is to be turned on
     * @param save True if the audio setting should be saved
     */
    audioControl: function(path, mute, save) {
        var topUtils = obtainTopPivotalUtils();
        if (topUtils.audioObject) {
            topUtils.audioObject.volume = mute?0:1;
        }
        topUtils.audioIsMuted = mute;
        if (mute) {
            topUtils.$(".alarms-mute").addClass('muted');
        }
        else {
            topUtils.$(".alarms-mute").removeClass('muted');
        }
        if (save) {
            PivotalUtils.getContentAsync(path + "/alarm_control/alarm/preferences/save","mute=" + (mute?"true":"false"));
        }
    },

    /**
     * Takes a list of error field IDs and will make sure that the first
     * one in the list is visible as well as marking all the containing subtabs
     * with an error indicator
     * @param form Form or ID of form containing the errors
     * @param errorFields List of error field IDs
     */
    showErrorTab: function(form, errorFields) {
        if (form && errorFields) {
            form = (typeof form === "string")?$('#' + form):$(form);
            form.find('ul.pill-bar li,.tab-pane').removeClass('active form-errors');
            var firstTab = true;
            for (var errorField in errorFields) {
                if (errorFields.hasOwnProperty(errorField)) {
                    var errorTab = form.find("[name=" + errorField.replace(/\./g,"\\.") + "]").closest(".tab-pane");
                    if(errorTab && errorTab.hasOwnProperty("length") && errorTab.length > 0) {
                        var errorPill = form.find('ul.pill-bar a[href=#' + errorTab[0].id + ']').closest('li');
                        if (firstTab) {
                            errorTab.addClass("active");
                            errorPill.addClass("active form-errors");
                            firstTab = false;
                        }
                        else {
                            errorPill.addClass("form-errors");
                        }
                    }
                }
            }
        }
    },

    /**
     * Gets the value of the cookie with the name specified.
     *
     * @param cname name
     * @returns cookie value
     */
    getCookie : function(cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for(var i=0; i<ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0)===' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) !== -1) {
                return c.substring(name.length,c.length);
            }
        }
        return ""; // default value
    },


    /**
     * Performs an XOR encryption of the given message txt with the given key.
     *
     * @param txt       Encrypted text
     * @param key       Encryption key
     */
    xorEncode : function(txt, key) {

        var ord = [];
        var buf = "";

        for (z = 1; z <= 255; z++) {
            ord[String.fromCharCode(z)] = z;
        }

        var j = 0;
        var z = 0;
        for (j = z = 0; z < txt.length; z++) {
            buf += String.fromCharCode(ord[txt.substr(z, 1)] ^ ord[key.substr(j, 1)]);
            j = (j+1 < key.length) ? j + 1 : 0;
        }

        return buf;
    },


    /**
     * Encode string into Base64, as defined by RFC 4648 [http://tools.ietf.org/html/rfc4648]
     * (instance method extending String object). As per RFC 4648, no newlines are added.
     *
     * @param {String} str The string to be encoded as base-64
     * @param {Boolean} [utf8encode=false] Flag to indicate whether str is Unicode string to be encoded
     *   to UTF8 before conversion to base64; otherwise string is assumed to be 8-bit characters
     * @returns {String} Base64-encoded string
     */
    base64Encode : function(str, utf8encode) {  // http://tools.ietf.org/html/rfc4648
      utf8encode =  (typeof utf8encode == 'undefined') ? false : utf8encode;
      var o1, o2, o3, bits, h1, h2, h3, h4, e=[], pad = '', c, plain, coded;
      var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

      plain = utf8encode ? this.utf8Encode(str) : str;

      c = plain.length % 3;  // pad string to length of multiple of 3
      if (c > 0) {
          while (c++ < 3) {
              pad += '='; plain += '\0';
          }
      }

      // note: doing padding here saves us doing special-case packing for trailing 1 or 2 chars

      for (c=0; c<plain.length; c+=3) {  // pack three octets into four hexets
        o1 = plain.charCodeAt(c);
        o2 = plain.charCodeAt(c+1);
        o3 = plain.charCodeAt(c+2);

        bits = o1<<16 | o2<<8 | o3;

        h1 = bits>>18 & 0x3f;
        h2 = bits>>12 & 0x3f;
        h3 = bits>>6 & 0x3f;
        h4 = bits & 0x3f;

        // use hextets to index into code string
        e[c/3] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
      }
      coded = e.join('');  // join() is far faster than repeated string concatenation in IE

      // replace 'A's from padded nulls with '='s
      coded = coded.slice(0, coded.length-pad.length) + pad;

      return coded;
    },

    /**
     * Decode string from Base64, as defined by RFC 4648 [http://tools.ietf.org/html/rfc4648]
     * (instance method extending String object). As per RFC 4648, newlines are not catered for.
     *
     * @param {String} str The string to be decoded from base-64
     * @param {Boolean} [utf8decode=false] Flag to indicate whether str is Unicode string to be decoded
     *   from UTF8 after conversion from base64
     * @returns {String} decoded string
     */
    base64Decode : function(str, utf8decode) {
      utf8decode =  (typeof utf8decode == 'undefined') ? false : utf8decode;
      var o1, o2, o3, h1, h2, h3, h4, bits, d=[], plain, coded;
      var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

      coded = utf8decode ? this.utf8Decode(str) : str;


      for (var c=0; c<coded.length; c+=4) {  // unpack four hexets into three octets
        h1 = b64.indexOf(coded.charAt(c));
        h2 = b64.indexOf(coded.charAt(c+1));
        h3 = b64.indexOf(coded.charAt(c+2));
        h4 = b64.indexOf(coded.charAt(c+3));

        bits = h1<<18 | h2<<12 | h3<<6 | h4;

        o1 = bits>>>16 & 0xff;
        o2 = bits>>>8 & 0xff;
        o3 = bits & 0xff;

        d[c/4] = String.fromCharCode(o1, o2, o3);
        // check for padding
        if (h4 == 0x40) {
            d[c/4] = String.fromCharCode(o1, o2);
        }
        if (h3 == 0x40) {
            d[c/4] = String.fromCharCode(o1);
        }
      }
      plain = d.join('');  // join() is far faster than repeated string concatenation in IE

      return utf8decode ? this.utf8Decode(plain) : plain;
    },

    /**
     * Encode multi-byte Unicode string into utf-8 multiple single-byte characters
     * (BMP / basic multilingual plane only)
     *
     * Chars in range U+0080 - U+07FF are encoded in 2 chars, U+0800 - U+FFFF in 3 chars
     *
     * @param {String} strUni Unicode string to be encoded as UTF-8
     * @returns {String} encoded string
     */
    utf8Encode : function(strUni) {
        // use regular expressions & String.replace callback function for better efficiency
        // than procedural approaches
        var strUtf = strUni.replace(
            /[\u0080-\u07ff]/g,  // U+0080 - U+07FF => 2 bytes 110yyyyy, 10zzzzzz
            function(c) {
                var cc = c.charCodeAt(0);
                return String.fromCharCode(0xc0 | cc>>6, 0x80 | cc&0x3f);
            });
        strUtf = strUtf.replace(
            /[\u0800-\uffff]/g,  // U+0800 - U+FFFF => 3 bytes 1110xxxx, 10yyyyyy, 10zzzzzz
            function(c) {
                var cc = c.charCodeAt(0);
                return String.fromCharCode(0xe0 | cc>>12, 0x80 | cc>>6&0x3F, 0x80 | cc&0x3f);
            });
        return strUtf;
    },

    /**
     * Decode utf-8 encoded string back into multi-byte Unicode characters
     *
     * @param {String} strUtf UTF-8 string to be decoded back to Unicode
     * @returns {String} decoded string
     */
    utf8Decode : function(strUtf) {
        // note: decode 3-byte chars first as decoded 2-byte strings could appear to be 3-byte char!
        var strUni = strUtf.replace(
            /[\u00e0-\u00ef][\u0080-\u00bf][\u0080-\u00bf]/g,  // 3-byte chars
            function(c) {  // (note parentheses for precence)
                var cc = ((c.charCodeAt(0)&0x0f)<<12) | ((c.charCodeAt(1)&0x3f)<<6) | ( c.charCodeAt(2)&0x3f);
                return String.fromCharCode(cc);
            }
        );
        strUni = strUni.replace(
            /[\u00c0-\u00df][\u0080-\u00bf]/g,                 // 2-byte chars
            function(c) {  // (note parentheses for precence)
                var cc = (c.charCodeAt(0)&0x1f)<<6 | c.charCodeAt(1)&0x3f;
                return String.fromCharCode(cc);
            }
        );
        return strUni;
    },

    /**
     * Format a potentially large number of bytes into kB, MB or GB depending on the size
     *
     * @param size File size in bites
     * @returns {string} Nicely formatted file size (e.g "12.4 kB", "120 bytes")
     */
    formatFileSize : function(size){
        if(!size) {
            return "";
        }

        var unit = "bytes";
        var value = size;
        if(size >= 1073741824){
            unit = "GB";
            value = size / 1073741824;
        } else if(size >= 1048576) {
            unit = "MB";
            value = size / 1048576;
        } else if(size >= 1024){
            unit = "kB"
            value = size / 1024;
        }

        value = Math.round(100 * value) / 100;
        return value + " " + unit;
    },

    presentationMode : {
        /**
         * Number of seconds to keep a dashboard on display.
         */
        interval : 30,

        /**
         * Array of dashboards to iterate over.
         */
        dashboards : [],

        /**
         * Current count of seconds dashboard has been on display
         */
        count: 1,

        /**
         * Keep track of the timer so we can cancel it in various places if need be.
         */
        timer: null,

        /**
         * Keep track of the current dashboard on display.
         */
        current : null,

        /**
         * Keep track of the last dashboard on display before presentation mode started.
         */
        previous : null,

        /**
         * This will initialise the presentation mode
         *
         * @param {int} interval Number of seconds between each dashboard
         * @param {array} dashboards Array of dashboards to iterate through
         * @param {int} current The current dashboard id
         * @param {int} previous The previous dashboard id
         * @param {boolean} skip True if this dashboard should be skipped
         */
        init: function (interval, dashboards, current, previous, skip) {

            PivotalUtils.presentationMode.interval = interval;
            PivotalUtils.presentationMode.dashboards = dashboards;
            PivotalUtils.presentationMode.current = current;
            PivotalUtils.presentationMode.previous = previous;

            if ($('body').hasClass('presentation-mode') && !$('body').hasClass('presentation-mode-stationary') && dashboards.length > 1) {
                // Add the bootstrap progress bar to the DOM.
                jQuery('body').prepend(
                    jQuery('<div/>', {
                        id: 'presentation-progress',
                        class: 'progress'
                    }).append(
                        jQuery('<div/>', {
                            class: 'progress-bar',
                            role: '',
                            valuemin: 0,
                            valuemax: 100
                        })
                    )
                );

                if(skip) interval = 3;
                PivotalUtils.presentationMode.timer = setInterval(function () {
                    var percent = (PivotalUtils.presentationMode.count / interval * 100);
                    if (percent > 100) {
                        clearInterval(PivotalUtils.presentationMode.timer);
                        for (var i = 0; i < dashboards.length; i++) {
                            if (current == dashboards[i]) {
                                if (i < dashboards.length-1) {
                                    window.location.replace('/dashboard/presentationview?id=' + PivotalUtils.presentationMode.dashboards[i + 1] + '&mode=presentation');
                                }
                                else {
                                    window.location.replace('/dashboard/presentationview?id=' + PivotalUtils.presentationMode.dashboards[0] + '&mode=presentation');
                                }
                            }
                        }
                    } else {
                        $('#presentation-progress .progress-bar').css('width', percent + '%');
                        PivotalUtils.presentationMode.count++;
                    }
                }, 1000);
            }
        },

        /**
         *
         */
        start: function () {
            if(PivotalUtils.presentationMode.dashboards.length > 0) {
                window.location.replace('/dashboard/presentationview?id=' + PivotalUtils.presentationMode.dashboards[0] + '&mode=presentation');
            } else {
                alert('No dashboards configured.');
            }
        },

        /**
         *
         */
        stop: function () {
            window.location.replace('/dashboard/presentationview?stop=true&id=' + PivotalUtils.presentationMode.previous);
        }
    },

    log: function(message) {
        if (console)
            console.log(message);
    },

    // Used in the grid where we were getting the selection opened
    // as well as the download happening
    openURL: function(thisEvent, URL, newWindow) {
        if (thisEvent) thisEvent.stopPropagation();
        if (thisEvent) thisEvent.cancelBubble = true;
        if (newWindow) {
            var win = window.open(URL,'_blank');
            win.focus();
        }
        else
            document.location.href = URL;
        return false;
    }
};
// <<<<< End of PivotalUtils class

/**
 * Trims whitespace
 * @returns {string}
 */
String.prototype.trim = function () {
    return this.replace(/(?:(?:^|\n)\s+|\s+(?:$|\n))/g, "");
};

/**
 * Trims leading whitespace
 * @returns {string}
 */
String.prototype.trimLead = function () {
    return this.replace(/(?:(?:^|\n)\s+)/g, "");
};

/**
 * Trims trailing whitespace
 * @returns {string}
 */
String.prototype.trimTail = function () {
    return this.replace(/(\s+(?:$|\n))/g, "");
};

/**
 * Returns true if the string starts with this string
 */
if (typeof String.prototype.startsWith !== 'function') {
    String.prototype.startsWith = function (str) {
        return this.slice(0, str.length) === str;
    };
}

/**
 * Returns true if the string ends with this string
 */
if (typeof String.prototype.endsWith !== 'function') {
    String.prototype.endsWith = function (str) {
        return this.slice(-str.length) === str;
    };
}

