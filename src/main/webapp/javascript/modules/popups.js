/**
 * A Popups namespace for showing the popups
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("popups");
    psApp.popups = (function () {

        /** Popup width/height settings **/

        var siteHeight = "80%";
        var siteWidth ="90%";
        var machineryHeight = "80%";
        var machineryWidth ="90%";
        var emailHeight = "70px";
        var emailWidth ="850px";
        var meetingHeight = "80%";
        var meetingWidth = "90%";
        var advancedSearchHeight = "80%";
        var advancedSearchWidth ="90%";

        /**
         * opens up the email compose screen keeping the reference to the case Id
         * probably called from case admin
         * @param path AppPath
         * @param id id of the related case
         * @param settings map of settings
         *
         */
        var emailCompose = function (path, id, settings) {

            if (!path) path = "";
            if (!id) id = "";

            var fullPath = path + "/case/email/" + id;

            // Set default dimensions
            putSetting(settings, "width", emailWidth, false);
            putSetting(settings, "height", emailHeight, false);
            putSetting(settings, "name", "case-email", true);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the case edit popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var caseEdit = function (path, id, settings) {

            if (!path) path = "";
            if (!id) id = "";
            var fullPath = path + "/case/edit/" + id;
            var queryString = "";
            var tabName = getSetting(settings, "opentab", "");
            if (tabName !== "")
                queryString += (queryString===""?"":"&") + "opentab=" + tabName;

            var readOnlyRecord = getSetting(settings, "readonlyrecord", "");
            if (readOnlyRecord !== "")
                queryString += (queryString===""?"":"&") + "readonlyrecord=" + readOnlyRecord;

            if (queryString !== "")
                fullPath += "?" + queryString;

            // Set default dimensions

            putSetting(settings, "width", siteWidth, false);
            putSetting(settings, "height", siteHeight, false);
            putSetting(settings, "name", "case-edit", true);
            var customActions = [
                {
                    name: "external-link",
                    handler: function () {

                        var win = window.open(window.top.location.href,"_blank");
                        if (win) win.focus();
                    }
                },
                "Maximize", "Refresh", "Close"
            ];
            putSetting(settings, "customActions", customActions);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the site register popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var siteRegister = function (path, id, settings) {
            if (!path) path = "";
            if (!id) id = "";

            var fullPath = path + "/site/";
            if (id && id >>> 0)
                fullPath += "edit/" +id;
            else
                fullPath += "register";

            putSetting(settings, "width", siteWidth, false);
            putSetting(settings, "height", siteHeight, false);
            putSetting(settings, "name", "site-edit", true);
            var customActions = [
                {
                    name: "external-link",
                    handler: function () {

                        var win = window.open(window.top.location.href,"_blank");
                        if (win) win.focus();
                    }
                },
                "Maximize", "Refresh", "Close"
            ];
            putSetting(settings, "customActions", customActions);

            openModalWindow(fullPath, settings);

        };

        /**
         * opens up the site view popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var siteView = function (path, id, settings) {
            if (!path) path = "";
            if (!id) id = "";
            var fullPath = path + "/site/view/" + id;
            putSetting(settings, "width", 800, false);
            putSetting(settings, "height", siteHeight, false);
            putSetting(settings, "name", "site-view", true);
            var customActions = [
                "Maximize", "Refresh", "Close"
            ];
            putSetting(settings, "customActions", customActions);

            openModalWindow(fullPath, settings);

        };

        /**
         * opens up the site edit popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var siteEdit = function (path, id, settings) {

            if (!path) path = "";
            if (!id) id = "";
            var fullPath = path + "/site/edit/" + id;
            var queryString = "";
            var tabName = getSetting(settings, "opentab", "");
            if (tabName !== "")
                queryString += (queryString===""?"":"&") + "opentab=" + tabName;

            var readOnlyRecord = getSetting(settings, "readonlyrecord", "");
            if (readOnlyRecord !== "")
                queryString += (queryString===""?"":"&") + "readonlyrecord=" + readOnlyRecord;

            if (queryString !== "")
                fullPath += "?" + queryString;

            // Set default dimensions

            putSetting(settings, "width", siteWidth, false);
            putSetting(settings, "height", siteHeight, false);
            putSetting(settings, "name", "site-edit", true);
            var customActions = [
                {
                    name: "external-link",
                    handler: function () {

                        var win = window.open(window.top.location.href,"_blank");
                        if (win) win.focus();
                    }
                },
                "Maximize", "Refresh", "Close"
            ];
            putSetting(settings, "customActions", customActions);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the machine register popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var machineryRegister = function (path, siteId, settings) {
            putSetting(settings, "params", "machineryaction=register", false);
            machineryEdit(path, siteId, null, settings);
        };

        /**
         * opens up the machine list popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var machineryList = function (path, siteId, settings) {

            putSetting(settings, "params", "machineryaction=list", false);
            machineryEdit(path, siteId, null, settings);
        };

        /**
         * opens up the machine register popup
         * @param path AppPath
         * @param siteId id to be edited
         * @param machineryId id to be edited
         * @param settings map of settings
         *
         */
        var machineryEdit = function (path, siteId, machineryId, settings) {
            if (!path) path = "";
            if (siteId) {
                var fullPath = path + "/machinery/edit/" + siteId;
                if (machineryId)
                    fullPath += "/" + machineryId;

                var params=getSetting(settings, "params", "");
                if (params !== "")
                    fullPath += "?" + params;

                putSetting(settings, "width", machineryWidth, false);
                putSetting(settings, "height", machineryHeight, false);
                putSetting(settings, "name", "machinery-edit", true);

                var customActions = [
                    {
                        name: "external-link",
                        handler: function () {

                            var win = window.open(window.top.location.href, "_blank");
                            if (win) win.focus();
                        }
                    },
                    "Maximize", "Refresh", "Close"
                ];
                putSetting(settings, "customActions", customActions);

                openModalWindow(fullPath, settings);
            }
        };

        var openMachineryExemption = function(path, machineryId, action, settings) {
            if (!path) path = "";
            if (machineryId) {
                var fullPath = path + "/machinery/exemption";
                if (machineryId)
                    fullPath += "/" + machineryId;

                if (action)
                    fullPath += "/" + action;

                putSetting(settings, "width", "900px", false);
                putSetting(settings, "height", "700px", false);
                putSetting(settings, "name", "machinery-exemption", true);

                openModalWindow(fullPath, settings);
            }
        };
        /**
         * opens up the machine register popup
         * @param path AppPath
         * @param siteId id to be edited
         * @param machineryId id to be edited
         * @param settings map of settings
         *
         */
        var machineryEditReplace = function (path, siteId, machineryId, settings) {
            if (!path) path = "";

            if (siteId) {
                var fullPath = path + "/machinery/edit/" + siteId;
                if (machineryId)
                    fullPath += "/" + machineryId;

                psApp.windowManager.getCurrentWindow().window.location.href  = fullPath;
            }
        };

        /**
         * opens up the case edit popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var advancedSearch = function (path, settings) {

            if (!path) path = "";
            var fullPath = path + "/advancedsearch/search?clear=true";

            // Set default dimensions

            putSetting(settings, "width", advancedSearchWidth, false);
            putSetting(settings, "height", advancedSearchHeight, false);
            putSetting(settings, "name", "AdvancedSearch", true);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the meeting edit popup
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         * @param isTemplate if true then editing template
         * @param fromTemplate if true then creating a meeting from template
         *                     and the id is then the template id
         *
         */
        var meetingEdit = function (path, id, settings, isTemplate, fromTemplate) {

            if (!path) path = "";
            if (!id) id = "";

            var fullPath = path + "/meeting/edit" + (isTemplate ? "_template" : "") + "/" + id;

            if (fromTemplate)
                fullPath += "?CreateFromTemplate=yes";

            // Set default dimensions
            putSetting(settings, "width", meetingWidth, false);
            putSetting(settings, "height", meetingHeight, false);
            putSetting(settings, "name", "meeting-edit", true);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the meeting edit popup using a caseMeeting id
         * probably called from case admin
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var caseMeetingEdit = function (path, id, settings) {

            if (!path) path = "";
            if (!id) id = "";

            var fullPath = path + "/meeting/edit/" + id + "?FromCase=yes";

            // Set default dimensions
            putSetting(settings, "width", meetingWidth, false);
            putSetting(settings, "height", meetingHeight, false);
            putSetting(settings, "name", "meeting-edit", true);

            openModalWindow(fullPath, settings);
        };

        /**
         * opens up the case edit popup using a caseMeeting id
         * probably called from meeting admin
         * @param path AppPath
         * @param id id to be edited
         * @param settings map of settings
         *
         */
        var meetingCaseEdit = function (path, id, settings) {

            if (!path) path = "";
            if (!id) id = "";

            var fullPath = path + "/case/edit/" + id + "?FromMeeting=yes";

            // Set default dimensions
            putSetting(settings, "width", siteWidth, false);
            putSetting(settings, "height", siteHeight, false);
            putSetting(settings, "name", "case-edit", true);

            openModalWindow(fullPath, settings);
        };

        /**
         * Opens the mapping window
         *
         * @param path The root application path
         * @param caseId The case identifier
         * @param polygons The polygons to load into the map
         * @param coordinates The coordinates to centre on (priority 1)
         * @param postcode The postcode to search and centre on (priority 2)
         * @param address The address to search and centre on (priority 3)
         * @param settings Any settings for the window
         */
        var openMappingWindow = function (path, caseId, polygons, coordinates, postcode, address, settings) {

            if (!path) path = "";
            var fullPath = path + "/mapping/" + caseId;
            if (polygons)
                top["polygons" + caseId] = polygons;

            if (coordinates)
                fullPath = fullPath + "?coordinates=" + encodeURIComponent(coordinates);
            else if (postcode)
                fullPath = fullPath + "?postcode=" + encodeURIComponent(postcode);
            else if (address)

                fullPath = fullPath + "?address=" + encodeURIComponent(address);

            // Set default dimensions
            putSetting(settings, "width", "1000px", true);
            putSetting(settings, "height", "800px", true);
            putSetting(settings, "name", "casemapping-edit", true);

            console.log("Opening mapping window [fullPath = " + (fullPath?fullPath:"") + " [Polygons = " + (polygons?polygons:"") + "]" + JSON.stringify(settings));

            return openModalWindow(fullPath, settings);
        };

        /**
         * Opens the mapping window
         *
         * @param path The root application path
         * @param caseId The case identifier
         * @param polygons The polygons to load into the map
         * @param coordinates The coordinates to centre on (priority 1)
         * @param postcode The postcode to search and centre on (priority 2)
         * @param address The address to search and centre on (priority 3)
         * @param settings Any settings for the window
         */
        var openMappingImportWindow = function (path, settings) {

            if (!path) path = "";
            var fullPath = path + "/mapping/import";

            // Set default dimensions
            putSetting(settings, "width", "500px", true);
            putSetting(settings, "height", "400px", true);
            putSetting(settings, "name", "casemapping-import", true);

            console.log("Opening mapping import window [fullPath = " + (fullPath?fullPath:"") + " " + JSON.stringify(settings));

            return openModalWindow(fullPath, settings);
        };

        /**
         * Opens the wizard window
         *
         * @param path      The root application path
         * @param id        The id of the wizard to open
         * @param settings  Any settings for the window
         */
        var openNewWizardWindow = function (path, id, settings) {

            if (!path) path = "";
            var fullPath = path + "/wizard/new";
            if (id) fullPath += "/" + id;

            // Set default dimensions
            putSetting(settings, "width", "90%", true);
            putSetting(settings, "height", "90%", true);

            return openModalWindow(fullPath, settings);
        };

        /**
         * Opens the wizard window
         *
         * @param path      The root application path
         * @param id        The id of the wizard data record to open
         * @param settings  Any settings for the window
         */
        var openGetWizardWindow = function (path, id, settings) {

            if (!path) path = "";
            var fullPath = path + "/wizard/get";
            if (id) fullPath += "/" + id;

            // Set default dimensions
            putSetting(settings, "width", "90%", true);
            putSetting(settings, "height", "90%", true);

            return openModalWindow(fullPath, settings);
        };

        var openCopyWizardWindow = function(wizardName, sourceEntityName, sourceEntityId) {

            var settings = {};
            var fullPath = globalPaths.appPath + "/wizard/copyfromentity";
            fullPath += "?wizardName=" + encodeURIComponent(wizardName) + "&sourceEntityName=" + encodeURIComponent(sourceEntityName) + "&sourceEntityId=" + encodeURIComponent(sourceEntityId);

            // Set default dimensions
            putSetting(settings, "width", "90%", true);
            putSetting(settings, "height", "90%", true);

            return openModalWindow(fullPath, settings);
        };

        /**
         * Used by other popup handlers,
         * This method gets out the standard settings
         *
         * @param fullPath path to the content for the popup
         * @param settings Settings for the popup
         *
         */
        function openModalWindow(fullPath, settings) {

            var windowName = getSetting(settings, "name", "");
            var windowTitle = getSetting(settings, "title", "");
            var windowWidth = getSetting(settings, "width", "600px");
            var windowHeight = getSetting(settings, "height", "600px");
            var closeFunction = getSetting(settings, "closeFunction", false);
            var useIFrame = getSetting(settings, "useIFrame", true);
            var customActions = getSetting(settings, "customActions", false);

            return psApp.windowManager.open({title:windowTitle, width:windowWidth, height:windowHeight, content:fullPath, useIFrame:useIFrame, customActions:customActions, closeFunction:closeFunction});

        }


        /**
         *
         * @param settings settings map
         * @param key name of setting to get
         * @param defaultValue default if key not present
         *
         */
        function getSetting(settings, key, defaultValue) {

            var returnValue = defaultValue;
            if (settings && settings.hasOwnProperty(key))
                returnValue = settings[key];

            return returnValue;
        }

        function putSetting(settings, key, value, isDefault) {

            if (!settings) settings = {};
            if (!isDefault || !settings.hasOwnProperty(key))
                settings[key] = value;
        }

        return {
                emailCompose: emailCompose,
                openMappingWindow: openMappingWindow,
                openMappingImportWindow:openMappingImportWindow,
                caseEdit: caseEdit,
                siteEdit: siteEdit,
                siteRegister: siteRegister,
                siteView: siteView,
                machineryEdit: machineryEdit,
                machineryList: machineryList,
                machineryEditReplace: machineryEditReplace,
                machineryRegister: machineryRegister,
                openMachineryExemption: openMachineryExemption,
                meetingEdit: meetingEdit,
                caseMeetingEdit: caseMeetingEdit,
                meetingCaseEdit: meetingCaseEdit,
                openNewWizardWindow: openNewWizardWindow,
                openGetWizardWindow: openGetWizardWindow,
                openCopyWizardWindow: openCopyWizardWindow,
                getSetting: getSetting,
                putSetting: putSetting,
                advancedSearch:advancedSearch,
                openModalWindow:openModalWindow
            }
    })();
}
