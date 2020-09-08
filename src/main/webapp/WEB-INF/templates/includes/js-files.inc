#set ($ALL = "$!GlobalIncludes"=="")

### Library Files ###

<link rel="icon" type="image/png" href="$AppPath/themes/$Theme/imgs/favicon.ico?v=2"/>

## We always have to have JQuery
<script src="$AppPath/javascript/jquery/jquery-1.11.1.min.js" type="text/javascript"></script>
<script src="$AppPath/javascript/pivotalutils.js" type="text/javascript"></script>
<script src="$AppPath/javascript/common.js" type="text/javascript"></script>
<script src="$AppPath/javascript/custom-plugins.js" type="text/javascript"></script>

## Load in the modules
<script src="$AppPath/javascript/modules/psApp.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/logger.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/popups.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/postcode.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/workflow.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/adminutils.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/autosave.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/froalaHelper.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/login.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/pattern.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/ui.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/promise.js" type="text/javascript"></script>
##<script src="$AppPath/javascript/modules/stackandpop.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/eventmanager.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/windowmanager.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/popups.js" type="text/javascript"></script>
<script src="$AppPath/javascript/modules/dateutils.js" type="text/javascript"></script>

<script>
    // Load in the language file for the common library
    PivotalUtils.i18nBundle = {
        navigateAwayMessage: "#I18N("global.error.unsaved.changes.full")"
        ,autoSaveFoundMessage: "#I18N("autosave.message.foundrecord")"
        ,postcodeUrlEmpty: "#I18N("postcode.lookup.urlempty.error")"
        ,confimButtonDefaultConfirm: "#I18N("global.confirmation.button.default.confirm")"
        ,confimButtonDefaultCancel: "#I18N("global.confirmation.button.default.cancel")"
    }
</script>
<script src="$AppPath/javascript/jquery-knob/jquery.knob.min.js" type="text/javascript"></script>

##<script src="$AppPath/javascript/StickyTableHeaders/jquery.stickytableheaders.js" type="text/javascript"></script>

## Froala Editor
#if($ALL || $GlobalIncludes.contains($INC.FROALA))
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/froala_editor.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/table.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/font_family.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/font_size.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/paragraph_style.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/colors.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/fullscreen.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/inline_style.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/image.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/image_manager.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/align.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/code_beautifier.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/code_view.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/link.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/paragraph_format.min.js" type="text/javascript"></script>
    <script src="$AppPath/javascript/froala_editor/froala_editor_2.3.0/js/plugins/save.min.js" type="text/javascript"></script>

    <script>
        $(document).ready(function() {
            // Set the accesskey
            $.FroalaEditor.DEFAULTS.key = '$froalaAccessKey';
        });
    </script>
#end

#if($ALL || $GlobalIncludes.contains($INC.JQUERY_UI))
<script src="$AppPath/javascript/jquery-ui/jquery-ui-1.11.4.min.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.COLORBOX))
<script src="$AppPath/javascript/colorbox/jquery.colorbox-min.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.KENDO))
<script src="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/js/kendo.all.min.js" type="text/javascript"></script>
##<script src="$AppPath/javascript/kendo/v2017.2.504/kendo-ui-plugins/kendo.web.plugins.js" type="text/javascript"></script>
#end
#if($ALL || $GlobalIncludes.contains($INC.BOOTSTRAP))
##PMW
<script src="$AppPath/javascript/bootstrap/js/bootstrap.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.EASYPIECHART))
<script src="$AppPath/javascript/easy-pie-chart/jquery.easypiechart.min.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.BLOCKLY))
<!-- blockly libraries -->

##<script src="$AppPath/javascript/blockly/blockly_compressed.js" type="text/javascript" ></script>
##<script src="$AppPath/javascript/blockly/blocks_compressed.js" type="text/javascript"></script>
##<script src="$AppPath/javascript/blockly/python_compressed.js" type="text/javascript"></script>
##<script src="$AppPath/javascript/acorn/acorn_interpreter.js" type="text/javascript"></script>
##
##<!-- our own blockly class -->
##<script src="$AppPath/javascript/field_transducerinput.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.MENU))
<script src="$AppPath/javascript/menu.js" type="text/javascript"></script>
#end

#set($LocaleString = $Internationalisation.getLocaleString())
#if(!$LocaleString)
    #set($LocaleString = "en-GB");
#end

#if($ALL || $GlobalIncludes.contains($INC.BLOCKLY))
##<script src="$AppPath/javascript/blockly/msg/js/${LocaleString.substring(0,2)}.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.KENDO))
<script src="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/js/cultures/kendo.culture.${LocaleString}.min.js" type="text/javascript"></script>
<script>
    kendo.culture("$!LocaleString")
</script>
#end

<!-- Clipboard -->
<script src="$AppPath/javascript/clipboard/clipboard.min.js" type="text/javascript"></script>

#### Custom JS Files ####

## Raphael

#if($ALL || $GlobalIncludes.contains($INC.RAPHAEL))
##<script src="$AppPath/javascript/raphael/raphael.js" type="text/javascript"></script>
##<script src="$AppPath/javascript/raphael/raphael.export.js" type="text/javascript"></script>
#end

## Velocity Js
#if($ALL || $GlobalIncludes.contains($INC.VELOCITY))
<script src="$AppPath/javascript/velocity/velocity.min.js" type="text/javascript"></script>
#end

## NRMM gauge - need raphael

#if($ALL || $GlobalIncludes.contains($INC.GAUGE))
##<script src="$AppPath/javascript/justgage/justgage.1.0.1.min.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.THREEJS))
<script src="$AppPath/javascript/three/three.min.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/Detector.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/OrbitControls.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/threex.keyboardstate.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/threex.windowresize.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/THREEx.FullScreen.js" type="text/javascript"></script>
<script src="$AppPath/javascript/three/CSS3DRenderer.js" type="text/javascript"></script>
#end

#if($ALL || $GlobalIncludes.contains($INC.CODEMIRROR))
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/lib/codemirror.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/mode/velocity/velocity.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/mode/properties/properties.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/mode/xml/xml.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/hint/show-hint.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/hint/xml-hint.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/edit/closetag.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/edit/matchbrackets.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/edit/closebrackets.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/dialog/dialog.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/search/searchcursor.js" type="text/javascript"></script>
<script src="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/search/search.js" type="text/javascript"></script>
#end
<script>
    var globalPaths = {
            appPath: "$!AppPath",
            pageName: "$!PageName",
            subPageName:"$!SubPageName",
            actionPageName: "$!ActionPageName",
            subActionPageName:"$!SubActionPageName"
        };
</script>
