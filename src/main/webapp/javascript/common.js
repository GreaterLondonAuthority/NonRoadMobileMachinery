/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

/*global $:false, jQuery:false */
"use strict";

// PhantomJS doesn't support bind yet
Function.prototype.bind = Function.prototype.bind || function (thisp, arg) {
    var fn = this;
    return function () {
    return fn.apply(thisp, arg);
    };
};

/**
 * Obtain a reference to the topmost window in the applications domain.
 * @param topWindow the starting window frame - can be null when
 * @returns {Object} The highest found window that has a PivotalUtils
 */
function obtainTopWindow(topWindow) {
    try {
        if (topWindow.parent != topWindow && topWindow.parent.PivotalUtils) {
            return obtainTopWindow(topWindow.parent);
        }
    }
    catch (x) {
        // This will fall out to the correct highest window
    }
    return topWindow;
}

/**
 * Obtain a reference to the topmost window that has a pivotalutils reference
 * @returns {PivotalUtils} The topmost utils
 */
function obtainTopPivotalUtils() {
    var topWindow = obtainTopWindow(self);
    if(topWindow && topWindow.PivotalUtils)
        return topWindow.PivotalUtils;
    else return null;
}

// setSidebarHeight on load
$(document).ready(PivotalUtils.setSidebarHeight);

// setSidebarHeight on window resize
var setSidebarHeightTimeout;
$(window).resize(function () {
    clearTimeout(setSidebarHeightTimeout);
    setSidebarHeightTimeout = setTimeout(PivotalUtils.setSidebarHeight, 300);
});

// setSidebarHeight when dashboard widget drag/drop/resize increases the scroll
$(window).load(function () {
    clearTimeout(setSidebarHeightTimeout);
    setSidebarHeightTimeout = setTimeout(PivotalUtils.setSidebarHeight, 300);
});

var AIM = {
    /**
     * Construct an invisible IFRAME to catch the output from the form upload
     * @param form Form to change target of
     * @param config Configuration - can have 2 methods onStart and onComplete
     * onStart is called when the submit is activated and can cancel a submission
     * onComplete is called once the iframe has been loaded
     */
    submit: function (form, config) {
        var name = 'f' + Math.floor(Math.random() * 99999);
        var div = document.createElement('DIV');
        div.id = "div_" + name;
        div.innerHTML = '<iframe style="display:none" src="about:blank" id="' + name + '" name="' + name + '" onload="AIM.loaded(\'' + name + '\')"></iframe>';
        document.body.appendChild(div);
        var iFrame = document.getElementById(name);
        if (config && typeof(config.onComplete) === 'function') {
            iFrame.onComplete = config.onComplete;
        }
        form.setAttribute('target', name);
        if (config && typeof(config.onStart) === 'function') {
            if (config.onStart()) {
                form.submit();
            }
            else {
                AIM.remove(div);
            }
        }
        else {
            form.submit();
        }
    },

    /**
     * This is the callback made by the IFRAME on the onLoad event
     * It calls any callback registered in the configuration and then
     * removes the temporary IFRAME from the document
     * @param id ID of the IFRAME
     */
    loaded: function (id) {
        var documentContainer;
        var iFrame = document.getElementById(id);
        if (iFrame.contentDocument){
            documentContainer = iFrame.contentDocument;
        }
        else if (iFrame.contentWindow) {
            documentContainer = iFrame.contentWindow.document;
        }
        else {
            documentContainer = window.frames[id].document;
        }
        if (documentContainer.location.href === "about:blank") {
            return;
        }
        if (typeof(iFrame.onComplete) === 'function') {
            if (documentContainer.contentType.indexOf("json")>-1) {
                iFrame.onComplete(documentContainer, documentContainer.body.innerText);
            }
            else {
                iFrame.onComplete(documentContainer, documentContainer.body.innerHTML);
            }
            AIM.remove(documentContainer);
        }
    },

    /**
     * Removes the div containing th temporary IFRAME
     * @param div Div to remove
     */
    remove : function(div) {
        if (div.parentWindow) {
            var tmp = document.getElementById("div_" + div.parentWindow.name);
            if (tmp) {
                document.body.removeChild(tmp);
            }
        }
    }
};

/**
 * Provide a console for those browser that don't
 */
var console = window.console;
if (!window.console) {
    console = {log: function(text) {}};
}

/**
 * MacroUtils namespace functions
 */
