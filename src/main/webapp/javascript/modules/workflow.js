/**
 * A Popups namespace for running workflows from js
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("workflow");
    psApp.workflow = (function () {

        execute = function(appPath, workflowCode, params, callback) {

            var url = appPath + "/admin/workflow/execute";
            if (params)
                params = params + "&code=" + workflowCode;
            else
                params = "code=" + workflowCode;

            var response = PivotalUtils.getJsonContent(url, params, false);
            if (callback)
                callback(response);

            // PivotalUtils.getJsonContentAsync(url, params, function (response) {
            //     if (callback)
            //         callback(response);
            // }, false);
        };

        executeWait = function(appPath, workflowCode, params) {

            var url = appPath + "/admin/workflow/execute";
            if (params)
                params = params + "&code=" + workflowCode;
            else
                params = "code=" + workflowCode;

            return PivotalUtils.getJsonContent(url, params, false);
        };

        evalResponse = function(workflowResponse) {
            if (workflowResponse && workflowResponse.information)
                return eval(workflowResponse.information.trim());
            else
                return "";
        };

        return {
            execute: execute,
            executeWait: executeWait,
            evalResponse: evalResponse
            }
    })();
}
