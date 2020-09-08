/**
 * A Popups namespace for showing the postcode
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("postcode");
    psApp.postcode = (function () {

        var rootURL = "https://api.postcodes.io/";
        var fullURL = rootURL + "postcodes/#postcode#";
        var partialURL = rootURL + "outcodes/#postcode#";
        var postcodeURL = rootURL + "postcodes?lon=#longitude#&lat=#latitude#";
        var nearestPostcodeURL = rootURL + "outcodes?lon=#longitude#&lat=#latitude#";

        // Returns an object with details in it
        getLocation = function(postcode, callback) {

            doFullSearch(postcode, function(searchResult) {

                var returnValue = buildReturnObject(searchResult, true);

                if (returnValue.found) {
                    // found it, send back
                    callback(returnValue);
                }
                else {
                // if we haven't found by full search then do inward search
                    doPartialSearch(postcode, function(searchResult) {
                        if (callback)
                            callback(buildReturnObject(searchResult, false));
                    });
                }
            });
        };

        getPostcode = function(longitude, latitude, callback) {

            if (latitude !== "" && latitude !== "")
                doSearch(postcodeURL.replace("#longitude#", encodeURIComponent(longitude)).replace("#latitude#", encodeURIComponent(latitude)), function(searchResult) {

                    var returnObject = buildReturnObject(searchResult, true);
                    // If no borough try again with nearest outcode
                    if (!returnObject.borough) {
                        doSearch(nearestPostcodeURL.replace("#longitude#", encodeURIComponent(longitude)).replace("#latitude#", encodeURIComponent(latitude)), function(searchResult) {
                            var returnObject = buildReturnObject(searchResult, false);
                            // getRegion(returnObject, callback);
                            callback(returnObject);
                        });
                    }
                    else {
                        // getRegion(returnObject, callback);
                        callback(returnObject);
                    }
                });
            else
                callback("");
        };

        getRegion = function (returnObject, callback) {
            if (returnObject.region || !returnObject.postcode)
                callback(returnObject);
            else {
                // do a post code lookup to get the region
                doFullSearch(returnObject.postcode, function (pcResult) {
                    var newReturnObject = buildReturnObject(pcResult);
                    if (newReturnObject.region)
                        returnObject.region = newReturnObject.region;

                    callback(returnObject);
                });
            }
        };

        doFullSearch = function(postcode, callback) {

            if (postcode !== "" && postcode.length > 4)
                doSearch(fullURL.replace("#postcode#", encodeURIComponent(postcode)), callback);
            else
                callback("");
        };

        doPartialSearch = function(postcode, callback) {
            if (postcode !== "")
                doSearch(partialURL.replace("#postcode#", encodeURIComponent(postcode)), callback);
            else
                callback("");
        };

        doSearch = function(url, callback) {

            if (url !== "") {
                $.ajax({
                   type: "GET",
                   url: url,
                   success: function (response) {
                       if (callback)
                            callback(response);
                   },
                   error: function (xhr, ajaxOptions, thrownError) {
                       var msg = '';
                       if (xhr.status === 0) {
                           msg = 'Not connect.\n Verify Network.';
                       } else if (xhr.status == 404) {
                           msg = 'Requested page not found. [404]';
                       } else if (xhr.status == 500) {
                           msg = 'Internal Server Error [500].';
                       } else if (thrownError === 'parsererror') {
                           msg = 'Requested JSON parse failed.';
                       } else if (thrownError === 'timeout') {
                           msg = 'Time out error.';
                       } else if (thrownError === 'abort') {
                           msg = 'Ajax request aborted.';
                       } else {
                           msg = 'Uncaught Error.\n' + xhr.responseText;
                       }
                       callback(msg);
                   }
               });
            }
            else
                alert(PivotalUtils.i18nBundle.postcodeUrlEmpty);
        };

        buildReturnObject = function(pcResult, specificSearch) {

            var returnValue = {};
            returnValue.specific = !!specificSearch;
            try {
                if (typeof pcResult === 'string') {
                    returnValue.error = "Postcode search error \r\n" + pcResult;
                    returnValue.inError = true;
                    returnValue.found = false;
                } else {

                    if (pcResult && pcResult.status) {

                        // Found it
                        if (pcResult.status === 200) {
                            returnValue.found = true;
                            returnValue.inError = false;
                        } else {

                            // wrong status
                            returnValue.found = false;
                            returnValue.inError = true;
                            returnValue.error = "Postcode search returned wrong status - " + pcResult.status;
                        }
                        returnValue.status = pcResult.status;
                        if (pcResult.result) {
                            returnValue.data = pcResult.result;
                            var resultObject;
                            if (pcResult.result.length) {
                                resultObject = pcResult.result[0];
                            } else
                                resultObject = pcResult.result;

                            returnValue.longitude = resultObject.longitude ? resultObject.longitude : "";
                            returnValue.latitude = pcResult.result.latitude ? resultObject.latitude : "";
                            returnValue.region = pcResult.result.region ? resultObject.region : "";
                            returnValue.borough = resultObject.admin_district ? resultObject.admin_district : "";
                            returnValue.postcode = resultObject.postcode ? resultObject.postcode : "";
                        }
                    }
                }
            }
            catch(x) {
                if (!returnValue.inError) {
                    returnValue.inError = true;
                    returnValue.error = "Postcode search error - " + x;
                }
            }
            return returnValue;
        };

        return {
            getLocation : getLocation,
            getPostcode : getPostcode
            }
    })();
}
