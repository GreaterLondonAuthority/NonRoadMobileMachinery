/**
 * Pattern
 * Provides standard methods that allow pattern matching
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("pattern");
    psApp.pattern = (function () {

        var logger = psApp.logger.getLogger("psApp.Pattern", null);

        var matchPattern = function(value, pattern, error) {

            if (pattern && pattern != "") {
                try {
                    var regExp = new RegExp(pattern, "g");
                    var res = value.match(regExp);
                    if (!res) {
                        if (error && error != "")
                            alert(error);
                        return false;
                    }
                }
                catch(e) {
                    alert(e + " " + error);
                    return false;
                }
            }
            return true;
        };

        return {
            matchPattern : matchPattern
        }
    })();
}
