#set ($ALL = "$!GlobalIncludes"=="")

## gridster and dashboards

#if($ALL || $GlobalIncludes.contains($INC.GRIDSTER))
<link rel="stylesheet" href="$AppPath/javascript/gridster/jquery.gridster.min.css" />
<style>
/*modification for dashboard resizer*/
    .gs-resize-handle-both {
        right: 20px;
        bottom:4px;
    }

</style>
<link rel="stylesheet" href="$AppPath/javascript/gridster/css/ionicons.min.css"/>
#end
## gridster and dashboards
#if($ALL || $GlobalIncludes.contains($INC.GRIDSTACK))
<link rel="stylesheet" href="$AppPath/javascript/gridstack/gridstack.css" />
#end

## colorbox styles

#if($ALL || $GlobalIncludes.contains($INC.COLORBOX))
<link rel="stylesheet" href="$AppPath/javascript/colorbox/colorbox.css" />
#end

## kendo

#if($ALL || $GlobalIncludes.contains($INC.KENDO))
##PMW
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.common-bootstrap.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.bootstrap.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.bootstrap.mobile.min.css" />
#end

## kendo labs stuff

#if($ALL || $GlobalIncludes.contains($INC.KENDO_LABS))
##<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui-plugins/kendo.web.plugins.css" />
#end

## kendo chart's CSS

#if($ALL || $GlobalIncludes.contains($INC.KENDO_CHARTS))
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.dataviz.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.dataviz.default.min.css" />
#end

## Codemirror editor

#if($ALL || $GlobalIncludes.contains($INC.CODEMIRROR))
<link rel="stylesheet" href="$AppPath/javascript/codemirror/codemirror-5.15.2/lib/codemirror.css" />
<link rel="stylesheet" href="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/hint/show-hint.css" />
<link rel="stylesheet" href="$AppPath/javascript/codemirror/codemirror-5.15.2/addon/dialog/dialog.css" />
<link rel="stylesheet" href="$AppPath/themes/$Theme/style/app-editor.css" />
#end

## Bootstrap

#if($ALL || $GlobalIncludes.contains($INC.BOOTSTRAP))
##<link rel="stylesheet" href="$AppPath/javascript/bootstrap/css/bootstrap.min.css" />
#end

## Always need our fonts and styles

<link rel="stylesheet" href="$AppPath/fonts/OpenSans-Regular/open-sans.css" />
<link rel="stylesheet" href="$AppPath/fonts/OpenSans-Condensed/open-sans-cond.css" />
<link rel="stylesheet" href="$AppPath/fonts/font-awesome-4.7.0/css/font-awesome.min.css" />
<link rel="stylesheet" href="$AppPath/themes/$Theme/style/app.css" />

## Froala Editor
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/froala_editor.min.css"/>
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/froala_style.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/table.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/colors.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/fullscreen.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/image.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/image_manager.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/code_view.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/char_counter.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/draggable.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/emoticons.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/file.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/line_breaker.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/quick_insert.min.css" />
<link rel="stylesheet" href="$AppPath/javascript/froala_editor/froala_editor_2.3.0/css/plugins/video.min.css" />

<!--[if IE]>
<link rel="stylesheet" href="$AppPath/themes/$Theme/style/ie.css" />
<![endif]-->


## Allow the page to define some other custom CSS (useful for plugins)
#if("$!ExtraCSSPaths" != "")
    #foreach($CSSPath in $ExtraCSSPaths)
        <link rel="stylesheet" href="$CSSPath" />
    #end
#end
<style>

    /* matches the custom action name */
    .k-i-external-link
    {
        background-color: inherit;
        background-image: url("$AppPath/themes/$Theme/imgs/external-link.png");
    }

</style>
