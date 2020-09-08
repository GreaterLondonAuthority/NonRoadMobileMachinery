/**
 * Some UI utility methods that come in useful. Ensures that it is not interfered with
 * as it is outside of the scope of everything.
 *
*/
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("ui");
    psApp.ui = (function() {

        /**
         * Creates a new UI instance.
         *
         * @constructor
         */
        var UI = function() {
            // Nothing really to do yet
        };

        /**
         * Add the methods available to the UI helper.
         *
         * @type {{fadeColorInAndOut: fadeColorInAndOut, delayedFunction: delayedFunction}}
         */
        UI.prototype = {

            /**
             * Will fade the item into the color at the fadeIn time (milliseconds) specified and back to the original
             * color at the fadeOut time (milliseconds).
             *
             * @param selector The class|id of the element or the jquery selector.
             * @param options A map of the options.
             *          fadeInColor The color to fade into (any CSS colour).
             *          fadeIn The time in miliseconds to fade into the color (default 1000 milliseconds).
             *          fadeOutColor The color to fade out of (or the original color will be chosen).
             *          fadeOut The time in miliseconds to fade back to the color default 1000 milliseconds).
             *          callback The function to call when the animatiob has finished
             */
            fadeColorInAndOut: function(selector, options) {
                selector = selector instanceof jQuery ? selector : $(selector);
                var fadeOutColor = options["fadeOutColor"] || selector.css("backgroundColor");
                selector.animate({backgroundColor: options["fadeInColor"]}, options["fadeIn"] ? options["fadeIn"] : 1000, function () {
                    selector.animate({backgroundColor: fadeOutColor}, options["fadeOut"] ? options["fadeOut"] : 1000, function() {
                        if(options["callback"] && options["callback"] instanceof Function) options["callback"]();
                    });
                });
            },

            /**
             * Creates a timed callback for executing the callback at the specified interval (milliseconds).
             *
             * @param callback The function to call at a specified interval. Function expects no arguments.
             * @param interval The time in milliseconds that the callback will be executed (1000 milliseconds by default).
             * @param repeat The number of times this should be repeated.
             */
            delayedFunction: function(callback, interval, repeat) {
                if(!callback instanceof Function) throw new TypeError("callback must be a function");
                if(interval && typeof interval !== 'number') throw new TypeError("interval must be a number");
                if(repeat && typeof repeat !== 'number') throw new TypeError("repeat must be a number");

                // Create the timer
                repeat = repeat || 1;
                interval = interval || 1000;
                var timerHandler = function () {

                    // Call the function
                    callback();

                    // Call it again
                    if (--repeat > 0) {

                        // Add the timer again
                        setTimeout(timerHandler, interval);
                    }
                };
                setTimeout(timerHandler, interval);
            }
        };

        // We only want one instance of the UI instance as it is a helper
        return new UI();
    })();
}
