/**
 * Module pattern for providing the classes we require for the dashboard manager.
 *
*/
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("eventManager");
    psApp.eventManager = (function () {

        /**
         * The Dashboard event manager is the centrally used event manager for communicating between different parts
         * of the application. For example, a popup can request that a widget be added and any layout listening
         * can receive that event and add the widget.
         *
         * @returns {EventManager}
         */
        var EventManager = (function () {

            // Get a logger
            var logger = psApp.logger.getLogger("psApp.eventManager", null);

            /**
             * Creates a new event manager for pubsub events within the application.
             *
             * @constructor
             */
            var EventManager = function (name) {
                logger.info("Creating new EventManager: " + name);
                this.name = name;
                this.initialize();
            };

            /**
             * Add the methods for registering and handling events.
             *
             * @type {{initialize: initialize, addListener: addListener, dispatchEvent: dispatchEvent, removeListener: removeListener}}
             */
            EventManager.prototype = {

                /**
                 * Initialises the event manager
                 */
                initialize: function () {
                    // Declare listeners as an object (we map the event to an array of listeners)
                    this.listeners = {};
                },

                /**
                 * Adds a listener for the event type
                 *
                 * @param event The event to listen to
                 * @param handler The handler
                 *
                 * @returns {EventManager}
                 */
                addListener: function (event, handler) {
                    logger.debug(this.name + ":Handler added for event: " + event);
                    if (!this.listeners[event]) {
                        this.listeners[event] = [];
                    }
                    if (handler instanceof Function) {
                        this.listeners[event].push(handler);
                    }
                    return this;
                },

                /**
                 * Dispatches the event onto the bus.
                 *
                 * @param event The event that has occurred
                 * @param params The parameters to add to the event
                 *
                 * @returns {EventManager}
                 */
                dispatchEvent: function (event, params) {
                    logger.debug(this.name + ":Event dispatched: " + event);

                    // Ensure that the event is present
                    if (this.listeners[event]) {

                        // loop through listeners array
                        for (var index = 0; index < this.listeners[event].length; index++) {

                            // Just execute the listener
                            this.listeners[event][index].call(window, params);
                        }
                    }
                    return this;
                },

                /**
                 * Removes the handler for the event
                 *
                 * @param event The event to unregister
                 * @param handler the handler that is to be removed
                 *
                 * @returns {EventManager}
                 */
                removeListener: function (event, handler) {
                    logger.debug(this.name + ":Handler removed for event: " + event);
                    // We can only remove the handler if it exists
                    if (this.listeners[event]) {
                        for (var i = 0, l = this.listeners[event].length; i < l; i++) {
                            if (this.listeners[event][i] === handler) {

                                // Remove the item from the array
                                this.listeners[event].slice(i, 1);
                                break;
                            }
                        }
                    }
                    return this;
                }
            };
            return EventManager;
        })();

        // Singleton of the event manager
        var SingletonEventManager = new EventManager("Global EventManager");

        // Return the event manager
        return {
            getInstance: function () {
                return SingletonEventManager;
            },
            EventManager: EventManager
        };
    })();
}
