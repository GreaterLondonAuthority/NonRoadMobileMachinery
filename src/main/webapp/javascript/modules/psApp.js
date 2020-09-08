/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

/**
 * This handles the internal namespace allowing new modules to be added to the global defined Namespace.
 * You can define new namespace modules or add to the 'psApp' namespace.
 *
* @type {psApp|*|Function}
 */
var psApp = psApp || (function() {

    /**
     * The wrapper for managing global namespaces
     *
    */
    var GlobalNameSpaceManager = (function() {

        // Constants
        var PERIOD = '.';

        /**
         * Will create a new namespace instance.
         *
         * @constructor
         * @param globalNameSpace {String} The global name space
         */
        var GlobalNameSpaceManager = function(globalNameSpace) {
            // We want to store what the global namespace will be incase the user specifies this in the namespace
            // creation method
            this._globalNameSpace = globalNameSpace;
        };

        /**
         * The public methods for creating new namespaces
         *
         * @type {{namespace: namespace}}
         */
        GlobalNameSpaceManager.prototype = {

            /**
             * This will create the new namespace and add the module.
             * The module will then be available under namespace using dot notation.
             * Useful utility taken from the YUI library {@link https://github.com/yui/yui3}
             *
             * @method namespace
             * @param {String} namespace* One or more namespaces to create.
             * @returns {Object} Reference to the last namespace object created.
             */
            namespace: function() {
                var a = arguments, o, i = 0, j, d, arg;

                // For each namespace argument passed we need to create the namespace specified
                for (; i < a.length; i++) {

                    // Reset base object per argument or it will get reused from the last
                    // The start object is "this"
                    o = this;
                    arg = a[i];

                    // Skip this if no "." is present
                    if (arg.indexOf(PERIOD) > -1) {
                        d = arg.split(PERIOD);

                        // If the namespace starts with the defined global space then we ignore this and
                        // move onto the next block
                        for (j = (d[0] == this._globalNameSpace) ? 1 : 0; j < d.length; j++) {
                            o[d[j]] = o[d[j]] || {};
                            o = o[d[j]];
                        }
                    }
                    else {

                        // Either use the existing or create a new empty object
                        o[arg] = o[arg] || {};

                        // Reset base object to the new object so it's returned
                        o = o[arg];
                    }
                }
                return o;
            }
        };

        // Return the classes under the namespace
        return GlobalNameSpaceManager;
    })();

    // Our own global namespace
    var psAppNameSpace = new GlobalNameSpaceManager("psApp");

    // Now add the global name space manager to the namespace
    psAppNameSpace.namespace("GlobalNameSpaceManager");
    psAppNameSpace.GlobalNameSpaceManager = GlobalNameSpaceManager;

    // Return this namespace and a method for creating new namespace managers
    return psAppNameSpace;
})();

// Assign the scope to the current window (makes it easier to test)
window.psApp = window.psApp || psApp;
