/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

/**
 * This will add the dashboard module to the psApp global namespace
 *
* @type {psApp|*|Function}
 */

// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("dashboards");
    psApp.dashboards = (function($, PivotalUtils) {

        /**
         * This is the global namespace to use the utility classes and dashboard functionality. It contains some useful utility methods
         * that can be used outside of dashboards.
         * JQuery functionality is required so if it is not present it will be loaded by the script.
         *
        * @type {Dashboards|*}
         */
        var Dashboards = (function() {

            /**
             * This is the configuration expected for creating any manager.
             * Each manager requires the same configuration (except changing the respective ID)
             *
             * @returns {Configuration}
            */
            var Configuration = (function () {

                /**
                 * Encapsulate the class from the outside world, which has the effect of making the properties private
                 *
                 * @param configMap The map of configuration that should contain the following
                 *          edit: Whether the dashboard should be shown in edit mode (true by default)
                 *          appPath: The root path of the application E.g. '/psApp'
                 *          rootPath: The root path of the application dashboard E.g. '/psApp/dashboard'
                 *          apiVersion: The version of the API that you are using (defaults to '1.0')
                 *          restPath: The path to the REST interface (by default this uses '{rootPath/apiVersion}')
                 *          layoutEndPoint: The end point for the REST dashboard layout (defaults to 'layout')
                 *          widgetsEndPoint: The end point for the REST widgets layout (defaults to 'widgets')
                 *          dashboardId: The identifier of the dashboard that should be loaded or the parent of the widget
                 *          dashboard: If there is a parent dashboard manager getDashboard() will return this instance
                 *          widgetId: The identifier of the widget that should be loaded
                 *          layoutManagerClass: The layout manager to use
                 *          widgetDialog: The properties for the widget dialog
                 *              id: The id to give to the dialog window (this defaults to 'widgets-dialog')
                 *              title: The title to show for the widget dialog
                 *              width: The width of the dialog (defaults to 650px)
                 *              width: The height of the dialog (defaults to 450px)
                 *              url: The url to load within the dialog
                 *          widgetConfigurationDialog: The properties for the widget configuration dialog
                 *              id: The id to give to the dialog window (this defaults to 'widget-"+ widgetId + "-configuration-dialog')
                 *              title: The title to show for the widget configuration dialog
                 *              width: The width of the configuration dialog (defaults to 650px)
                 *              width: The height of the configuration dialog (defaults to 450px)
                 *              url: The url to load within the configuration dialog
                 */
                var Configuration = function (configMap) {
                    this._configMap = configMap || {};
                    this.initialize();
                };

                /**
                 * Add in the required methods for configuration
                 *
                 * @type {{initialize: initialize, updateProperties: updateProperties, clone: clone}}
                 */
                Configuration.prototype = {

                    /**
                     * Will initialize the configuration
                     */
                    initialize: function() {
                        this.edit = this._configMap.hasOwnProperty("edit") ? this._configMap.edit : true;
                        this.mappingId = this._configMap.mappingId ? this._configMap.mappingId : null;
                        this.appPath = this._configMap.appPath || ""; // SS-419 JS treat empty string as false whereas it should be just empty
                        this.rootPath = this.appPath + this._configMap.rootPath || null;
                        this.apiVersion = this._configMap.apiVersion || null;
                        this.restPath = this._configMap.restPath || (this.rootPath + "/" + this.apiVersion);
                        this.layoutEndPoint = this._configMap.layoutEndPoint || "layout";
                        this.widgetsEndPoint = this._configMap.widgetsEndPoint || "widgets";
                        this.dashboardId = this._configMap.dashboardId || null;
                        this.widgetId = this._configMap.widgetId || null;
                        this.widgetConfigurationModel = this._configMap.widgetConfigurationModel || null;
                        this.widgetPropertyModel = this._configMap.widgetPropertyModel || null;
                        this.widgetConfiguration = this._configMap.widgetConfiguration || null;
                        this.dashboard = this._configMap.dashboard || null;
                        this.widget = this._configMap.widget || null;
                        this.propertyName = this._configMap.propertyName || null;
                        this.dashboardRestPath = this.restPath + "/" + this.dashboardId;
                        this.widgetsRestPath = this.dashboardRestPath + "/" + this.widgetsEndPoint;
                        this.widgetRestPath = this.widgetsRestPath + "/" + this.widgetId;
                        this.widgetRestContentPath = this.widgetRestPath + "/content" + ("?edit=" + this.edit) + (this.mappingId ? ("&mappingId="+this.mappingId) : "");
                        this.layoutManagerClass = this._configMap.layoutManagerClass || null;

                        // We create an empty map
                        this.widgetDialog = {};
                        this._configMap.widgetDialog = this._configMap.widgetDialog || {};
                        this.widgetConfigurationDialog = {};
                        this._configMap.widgetConfigurationDialog = this._configMap.widgetConfigurationDialog || {};
                        this.widgetAnalyserDialog = {};
                        this._configMap.widgetAnalyserDialog = this._configMap.widgetAnalyserDialog || {};
                        this.dashboardDeleteConfirmation =  {};
                        this._configMap.dashboardDeleteConfirmation = this._configMap.dashboardDeleteConfirmation || {};
                        this.widgetDeleteConfirmation = {};
                        this._configMap.widgetDeleteConfirmation = this._configMap.widgetDeleteConfirmation || {};

                        // Default values for the widget dialog
                        this.widgetDialog.id = this._configMap.widgetDialog.id || "widgets-dialog";
                        this.widgetDialog.title = this._configMap.widgetDialog.title || "No Title Provided";
                        this.widgetDialog.width = this._configMap.widgetDialog.width || 970;
                        this.widgetDialog.height = this._configMap.widgetDialog.height || "80%";
                        this.widgetDialog.url = this._configMap.widgetDialog.url || (this.rootPath + "/" + this.widgetsEndPoint);

                        // Default values for the widget configuration dialog
                        this.widgetConfigurationDialog.id = this._configMap.widgetConfigurationDialog.id || ("widget-"+ this.widgetId + "-configuration-dialog");
                        this.widgetConfigurationDialog.title = this._configMap.widgetConfigurationDialog.title || "No Title Provided";
                        this.widgetConfigurationDialog.width = this._configMap.widgetConfigurationDialog.width || 800;
                        this.widgetConfigurationDialog.height = this._configMap.widgetConfigurationDialog.height || "80%";
                        this.widgetConfigurationDialog.url = this._configMap.widgetConfigurationDialog.url || (this.rootPath + "/configuration?dashboardId=" + this.dashboardId + "&widgetId=" + this.widgetId);

                        // Default values for the widget analyser dialog
                        this.widgetAnalyserDialog.id = this._configMap.widgetAnalyserDialog.id || ("widget-"+ this.widgetId + "-analyser-dialog");
                        this.widgetAnalyserDialog.title = this._configMap.widgetAnalyserDialog.title || "No Title Provided";
                        this.widgetAnalyserDialog.width = this._configMap.widgetAnalyserDialog.width || "80%";
                        this.widgetAnalyserDialog.height = this._configMap.widgetAnalyserDialog.height || "80%";
                        this.widgetAnalyserDialog.url = this._configMap.widgetAnalyserDialog.url || (this.appPath + "/analyser/widget?dashboard_id=" + this.dashboardId + "&widget_id=" + this.widgetId);

                        // Default values for the dashboard delete confirmation
                        this.dashboardDeleteConfirmation.id = this._configMap.dashboardDeleteConfirmation.id || ("confirm-delete-dashboard-"+ this.dashboardId);
                        this.dashboardDeleteConfirmation.title = this._configMap.dashboardDeleteConfirmation.title || "Confirm Deletion";
                        this.dashboardDeleteConfirmation.messageText = this._configMap.dashboardDeleteConfirmation.messageText || "Are you sure you wish to delete the dashboard?";
                        this.dashboardDeleteConfirmation.confirmButtonText = this._configMap.dashboardDeleteConfirmation.confirmButtonText || "Yes";
                        this.dashboardDeleteConfirmation.cancelButtonText = this._configMap.dashboardDeleteConfirmation.cancelButtonText || "No";

                        // Default values for the widget delete confirmation
                        this.widgetDeleteConfirmation.id = this._configMap.widgetDeleteConfirmation.id || ("confirm-delete-widget-"+ this.widgetId);
                        this.widgetDeleteConfirmation.title = this._configMap.widgetDeleteConfirmation.title || "Confirm Deletion";
                        this.widgetDeleteConfirmation.messageText = this._configMap.widgetDeleteConfirmation.messageText || "Are you sure you wish to delete the widget?";
                        this.widgetDeleteConfirmation.confirmButtonText = this._configMap.widgetDeleteConfirmation.confirmButtonText || "Yes";
                        this.widgetDeleteConfirmation.cancelButtonText = this._configMap.widgetDeleteConfirmation.cancelButtonText || "No";

                        // The configuration values for extracting any config properties
                        this.widgetConfigurationRestPath = this.widgetRestPath + "/configuration";
                        this.widgetConfigurationRestPathFull = this.widgetConfigurationRestPath + (this.mappingId ? ("?mappingId="+this.mappingId) : "");

                        // The configuration values for extracting any config properties
                        this.widgetConfigurationPropertyRestPath = this.widgetConfigurationRestPath + "/" + this.propertyName + (this.mappingId ? ("?mappingId="+this.mappingId) : "");
                    },

                    /**
                     * Will update the properties using those supplied
                     *
                     * @param {{}} overridingProperties an overriding properties
                     */
                    updateProperties: function(overridingProperties) {
                        for (var override in overridingProperties) {
                            if(overridingProperties.hasOwnProperty(override)) {
                                this[override] = overridingProperties[override];
                            }
                        }
                    },

                    /**
                     * Will clone the current configuration and return a new copy. Any values passed into the method
                     * will override the existing properties.
                     *
                     * @param {{}} overridingProperties an overriding properties
                     * @returns {Configuration} A clone of this configuration map
                     */
                    clone: function (overridingProperties) {
                        var cloneMap = {};
                        for (var prop in this._configMap) {
                            if(this._configMap.hasOwnProperty(prop)) {
                                cloneMap[prop] = this._configMap[prop];
                            }
                        }
                        for (var override in overridingProperties) {
                            if(overridingProperties.hasOwnProperty(override)) {
                                cloneMap[override] = overridingProperties[override];
                            }
                        }
                        return new Configuration(cloneMap);
                    }
                };

                // Return the configuration
                return {
                    'new': function(configMap){return new Configuration(configMap);},
                    Configuration: Configuration
                };
            })();

            // The dashboard event manager - reserved for events related to all dashboard functionality
            var dashboardEventManager = new psApp.eventManager.EventManager("Dashboard Event Manager");

            /**
             * Module pattern for providing the classes we require for the dashboard manager.
             *
            */
            var Dashboard = (function () {

                /**
                 * Builds a new dashboard manager that will manage one dashboard specified by the identifier.
                 *
                 * @returns {Dashboard}
                 */
                var Dashboard = (function () {

                    // Our own logger
                    var logger = psApp.logger.getLogger("psApp.Dashboards.Dashboard", null);

                    /**
                     * The actual constructor that will be used and returned, keeping the rest of the item encapsulated (private).
                     *
                     * @param configuration The dashboard configuration from above
                     */
                    var Dashboard = function (configuration) {
                        // Store the configuration
                        this._configuration = configuration;
                        this.initialize();
                    };

                    /**
                     * Add the methods for manipulating a dashboard.
                     *
                     * @type {{initialize: initialize, editMode: editMode, getConfig: getConfig, getId: getId, refresh: refresh, delete: 'delete', deleteDialog: deleteDialog, getModel: getModel, refreshWidgets: refreshWidgets, getLayout: getLayout, getMobileLayout: getMobileLayout, getWidgets: getWidgets, getWidget: getWidget, getProxyWidget: getProxyWidget, showWidgetsDialog: showWidgetsDialog, lockDashboard: lockDashboard, unlockDashboard: unlockDashboard, dashboardLockHandler: dashboardLockHandler, newWidgetAddedHandler: newWidgetAddedHandler, removeNewWidgetAddedHandler: removeNewWidgetAddedHandler, onWidgetUpdateHandler: onWidgetUpdateHandler, removeOnWidgetUpdateHandler: removeOnWidgetUpdateHandler, widgetUpdated: widgetUpdated, addWidget: addWidget}}
                     */
                    Dashboard.prototype = {

                        /**
                         * Initialises the event manager
                         */
                        initialize: function() {
                            if(!this._configuration.dashboardId) {
                                logger.error("Manager instance creation attempted without providing the dashboard identifier");
                                throw new Error("You must provide the dashboard ID within the configuration");
                            }

                            // Setup the manager
                            this.refresh();
                        },

                        /**
                         * Returns whether the dashboard is in edit mode
                         *
                         * @returns {boolean} true if the dashboard is in edit mode
                         */
                        editMode: function() {
                            return (this._configuration.edit  ? true : false);
                        },

                        /**
                         * This returns the configuration for this dashboard.
                         *
                         * @returns {*}
                         */
                        getConfig: function() {
                            return this._configuration;
                        },

                        /**
                         * Will return the identifier of the dashboard this is managing
                         *
                         * @returns {*} The id of the dashboard
                         */
                        getId: function() {
                            return Number(this._configuration.dashboardId);
                        },

                        /**
                         * Will refresh the current dashboard model
                         */
                        refresh: function() {
                            // Reset all the promises
                            this._dashboardPromise = null;
                            this._deleteDashboardPromise = null;
                            this._layoutPromise = null;
                            this._widgetsPromise = null;
                        },

                        /**
                         * Will attempt to delete the dashboard
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        'delete': function() {

                            // Attempt to delete the dashboard
                            var dashboardManager = this;
                            this._deleteDashboardPromise = this._deleteDashboardPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Deleting dashboard [" + dashboardManager._configuration.dashboardId + "] from URL: " + dashboardManager._configuration.dashboardRestPath);

                                // We want to load the dashboard model
                                var request = $.ajax({
                                    cache: false,
                                    url: dashboardManager._configuration.dashboardRestPath,
                                    type: "DELETE"
                                });

                                // If the result is successful we can resolve the ticket
                                request.done(function(result) {
                                    logger.debug("Received dashboard [" + dashboardManager._configuration.dashboardId + "] result");
                                    logger.log(result);
                                    if (!result || !result["success"]) {
                                        logger.debug("Failed to delete dashboard [" + dashboardManager._configuration.dashboardId + "]");
                                        reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                    }
                                    else {
                                        logger.debug("Successfully deleted dashboard [" + dashboardManager._configuration.dashboardId + "]");

                                        // We need to initialize this manager again as the dashboard has been deleted
                                        dashboardManager.initialize();
                                        resolve(result["success"]);
                                    }
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {

                                    // As this is most likely a connection issue we should allow the user to try again
                                    dashboardManager._deleteDashboardPromise = null;
                                    reject(error);
                                });
                            });
                            return this._deleteDashboardPromise;
                        },

                        /**
                         * Wraps the delete feature in a dialog window that will be shown to the user before the delete is called.
                         * The deleteCallback function will be passed either true or an error if it could not be deleted.
                         *
                         * @param {Function} deleteCallback Will be called if the user chooses to delete the widget
                         * @param {Function} cancelCallback Will be called if the user chooses to cancel the deletion
                         */
                        deleteDialog: function(deleteCallback, cancelCallback) {
                            if(deleteCallback && !(deleteCallback instanceof Function))
                                throw new Error("You must provide a valid delete callback function");
                            if(cancelCallback && !(cancelCallback instanceof Function))
                                throw new Error("You must provide a valid cancel callback function");
                            if(!PivotalUtils)
                                throw new Error("PivotalUtils library is not available");

                            // Will open the modal dialog loading the URL to the list of widgets that was configured
                            PivotalUtils.showConfirmationDialog(
                                this._configuration.dashboardDeleteConfirmation.id,
                                this._configuration.dashboardDeleteConfirmation.title, {
                                    messageText: this._configuration.dashboardDeleteConfirmation.messageText,
                                    confirmText: this._configuration.dashboardDeleteConfirmation.confirmButtonText,
                                    cancelText: this._configuration.dashboardDeleteConfirmation.cancelButtonText,
                                    showCancelButton: true,
                                    confirmFunction: function () {

                                        // We just need to call the delete handler and then use the callbacks
                                        this.delete().then(
                                            function(success) {
                                                if(deleteCallback) deleteCallback(success);
                                            },
                                            function(error) {
                                                if(deleteCallback) deleteCallback(error);
                                            }
                                        )
                                    }.bind(this),
                                    cancelFunction: function() {
                                        if(cancelCallback) cancelCallback();
                                    }
                                }
                            );
                        },

                        /**
                         * Will return a promise to the dashboard model that will be retrieved remotely or from its cache.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getModel: function() {

                            // We want to load the dashboard model and store it as a promise
                            var dashboardManager = this;
                            this._dashboardPromise = this._dashboardPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading dashboard model [" + dashboardManager._configuration.dashboardId + "] from URL: " + dashboardManager._configuration.dashboardRestPath);

                                // Make the request and then wait for the response
                                var request = $.ajax({
                                    cache: false,
                                    url: dashboardManager._configuration.dashboardRestPath,
                                    type: "GET",
                                    dataType: "json"
                                });

                                // We want to load the dashboard model
                                request.done(function(result) {
                                    logger.debug("Received dashboard [" + dashboardManager._configuration.dashboardId + "] result");
                                    logger.log(result);
                                    if (!result || !result["success"] || !result["dashboard"]) {
                                        logger.debug("Failed to retrieve dashboard model [" + dashboardManager._configuration.dashboardId + "]");
                                        reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                    }
                                    else {
                                        // We have the dashboard to pass back
                                        logger.debug("Successfully retrieved dashboard model [" + dashboardManager._configuration.dashboardId + "]");
                                        logger.log(result["dashboard"]);
                                        resolve(result["dashboard"]);
                                    }
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {

                                    // As this is most likely a connection issue we should allow the user to try again
                                    dashboardManager._dashboardPromise = null;
                                    reject(error);
                                });
                            });
                            return this._dashboardPromise;
                        },

                        /**
                         * This will refresh the widget list the next time that it is requested.
                         */
                        refreshWidgets: function() {

                            // Nullify the widgets promise so that they will be loaded again
                            this._widgetsPromise = null;
                        },

                        /**
                         * Will return a promise to the layout model that will be retrieved.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getLayout: function() {

                            // We want to load the layout model for the current dashboard
                            var dashboardManager = this;
                            this._layoutPromise = this._layoutPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading layout model for dashboard [" + dashboardManager._configuration.dashboardId + "]");

                                // We just need the actual model to retrieve the layout identifier
                                dashboardManager.getModel().then(function(dashboard) {
                                    logger.debug("Using dashboard model [" + dashboardManager._configuration.dashboardId + "] to get layout");

                                    // Now we have the model we can resolve the promise
                                    if(!dashboard || !dashboard["layout"]) {
                                        logger.debug("Dashboard model [" + dashboardManager._configuration.dashboardId + "] does not have a layout");
                                        reject(new Error("No layout exists for the dashboard [" + dashboardManager._configuration.dashboardId + "]"));
                                    }
                                    else {
                                        logger.debug("Layout model [" + dashboard["layout"] + "] retrieved");

                                        // We basically pass along the configuration for it to manage
                                        var configuration = dashboardManager._configuration.clone({
                                            widgetId: dashboard["layout"],
                                            dashboardId: dashboardManager._configuration.dashboardId,
                                            dashboard: dashboardManager
                                        });
                                        resolve(Widget.new(configuration));
                                    }
                                }, function(error) {
                                    logger.debug("Error loading dashboard model [" + dashboardManager._configuration.dashboardId + "]");
                                    reject(error);
                                });
                            });
                            return this._layoutPromise;
                        },

                        /**
                         * This is technically the same as the normal getLayout method but it will likely change in the
                         * future, hence why there is a particular method for it now.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getMobileLayout: function() {
                            return this.getLayout();
                        },

                        /**
                         * Will return a promise to access the list of widgets models that will be retrieved.
                         * The actual promise content will be an array of {Widget} instances
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getWidgets: function() {

                            // We want to load the layout model for the current dashboard
                            var dashboardManager = this;
                            this._widgetsPromise = this._widgetsPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading widget models for dashboard [" + dashboardManager._configuration.dashboardId + "]");

                                /**
                                 * This will create widget managers from the list of widget identifiers
                                 *
                                 * @param widgets The list of identifiers
                                 * @returns {Array} The list of widget managers
                                 */
                                var widgetParser = function(widgets) {
                                    // Create our list of managers
                                    var widgetManagers = [];
                                    for(var i = 0; i < widgets.length; i++) {
                                        var id = widgets[i];

                                        // Check that this is a list of numbers
                                        if(!isNaN(parseInt(id))) {
                                            logger.debug("Widget model [" + id + "] found");

                                            // We basically pass along the configuration for it to manage
                                            var configuration = dashboardManager._configuration.clone({
                                                widgetId: id,
                                                dashboardId: dashboardManager._configuration.dashboardId,
                                                dashboard: dashboardManager
                                            });
                                            widgetManagers.push(Widget.new(configuration));
                                        }
                                        else {
                                            logger.debug("Dashboard model [" + dashboardManager._configuration.dashboardId + "] returned a non numeric widget id: " + id);
                                        }
                                    }
                                    return widgetManagers;
                                };

                                // We just need the actual model to retrieve the layout identifier
                                dashboardManager.getModel().then(function(dashboard) {
                                    logger.debug("Using dashboard model [" + dashboardManager._configuration.dashboardId + "] to get widgets");

                                    // Now we have the model we can resolve the promise
                                    if(!dashboard) {
                                        logger.debug("Dashboard model [" + dashboardManager._configuration.dashboardId + "] does not exist");
                                        reject(new Error("No dashboard [" + dashboardManager._configuration.dashboardId + "] model"));
                                    }
                                    else {
                                        // SS-2007 Always fetch the widgets to ensure that they are correctly synced
                                        // I originally used the dashboard model but as this is synced at different times
                                        // it means keeping it in sync in JS which causes more issues than it is worth
                                        logger.debug("Loading dashboard [" + dashboardManager._configuration.dashboardId + "] widgets remotely");

                                        // Make the request and then wait for the response
                                        var request = $.ajax({
                                            cache: false,
                                            url: dashboardManager._configuration.widgetsRestPath,
                                            type: "GET",
                                            dataType: "json"
                                        });

                                        // We need to load the widgets remotely
                                        request.done(function (result) {
                                            logger.debug("Received dashboard [" + dashboardManager._configuration.dashboardId + "] widgets result");
                                            logger.log(result);
                                            if (!result || !result["success"] || !result["widgets"]) {
                                                logger.debug("Failed to retrieve dashboard " + dashboardManager._configuration.dashboardId + "] widget models");
                                                reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                            }
                                            else {
                                                // We have the widgets to pass back
                                                logger.debug("Successfully retrieved dashboard [" + dashboardManager._configuration.dashboardId + "] widget models");
                                                logger.log(result["widgets"]);
                                                resolve(widgetParser(result["widgets"]));
                                            }
                                        });

                                        // If the request fails then we should send back a rejection
                                        request.fail(function(error) {

                                            // As this is most likely a connection issue we should allow the user to try again
                                            dashboardManager._widgetsPromise = null;
                                            reject(error);
                                        });
                                    }
                                }, function(error) {
                                    logger.debug("Error loading dashboard model [" + dashboardManager._configuration.dashboardId + "]");
                                    reject(error);
                                });
                            });
                            return this._widgetsPromise;
                        },

                        /**
                         * Will return a promise to access the list of widgets models that will be retrieved.
                         * The actual promise content will be an array of {Widget} instances
                         *
                         * @param widgetId The widget identifier to load
                         * @returns {psApp.promise.Promise|*}
                         */
                        getWidget: function(widgetId) {
                            // We need to load the widgets for this dashboard and then return the one that matches
                            var dashboardManager = this;
                            return psApp.promise.new(function(resolve, reject) {

                                // Get the list of widgets
                                dashboardManager.getWidgets().then(function(widgets) {

                                    // Now search for the widget that we want
                                    var widget = null;
                                    if(widgets) {
                                        for(var i = 0; i < widgets.length; i++) {
                                            var w = widgets[i];
                                            if(w.getId() == widgetId) {
                                                widget = w;
                                                break;
                                            }
                                        }
                                    }

                                    // Resolve by sending back the widget
                                    if(widget)
                                        resolve(widget);
                                    else
                                        reject(new Error("The widget " + widgetId + " does not belong to the dashboard"));
                                }, function(error) {

                                    // Simple rejection
                                    reject(error);
                                });
                            });
                        },

                        /**
                         * Will return a detached reference to the widget manager where you need an instace immediately.
                         *
                         * @param widgetId The widget identifier to create a detached widget instance
                         * @returns {{}} A Proxy widget manager that may not be related to this item
                         */
                        getProxyWidget: function(widgetId) {

                            // We basically pass along the configuration for it to manage
                            var configuration = this._configuration.clone({
                                widgetId: widgetId,
                                dashboardId: this._configuration.dashboardId,
                                dashboard: this
                            });

                            // Just return back a new instance
                            return Widget.new(configuration);
                        },

                        /**
                         * Will display open a dialog allowing the user to view and select from a list of widgets.
                         * This can be changed from the configuration if you want to use a custom widget selector or change
                         * the properties of the window.
                         */
                        showWidgetsDialog: function() {
                            if(!PivotalUtils)
                                throw new Error("PivotalUtils library is not available");

                            // Will open the modal dialog loading the URL to the list of widgets that was configured
                            psApp.windowManager.open({
                                title:this._configuration.widgetDialog.title,
                                width:this._configuration.widgetDialog.width,
                                height:this._configuration.widgetDialog.height,
                                content:this._configuration.widgetDialog.url});
                        },

                        /**
                         * SS-675
                         * This will pass along the lock request to the current layout manager
                         */
                        lockDashboard: function() {

                            // Fire an event requesting that a lock should be performed
                            // (the context depends on the layout manager)
                            dashboardEventManager.dispatchEvent("lockDashboard"+this.getId(), true);
                        },

                        /**
                         * SS-675
                         * This will pass along the unlock request to the current layout manager
                         */
                        unlockDashboard: function() {

                            // Fire an event requesting that a unlock should be performed
                            // (the context depends on the layout manager)
                            dashboardEventManager.dispatchEvent("lockDashboard"+this.getId(), false);
                        },

                        /**
                         * SS-675
                         * This is a helper methods that performs a common action within the event manager
                         * and allows you to be notified when an event is triggered to lock or unlock the
                         * dashboard layout manager.
                         *
                         * The event will contain true if the dashboard should be locked or false if it should
                         * be unlocked.
                         *
                         * @param handler This will be called when the lock status changes (true|false)
                         */
                        dashboardLockHandler: function(handler) {

                            // Add the listener for when a lock event has been received
                            dashboardEventManager.addListener("lockDashboard"+this.getId(), handler);
                        },

                        /**
                         * This is a helper methods that performs a common action within the event manager
                         * and allows you to be notified when an event is triggered to add a widget to the dashboard.
                         * The event will contain the widget descriptor identifier of the widget to create.
                         *
                         * @param handler This will be called when a widget is added to the dashboard
                         */
                        newWidgetAddedHandler: function(handler) {

                            // Add the listener for when the widget has been added to the dashboard
                            dashboardEventManager.addListener("widgetAddedForDashboard"+this.getId(), handler);
                        },

                        /**
                         * Will remove the handler from being called when new widgets are added to the dashboard
                         *
                         * @param handler This is the expected handler for when
                         */
                        removeNewWidgetAddedHandler: function(handler) {

                            // Remove the listener for this event
                            dashboardEventManager.removeListener("widgetAddedForDashboard"+this.getId(), handler);
                        },

                        /**
                         * This is a helper methods that performs a common action within the event manager
                         * and allows you to be notified when an event is triggered that a widget within the dashboard
                         * has been updated. This is something that could cause the widget view to change. The event
                         * will contain the identifier of the widget that has been updated.
                         *
                         * @param handler This will be called when a widget is added to the dashboard
                         */
                        onWidgetUpdateHandler: function(handler) {

                            // Add the listener for when the widget has been added to the dashboard
                            dashboardEventManager.addListener("widgetUpdatedForDashboard"+this.getId(), handler);
                        },

                        /**
                         * Will remove the handler from being called when widgets for the dashboard are updated
                         *
                         * @param handler This is the expected handler for when
                         */
                        removeOnWidgetUpdateHandler: function(handler) {

                            // Remove the listener for this event
                            dashboardEventManager.removeListener("widgetUpdatedForDashboard"+this.getId(), handler);
                        },

                        /**
                         * This will send the event that a widget has been updated.
                         *
                         * @param widgetId The identifier of the widget
                         */
                        widgetUpdated: function(widgetId) {

                            // We need to fire the event that the widget has had its properties changed
                            dashboardEventManager.dispatchEvent("widgetUpdatedForDashboard"+this.getId(), widgetId);
                        },

                        /**
                         * This requests that the specific widget be added to the current dashboard. Any layouts that would
                         * be interesting in this event would add a listener to the dashboard for onAddWidgetEventHandler.
                         *
                         * @param widgetDescriptorId The unique widget descriptor identifier (which is unique to each widget)
                         * @param success The handler for when the widget is successfully added
                         * @param failure The handler for when the widget cannot be added (Will pass the error object)
                         */
                        addWidget: function(widgetDescriptorId, success, failure) {

                            // We need to make the request to add this widget and then return success or failure back
                            // the originator and then dispatch the 'widgetAddedForDashboard{id}' event so that anything listening
                            // on the dashboard will be notified

                            // Attempt to fetch the content and then handle the result
                            var dashboardManager = this;
                            psApp.promise.new(function(resolve, reject) {

                                // We want to load the dashboard model
                                var request = $.ajax({
                                    cache: false,
                                    url: dashboardManager._configuration.widgetsRestPath,
                                    type: "POST",
                                    dataType: "json",
                                    contentType: "application/json; charset=utf-8",
                                    data: JSON.stringify({identifier: widgetDescriptorId})
                                });

                                // If the result is successful we can resolve the ticket
                                request.done(function(result) {
                                    logger.debug("Received new widget for dashboard [" + dashboardManager._configuration.dashboardId + "] result");
                                    logger.log(result);
                                    if (!result || !result["success"]) {
                                        logger.debug("Failed to create widget for dashboard [" + dashboardManager._configuration.dashboardId + "]");
                                        reject(new Error(!result["errorMessage"] ? "Failed to create widget" : result["errorMessage"]));
                                    }
                                    else {
                                        logger.debug("Successfully created widget [" + result["id"] + "] for dashboard [" + dashboardManager._configuration.dashboardId + "]");

                                        // Create the configuration for the dashboard
                                        var configuration = dashboardManager._configuration.clone({
                                            widgetId: result["id"],
                                            dashboardId: dashboardManager._configuration.dashboardId,
                                            dashboard: dashboardManager
                                        });
                                        var widget = Widget.new(configuration);

                                        // Now ensure that the widgets are refreshed again
                                        dashboardManager.refreshWidgets();

                                        // Pass the widget back to the handler
                                        resolve(widget);
                                    }
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {
                                    logger.debug("Error attempting to create widget for dashboard [" + dashboardManager._configuration.dashboardId + "]");
                                    reject(error);
                                });
                            }).then(function(widget) {

                                // We need to fire the event that the widget has been added
                                dashboardEventManager.dispatchEvent("widgetAddedForDashboard"+dashboardManager.getId(), widget);

                                // The widget has been successfully added
                                success(widget);
                            }, function(error) {

                                // handle the failure callback
                                failure(error);
                            });
                        }
                    };

                    // Return the manager class
                    return Dashboard;
                })();

                /**
                 * Will create a new {Dashboard} instance using the configuration.
                 *
                 * @param configuration The configuration for the dashboard
                 * @returns {Dashboard} A new instance
                 */
                function createDashboard(configuration) {
                    return new Dashboard(configuration);
                }

                // Return the classes under the namespace
                return {
                    'new': createDashboard,
                    Dashboard: Dashboard
                };
            })();

            /**
             * Module pattern for providing the classes we require for the widget manager.
             *
            */
            var Widget = (function () {

                /**
                 * Builds a new dashboard manager that will manage one dashboard specified by the identifier.
                 *
                 * @returns {Widget}
                 */
                var Widget = (function () {

                    // Our own logger
                    var logger = psApp.logger.getLogger("psApp.dashboards.Widget", null);

                    /**
                     * The actual constructor that will be used and returned, keeping the rest of the item encapsulated (private).
                     *
                     * @param configuration The dashboard configuration from above
                     */
                    var Widget = function (configuration) {
                        // Store the configuration
                        this._configuration = configuration;
                        this._updateListeners = {};
                        this._styleChangeRequestListeners = {};
                        this.initialize();
                    };

                    /**
                     * Add the methods for manipulating the widget.
                     *
                     * @type {{initialize: initialize, getConfig: getConfig, refresh: refresh, getDashboardId: getDashboardId, getDashboard: getDashboard, getId: getId, getContentUrl: getContentUrl, getContent: getContent, embedContent: embedContent, delete: 'delete', deleteDialog: deleteDialog, getModel: getModel, getConfiguration: getConfiguration, onWidgetUpdateHandler: onWidgetUpdateHandler, removeOnWidgetUpdateHandler: removeOnWidgetUpdateHandler, onWidgetStyleHandler: onWidgetStyleHandler, removeOnWidgetStyleHandler: removeOnWidgetStyleHandler, requestWidgetFrameStyleChange: requestWidgetFrameStyleChange, getWidth: getWidth, getHeight: getHeight, getTitle: getTitle, setWidth: setWidth, setHeight: setHeight, setTitle: setTitle, getMaxWidth: getMaxWidth, getMaxHeight: getMaxHeight, getMinWidth: getMinWidth, getMinHeight: getMinHeight, isResizable: isResizable, widgetUpdated: widgetUpdated, showConfigurationDialog: showConfigurationDialog}}
                     */
                    Widget.prototype = {

                        /**
                         * Initialises the event manager
                         */
                        initialize: function() {
                            if(!this._configuration.widgetId) {
                                logger.error("Manager instance creation attempted without providing the widget identifier");
                                throw new Error("You must provide the widget ID within the configuration");
                            }

                            // Setup the manager
                            this.refresh();
                        },

                        /**
                         * This returns the configuration for this dashboard.
                         *
                         * @returns {*}
                         */
                        getConfig: function() {
                            return this._configuration;
                        },

                        /**
                         * Will refresh the current widget model
                         */
                        refresh: function() {
                            // Reset all the promises
                            this._dashboardPromise = null;
                            this._widgetPromise = null;
                            this._deleteWidgetPromise = null;
                            this._contentPromise = null;
                            this._configurationPromise = null;
                        },

                        /**
                         * Will return the dashboard ID that this widget belongs
                         *
                         * @returns {number}
                         */
                        getDashboardId: function() {
                            if(this._configuration.dashboardId) return this._configuration.dashboardId;
                            else if(this._configuration.dashboard) return this._configuration.dashboard.getId();
                            else return null;
                        },

                        /**
                         * This will return the dashboard manager for this widget. If the manager already exists within
                         * the configuration it will be returned, otherwise the widget dashboard identifier will be used to load
                         * a new dashboard manager.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getDashboard: function() {

                            // We want to return the manager to the dashboard
                            var widgetManager = this;
                            this._dashboardPromise = this._dashboardPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading dashboard for widget [" + widgetManager._configuration.widgetId + "]");

                                // If we already have the dashboard we can return this
                                var dashboardId;
                                if(widgetManager._configuration.dashboard) return resolve(widgetManager._configuration.dashboard);
                                else if(widgetManager._configuration.dashboardId) dashboardId = widgetManager._configuration.dashboardId;
                                else {
                                    // We just need the actual model to retrieve the layout identifier
                                    widgetManager.getModel().then(function(widget) {
                                        logger.debug("Using widget model [" + widgetManager._configuration.widgetId + "] to get dashboard");

                                        // Now we have the model we can resolve the promise
                                        if(widget && widget["dashboardId"]) {

                                            // We basically pass along the configuration for it to manage
                                            dashboardId = widget["dashboardId"];
                                        }
                                    });
                                }

                                // Now we should have a dashboard id to load
                                if(dashboardId) {
                                    logger.debug("Found dashboard [" + dashboardId + "] for widget [" + widgetManager._configuration.widgetId + "]");
                                    var configuration = widgetManager._configuration.clone({
                                        dashboardId: dashboardId
                                    });
                                    resolve(Dashboard.new(configuration));
                                }
                                else {
                                    logger.debug("Could not locate dashboard identifier for widget [" + widgetManager._configuration.widgetId + "]");
                                    reject(new Error("Widget [" + widgetManager._configuration.widgetId + "] does not have parent dashboard"));
                                }
                            });
                            return this._dashboardPromise;
                        },

                        /**
                         * Will return the identifier of the widget this is managing
                         *
                         * @returns {number} The id of the widget
                         */
                        getId: function() {
                            return Number(this._configuration.widgetId);
                        },

                        /**
                         * Will return the URL to the content for this widget. This is useful for layouts that want
                         * to embed the content within an iframe.
                         *
                         * @return {string} the URL to the content
                         */
                        getContentUrl: function() {
                            return this._configuration.widgetRestContentPath;
                        },

                        /**
                         * This will return the actual content of the widget by loading the content url and returning
                         * the content.
                         *
                         * @return {psApp.promise.Promise|*}
                         */
                        getContent: function() {
                            var widgetManager = this;
                            this._contentPromise = this._contentPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading content for widget [" + widgetManager._configuration.widgetId + "]");

                                // Make the request and then wait for the response
                                var request = $.ajax({
                                    cache: false,
                                    url: widgetManager.getContentUrl(),
                                    type: "GET"
                                });

                                // We want to load the dashboard model
                                request.done(function(content) {
                                    logger.debug("Received content for widget [" + widgetManager._configuration.widgetId + "]");
                                    resolve(content);
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {

                                    // As this is most likely a connection issue we should allow the user to try again
                                    widgetManager._contentPromise = null;
                                    reject(error);
                                });
                            });
                            return this._contentPromise;
                        },

                        /**
                         * This is a convenience method that will call getContent and then embed that content within the
                         * element specified. Any error will be embedded within the content area.
                         *
                         * @param {*} selector This can be either the element class, id or jQuery selector
                         */
                        embedContent: function(selector) {
                            selector = selector instanceof $ ? selector : $(selector);
                            this.getContent().then(function(content) {
                                selector.html(content);
                            }, function(error) {
                                logger.error("Could not embed content: " + error);
                                selector.html(error.toString());
                            })
                        },

                        /**
                         * Will attempt to delete the dashboard
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        'delete': function() {

                            // Attempt to delete the widget
                            var widgetManager = this;
                            this._deleteWidgetPromise = this._deleteWidgetPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Deleting widget [" + widgetManager._configuration.widgetId + "] from URL: " + widgetManager._configuration.widgetRestPath);

                                // We want to load the widget model
                                var request = $.ajax({
                                    cache: false,
                                    url: widgetManager._configuration.widgetRestPath,
                                    type: "DELETE"
                                });

                                // If the result is successful we can resolve the ticket
                                request.done(function(result) {
                                    logger.debug("Received widget [" + widgetManager._configuration.widgetId + "] result");
                                    logger.log(result);
                                    if (!result || !result["success"]) {
                                        logger.debug("Failed to delete widget [" + widgetManager._configuration.widgetId + "]");
                                        reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                    }
                                    else {
                                        logger.debug("Successfully deleted widget [" + widgetManager._configuration.widgetId + "]");

                                        // We need to refresh the widgets of the parent dashboard
                                        widgetManager.getDashboard().then(function(dashboard) {

                                            // Now ensure that the widgets are refreshed again
                                            dashboard.refreshWidgets();
                                        });

                                        // Remove the listeners (as the widget is now deleted)
                                        for(var updatehandler in widgetManager._updateListeners) {
                                            if(widgetManager._updateListeners.hasOwnProperty(updatehandler)) {
                                                widgetManager.removeOnWidgetUpdateHandler(updatehandler);
                                            }
                                        }

                                        // And the style handlers
                                        for(var stylehandler in widgetManager._styleChangeRequestListeners) {
                                            if(widgetManager._styleChangeRequestListeners.hasOwnProperty(stylehandler)) {
                                                widgetManager.removeOnWidgetStyleHandler(stylehandler);
                                            }
                                        }

                                        // Resolve this promise
                                        resolve(result["success"]);
                                    }
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {

                                    // As this is most likely a connection issue we should allow the user to try again
                                    widgetManager._deleteWidgetPromise = null;
                                    reject(error);
                                });
                            });
                            return this._deleteWidgetPromise;
                        },

                        /**
                         * Wraps the delete feature in a dialog window that will be shown to the user before the delete is called.
                         * The deleteCallback function will be passed either true or an error if it could not be deleted.
                         *
                         * @param {Function} deleteCallback Will be called if the user chooses to delete the widget with the success flag from the server
                         * @param {Function} cancelCallback Will be called if the user chooses to cancel the deletion
                         */
                        deleteDialog: function(deleteCallback, cancelCallback) {
                            if(deleteCallback && !(deleteCallback instanceof Function))
                                throw new Error("You must provide a valid delete callback function");
                            if(cancelCallback && !(cancelCallback instanceof Function))
                                throw new Error("You must provide a valid cancel callback function");
                            if(!PivotalUtils)
                                throw new Error("PivotalUtils library is not available");

                            // Will open the modal dialog loading the URL to the list of widgets that was configured
                            PivotalUtils.showConfirmationDialog(
                                this._configuration.widgetDeleteConfirmation.id,
                                this._configuration.widgetDeleteConfirmation.title, {
                                    messageText: this._configuration.widgetDeleteConfirmation.messageText,
                                    confirmText: this._configuration.widgetDeleteConfirmation.confirmButtonText,
                                    cancelText: this._configuration.widgetDeleteConfirmation.cancelButtonText,
                                    showCancelButton: true,
                                    confirmFunction: function () {

                                        // We just need to call the delete handler and then use the callbacks
                                        this.delete().then(
                                            function(success) {
                                                if(deleteCallback) deleteCallback(success);
                                            },
                                            function(error) {
                                                if(deleteCallback) deleteCallback(error);
                                            }
                                        )
                                    }.bind(this),
                                    cancelFunction: function() {
                                        if(cancelCallback) cancelCallback();
                                    }
                                }
                            );
                        },

                        /**
                         * Will return a promise to the dashboard model that will be retrieved.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getModel: function() {

                            // We want to load the dashboard model and store it as a promise
                            var widgetManager = this;
                            this._widgetPromise = this._widgetPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading widget model [" + widgetManager._configuration.widgetId + "] from URL: " + widgetManager._configuration.widgetRestPath);

                                // Make the request and then wait for the response
                                var request = $.ajax({
                                    cache: false,
                                    url: widgetManager._configuration.widgetRestPath,
                                    type: "GET",
                                    dataType: "json"
                                });

                                // We want to load the widget model
                                request.done(function (result) {
                                    logger.debug("Received widget [" + widgetManager._configuration.widgetId + "] result");
                                    logger.log(result);
                                    if (!result || !result["success"] || !result["widget"]) {
                                        logger.debug("Failed to retrieve widget model [" + widgetManager._configuration.widgetId + "]");
                                        reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                    }
                                    else {
                                        // We have the widget to pass back
                                        logger.debug("Successfully retrieved widget model [" + widgetManager._configuration.widgetId + "]");
                                        logger.log(result["widget"]);
                                        resolve(result["widget"]);
                                    }
                                });

                                // If the request fails then we should send back a rejection
                                request.fail(function(error) {

                                    // This is most likely a connection
                                    widgetManager._widgetPromise = null;
                                    reject(error);
                                });
                            });
                            return this._widgetPromise;
                        },

                        /**
                         * Will return a promise to this widgets configuration. If the configuration can be retrieved it will be returned or
                         * it will fail with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getConfiguration: function() {

                            // We want to load the configuration for this widget
                            var widgetManager = this;
                            this._configurationPromise = this._configurationPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading configuration for widget [" + widgetManager._configuration.widgetId + "]");

                                // We just need the actual model to retrieve the layout identifier
                                widgetManager.getModel().then(function(widget) {
                                    logger.debug("Using widget model [" + widgetManager._configuration.widgetId + "] to get configuration");

                                    // Now we have the model we can resolve the promise
                                    if(!widget || !widget["configuration"]) {
                                        logger.debug("Widget model [" + widgetManager._configuration.widgetId + "] does not have any configuration");
                                        reject(new Error("No configuration exists for the widget [" + widgetManager._configuration.widgetId + "]"));
                                    }
                                    else {
                                        logger.debug("Configuration from widget [" + widgetManager._configuration.widgetId + "] retrieved");
                                        logger.log(widget["configuration"]);

                                        // We need to create a new configuration manager
                                        var configuration = widgetManager._configuration.clone({
                                            widgetConfigurationModel: widget["configuration"],
                                            widget: widgetManager
                                        });
                                        resolve(Configuration.new(configuration));
                                    }
                                }, function(error) {
                                    logger.debug("Error loading widget model [" + widgetManager._configuration.widgetId + "]");

                                    // As there has been an error we should try again in the future
                                    widgetManager._configurationPromise = null;
                                    reject(error);
                                });
                            });
                            return this._configurationPromise;
                        },

                        /**
                         * This is a helper methods that performs a common action within the event manager
                         * and allows you to be notified when an event is triggered that a widget within the dashboard
                         * has been updated. This is something that could cause the widget view to change. The event
                         * will contain the identifier of the widget that has been updated.
                         *
                         * @param handler This will be called when a widget is added to the dashboard
                         */
                        onWidgetUpdateHandler: function(handler) {

                            // We will proxy the actual dashboard widget change event and listen for events about ourself
                            this._updateListeners[handler] = function(widgetId) {
                                if(this.getId() == widgetId) handler(widgetId);
                            }.bind(this);
                            dashboardEventManager.addListener("widgetUpdatedForDashboard"+this.getDashboardId(), this._updateListeners[handler]);
                        },

                        /**
                         * Will remove the handler from being called when widgets for the dashboard are updated
                         *
                         * @param handler The handler that is to be removed
                         */
                        removeOnWidgetUpdateHandler: function(handler) {

                            // Remove the listener for this event
                            var listener = this._updateListeners[handler];
                            this._updateListeners[handler] = null;
                            if(listener) dashboardEventManager.removeListener("widgetUpdatedForDashboard"+this.getDashboardId(), listener);
                        },

                        /**
                         * A general purpose event manager which will allow a widget to request a style parent that can
                         * be picked up by the layout manager (if the layout provides this functionality - you will have
                         * to read the layout implementation on the particular style changes allowed)
                         *
                         * @param handler This will be called when a widget style change is requested
                         */
                        onWidgetStyleHandler: function(handler) {

                            // We will proxy the actual dashboard widget change event and listen for events about ourself
                            this._styleChangeRequestListeners[handler] = handler;
                            dashboardEventManager.addListener("widgetStyleChangeRequest"+this.getId(), this._styleChangeRequestListeners[handler]);
                        },

                        /**
                         * Will remove the handler from being called when widgets style requests are made
                         *
                         * @param handler The handler that is to be removed
                         */
                        removeOnWidgetStyleHandler: function(handler) {

                            // Remove the listener for this event
                            var listener = this._styleChangeRequestListeners[handler];
                            this._styleChangeRequestListeners[handler] = null;
                            if(listener) dashboardEventManager.removeListener("widgetStyleChangeRequest"+this.getId(), listener);
                        },

                        /**
                         * Calling this will cause an event to be fired that will request that the frame of the widget be updated
                         * using the property items being passed. Whether the layout will accept these changes depends on
                         * the functionality provided by the layout/widget chrome manager.
                         *
                         * @param properties The map of properties requesting to be changed
                         */
                        requestWidgetFrameStyleChange: function(properties) {

                            // We need to fire the event that a style change request has occurred
                            dashboardEventManager.dispatchEvent("widgetStyleChangeRequest"+this.getId(), properties);
                        },

                        /**
                         * Will return the width of the widget. The promise will return the width of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getWidth: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_width'
                            return getInternalPropertyValue.call(this, "width");
                        },

                        /**
                         * Will return the height of the widget. The promise will return the height of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getHeight: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_height'
                            return getInternalPropertyValue.call(this, "height");
                        },

                        /**
                         * Will return the title of the widget. The promise will return the title of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getTitle: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_title'
                            return getInternalPropertyValue.call(this, "title");
                        },

                        /**
                         * This will try to update the width value on the server for this widget. It will return a promise
                         * that will return whether the update was successful or it will fail with an error.
                         *
                         * @param width {number} The width to set for this widget
                         * @returns {psApp.promise.Promise|*}
                         */
                        setWidth: function(width) {

                            // We need to find and update the value for this property
                            return setInternalPropertyValue.call(this, "width", width);
                        },

                        /**
                         * This will try to update the height value on the server for this widget. It will return a promise
                         * that will return whether the update was successful or it will fail with an error.
                         *
                         * @param height {number} The height to set for this widget
                         * @returns {psApp.promise.Promise|*}
                         */
                        setHeight: function(height) {

                            // We need to find and update the value for this property
                            return setInternalPropertyValue.call(this, "height", height);
                        },

                        /**
                         * This will try to update the title value on the server for this widget. It will return a promise
                         * that will return whether the update was successful or it will fail with an error.
                         *
                         * @param title {string} The title to set for this widget
                         * @returns {psApp.promise.Promise|*}
                         */
                        setTitle: function(title) {

                            // We need to find and update the value for this property
                            return setInternalPropertyValue.call(this, "title", title);
                        },

                        /**
                         * Will return the max width of the widget. The promise will return the max width of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getMaxWidth: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_maxWidth'
                            return getInternalPropertyValue.call(this, "maxWidth");
                        },

                        /**
                         * Will return the max height of the widget. The promise will return the max height of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getMaxHeight: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_maxHeight'
                            return getInternalPropertyValue.call(this, "maxHeight");
                        },

                        /**
                         * Will return the min width of the widget. The promise will return the min width of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getMinWidth: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_minWidth'
                            return getInternalPropertyValue.call(this, "minWidth");
                        },

                        /**
                         * Will return the min height of the widget. The promise will return the min height of the widget or fail
                         * with an error.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getMinHeight: function() {

                            // Look inside of the configuration for a property named 'RESERVED_NAME_SPACE_minHeight'
                            return getInternalPropertyValue.call(this, "minHeight");
                        },

                        /**
                         * Will return whether the widget is resizable.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        isResizable: function() {
                            var widgetManager = this;
                            return psApp.promise.new(function(resolve, reject) {

                                // We just need the actual model to retrieve the layout identifier
                                widgetManager.getConfiguration().then(function(configuration) {

                                    // Now look for the property with the name
                                    configuration.getResizable().then(function(value) {
                                        resolve(value);
                                    }, function(error) {
                                        reject(error);
                                    });
                                }, function(error) {
                                    logger.debug("Error loading widget configuration model [" + widgetManager._configuration.widgetId + "]");
                                    reject(error);
                                });
                            });
                        },

                        /**
                         * If you call this is it will fire an event that the widget has been updated so that anything listening
                         * can be alerted.
                         */
                        widgetUpdated: function() {

                            // Load the dashboard to fire the event
                            this.getDashboard().then(function(dashboard) {
                                dashboard.widgetUpdated(this.getId());
                            }.bind(this), function() {
                                logger.error("Could not send widget ["  + this.getId() + "] update event as the dashboard could not be found")
                            }.bind(this));
                        },

                        /**
                         * Will display a dialog allowing the user to configure this widget.
                         */
                        showConfigurationDialog: function() {
                            if(!PivotalUtils)
                                throw new Error("PivotalUtils library is not available");

                            // Will open the modal dialog loading the URL to the list of widgets that was configured
                            psApp.windowManager.open({
                                title:this._configuration.widgetConfigurationDialog.title,
                                width:this._configuration.widgetConfigurationDialog.width,
                                height:this._configuration.widgetConfigurationDialog.height,
                                content:this._configuration.widgetConfigurationDialog.url,
                                useIFrame:true,
                                closeFunction: function() {
                                    // We will refresh the content when the window is closed
                                    this.widgetUpdated();
                                }.bind(this)});
                        },

                        /**
                         * Will display a dialog containing the analyser for this widget
                         */
                        showAnalyserDialog: function() {
                            if(!PivotalUtils)
                                throw new Error("PivotalUtils library is not available");

                            // Will open the modal dialog loading the URL to the list of widgets that was configured
                            psApp.windowManager.open({
                                title: this._configuration.widgetAnalyserDialog.title,
                                width: this._configuration.widgetAnalyserDialog.width,
                                height: this._configuration.widgetAnalyserDialog.height,
                                content: this._configuration.widgetAnalyserDialog.url,
                                useIFrame: true
                            });
                        }
                    };

                    // Private methods

                    /**
                     * Will try to retrieve the internal namespace property from this configuration
                     *
                     * @param propertyName The name of the internal property
                     * @returns {psApp.promise.Promise|*} The promise for the property
                     */
                    function getInternalProperty(propertyName) {
                        var widgetManager = this;
                        return psApp.promise.new(function(resolve, reject) {

                            // We just need the actual model to retrieve the layout identifier
                            widgetManager.getConfiguration().then(function(configuration) {

                                // Now look for the property with the name
                                configuration.getProperty(Configuration.Property.RESERVED_PROPERTY_NAMESPACE + "_" + propertyName).then(
                                    function(property) {
                                        resolve(property);
                                    }, function(error) {
                                        reject(error);
                                    }
                                );
                            }, function(error) {
                                logger.debug("Error loading widget configuration model [" + widgetManager._configuration.widgetId + "]");
                                reject(error);
                            });
                        });
                    }

                    /**
                     * Will try to retrieve the internal namespace property value from this configuration
                     *
                     * @param propertyName The name of the internal property
                     * @returns {psApp.promise.Promise|*} The promise for the property value
                     */
                    function getInternalPropertyValue(propertyName) {
                        var widgetManager = this;
                        return psApp.promise.new(function(resolve, reject) {

                            // We just need the actual model to retrieve the layout identifier
                            getInternalProperty.call(widgetManager, propertyName).then(function(property) {
                                property.getValue().then(function(value) {
                                    resolve(value);
                                }, function(error) {
                                    reject(error);
                                });
                            }, function(error) {
                                reject(error);
                            });
                        });
                    }

                    /**
                     * Will try to update the value of the internal namespace property value from this configuration.
                     * The promise will return a flag to whether the property could be updated or it will fail with
                     * and error.
                     *
                     * @param propertyName {string} The name of the internal property
                     * @param value {Object} The value to set
                     * @returns {psApp.promise.Promise|*} The promise for setting the value
                     */
                    function setInternalPropertyValue(propertyName, value) {
                        var widgetManager = this;
                        return psApp.promise.new(function(resolve, reject) {

                            // We just need the actual model to retrieve the layout identifier
                            getInternalProperty.call(widgetManager, propertyName).then(function(property) {

                                // Now try to update the value
                                property.setValue(value).then(function(property) {
                                    resolve(property ? true : false);
                                }, function(error) {
                                    reject(error);
                                })
                            }, function(error) {
                                reject(error);
                            });
                        });
                    }

                    // Return the manager class
                    return Widget;
                })();

                /**
                 * Builds a new dashboard manager that will manage one dashboard specified by the identifier.
                 *
                 * @returns {Configuration}
                 */
                var Configuration = (function() {

                    // Our own logger
                    var logger = psApp.logger.getLogger("psApp.dashboards.Widget.Configuration", null);

                    /**
                     * Will create a new Configuration manager wrapper for handling the configuration of a {Widget}.
                     * This class should always be passed the configuration model from the widget as it will not retrieve
                     * itself remotely. This is because the configuration is tightly coupled to the widget. This means that
                     * the widget is responsible for refreshing its Configuration instance.
                     *
                     * @param configuration The configuration for this Configuration
                     * @constructor
                     */
                    var Configuration = function (configuration) {

                        // Store the configuration
                        this._configuration = configuration;
                        this.initialize();
                    };

                    /**
                     * Adds the required functions required for the configuration.
                     *
                     * @type {{initialize: initialize, refresh: refresh, getModel: getModel, getProperties: getProperties, getProperty: getProperty, getSupportedExportFormats: getSupportedExportFormats, getResizable: getResizable, getMaxHeight: getMaxHeight, getMaxWidth: getMaxWidth, getMinHeight: getMinHeight, getMinWidth: getMinWidth, getDefaultHeight: getDefaultHeight, getDefaultWidth: getDefaultWidth}}
                     */
                    Configuration.prototype = {

                        /**
                         * Initialises the event manager
                         */
                        initialize: function() {
                            if(!this._configuration.widgetId) {
                                logger.error("Manager instance creation attempted without providing the widget identifier");
                                throw new Error("You must provide the widget ID within the configuration");
                            }
                            if(!this._configuration.widgetConfigurationRestPath) {
                                logger.error("Manager instance creation attempted without providing the configuration rest path");
                                throw new Error("You must provide the widget configuration rest path within the configuration");
                            }

                            // Refresh the model
                            this.refresh();
                        },

                        /**
                         * Will refresh the current widget model
                         */
                        refresh: function() {
                            // Reset all the promises
                            this._configurationPromise = null;
                            this._propertiesPromise = null;
                            this._propertyPromise = null;
                        },

                        /**
                         * Will return a promise to the configuration model that will be retrieved from the configiuration or
                         * remotely if it is not available.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getModel: function() {

                            // We want to load the dashboard model and store it as a promise
                            var configurationManager = this;
                            this._configurationPromise = this._configurationPromise || psApp.promise.new(function(resolve, reject) {

                                // We may already have been passed the model
                                if(configurationManager._configuration.widgetConfigurationModel) {
                                    resolve(configurationManager._configuration.widgetConfigurationModel);
                                }
                                else {
                                    // We need to load the configuration remotely
                                    logger.debug("Loading widget configuration model [" + configurationManager._configuration.widgetId + "] from URL: " + configurationManager._configuration.widgetConfigurationRestPath);

                                    // Make the request and then wait for the response
                                    var request = $.ajax({
                                        cache: false,
                                        url: configurationManager._configuration.widgetConfigurationRestPathFull,
                                        type: "GET",
                                        dataType: "json"
                                    });

                                    // We want to load the widget model
                                    request.done(function (result) {
                                        logger.debug("Received widget configuration [" + configurationManager._configuration.widgetId + "] result");
                                        logger.log(result);
                                        if (!result || !result["success"] || !result["configuration"]) {
                                            logger.debug("Failed to retrieve widget configuration model [" + configurationManager._configuration.widgetId + "]");
                                            reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                        }
                                        else {
                                            // We have the widget to pass back
                                            logger.debug("Successfully retrieved widget configuration model [" + configurationManager._configuration.widgetId + "]");
                                            logger.log(result["configuration"]);
                                            resolve(result["configuration"]);
                                        }
                                    });

                                    // If the request fails then we should send back a rejection
                                    request.fail(function(error) {

                                        // As it was a connection error we should try again
                                        configurationManager._configurationPromise = null;
                                        reject(error);
                                    });
                                }
                            });
                            return this._configurationPromise;
                        },

                        /**
                         * Will return a promise to the widget property models that will be retrieved from the server.
                         * The promise will contain the list of properties wrapped within a {Property} manager.
                         *
                         * @returns {psApp.promise.Promise|*}
                         */
                        getProperties: function() {

                            // We want to load the configuration for this widget
                            var configurationManager = this;
                            this._propertiesPromise = this._propertiesPromise || psApp.promise.new(function(resolve, reject) {
                                logger.debug("Loading property models for widget [" + configurationManager._configuration.widgetId + "]");

                                // We just need the actual model to retrieve the layout identifier
                                configurationManager.getModel().then(function(configuration) {
                                    logger.debug("Configuration from widget [" + configurationManager._configuration.widgetId + "] retrieved");

                                    // Create our list of managers
                                    var propertyManagers = [];
                                    var properties = configuration["properties"];
                                    if(properties) {
                                        logger.log(properties);
                                        for(var i = 0; i < properties.length; i++) {
                                            var property = properties[i];
                                            logger.debug("Property model [" + property["name"] + "] found");

                                            // We basically pass along the configuration for it to manage
                                            var config = configurationManager._configuration.clone({
                                                propertyName: property["name"],
                                                widgetPropertyModel: property,
                                                widgetConfiguration: configurationManager
                                            });
                                            propertyManagers.push(Property.new(config));
                                        }
                                    }
                                    else {
                                        logger.debug("Widget [" + configurationManager._configuration.widgetId + "] has no properties");
                                    }
                                    resolve(propertyManagers);
                                }, function(error) {
                                    logger.debug("Error loading widget configuration model [" + configurationManager._configuration.widgetId + "]");
                                    reject(error);
                                });
                            });
                            return this._propertiesPromise;
                        },

                        /**
                         * Will return a promise to the specific named property if it exists or an error if it does not or
                         * is not accessible
                         *
                         * @param propertyName The name of the property
                         * @returns {psApp.promise.Promise|*}
                         */
                        getProperty: function(propertyName) {

                            // First load the existing properties and then search for the property
                            var configurationManager = this;
                            return psApp.promise.new(function(resolve, reject) {

                                // Get the properties
                                configurationManager.getProperties().then(function(properties) {

                                    // Now search for the widget that we want
                                    var property = null;
                                    if(properties && properties.length) {
                                        for(var i = 0; i < properties.length; i++) {
                                            var p = properties[i];
                                            if(p.getName() == propertyName) {
                                                property = p;
                                                break;
                                            }
                                        }
                                    }

                                    // Resolve by sending back the property
                                    if(property)
                                        resolve(property);
                                    else
                                        reject(new Error("The widget does not have the property [" + propertyName + "]"));
                                }, function(error) {
                                    // Simple rejection
                                    reject(error);
                                });
                            });
                        },

                        /**
                         * Will return a promise to the list of supported export formats
                         *
                         * @returns {array} The list of exportable formats
                         */
                        getSupportedExportFormats: function() {
                            return getProperty.call(this, "supportedExportFormats");
                        },

                        /**
                         * Will return a promise to the list of supported export formats
                         *
                         * @returns {boolean} True if the widget supports the analyser
                         */
                        getAnalyserSupported: function() {
                            return getProperty.call(this, "analyserSupported");
                        },

                        /**
                         * Will return a promise to whether the widget is resizable
                         *
                         * @returns {boolean} true if the widget is resizable
                         */
                        getResizable: function() {
                            return getProperty.call(this, "resizable");
                        },

                        /**
                         * Will return a promise to the widget max height
                         *
                         * @returns {number} The max height or null if it does not have one
                         */
                        getMaxHeight: function() {
                            return getProperty.call(this, "maxHeight");
                        },

                        /**
                         * Will return a promise to the widget max width
                         *
                         * @returns {number} The max width or null if it does not have one
                         */
                        getMaxWidth: function() {
                            return getProperty.call(this, "maxWidth");
                        },

                        /**
                         * Will return a promise to the widget min height
                         *
                         * @returns {number} The min height or null if it does not have one
                         */
                        getMinHeight: function() {
                            return getProperty.call(this, "minHeight");
                        },

                        /**
                         * Will return a promise to the widget min width
                         *
                         * @returns {number} The min width or null if it does not have one
                         */
                        getMinWidth: function() {
                            return getProperty.call(this, "minWidth");
                        },

                        /**
                         * Will return a promise to the widget default height
                         *
                         * @returns {number} The default height or null if it does not have one
                         */
                        getDefaultHeight: function() {
                            return getProperty.call(this, "defaultHeight");
                        },

                        /**
                         * Will return a promise to the widget default width
                         *
                         * @returns {number} The default width or null if it does not have one
                         */
                        getDefaultWidth: function() {
                            return getProperty.call(this, "defaultWidth");
                        }
                    };

                    /**
                     * Will return the property specified
                     *
                     * @param propertyName The name of the property
                     * @returns {psApp.promise.Promise|*} A promise to the property
                     */
                    function getProperty(propertyName) {
                        var configurationManager = this;
                        return psApp.promise.new(function(resolve, reject) {

                            // Get the model and return the value
                            configurationManager.getModel().then(function(model) {
                                resolve(model[propertyName]);
                            }, function(error) {
                                reject(error);
                            });
                        });
                    }

                    /**
                     * Builds a new property manager that will manage one widget property specified by the identifier.
                     *
                     * @returns {Property}
                     */
                    var Property = (function () {

                        // Our own logger
                        var logger = psApp.logger.getLogger("psApp.dashboards.Widget.Configuration.Property", null);

                        // This is the reserved namespace for internally managed properties
                        // These are the properties that should not be configured externally
                        var RESERVED_PROPERTY_NAMESPACE = "internal_widget_property";

                        /**
                         * The actual constructor that will be used and returned, keeping the rest of the item encapsulated (private).
                         *
                         * @param configuration The property configuration from above
                         */
                        var Property = function (configuration) {

                            // Store the configuration
                            this._configuration = configuration;
                            this.initialize();
                        };

                        /**
                         * Adds the required functions for manipulating a property.
                         *
                         * @type {{initialize: initialize, isReserved: isReserved, getName: getName, getConfig: getConfig, refresh: refresh, getModel: getModel, getValue: getValue, setValue: setValue}}
                         */
                        Property.prototype = {

                            /**
                             * Initialises the property
                             */
                            initialize: function() {
                                if(!this._configuration.widgetId) {
                                    logger.error("Manager instance creation attempted without providing the widget identifier of the property");
                                    throw new Error("You must provide the widget ID within the configuration");
                                }
                                if(!this._configuration.widgetConfigurationRestPath) {
                                    logger.error("Manager instance creation attempted without providing the configuration rest path");
                                    throw new Error("You must provide the widget configuration rest path within the configuration");
                                }

                                // Setup the manager
                                this.refresh();
                            },

                            /**
                             * This will return true if this property is in the reserved namespace
                             *
                             * @return {boolean} True if this is a reserved property
                             */
                            isReserved: function() {
                                return (this.getName().indexOf(RESERVED_PROPERTY_NAMESPACE) == 0);
                            },

                            /**
                             * This will return the name of the property that is being managed
                             *
                             * @returns {string} The name of the widget
                             */
                            getName: function() {
                                return this._configuration.propertyName;
                            },

                            /**
                             * This returns the configuration for this property.
                             *
                             * @returns {*}
                             */
                            getConfig: function() {
                                return this._configuration;
                            },

                            /**
                             * Will refresh the current property model
                             */
                            refresh: function() {

                                // Reset all the promises
                                this._propertyPromise = null;
                                this._valuePromise = null;
                            },

                            /**
                             * Will return a promise to the property model that will be retrieved.
                             *
                             * @returns {psApp.promise.Promise|*}
                             */
                            getModel: function() {

                                // We want to load the property model and store it as a promise
                                var propertyManager = this;
                                this._propertyPromise = this._propertyPromise || psApp.promise.new(function(resolve, reject) {

                                    // We may already have been passed the model
                                    if(propertyManager._configuration.widgetPropertyModel) {
                                        resolve(propertyManager._configuration.widgetPropertyModel);
                                    }
                                    else {
                                        // We need to load the configuration remotely
                                        logger.debug("Loading property model [" + propertyManager.getName() + "] from URL: " + propertyManager._configuration.widgetConfigurationPropertyRestPath);

                                        // Make the request and then wait for the response
                                        var request = $.ajax({
                                            cache: false,
                                            url: propertyManager._configuration.widgetConfigurationPropertyRestPath,
                                            type: "GET",
                                            dataType: "json"
                                        });

                                        // We want to load the widget model
                                        request.done(function (result) {
                                            logger.debug("Received property model [" + propertyManager.getName() + "] result");
                                            logger.log(result);
                                            if (!result || !result["success"] || !result["property"]) {
                                                logger.debug("Failed to retrieve property model [" + propertyManager.getName() + "]");
                                                reject(new Error(!result["errorMessage"] ? "Failed to load" : result["errorMessage"]));
                                            }
                                            else {
                                                // We have the widget to pass back
                                                logger.debug("Successfully retrieved property model [" + propertyManager.getName() + "]");
                                                logger.log(result["property"]);
                                                resolve(result["property"]);
                                            }
                                        });

                                        // If the request fails then we should send back a rejection
                                        request.fail(function(error) {

                                            // As its a connection error we should try again
                                            propertyManager._propertyPromise = null;
                                            reject(error);
                                        });
                                    }
                                });
                                return this._propertyPromise;
                            },

                            /**
                             * This will return the value for this property. If the value can be retrieved the promise will contain
                             * the value or it will fail with the error.
                             *
                             * @returns {psApp.promise.Promise|*}
                             */
                            getValue: function() {
                                var propertyManager = this;
                                this._valuePromise = this._valuePromise || psApp.promise.new(function(resolve, reject) {

                                    // Get the property and return the value
                                    propertyManager.getModel().then(function(property) {
                                        resolve(property["value"]);
                                    }, function(error) {
                                        reject(error);
                                    });
                                });
                                return this._valuePromise;
                            },

                            /**
                             * Will set the value and also set the value on the server. It will return a {psApp.promise.Promise} that will
                             * allow yaou to determine whether the value was successfully saved remotely.
                             *
                             * @param value The value to set for this property
                             * @returns {psApp.promise.Promise|*}
                             */
                            setValue: function(value) {

                                // We want to load the dashboard model and store it as a promise
                                var propertyManager = this;
                                return psApp.promise.new(function(resolve, reject) {
                                    logger.debug("Updating property value [" + propertyManager.getName() + "] to URL: " + propertyManager._configuration.widgetConfigurationPropertyRestPath);

                                    // Update the value for this property
                                    var request = $.ajax({
                                        cache: false,
                                        url: propertyManager._configuration.widgetConfigurationPropertyRestPath,
                                        type: "PUT",
                                        dataType: "json",
                                        contentType: "application/json; charset=utf-8",
                                        data: JSON.stringify({value: value})
                                    });
                                    // When the request is done we need to call the resolved callback
                                    request.done(function (result) {
                                        logger.debug("Received property value update [" + propertyManager.getName() + "] result");
                                        logger.log(result);
                                        if (!result || !result["success"] || !result["property"]) {
                                            logger.debug("Failed to update property model value [" + propertyManager.getName() + "]");
                                            var error = new Error(!result["errorMessage"] ? "Failed to update" : result["errorMessage"]);
                                            reject(error);
                                        }
                                        else {

                                            // We have the widget to pass back
                                            logger.debug("Successfully updated property model value [" + propertyManager.getName() + "]");
                                            logger.log(result["property"]);

                                            // We should update the model and reset the value promise so that it reflects the current value
                                            propertyManager._configuration.widgetPropertyModel = result["property"];
                                            propertyManager.refresh();
                                            resolve(result["property"]);
                                        }
                                    });

                                    // If the request fails then we should send back a rejection
                                    request.fail(function (error) {
                                        reject(error);
                                    });
                                });
                            }
                        };

                        // Return the manager class
                        return {
                            'new': function(configuration) {return new Property(configuration);},
                            Property: Property,
                            RESERVED_PROPERTY_NAMESPACE: RESERVED_PROPERTY_NAMESPACE
                        };
                    })();

                    // Return the configuration classes under the namespace
                    return {
                        'new': function(configuration) {return new Configuration(configuration);},
                        Configuration: Configuration,
                        Property: Property
                    };
                })();

                // Return the classes under the namespace
                return {
                    'new': function(configuration) {return new Widget(configuration);},
                    Widget: Widget,
                    configuration: Configuration
                };
            })();

            // This is the current dashboard and is stored as a singleton - easy access through namespace
            var currentDashboard;

            /**
             * Will create a new dashboard and set it as the current dashboard.
             *
             * @param configuration The configuration for the dashboard
             * @returns {Dashboard} A new instance of the dashboard
             */
            function setAndCreateCurrentDashboard(configuration) {
                var dashboard = Dashboard.new(configuration);
                setCurrentDashboard(dashboard);
                return dashboard;
            }

            /**
             * Will set the current dashboard being managed.
             *
             * @param dashboard The dashboard that is currently being managed
             */
            function setCurrentDashboard(dashboard) {
                currentDashboard = dashboard;
            }

            /**
             * Will return the current dashboard that has been operated on
             *
             * @returns {Dashboard} The current dashboard instance
             */
            function getCurrentDashboard() {
                return currentDashboard;
            }

            /**
             * Will return whether the current dashboard is in edit mode
             *
             * @returns {boolean} True if the dashboard is in edit mode
             */
            function getEditMode() {

                // If there is no dashboard return false
                if(!getCurrentDashboard()) return false;
                else return getCurrentDashboard().editMode();
            }

            /**
             * This is a global method that will be picked up by the current dashboard being managed. It will trigger
             * the exact same function available on the dashboard manager itself. If you are managing multiple
             * dashboards, you should communicate directly with that manager.
             *
             * @param widgetDescriptorId The unique widget descriptor identifier (which is unique to each widget)
             * @param success The handler for when the widget is successfully added
             * @param failure The handler for when the widget cannot be added
             */
            function addWidget(widgetDescriptorId, success, failure) {

                // If there is no handler we need to return an error
                if(!getCurrentDashboard()) failure(new Error("There is no current dashboard"));
                else getCurrentDashboard().addWidget(widgetDescriptorId, success, failure);
            }

            /**
             * This will send an event on the dashboard event manager informing anything listening that the widget
             * configuration has changed and so should be updated and refreshed.
             *
             * @param widgetId The identifier of the widget that has been updated.
             */
            function widgetConfigurationChanged(widgetId) {

                // If there is no handler we need to return an error
                if(getCurrentDashboard()) getCurrentDashboard().widgetUpdated(widgetId);
            }

            /**
             * Will send out a properties updating event containing for anything
             * that is interested.
             */
            function sendPropertiesUpdatingEvent() {
                dashboardEventManager.dispatchEvent("propertiesUpdating");
            }

            /**
             * Will send out a properties updated event containing for anything
             * that is interested.
             */
            function sendPropertiesUpdatedEvent() {
                dashboardEventManager.dispatchEvent("propertiesUpdated");
            }

            /**
             * Will send out a properties updating event containing the error for anything
             * that is interested.
             *
             * @param error {Error} The error that occurred
             */
            function sendPropertiesUpdatingErrorEvent(error) {
                dashboardEventManager.dispatchEvent("propertiesUpdatingError", error);
            }

            /**
             * Will add the handlers for listening for property update events.
             *
             * @param propertiesUpdatingHandler When the properties have started being updated
             * @param propertiesUpdatedHandler When the properties have finished updating
             * @param propertiesErrorHandler When an error occurs updating any properties
             */
            function addPropertyChangeHandlers(propertiesUpdatingHandler, propertiesUpdatedHandler, propertiesErrorHandler) {
                if(propertiesUpdatingHandler instanceof Function)
                    dashboardEventManager.addListener("propertiesUpdating", propertiesUpdatingHandler);
                if(propertiesUpdatedHandler instanceof Function)
                    dashboardEventManager.addListener("propertiesUpdated", propertiesUpdatedHandler);
                if(propertiesErrorHandler instanceof Function)
                    dashboardEventManager.addListener("propertiesUpdatingError", propertiesErrorHandler);
            }

            /**
             * Will remove the handlers from listening for property update events.
             *
             * @param propertiesUpdatingHandler When the properties have started being updated
             * @param propertiesUpdatedHandler When the properties have finished updating
             * @param propertiesErrorHandler When an error occurs updating any properties
             */
            function removePropertyChangeHandlers(propertiesUpdatingHandler, propertiesUpdatedHandler, propertiesErrorHandler) {
                if(propertiesUpdatingHandler instanceof Function)
                    dashboardEventManager.removeListener("propertiesUpdating", propertiesUpdatingHandler);
                if(propertiesUpdatedHandler instanceof Function)
                    dashboardEventManager.removeListener("propertiesUpdated", propertiesUpdatedHandler);
                if(propertiesErrorHandler instanceof Function)
                    dashboardEventManager.removeListener("propertiesUpdatingError", propertiesErrorHandler);
            }

            // Return all the available packages
            return {

                // static methods
                setAndCreateCurrentDashboard: setAndCreateCurrentDashboard,
                setCurrentDashboard: setCurrentDashboard,
                getCurrentDashboard: getCurrentDashboard,
                getEditMode: getEditMode,
                addWidget: addWidget,
                widgetConfigurationChanged: widgetConfigurationChanged,
                addPropertyChangeHandlers: addPropertyChangeHandlers,
                removePropertyChangeHandlers: removePropertyChangeHandlers,
                sendPropertiesUpdatingEvent: sendPropertiesUpdatingEvent,
                sendPropertiesUpdatedEvent: sendPropertiesUpdatedEvent,
                sendPropertiesUpdatingErrorEvent: sendPropertiesUpdatingErrorEvent,

                // classes
                configuration: Configuration,
                dashboard: Dashboard,
                widget: Widget
            };
        })();

        // return the dashboard scope
        return Dashboards;
    })((typeof jQuery == "undefined" ? null : jQuery), (typeof PivotalUtils == "undefined" ? null : PivotalUtils));

    // Create the dashboards helper methods
    psApp.namespace("dashboardshelper");
    psApp.dashboardshelper = (function($) {

        /**
         * This function will expand the element to fit to the bottom of the main element by taking the offset
         * from the element and subtracting from the inner height of the main element.
         *
         * @param expandElement The element that will be extend the height
         * @param expandToElement The element that we will use as the height reference
         * @param offset The amount to offset the calculation
         */
        function maximiseWidgetHeight(expandElement, expandToElement, offset) {
            expandElement = expandElement instanceof $ ? expandElement : $(expandElement);
            if(expandToElement) expandToElement = expandToElement instanceof $ ? expandToElement : $(expandToElement);
            else expandToElement = $("body");

            // Now reset the height
            expandElement.innerHeight(expandToElement.innerHeight() - expandElement.offset().top - offset);

            // Now reset the MenuSub height
             setMenuSubItemHeight();

            //Setup the timer to determine when the resize drag end has occurred.
            var doitResize;
            window.onresize = function() {
                clearTimeout(doitResize);
                doitResize = setTimeout(function(){

                    // Now reset the height
                    setMenuSubItemHeight();

                }, 250);
            };


        }

        /**
         * This will set the MenuSubItem height according to  the browser height need to call on page load/resize
         */
        function setMenuSubItemHeight() {

            // load pg-container to find height of the container
            var container = $('#pgcontainer');

            //to find the dynamic max-height of MenuSubItem need to subtract menu item height from container height
            // sub-Menu Height = [pg container height ] - [[top bar] - [profile height] -[menu-section height * number of menu-section] -[Bottom button height] - [padding of sub-menu which is 1]]

            var subMenuHeight = container.outerHeight(true)-container.find(".topbar").outerHeight(true) - container.find(".profile").outerHeight(true)  - ($('#user-dashboard-menu-section').outerHeight() * container.find("aside nav ul li[id$='-menu-section']").length) -container.find("aside button").outerHeight(true)-1;

            // set all sub menu max-height according to the browser height

            // container.find("aside ul li ul").css("max-height",subMenuHeight);
        }

        /**
         * This will attempt to locate the dashboard belong to the parent frame
         */
        function hasParentDashboard(editMode) {
            var parentDashboard = getParentDashboard();
            return (parentDashboard && !editMode) || (parentDashboard && parentDashboard.editMode())
        }

        /**
         * This will attempt to locate the dashboard belong to the parent frame
         */
        function getParentDashboard() {
            if(typeof parent.psApp != 'undefined' && parent.psApp.dashboards.getCurrentDashboard())
                return parent.psApp.dashboards.getCurrentDashboard();
            return null;
        }

        /**
         * This will attempt to create a proxy widget reference to the parent frame for the selected widget
         *
         * @param widgetId The widget reference
         */
        function getParentProxyWidget(widgetId) {
            var parentDashboard = getParentDashboard();
            if(parentDashboard) return parentDashboard.getProxyWidget(widgetId);
            return null;
        }

        /**
         * This will attempt to create a proxy widget reference to the parent frame for the selected widget and then
         * send an event to request the change
         *
         * @param widgetId The widget reference
         * @param properties The property map
         */
        function requestWidgetStyleChange(widgetId, properties) {
            var parentDashboard = getParentDashboard();
            if(parentDashboard) {
                var widgetProxy = parentDashboard.getProxyWidget(widgetId);
                if(widgetProxy) widgetProxy.requestWidgetFrameStyleChange(properties);
            }
        }

        // Return the helper methods
        return {
            hasParentDashboard: hasParentDashboard,
            getParentDashboard: getParentDashboard,
            getParentProxyWidget: getParentProxyWidget,
            requestWidgetStyleChange: requestWidgetStyleChange,
            maximiseWidgetHeight: maximiseWidgetHeight,
            setMenuSubItemHeight: setMenuSubItemHeight
        }
    })((typeof jQuery == "undefined" ? null : jQuery), (typeof PivotalUtils == "undefined" ? null : PivotalUtils));
}
