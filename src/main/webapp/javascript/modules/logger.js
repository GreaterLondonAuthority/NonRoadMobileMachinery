/**
 * A simple logger that will attempt to log the output the specific stream specified.
 * You could write an adapter to send the log back to the server or use the pre-defined
 * client side debugger that is available. It expects the development tools to be available
 *
*/
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("logger");
    psApp.logger = (function (output) {

        // Modify the console to have a debug method
        (function () {
            // SS-512 only attempt to access if there is a console
            if (window.console) {
                var debug = window.console.debug;
                if (!debug) {
                    window.console.debug = function (message) {
                        this.log(message);
                    }
                }
            }
        })();

        // The loggers that are stored by index
        var _loggers = {};

        /**
         * Dummy logger
         *
         * @constructor
         */
        var DummyLogger = function () {
        };

        /**
         * Adding the Dummy functionality
         *
         * @type {{log: log, info: info, warn: warn, debug: debug, error: error}}
         */
        DummyLogger.prototype = {

            /**
             * Dummy
             */
            log: function (arguments) {
            },

            /**
             * Dummy
             */
            info: function (arguments) {
            },

            /**
             * Dummy
             */
            warn: function (arguments) {
            },

            /**
             * Dummy
             */
            debug: function (arguments) {
            },

            /**
             * Dummy
             */
            error: function (arguments) {
            }
        };
        var Dummy = new DummyLogger();

        /**
         * Will create a new logger instance. This is private and can only be accessed from
         * the #getLogger(string) function. The stream is the underlying mechanism for
         * storing objects. The output should follow the Console interface.
         *
         * @param {string} namespace The namespace for this logger
         * @param {Console} stream The output console to use - by default this is the client console
         * @constructor
         */
        var Logger = function (namespace, stream) {
            this._namespace = namespace;
            this._stream = stream;
        };

        /**
         * Adding the required functionality
         * @type {{log: log, info: info, warn: warn, debug: debug, error: error}}
         */
        Logger.prototype = {

            /**
             * Will print the arguments directly to the stream
             *
             * @param arguments The arguments to print
             */
            log: function (arguments) {
                this._stream.log(arguments);
            },

            /**
             * Will print the arguments as an info statement
             *
             * @param arguments The arguments to print
             */
            info: function (arguments) {
                this._stream.info(this.formatStatement(arguments));
            },

            /**
             * Will print the arguments as a warning statement
             *
             * @param arguments The arguments to print
             */
            warn: function (arguments) {
                this._stream.warn(this.formatStatement(arguments));
            },

            /**
             * Will print the arguments as a debug statement
             *
             * @param arguments  The arguments to print
             */
            debug: function (arguments) {
                this._stream.debug(this.formatStatement(arguments));
            },

            /**
             * Will print the arguments as an error statement
             *
             * @param arguments The arguments to print
             */
            error: function (arguments) {
                this._stream.error(this.formatStatement(arguments));
            },

            /**
             * This function is used to format the string that will be shown.
             *
             * @param arguments The list of arguments provided
             */
            formatStatement: function (arguments) {
                return this.getDateString() + " " + this._namespace + " - " + arguments;
            },

            /**
             * Will fetch the current date stamp as a string
             *
             * @returns {string} The current date as date time
             */
            getDateString: function () {
                var d = new Date();
                return d.toLocaleDateString() + " " + d.toLocaleTimeString();
            }
        };

        /**
         * Will return a unique new logger for the namespace specified
         *
         * @param namespace The namespace for the logger
         * @param stream The actual undelying output stream to use (by default the console is used)
         */
        function getLogger(namespace, stream) {
            if (!output || !window.console) return Dummy;
            if (!_loggers[namespace])
                return _loggers[namespace] = new Logger(namespace, (stream ? stream : window.console));
            return _loggers[namespace];
        }

        // Return the classes under the namespace
        return {
            getLogger: getLogger,
            Logger: Logger
        };
    })(false);
}