function AppEntitySelector(wrapperId, settings) {

    var handler= this;
    this.bound = false;
    this.isMultiSelect = false;
    this.wrapperId = wrapperId;
    this.wrapper = $(document.getElementById(wrapperId));
    this.wrapper.data("control", this);
    this.content = this.wrapper.find("div.k-multiselect-wrap");
    for (var setting in settings) {
        if (settings.hasOwnProperty(setting)) {
            this[setting] = settings[setting];
        }
    }

    /**
     * Shows the list of selected objects and adds the required handlers
     */
    this.show = function() {
        this.content.empty();
        this.content.append(kendo.render(kendo.template($("#entity-input-template").html()), this.basket));
        if (this.content.html()=='') {
            this.content.attr('placeholder', this.placeHolder);
            this.content.attr('hasplaceholder', true);
        }
        // Show tooltips
        PivotalUtils.showFormTooltips(this.content);
        if (this.resizeNow) {
            this.resizeNow();
        }
        if (this.readOnly !== true) {
            // Delete handler
            $(this.wrapper).find("span.k-delete").click(function (event) {
                event.stopPropagation();
                event.preventDefault();
                $(this).closest("div.entity-input").data("control").remove(this);
            });

            // Search handler if we don't have one
            if (!this.bound && !this.noSearch) {
                this.bound = true;
                $(this.wrapper).click(function (event) {
                    event.stopPropagation();
                    event.preventDefault();
                    var control = $(this).closest("div.entity-input").data("control");
                    var path = control.path;
                    var viewLocation = control.viewLocation ? control.viewLocation : "/admin/search_entity/search";
                    var width = control.width ? control.width : "700px";
                    var height = control.height ? control.height : "500px";
                    control.popup = psApp.windowManager.open({title:control.title, width:width, height:height,
                                    content:path + viewLocation + "?id=" + control.wrapperId +
                                    "&entity=" + control.entity +
                                    "&entitytype=" + control.entityTypeName +
                                    "&pagename=" + control.pageName +
                                    "&" + control.properties, useIFrame:true, closeFunction:handler.focusInput});
                    return false;
                });
            }
        }
    };

    /**
     * Removes the button from the list and updates the underlying storage
     * @param item Item element to remove
     */
    this.remove = function(item) {
        var value = $(item).closest("span.entity-value").attr("value");

        var basketCopy = [];
        for (var iBasket = 0; iBasket < this.basket.length; iBasket++) {
            if (!(this.basket[iBasket].value === value)) {
                basketCopy.push(this.basket[iBasket]);
            }
        }
        this.basket = basketCopy;

        $(item).parent().remove();
        this.updateInput();
    };

    /**
     * Adds a new value item to the basket
     * @param value Value to add
     * @param text Text of the item
     * @param description Description of the item
     */
    this.add = function(value, text, description) {
        if (this.isMultiSelect) {
            var basketCopy = [];
            for (var i = 0; i < this.basket.length; i++) {
                if (this.basket[i].value === value) {
                    return;
                }
                else {
                    basketCopy.push(this.basket[i]);
            }
            }
            basketCopy.push({"value": value, "text": text, "description": description});
            this.basket = basketCopy;
            this.show();
        }
        else {
            this.basket = [{"value":value, "text":text, "description":description}];
            this.saveItems();
        }
    };

    /**
     * Cancels the edit session and closes the popup if it exists
     * Notice that this is a destroy, not a close (IE requires this)
     */
    this.cancel = function() {
        if (this.source) {
            this.source.popup.destroy();
        }
    };

    /**
     * Updates the input control with the values from the basket
     */
    this.updateInput = function() {

        // Add the values to the input control
        var inputControl = this.wrapper.find("input");
        if (inputControl.length) {
            var values = "";
            if (this.basket && this.basket.length > 0) {
                for (var i = 0; i < this.basket.length; i++) {
                    values += (i > 0 ? "," : "") + this.basket[i].value;
                }
            }
            inputControl.val(values).trigger("change");
        }
    };

    /**
     * Saves the items back to the source and closes the popup
     * Notice that this is a destroy, not a close (IE requires this)
     */
    this.saveItems = function() {
        if (this.source) {
            this.source.basket = this.basket;
            this.source.show();
            this.source.updateInput();
            psApp.windowManager.close(true);
        }
    };

    /**
     * Will try and focus on the input box if it is visible
     */
    this.focusInput = function() {
        var inputControl = handler.wrapper.find("input");
        if (inputControl.length) {
            try {
                inputControl.focus();
            }
            catch (x) {
                $(inputControl).closest(window).focus();
            }
        }
    }
}
