<!DOCTYPE html>
<html>
<head>
    ## Set viewport for responsive elements
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    ## Set the default character encoding
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    ## Page title
    <title>$APPLICATION_INSTANCE_NAME:
        #if ("$!PageTitle"!="")
		    #I18N("$!PageTitle")
        #end
        #if ("$!SubPageName" != "" && "$!ShowSubTitle" == "" && !$Utils.doStringsMatch($PageName,"simulator"))
            - #I18N("${PageName}.${SubPageName}.title")
        #end
	</title>
    ## CSS files
	#parse("includes/css-files.inc")
	## JS libraries (custom files are called at bottom of page)
	#parse("includes/js-files.inc")

</head>

#set ($minified = $CurrentUser.Preferences.get("admin.user.minified"))

#if ($BodyClass)
<body class="$BodyClass #if ("$!minified" == "true")minified#end #if($REQ_mode == "presentation") presentation-mode#end #if($REQ_stationary == "1") presentation-mode-stationary#end">
#else
<body class="#if ("$!minified" == "true")minified#end #if($REQ_mode == "presentation") presentation-mode#end #if($REQ_stationary == "1") presentation-mode-stationary#end">
#end
