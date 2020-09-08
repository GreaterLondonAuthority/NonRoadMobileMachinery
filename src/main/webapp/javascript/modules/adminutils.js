/**
 * An Admin utils namespace for showing the admin utils common to cases and meetings
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("adminUtils");
    psApp.adminUtils = (function () {

        var openEditEmail =  function(id, settings, draftEmailId) {

            var fullPath = globalPaths.appPath + "/" + globalPaths.pageName + "/email/" + id + "?draftemailid=" + draftEmailId;

            // Create Settings
            psApp.popups.putSetting(settings, "height", "90%");
            psApp.popups.putSetting(settings, "width", "850px");

            psApp.popups.openModalWindow(fullPath, settings);
        };

        var getFieldValue = function(settings) {

            var returnValue = "";
            try {
                returnValue = PivotalUtils.getContent(globalPaths.appPath + "/" + globalPaths.pageName + "/get", "settings=" + settings.toString());
            }
            catch(X) {
                console.log(X);
            }

            return returnValue;

        };

        return {
            openEditEmail : openEditEmail,
            getFieldValue : getFieldValue
            }
    })();
}
