// Create a custom function within the query space
(function($) {

    // The actual color picker item
    var ccp = (function() {

        // The recent colours that are shared with all items
        var RECENT_SELECTED_COLORS = [];

        /**
         * This will create a new color picker for the text input selector
         *
         * @constructor
         */
        var CustomColorPicker = function(selector, settings) {
            this.selector = selector;
            this.settings = settings;
            init.call(this);
        };

        /**
         * This will initialise the component by proxying the original text input to a new element that it will
         * replaced with.
         */
        function init() {

            // Determine the value to use
            var inputValue = this.selector.val() ||  this.settings.value;

            // Copy over the value that has been specified
            try {
                // Try to parse the color
                var parsedColor = kendo.parseColor(inputValue);
                this.realValue = parsedColor.toCss();
                this.selector.val(this.realValue);
            } catch(ex) {
                this.realValue = null;
            }

            // Create the recent colour palette if one has not been provided
            var recentPaletteColors = this.settings.recentPaletteColors.length != 0 ?
                this.settings.recentPaletteColors : this.settings.standardPaletteColors;

            // If the global recent color has not been set then initialise it with the standard colors
            if(RECENT_SELECTED_COLORS.length == 0) {
                $.each(recentPaletteColors, function() {
                    RECENT_SELECTED_COLORS.push(this);
                });
            }

            // ****************************************
            // Create the actual color selector panel
            // ****************************************

            // Create a temporary color picker
            this.colorPicker = $('<div />', {class: "colorMenuPicker"});

            // We want a click handler that recognises when a click has occurred outside of a handler
            $("html").click(function() {

                // If a click has occurred outside of the color picker then just close the picker
                if(!this.keepPickerOpen) close.call(this, false);
                else this.keepPickerOpen = false;
            }.bind(this));

            // This will be called when the color is changed and will sync all the colours and the underlying text input
            this.colorChange = function(event) {
                if(event.value) {
                    this.colorChosen = true;

                    // Save the current colour value
                    this.realValue = event.value;
                    this.selector.val(this.realValue);

                    // Now set the color picker color
                    this.selectedColorSelector.css("background-color", this.realValue);

                    // Each selector has to be synced with the chosen color
                    if(this.settings.showRecent && (!event.sender || event.sender.element.attr("id") != this.recentColorPalette.attr("id")) && this.recentColorPalette.data("kendoColorPalette")) {
                        this.recentColorPalette.data("kendoColorPalette").value(this.realValue);
                    }
                    if(this.settings.showStandard && (!event.sender || event.sender.element.attr("id") != this.standardColorPalette.attr("id"))&& this.standardColorPalette.data("kendoColorPalette")) {
                        this.standardColorPalette.data("kendoColorPalette").value(this.realValue);
                    }
                    if(this.settings.showCustom && (!event.sender || event.sender.element.attr("id") != this.customColorPalette.attr("id"))&& this.customColorPalette.data("kendoFlatColorPicker")) {
                        this.customColorPalette.data("kendoFlatColorPicker").value(this.realValue);
                    }

                    // Execute the change callback
                    this.settings.change.call(this);
                }
            }.bind(this);

            this.colorChangeAndClose = function(event) {
                this.colorChange(event);

                if (!this.keepPickerOpen) {
                    close.call(this, false);
                }
                else {
                    this.keepPickerOpen = false;
                }
            }.bind(this);

            // ***********************************
            // Create the actual proxy element
            // ***********************************

            // Create a span that will allow us to contain the content more easily
            this.proxyWrapper = $('<span />', {
                class: "k-widget k-colorpicker k-header"
            });

            // We need to create a new element to replace the 'this' and we will proxy
            // the result to the original element
            this.proxyElement = $('<span />', {
                id: "proxy-" + this.selector.attr("id"),
                class: "k-picker-wrap k-state-default"
            });

            // Add the hover event
            this.addHoverHandler = function() {
                this.proxyElement.hover(function() {
                    $(this).addClass("k-state-hover");
                }, function() {
                    $(this).removeClass("k-state-hover");
                });
            };

            // Remove the hover event
            this.removeHoverHandler = function() {
                this.proxyElement.off("mouseenter mouseleave");
            };
            this.addHoverHandler();

            // This is the element that will display the current color
            this.selectedColorSelector = $('<span />', {
                id: "proxy-" + this.selector.attr("id") + "-selected-color",
                style: "background-color:" + this.realValue + ";",
                class: "k-selected-color"
            });
            this.proxyElement.append(this.selectedColorSelector);

            // Create the drop down element
            this.dropDownSelector = $('<span />', {
                class: "k-select"
            }).append($('<span />', {
                class: "k-icon k-i-arrow-s"
            }));
            this.proxyElement.append(this.dropDownSelector);
            this.proxyWrapper.append(this.proxyElement);

            // Place the selector and the button before the current text box
            this.selector.before(this.colorPicker);
            this.selector.before(this.proxyWrapper);

            // We now want to move this inside of our first
            this.proxyWrapper.append(this.selector);
            this.selector.hide();

            // Add the click handler
            this.clickHandler = function(event) {

                // When the button is clicked we want to slide in the widget
                if(this.colorPicker.is(":visible")) {
                    close.call(this, false);
                }
                else {
                    open.call(this, false);
                }

                // Stop this event from propogating
                event.stopPropagation();
            }.bind(this);
            this.dropDownSelector.on("click", this.clickHandler);
        }

        /**
         * A new column div will be created with the header and the selector as the content
         */
        function createColumn(selector, tabId, active) {
            return $('<div />', {
                        role: 'tabpanel',
                        class: 'tab-pane ' + (active ? active : ''),
                        id: tabId
                    }).append(
                        $('<div />', {
                            class: "column"
                        }).append(selector)
                    );
        }

        /**
         * This will create the custom color picker component
         */
        function createColorPicker() {

            // Create the color picker selector
            this.colorPicker = $('<div />', {
                id: "colorpicker" + this.selector.attr("id"),
                class: "colorMenuPicker",
                style: "z-index: 10004;"
            });

            // This is the tab panel content that all the tabs are contained within
            this.tabPanel = $('<div />',{
                role: 'tabpanel'
            });

            // Add the tab panel to the color picker
            this.colorPicker.append(this.tabPanel);

            // Create the column headers
            this.tabPanel.append(
                $('<ul />',{
                    class: 'nav nav-tabs',
                    role: 'tablist'
                }).append(
                    $('<li />',{
                        role: 'presentation',
                        class: 'active'
                    }).append(
                        $('<a />',{
                            href: '#recent-tab-' + this.selector.attr("id"),
                            'aria-controls': 'recent-tab-' + this.selector.attr("id"),
                            role: 'tab',
                            'data-toggle': 'tab',
                            text: this.settings.i18n.recentColorTitle,
                            click: function(e) {
                                e.preventDefault();
                                $(this).tab('show');
                            }
                        })
                    )
                ).append(
                    $('<li />',{
                        role: 'presentation'
                    }).append(
                        $('<a />',{
                            href: '#common-tab-' + this.selector.attr("id"),
                            'aria-controls': 'common-tab-' + this.selector.attr("id"),
                            role: 'tab',
                            'data-toggle': 'tab',
                            text: this.settings.i18n.standardColorTitle,
                            click: function(e) {
                                e.preventDefault();
                                $(this).tab('show');
                            }
                        })
                    )
                ).append(
                    $('<li />',{
                        role: 'presentation'
                    }).append(
                        $('<a />',{
                            href: '#custom-tab-' + this.selector.attr("id"),
                            'aria-controls': 'custom-tab-' + this.selector.attr("id"),
                            role: 'tab',
                            'data-toggle': 'tab',
                            text: this.settings.i18n.customColorTitle,
                            click: function(e) {
                                e.preventDefault();
                                $(this).tab('show');
                            }
                        })
                    )
                ));

            // The tab content is where the kendo color pickers will be added (as tab panes)
            this.tabContent = $('<div />', {
                class: 'tab-content'
            });

            // Add the tab content to the color picker
            this.tabPanel.append(this.tabContent);

            // This will stop the clicks going beyond the main color picker
            this.colorPicker.click(function(event) {

                // Stop this event from propagating
                event.stopPropagation();
            });

            // If we should add the recent color palette
            if(this.settings.showRecent) {

                // Create a Div for the recently used palette
                this.recentColorPalette = $('<div />', {
                    id: "recent-palette-" + this.selector.attr("id")
                });

                // Create a column for the recent colors
                this.tabContent.append(createColumn(this.recentColorPalette, 'recent-tab-' + this.selector.attr("id"), 'active'));
            }

            // If we should add the standard color palette
            if(this.settings.showStandard) {

                // Create a Div for the standard color palette
                this.standardColorPalette = $('<div />', {
                    id: "standard-palette-" + this.selector.attr("id")
                });

                // Create a column for the standard colors
                this.tabContent.append(createColumn(this.standardColorPalette, 'common-tab-' + this.selector.attr("id")));
            }

            // If we should add the custom color palette
            if(this.settings.showCustom) {

                // Create a new input for the custom color picker
                this.customColorPalette = $('<div />', {
                    id: "custom-palette-" + this.selector.attr("id")
                });

                // Create a column for the custom color
                this.tabContent.append(createColumn(this.customColorPalette, 'custom-tab-' + this.selector.attr("id")));
            }
        }

        /**
         * Because you cannot update the colors in an existing kendo color picker (well if you can I can't seem to find it)
         * we have to recreate the color picker to use the new colors
         */
        function recreateColorPicker() {

            // If the current colour picker exists exists then we need to remove it
            var previousColorPicker = this.colorPicker;

            // Then recreate it again
            createColorPicker.call(this);

            // Add it before the current picker and then remove the previous one
            previousColorPicker.before(this.colorPicker);
            previousColorPicker.remove();

            // Find the position of the element we are bound to as we will place the drop down
            // relative to this position
            var offset = this.proxyWrapper.position();

            // Get the actual height of the element
            var height = this.proxyWrapper.outerHeight();

            // Set the position of the picker
            this.colorPicker.offset({top: offset.top+height, left: offset.left});
        }

        /**
         * This will bind the kendo selectors to the specific items
         */
        function bindKendo() {

            // Initialise the kendo pickers if they have not already been set
            if(this.settings.showRecent && !this.recentColorPalette.data("kendoColorPalette")) {

                // Use the kendo pallete to create a color selector with the common colors
                this.recentColorPalette.kendoColorPalette({
                    columns: this.settings.columns,
                    tileSize: this.settings.tileSize,
                    palette: RECENT_SELECTED_COLORS,
                    value: this.realValue,
                    change: this.colorChangeAndClose
                });
            }

            // Use the kendo pallete to create a color selector with the common colors
            if(this.settings.showStandard && !this.standardColorPalette.data("kendoColorPalette")) {
                this.standardColorPalette.kendoColorPalette({
                    columns: this.settings.columns,
                    tileSize: this.settings.tileSize,
                    palette: this.settings.standardPaletteColors,
                    value: this.realValue,
                    change: this.colorChangeAndClose
                });
            }

            // Bind the custom color selector
            if(this.settings.showCustom && !this.customColorPalette.data("kendoFlatColorPicker")) {
                this.customColorPalette.kendoFlatColorPicker({
                    preview: true,
                    value: this.realValue,
                    change: this.colorChange
                });
            }
        }

        /**
         * If the item is active (has been opened) it will set the correct classes on the item
         *
         * @param active true if this item is active
         */
        function isActive(active) {
            if(active) {
                this.proxyWrapper.addClass("k-state-border-down");
                this.proxyElement.addClass("k-state-active k-state-border-down k-state-focused");
            }
            else {
                this.proxyWrapper.removeClass("k-state-border-down");
                this.proxyElement.removeClass("k-state-active k-state-border-down k-state-focused");
            }
        }

        /**
         * This will open the colour picker window
         *
         * @param keepPickerOpen true if the menu should be kept open when a click occurs outside of the picker
         */
        function open(keepPickerOpen) {
            if(!this.colorPicker.is(":visible")) {
                this.keepPickerOpen = keepPickerOpen;

                // We need to dispose of the current picker
                recreateColorPicker.call(this);

                // Open the slider
                this.colorPicker.slideDown("fast");

                // Bind the kendo elements
                bindKendo.call(this);

                // The item is active
                isActive.call(this, true);

                // Call the on open handler
                this.settings.onOpen.call(this);
            }
        }

        /**
         * This will close the colour picker window
         *
         * @param keepPickerOpen true if the menu should be kept open when a click occurs outside of the picker
         */
        function close(keepPickerOpen) {
            if(this.colorPicker.is(":visible")) {
                this.keepPickerOpen = keepPickerOpen;

                // Close the slider
                this.colorPicker.slideUp("fast");

                // The item is no longer active
                isActive.call(this, false);

                // Call the on open handler
                this.settings.onClose.call(this);

                // Add the color selected to the recent color list if a color has been selected (i.e not just opened and closed)
                if(this.colorChosen) {
                    addColorToRecents.call(this, this.realValue);
                    this.colorChosen = false;
                }
            }
        }

        /**
         * This will update the recent color list by putting the recent selected color to
         * the beginning of the list
         *
         * @param color the color to add to the recently used list
         */
        function addColorToRecents(color) {

            // Is the color already in the array?
            var position = function() {
                for(var i = 0; i < RECENT_SELECTED_COLORS.length; i++) {
                    if(color == RECENT_SELECTED_COLORS[i]) {
                        return i;
                    }
                }
                return -1;
            }();
            if(position != -1) {

                // Remove the item from the array
                RECENT_SELECTED_COLORS.splice(position, 1);
            }

            // If the array is larger than the standard color palette (keep it clean) remove the last color
            if(RECENT_SELECTED_COLORS.length == this.settings.standardPaletteColors.length)
                RECENT_SELECTED_COLORS.pop();

            // Now push on the latest color
            RECENT_SELECTED_COLORS.unshift(color);
        }

        /**
         * Public available functions
         *
         * @type {{enable: enable, open: open, close: close, value: value, recentColors: recentColors}}
         */
        CustomColorPicker.prototype = {

            /**
             * Will enable/disable the picker selector
             *
             * @param enable true if to enable or false to disable (by default it will attempt to enable)
             */
            enable: function(enable) {

                // Basically disable the main wrapper
                enable = typeof enable === "undefined" || enable;
                if(!enable && !this.proxyElement.hasClass("k-state-disabled")) {
                    this.close();
                    this.dropDownSelector.off("click", this.clickHandler);
                    this.proxyElement.addClass("k-state-disabled");
                    this.removeHoverHandler();
                }
                else if(enable && this.proxyElement.hasClass("k-state-disabled")) {
                    this.dropDownSelector.on("click", this.clickHandler);
                    this.proxyElement.removeClass("k-state-disabled");
                    this.addHoverHandler();
                }
            },

            /**
             * This will open the color picker window (if it is closed)
             */
            open: function() {
                open.call(this, true);
            },

            /**
             * This will close the color picker window (if it is open)
             */
            close: function() {
                close.call(this, true);
            },

            /**
             * Will either return the value (if no parameter is provided) or will set the color to that provided.
             *
             * @param color The color to set for this widget
             * @returns {*} The current color in CSS format E.g. #ffffff
             */
            value: function(color) {
                if(color) {
                    try {
                        // Try to parse the color
                        color = kendo.parseColor(color);

                        // Fire the color change event
                        this.colorChange({
                            value: color.toCss()
                        });
                    } catch(ex) {
                        alert('Cannot parse color: "' + color + '"');
                    }
                }
                return this.realValue;
            },

            /**
             * This will return the list of current used colors
             *
             * @returns {Array} The array of the last used colors
             */
            recentColors: function() {

                // Return the recent colour list
                return RECENT_SELECTED_COLORS;
            }
        };

        // Return the classes under the namespace
        return {
            CustomColorPicker: CustomColorPicker
        };
    })();

    // Add the style rules
    (function() {
        var stylesheet = document.styleSheets[0],
            selectors = [
                "div.colorMenuPicker",
                "div.column",
                "div.column h3.title",
                "div.colorMenuPicker .k-flatcolorpicker",
                "div.colorMenuPicker ul li:first-child a",
                "div.colorMenuPicker ul li:last-child a",
                "div.colorMenuPicker ul li a"
            ],
            rules = [
            "{" +
                "position: absolute;" +
                "display: none;" +
                "border: 1px solid;" +
                "border-color: #dbdbde;" +
                "background-color: #fff;" +
            "}",
            "{" +
                "width: 100%;" +
                "text-align: center;" +
                "padding: 10px;" +
            "}",
            "{" +
                "color: #515967;" +
                "text-transform: uppercase;" +
                "font-size: 11px;" +
                "font-weight: normal;" +
            "}",
            "{" +
                "width: 100%;" +
            "}",
            "{" +
                "border-left: none !important;" +
            "}",
            "{" +
                "border-right: none !important;" +
            "}",
            "{" +
                "border-top: none !important;" +
                "border-radius: 0;" +
                "margin-right: 0;" +
            "}"];

        // Add the rule based on what function is available
        if (stylesheet) {
            for (var i = 0; i < selectors.length; i++) {
                if (stylesheet.insertRule) {
                    stylesheet.insertRule(selectors[i] + rules[i], stylesheet.cssRules.length);
                } else if (stylesheet.addRule) {
                    stylesheet.addRule(selectors[i], rules[i], -1);
                }
            }
        }
    })();

    // Add the new colour picker function to bind our own colour picker to the item selected
    $.fn.nrmmColorPicker = function(options) {

        // We will need configuration for the color picker
        var settings = $.extend({

            // The color to use if there is no color within the input already
            value: "#ffffff",

            // By default we will use 8 columns
            columns: 8,

            // The tile size will be 30x30px
            tileSize: {
                width: 40,
                height: 40
            },

            // What to show within the widget
            showRecent: true,
            showStandard: true,
            showCustom: true,

            // We define an empty anonymous function so that
            // we don't need to check its existence before calling it.
            onClose: function() {},
            onOpen: function() {},
            change: function() {},

            // These colors are the recently used ones.
            // By default if nothing is provided it will start with the standard palette colors
            recentPaletteColors: [],

            // These colors are for the normal palette range
            standardPaletteColors: ["#000000","#7f7f7f","#880015","#ed1c24","#ff7f27","#fff200","#22b14c","#00a2e8","#3f48cc","#a349a4","#ffffff","#c3c3c3","#b97a57","#ffaec9","#ffc90e","#efe4b0","#b5e61d","#99d9ea","#7092be","#c8bfe7","#9296d7","#f2c76f","#085d82","#e9d8f5"],

            // The internationalisation values
            i18n: {
                apply: "Apply",
                cancel: "Cancel",
                recentColorTitle: "Recently Used Colors",
                standardColorTitle: "Common Colors",
                customColorTitle: "Select Custom Color"
            }
        }, options);

        // We need the kendo color picker for this widget to work
        var kendoColorPalette = $.fn.kendoColorPalette;

        // Make sure that this item is a text input type
        if (kendoColorPalette && this.is("[type=text]")) {

            // Create a new color picker for 'this' item
            var customColorPicker = new ccp.CustomColorPicker(this, settings);

            // Add this widget to the data field
            this.data("nrmmColorPicker", customColorPicker);
            return customColorPicker;
        }

        // Then we cannot do anything with this element so return false
        return false;
    };
}(jQuery));


