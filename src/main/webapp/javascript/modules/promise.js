/**
 * A Promise namespace for handling asynchronous tasks in an approachable manner. Allows you to store the item in the promise
 * without changing application logic that depends on the promise. For example, someone may expect to do something based on a promise,
 * and the first time this is loading they will wait. If someone else attepts to do something on the promise, the statement can be
 * returned immediately.
 *
*/
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("promise");
    psApp.promise = (function () {

        /**
         * This is a simple promise that should be returned from asynchronous methods, but only nested once (no chaining of results).
         * There are more advanced Promise packages available that provide the ability to chain promises, but this was not required
         * by the dashboard. As it follows the same interface, you can swap in one of these packages at a later date if required.
         *
         * @returns {SimplePromise}
         */
        var SimplePromise = (function () {

            /**
             * Initialises a new Promise using the callback function. The callback takes two
             * parameters, resolve and reject. Do something within the callback and then call resolve if everything worked,
             * otherwise call reject. reject will accept an {Error} like throw.
             *
             * @param resolver the resolver to be executed and will be called with two parameters
             * @constructor
             */
            var SimplePromise = function (resolver) {

                // Some error handling to ensure we have the correct items
                if (!(resolver instanceof Function)) {
                    throw new TypeError('You must pass a resolver function as the first argument to the promise constructor');
                }

                if (!(this instanceof SimplePromise)) {
                    throw new TypeError("Failed to construct 'Promise': Please use the 'new' operator, this object constructor cannot be called as a function.");
                }

                // Initialise a new subscribers list
                this._subscribers = [];

                // Now call the resolver
                invokeResolver(resolver, this);
            };

            /**
             * Will invoke the resolver and pass in the success and rejection functions.
             *
             * @param resolver The resolver to execute
             * @param promise The actual promise that is being waited on
             */
            function invokeResolver(resolver, promise) {

                // The resolve promise to use when successfull
                function resolvePromise(value) {
                    fulfill(promise, value);
                }

                // The rejection function to use when failed
                function rejectPromise(reason) {
                    reject(promise, reason);
                }

                try {
                    // Now call the resolver
                    resolver(resolvePromise, rejectPromise);
                } catch (e) {
                    rejectPromise(e);
                }
            }

            /**
             * Will fulfill the promise
             *
             * @param promise The promise to fulfill
             * @param value The value passed by the resolver
             */
            function fulfill(promise, value) {

                // The resolver has indicated that the promise has been fulfilled
                // If the promise has already been handled we can ignore it
                if (promise._state !== STATE_WAITING) {
                    return;
                }
                promise._state = STATE_FULFILLED;
                promise._detail = value;

                // Publish this fulfillment
                publish(promise);
            }

            /**
             * Will reject the promise
             *
             * @param promise The promise to reject
             * @param reason The reason passed by the resolver
             */
            function reject(promise, reason) {

                // The resolver has indicated that the promise has been rejected
                // If the promise has already been handled we can ignore it
                if (promise._state !== STATE_WAITING) {
                    return;
                }
                promise._state = STATE_REJECTED;
                promise._detail = reason;

                // Publish this rejection
                publish(promise);
            }

            /**
             * Will invoke the correct callback based on the state.
             *
             * @param state The current state of the promise
             * @param callbackPayload The callback payload containing the promise and handlers
             * @param detail The actual detail parameter
             */
            function invokeCallback(state, callbackPayload, detail) {
                var callback = (state === STATE_FULFILLED) ? callbackPayload.onFulfilled : callbackPayload.onRejection;
                var hasCallback = (callback instanceof Function);

                // Now try to call the call back if it exists and remember the value
                if (hasCallback) {
                    callback(detail);
                }
            }

            // The states of the promise
            var STATE_WAITING = void 0,
                STATE_FULFILLED = 1,
                STATE_REJECTED = 2;

            /**
             * Will subscribe the promise to the methods
             *
             * @param promise The promise to be fulfilled
             * @param onFulfilled The handler for the success
             * @param onRejection The handler for the failure
             */
            function subscribe(promise, onFulfilled, onRejection) {
                // Add the subscriber to the current list
                promise._subscribers.push({
                    promise: promise,
                    onFulfilled: onFulfilled,
                    onRejection: onRejection
                });
            }

            /**
             * Publishes the promise by calling the appropriate handler
             *
             * @param promise The promise to be fulfilled
             */
            function publish(promise) {
                for (var i = 0; i < promise._subscribers.length; i++) {
                    invokeCallback(promise._state, promise._subscribers[i], promise._detail);
                }

                // Now that we have handled the subscribers we can clear up
                promise._subscribers = null;
            }

            /**
             * Add the functionality required for a promise
             * @type {{}}
             */
            SimplePromise.prototype = {
                constructor: SimplePromise,

                /**
                 * The detail passed from the resolver and to b passed to the handler
                 */
                _detail: undefined,

                /**
                 * The list of resolvers waiting for an answer
                 */
                _subscribers: undefined,

                /**
                 * The current state of the promise
                 */
                _state: undefined,

                /**
                 * Both parameters are option but must be functions or will be ignored.
                 *
                 * @param onFulfilled Called when the promise has been fulfilled
                 * @param onRejection Called when the promise has been rejected
                 */
                then: function (onFulfilled, onRejection) {

                    // The user expects something to happen once the item has finished
                    if (this._state) {

                        // This has already finished so we can just handle the callback
                        invokeCallback(this._state, {promise: this, onFulfilled: onFulfilled, onRejection: onRejection}, this._detail);
                    }
                    else {

                        // This is in waiting state so we have to add the listener to the queue
                        subscribe(this, onFulfilled, onRejection);
                    }
                },

                /**
                 * Parameter is option but is expected to be a function.
                 *
                 * @param onRejection Called when the promise is rejected
                 */
                'catch': function (onRejection) {

                    // Just a convenience of the then method just having a onRejection
                    this.then(null, onRejection);
                }
            };

            // Return the class to initialize
            return SimplePromise;
        })();

        // Return the classes under the namespace
        return {
            'new': function (resolver) {
                return new SimplePromise(resolver);
            },
            Promise: SimplePromise
        };
    })();
}
