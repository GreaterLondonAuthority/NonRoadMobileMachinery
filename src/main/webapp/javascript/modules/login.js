/**
 * Basket
 * Provides standard methods that allow processing basket items
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("login");
    psApp.login = (function () {

        var logger = psApp.logger.getLogger("psApp.Login", null);

        /**
         * Encrypt any non-empty passwords. We encrypt both
         * of them as they will both contain the password as plaintext.
         */

        var encryptAllPasswords = function(suffix) {
            // loop through all password inputs
            $("[type=password]").each(function (index) {
                // get original value
                var value = $(this).val();
                if (value != "") {
                    // set the value to encrypt and base64-encode
                    if (suffix && suffix != "")
                        $("#" + this.id + suffix).val(encryptPassword(value));
                    else
                        $(this).val(encryptPassword(value));
                }
            });
        };

        /**
         * If the password is not empty then xor encrypts it and base64-encodes it,
         * otherwise returns an empty string
         *
         * @param rawPassword the contents of the password field.
         * @return if password empty then empty string else xor-encrypted, base64-encoded string
         */

        var encryptPassword = function(rawPassword) {
            logger.debug("encrypting password");
            if (rawPassword.trim == "") {
                return rawPassword;
            }
            var sessionId = PivotalUtils.getCookie("JSESSIONID");
            var passwordEncrypted = PivotalUtils.xorEncode(rawPassword, sessionId);
            var passwordEncryptedEncoded = PivotalUtils.base64Encode(passwordEncrypted);
            return passwordEncryptedEncoded;
        };

        var passwordsMatch = function() {
            var valid = true;
            var lastVal = "";
            var first = true;
            $("input[type='password']").each(function() {
                if (first) {
                    lastVal = $(this).val();
                    first = false;
                } else {
                    valid = valid && $(this).val() === lastVal;
                }
            });
            return valid;
        };

        return {
            encryptAllPasswords : encryptAllPasswords,
            encryptPassword : encryptPassword,
            passwordsMatch : passwordsMatch
        }
    })();
}
