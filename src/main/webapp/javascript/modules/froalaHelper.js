/**
 * Froala Helper
 * Provides standard methods that are sued with the froala editor
 *
 */
// We need to create the correct namespace for this item
if (typeof psApp != 'undefined') {

    // Then the psApp namespace has been defined
    psApp.namespace("froalaHelper");
    psApp.froalaHelper = (function () {
        var logger = psApp.logger.getLogger("psApp.froalaHelper", null);

        var fieldName = "";
        var editorId = "";
        var getParagraphTypes = function() {
            return {
                    numberedParagraph1: 'Number Paragraph Level 1'
                    ,numberedParagraph2: 'Number Paragraph Level 2'
                    ,numberedParagraph3: 'Number Paragraph Level 3'
                    }
        };

        var paragraphNumbersInit = function (newEditorId, newFieldName) {
            fieldName = newFieldName;
            editorId = newEditorId;
            $(newEditorId).closest("div").addClass("counterReset");
            $(".numberedParagraph").remove();
       };

       var paragraphNumbersBuild = function () {

            // Load in the result
            var level1 = 0;
            var level2 = 0;
            var level3 = 0;
            var sep = ".";
            $('[class^="numberedParagraph"]').each(function (index) {
                var newText = "";
                var className = $(this).attr("class");

                if (className == "numberedParagraph1") {
                    level1 += 1;
                    level2 = 0;
                    level3 = 0;
                    newText = level1 + sep;
                }
                else if (className == "numberedParagraph2") {
                    level2 += 1;
                    level3 = 0;
                    newText = level1 + sep + level2 + sep;
                }
                else if (className == "numberedParagraph3") {
                    level3 += 1;
                    newText = level1 + sep + level2 + sep + level3 + sep;
                }
                $(this).prepend("<span class=\"numberedParagraph\">" + newText + "&nbsp;</span>");
            });


           if (fieldName && editorId)
               $(fieldName).val($(editorId).froalaEditor('html.get', true));

        };

        var finish = function() {

            // See if we need to swap back to normal mode
            if ($(editorId).froalaEditor('codeView.isActive'))
                $(editorId).froalaEditor('codeView.toggle');

            paragraphNumbersBuild();
        };

        var initialise = function(elementSelector) {
            // Do a select all and clear just to try and sort out problem in ie
            // Problem is when edit a report, close popup.  The next popup would not allow editing
            // something to do with contenteditable attribute on the div not being recognized.
            // a manual control-a on the form would allow editing so this is what is happening
            // via the code
            try {

                var thisElement = $(elementSelector)[0];
                if (document.selection) {
                    var range = document.body.createTextRange();
                    range.moveToElementText(thisElement);
                    range.select();
                } else if (window.getSelection) {
                    var range = document.createRange();
                    range.selectNode(thisElement);
                    window.getSelection().removeAllRanges();
                    window.getSelection().addRange(range);
                }
                var sel = window.getSelection ? window.getSelection() : document.selection;
                if (sel) {
                    if (sel.removeAllRanges) {
                        sel.removeAllRanges();
                    } else if (sel.empty) {
                        sel.empty();
                    }
                }
            }
            catch(X){}
        };

        return {
                getParagraphTypes : getParagraphTypes,
                paragraphNumbersBuild : paragraphNumbersBuild,
                paragraphNumbersInit : paragraphNumbersInit,
                initialise : initialise,
                finish : finish
        }
    })();
}