// Adds a new multiple email input box
(function ($) {

    $.fn.multipleEmail = function () {

        return this.each(function () {

            // internal variable to store the currentValue
            this.valueList = [];

            // store the jquery object where this behaviour is being added to
            this.orig = $(this);

            // list of email addresses as unordered list
            this.emailList = $('<ul />');

            // container div
            this.container = $('<div class="multipleInput-container k-input k-textbox" />');

            //Add click handler to th focus the input box
            this.container.click(function () {
                this.input.focus();
            }.bind(this));

            // handler to remove a box indicator from the list of emails by clicking the X.
            // This handler will be added to the X in each box
            this.removeEmailBox = function (e) {
                var emailBox = $(e.target).parent();
                this.removeEmailObj(emailBox);
                e.preventDefault();

            }.bind(this);

            /**
             * Removes an email box
             * @param objToRemove List Item Box
             * @returns {*}
             */
            this.removeEmailObj = function (objToRemove) {
                var boxValue = null;

                if(objToRemove) {
                    boxValue = objToRemove.find("span").first().html();
                    objToRemove.remove();


                    // Remove from the internal value array
                    var i = this.valueList.indexOf(boxValue);
                    if (i > -1) {
                        this.valueList.splice(i, 1);
                    }

                    // Update the value of the origin input
                    this.orig.val(this.getStringValue());
                }
                return boxValue;
            }.bind(this);

            // handler to edit an email address by clicking on the list item
            this.editEmail = function (e) {
                var emailBox = $(e.target).parent();
                e.preventDefault();

                var boxValue = this.removeEmailObj(emailBox);

                this.input.val(boxValue ? boxValue : "");

            }.bind(this);

            // Function to add a new email value
            this.addEmailValue = function (val) {

                if (!val || val == "") return;

                // validate the email address if type is email
                var isValidEmail = PivotalUtils.isEmail(val)

                var newListItem = $('<li class="multipleInput-email' + (isValidEmail ? '' : ' invalid-email') + '"><span>' + val + '</span></li>');
                // Add the remove X
                newListItem.append($('<a href="#" class="multipleInput-close" title="Remove" />').click(this.removeEmailBox));
                // append to list of emails with remove button
                this.emailList.append( newListItem );

                newListItem.click(this.editEmail);

                // Empty the placeholder
                this.input.attr('placeholder', '');

                // Empty the input after the the current value has been added
                this.input.val('');

                // Add to the values array
                this.valueList.push(val);

                // Update the value of the origin input
                this.orig.val(this.getStringValue());

            }.bind(this);

            // Function to get the array list of email values
            this.getValue = function () {
                return this.valueList;

            }

            // Function to get the comma separated list of email values
            this.getStringValue = function () {
                return this.valueList.join(",");
            }

            // Function handler to the key up on the input box. This will look for the characters that split emails like ; , or enters and add a new entry to the email list
            this.characterInputHandler = function (event) {

                var is_firefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
                if (event.which == 13 || event.which == 188 || (is_firefox && event.which == 59) || (!is_firefox && event.which == 186)) {

                    var val = $(event.target).val();
                    // key press is semicolon or comma
                    if (event.which != 13) {
                        val = val.slice(0, -1); // comma/semicolon from value
                    }

                    this.addEmailValue(val);
                }

            }.bind(this);

            // input
            this.input = $('<input type="text"/>');
            this.input.keyup(this.characterInputHandler);
            this.input.focusout(function () {
                this.addEmailValue(this.input.val())
            }.bind(this))

            // insert elements into DOM
            this.container.append(this.emailList).append(this.input).insertAfter($(this));

            // set the inital value to match whatever the value of the input is.
            var tmpValue = $(this).val();
            var startupList = tmpValue ? tmpValue.split(",") : [];
            // Add the indicatior boxes for the init values
            for (var currIdx in startupList) {
                this.addEmailValue(startupList[currIdx]);
            }

            //Add the get value functions to the data of the transformed input
            $(this).data("multipleEmails", {
                getStringValue: this.getStringValue.bind(this),
                getValue: this.getValue.bind(this)
            })

            return $(this).hide();
        });
    };
})(jQuery);
