/**
 * A AutoSave namespace for admin of autosave
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("autosave");
    /**
     * path             path to entity being updated
     * template         template the autosave is for
     * formElement      form that contains the data
     * dbKey            key of record being updated
     * progressElement  Element that is toggled to indicate auto save activity
     *                  if true then an element is added to the form
     *                  if false then no element is used
     *                  otherwise is assumed to be jquery element
     *
     */
    psApp.autosave = (function (path, template, formElement, dbKey, progressSetting) {

        var initialData = "";
        var lastData = "";
        var period = 30;
        var processing = false;
        var delaying = false;
        var progressElement;
        var paused = false;

        if (progressSetting == true)
            if (formElement) {
                formElement.append("<div id='autoSaveProgress'</div>");
                progressElement = $("#autoSaveProgress");
            }
        else if (progressSetting != false)
            progressElement = progressSetting;

        try {

            // Initialise the autosave on the server for this record

            initialData = formElement.serialize();
            lastData = initialData;
            var disabled = false;
            PivotalUtils.getJsonContentAsync(path + "/autosave/init/" + template + "/" + dbKey, initialData, function (result) {
                // Check result, show error or get refresh period

                if (result.inError) {
                    autoSaveLog("Disabled - " + result.error, true);
                }
                else {
                    period = parseInt(result.information);

                    if (isNaN(period))
                        period = 30;

                    autoSaveLog("Initialised - Refresh Period = " + period);

                    if (result.data["timeAdded"]) {
                        if (confirm(PivotalUtils.i18nBundle.autoSaveFoundMessage + " : " + result.data["timeAdded"])) {
                            formElement.hide();
                            autoSaveLog("User has selected to restore data");
                            var newLocation = document.location.href;
                            if (newLocation.indexOf('?') < 0) newLocation += "?";
                            newLocation = newLocation.replace("\?", "?autosaverestore=" + template + "&");
                            document.location.href = newLocation;
                            psApp.windowManager.setDirty();
                        }
                        else {
                            // Don't want it so remove it
                            autoSaveLog("User has selected to remove data")
                            autoSaveRemoveData();
                        }
                    }
                }
            }, true);

            // Set initial Data after the form has settled
            // Set timer for autosave
            if (!disabled) {
                setTimeout(function () {
                    autoSaveLog("Auto save data initialised");
                    initialData = formElement.serialize();
                    setTimeout(doAutoSave, period * 1000);
                }, 5000);
            }
        }
        catch (X) {
            autoSaveLog(X, true);
        }

        // Create the function to do the timed autosave

        var doAutoSave = function () {

            autoSaveLog("Tick");
            processing = true;
            delaying = true;

            if (paused) {
                autoSaveLog("Paused");
            }
            else {
                // add timeout so the progress displays for at least 1 seconds
                setTimeout(function () {
                    delaying = false;
                    autoSaveProgressOff();
                }, 1000);

                autoSaveProgressOn();
                // If data has changed then auto save it

                var newData = formElement.serialize();

                if (lastData != newData) {
                    lastData = newData;
                    if (initialData == newData) {
                        autoSaveLog("Data the same as initial data")
                        autoSaveRemoveData();
                    }
                    else {
                        // debugger
                        autoSaveAddData(newData);
                        // psApp.windowManager.setDirty();
                    }
                }
                processing = false;
                autoSaveProgressOff();
            }
            // Sleep until time to autosave

            setTimeout(doAutoSave, period * 1000);
        };

        var autoSaveAddData = function(data) {
            var retValue = true;
            autoSaveProgressOn();
            autoSaveLog("Adding AutoSave data");
            PivotalUtils.getJsonContentAsync(path + "/autosave/" + template + "/" + dbKey, data, function (result) {
                if (result.inError) {
                    autoSaveLog("Save Fail - " + result.error);
                    retValue = false;
                }
                else {
                    autoSaveLog("Save Success");
                }
            }, true);

            autoSaveProgressOff();
            return retValue;
        };

        var autoSaveRemoveData = function() {

            var retValue = true;
            autoSaveProgressOn();
            autoSaveLog("Deleting AutoSave data");
            PivotalUtils.getJsonContentAsync(path + "/autosave/remove/" +  template + "/" + dbKey, "", function (result) {
                if (result.inError) {
                    autoSaveLog("Deletion fail - " + result.error);
                    retValue = false;
                }
                else {
                    autoSaveLog("Deletion success");
                }
            }, true);

            autoSaveProgressOff();
            return retValue;
        };

        var autoSaveLog = function(message, sendAlert) {

            if (message && (console || sendAlert)) {
                message = "Auto Save - " + path + (dbKey?("/" + dbKey):"") + " - " + message;

                // if (console) console.log(message);
                if (sendAlert) alert(sendAlert);
            }
        };

        var autoSaveProgressOn = function() {
            autoSaveProgressSwitch("on");
        };
        var autoSaveProgressOff = function() {
            if (!delaying && !processing)
                autoSaveProgressSwitch("off");
        };
        var autoSaveProgressSwitch = function (dir) {

            if (progressElement) {
                 if (dir == "on")
                     progressElement.show();
                 else if (dir == "off")
                     progressElement.hide();
                 else if(!dir)
                     progressElement.toggle();

            }
        };

        var pause = function() {
            paused = true;
        }
        var resume = function() {
            paused = false;
        }
        var isPaused = function() {
            return paused;
        }
        autoSaveProgressOff();

        return {
            pause : pause,
            resume : resume,
            isPaused : isPaused
        };

    });
}
