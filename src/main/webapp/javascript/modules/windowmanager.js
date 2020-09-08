/**
 * The Modal Window manager
 * Provides all the methods to track and manage a stack of modal windows that are based
 * around the Kendo UI Window.
 * All modal windows have to be owned by the top level browser window so that they can be
 * used to overlap each other but still be launch-able from within IFrames
 *
*/
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("windowManager");
    psApp.windowManager = (function () {
        var logger = psApp.logger.getLogger("psApp.windowManager", null);

        // The master Window Manager object
        var topWindowManager;

        // Stack of windowObject
        var windowStack = [];

        // Overall form dirty flag which is used when we don't have any windows in the stack
        var isDirty = false;

        /**
         * Obtain a reference to the topmost window in the applications domain.
         * @param topWindow the starting window frame - can be null when
         * @returns {Object} The highest found window that has a PivotalUtils
         */
        function obtainTopWindow(topWindow) {
            try {
                if (topWindow.parent != topWindow && topWindow.parent.psApp) {
                    return obtainTopWindow(topWindow.parent);
                }
            }
            catch (x) {}
            return topWindow;
        }

        /**
         * Window manager is static so this is only called once
         * @constructor
         */
        var WindowManager = function () {
            // Establish the top level container - this is the highest window in the stack
            // that supports this javascript module. This isn't necessarily window.top if the
            // application is being used within a foreign host e.g. Nlyte
            var topWindow = obtainTopWindow(self);
            if (self!==topWindow) {
                topWindowManager = topWindow.psApp.windowManager.topWindowManager;
            }
            else {
                topWindowManager = this;
                this.topWindowManager = this;
            }
        };

        /**
         * Add the methods available to the window manager.
         */
        WindowManager.prototype = {

            /**
             * Shows a Kendo modal dialog with content read from the URL.
             * The modal window will always be opened from the top window
             * @props map
             *        title Title to display
             *        width Initial width of the window (e.g. 400 or 70%)
             *        height Initial height of the window (e.g. 400 or 70%)
             *        content URL to get source
             *        useIFrame True if the content should be rendered inside an IFrame)
             *        closeFunction The function to call when the window is closed
             *        customActions List of actions to use
             *        ignoreDirty If true, ignore the dirty flag
             * @returns windowObject The KendoWindow object
             */
            open: function (props) {
                if (this !== topWindowManager) {
                    return topWindowManager.open(props);
                }

                // Create a new window and add it to the top of stack
                var win = new windowObject(props);
                windowStack.unshift(win);
                logger.info("Stack size: " + windowStack.length);
                return win;
            },

            /**
             * Get the current top most window
             *
             * @returns windowObject The currently top most modal window
             */
            getCurrentWindow: function () {
                if (this !== topWindowManager) {
                    return topWindowManager.getCurrentWindow();
                }

                // If we have something in the stack
                return windowStack.length == 0 ? null : windowStack[0];
            },

            /**
             * Get the 2nd most top window
             *
             * @returns windowObject The 2nd top most modal window
             */
            getParentWindow: function () {
                if (this !== topWindowManager) {
                    return topWindowManager.getParentWindow();
                }

                // If we have more than one thing in the stack
                return windowStack.length < 2 ? null : windowStack[1];
            },

            /**
             * Closes the current top most window
             * @param forceClose True if the window should be closed even if it is dirty
             * @return boolean True if the close was cancelled
             */
            close: function (forceClose) {
                if (this !== topWindowManager) {
                    return topWindowManager.close(forceClose);
                }
                // If there is something to close
                var cancelClose = false;
                if (windowStack.length > 0) {
                    var win = windowStack[0];

                    // If there is a close handler, then invoke and check
                    // to see if the caller has cancelled the close
                    if (win.closeFunction) {
                        cancelClose = win.closeFunction.call(win);
                    }

                    // If we have a dirty flag set, then don't close the window
                    // unless we are overriding
                    if (!cancelClose) {
                        cancelClose = !forceClose && !win.ignoreDirty && win.isDirty && (window.confirm(PivotalUtils.i18nBundle.navigateAwayMessage) !== true);
                    }

                    if (!cancelClose) {
                        windowStack.shift()._close();

                        // Set the focus to the previous input or the new frame
                        if (win.focusedElement) {
                            win.focusedElement.focus();
                        }
                        else if (windowStack.length > 0 && windowStack[0].frame) {
                            windowStack[0].focus();
                        }
                        logger.info("Stack size: " + windowStack.length);
                    }
                    else {
                        logger.info("Close cancelled:" + win.id);
                    }
                }
                return cancelClose;
            },

            /**
             * Closes all windows
             * @param forceClose True if the window should be closed even if it is dirty
             */
            closeAll: function (forceClose) {
                if (this !== topWindowManager) {
                    return topWindowManager.closeAll(forceClose);
                }

                // Close all the windows from the top, only stopping
                // if the user cancels
                while (windowStack.length > 0) {
                    if (this.close(forceClose)) {
                        break;
                    }
                }
            },

            /**
             * Sets the name of the active tab that this window is showing
             * This is used when a user refreshes the window - if there is
             * an active tab, then this name is appended to the URL
             * @param tabName Name of the tab
             */
            setActiveTab: function (tabName) {
                var win = this.getCurrentWindow();
                if (win) {
                    win.activeTab = tabName;
                }
            },

            /**
             * Called when the user clicks the refresh button
             * This method will check to see if the url of the frame should
             * be adjusted before refreshing - obviously this is only effective
             * if we are using an iframe
             */
            refresh: function () {
                if (this !== topWindowManager) {
                    return topWindowManager.refresh();
                }
                var ok = true;

                // If there is something to refresh
                if (windowStack.length > 0) {

                    // If we have an activeTab and an iframe
                    var win = windowStack[0];

                    // If we have a dirty flag set, then don't close the window
                    // unless we are overriding
                    var cancelRefresh = !win.ignoreDirty && win.isDirty && !window.confirm(PivotalUtils.i18nBundle.navigateAwayMessage);

                    if (cancelRefresh) {
                        logger.info("Refresh cancelled by dirty prompt")
                        ok = false;
                    }
                    else if (win.activeTab && win.iframe) {
                        win.window.location.href = win.contentVal + (win.contentVal.indexOf('?') < 0 ? "?" : "&") + "tab=" + win.activeTab;
                        logger.info("Refresh cancelled by active tab: " + windowStack[0].id);
                        ok = false;
                    }
                    else {
                        logger.info("Refreshed: " + windowStack[0].id);
                    }
                }
                // Carry on with the standard refresh
                return ok;
            },

            /**
             * Set the window as dirty for the current window
             * If not windows exist, set it for the current document
             */
            setDirty: function () {
                if (this !== topWindowManager) {
                    return topWindowManager.setDirty();
                }
                console.log("SetDirty");
                // If there is something to refresh
                if (windowStack.length > 0) {
                    windowStack[0].isDirty = true;
                    logger.info("Set dirty: " + windowStack[0].id);
                }
                else {
                    isDirty = true;
                    logger.info("Set master dirty");
                }
            },

            /**
             * Clear the dirty flag on the current or all windows
             * If no window exists, then clear it for the current document
             * @param all If true, all windows dirty flags are reset
             */
            clearDirty: function (all) {
                if (this !== topWindowManager) {
                    return topWindowManager.clearDirty(all);
                }
                console.log("ClearDirty");
                // If there is something to do
                if (windowStack.length > 0) {
                    if (all) {
                        $.each(windowStack, function (i, win) {
                            win.isDirty = false;
                            logger.info("Reset dirty:" + win.id);
                        });
                    }
                    else {
                        windowStack[0].isDirty = false;
                        logger.info("Reset dirty:" + windowStack[0].id);
                    }
                }
                else {
                    isDirty = false;
                    logger.info("Reset master dirty");
                }
            },

            /**
             * Returns the status of the dirty flag for the current window or if
             * no windows exist, the current document
             * @return boolean True if the dirty flag is set
             */
            isDirty: function () {
                if (this !== topWindowManager) {
                    return topWindowManager.isDirty();
                }
                if (windowStack.length > 0) {
                    return windowStack[0].isDirty;
                }
                else {
                    return isDirty;
                }
            }
        };

        /**
         * Creates a Kendo modal dialog with content read from the URL.
         * The modal window will always be opened from the top window
         *
         * @props map
         *        title Title to display
         *        width Initial width of the window (e.g. 400 or 70%)
         *        height Initial height of the window (e.g. 400 or 70%)
         *        content URL to get source
         *        useIFrame True if the content should be rendered inside an IFrame)
         *        closeFunction The function to call when the window is closed
         *        customActions List of actions to use
         *        ignoreDirty If true, ignore the dirty flag
         *
         * @returns The KendoWindow object
         */
        var windowObject = function (props) {
            var date = new Date();
            this.id = "kendoWindow_" + date.getTime();
            this.title = props.title;
            this.width = props.width?props.width:"800px";
            this.height = props.height?props.height:"600px";
            this.contentVal = props.content;
            this.adjustedUrl = props.content;
            this.useIFrame = props.useIFrame;
            this.closeFunction = props.closeFunction;
            this.customActions = props.customActions;
            this.ignoreDirty =  props.ignoreDirty;
            this.isDirty = false;
            this.activeTab = false;
            this.focusedElement = document.activeElement;

            // Construct the UI
            var div = document.createElement('div');
            div.id = this.id;
            document.body.appendChild(div);
            var modalWindow = $(div);

            // Add on any custom actions
            var allActions = ["Maximize", "Refresh", "Close"];
            if (this.customActions) {
                allActions = [];
                for (i = 0; i < this.customActions.length; i++) {
                    if (this.customActions[i].name) {
                        allActions.push(this.customActions[i].name);
                    }
                    else {
                        allActions.push(this.customActions[i]);
                    }
                }
            }

            // Setup the kendow window properties
            var properties = {
                modal: true,
                visible: false,
                pinned: true,
                appendTo: document.body,
                iframe: this.useIFrame
            };
            if (this.width) {
                properties.width = this.width;
            }
            if (this.height) {
                properties.height = this.height;
            }
            if (this.title) {
                properties.title = this.title;
            }
            if (allActions) {
                properties.actions = allActions;
            }

            // Make sure the URL is unique (cache buster)
            if (this.contentVal) {
                if (this.contentVal.match(/(^http:)|(^https:)|(^\/)/gi)) {
                    this.contentVal += (this.contentVal.indexOf('?') > -1 ? '&' : '?') + '_' + date.getTime();
                    properties.content = this.contentVal;
                }
                else {
                    modalWindow.html(this.contentVal);
                }
            }

            // Create the actual kendo window
            this.kendoWrapper = modalWindow.kendoWindow(properties);

            // Make the kendo window and it's content methods easily available
            this.kendoWindow = $(modalWindow[0]).data('kendoWindow');

            // Find the containing frame if there is one and add some
            // useful shortcuts
            var frames = $(this.kendoWrapper).find("iframe");
            if (frames.length > 0) {
                this.iframe = frames[0];
                this.window = (this.iframe.contentWindow||this.iframe);
            }
            else {
                this.window = self;
            }
            this.document = this.window.document;

            // Add on the custom button click handlers
            if (this.customActions) {
                for (var i = 0; i < this.customActions.length; i++) {
                    if (this.customActions[i].name && this.customActions[i].handler) {
                        var button = this.kendoWindow.wrapper.find(".k-i-" + this.customActions[i].name.toLowerCase()).closest("a");
                        button.data("callback", this.customActions[i].handler);
                        button.click(function () {
                            var callback = $(this).data("callback");
                            if (callback) {
                                callback.call(psApp.windowManager.getCurrentWindow());
                            }
                            return false;
                        });
                    }
                }
            }

            // Open the UI and center it
            this.kendoWindow.center().open();

            // Override the refresh action so that we can force the dialog
            // to return to the tab being viewed (if any)
            button = $($(this.kendoWindow.wrapper.find(".k-i-refresh").closest("a"))[0]);
            button.click(function () {
                return psApp.windowManager.refresh();
            });

            // Override the close action so that we can check the dirty flag and any
            // callback there might be. We always return false so that the close method
            // can programmatically close the kendo window
            button = $($(this.kendoWindow.wrapper.find(".k-i-close").closest("a"))[0]);
            button.click(function () {
                psApp.windowManager.close();
                return false;
            });

            // Some sanity
            logger.info(this.toString());
        };

        /**
         * Add the methods available to the window manager.
         */
        windowObject.prototype = {

            /**
             * Closes the top level window
             * This method is here as a convenience and shorthand for calling psApp.windowManager.close()
             * and is to catch the perhaps less than obvious mistake
             * @param forceClose True if the window should be closed even if it is dirty
             * @return boolean True if the close was cancelled
             */
            close: function (forceClose) {
                return psApp.windowManager.close(forceClose);
            },

            /**
             * Closes the kendo window if it exists - this is an internal call
             * and should never be used from outside this module
             */
            _close: function () {
                if (this.kendoWindow != null) {
                    this.kendoWindow.close();

                    // kendo leaves the wrapper hanging about which we want to get rid of because
                    // it clutters up the DOM - however, in IE and error will occur if any code
                    // inside the DOM element is invoked
                    var that = this;
                    setTimeout(function() {
                        that.kendoWindow.destroy();
                    },1000);
                }
            },

            /**
             * Surrogate for the standard kendo content method
             * @param value Value to set
             * @returns string Current content of the window
             */
            content: function(value) {
                return this.kendoWindow.content(value);
            },

            /**
             * Returns the string representation of the window
             * @return String representation of the window
             */
            toString: function () {
                var tmp = "";
                for (var prop in this) {
                    if (this.hasOwnProperty(prop)) {
                        tmp += (tmp === "" ? "" : ", ") + prop + "=" + this[prop];
                    }
                }
                return "Created Window [" + tmp + "]";
            }
        };

        // We only want one instance of the window manager
        return new WindowManager();
    })();

}

