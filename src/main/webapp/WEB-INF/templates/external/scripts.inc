<!-- ************************ -->
<!-- Start of Pivotal Scripts -->
<!-- ************************ -->
#set($CleanAppVersion = $Utils.encodeURL("$!APPLICATION_VERSION"))
##
## Standard CDN libraries
##
<script src="https://code.jquery.com/jquery-3.4.1.min.js" integrity="sha256-CSXorXvZcTkaix6Yvo6HppcZGetbYMGWSFlBw8HfCJo=" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.bundle.min.js"></script>
<script src="https://kit.fontawesome.com/0bf971c943.js" crossorigin="anonymous"></script>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" />
##
## Kendo libraries
##
#set($LocaleString = $Internationalisation.getLocaleString())
#if(!$LocaleString)
    #set($LocaleString = "en-GB");
#end
<script src="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/js/kendo.all.min.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/js/cultures/kendo.culture.${LocaleString}.min.js?$CleanAppVersion" ></script>
<script>
    kendo.culture("$!LocaleString")
</script>
##
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.common-bootstrap.min.css?$CleanAppVersion" />
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.bootstrap.min.css?$CleanAppVersion" />
<link rel="stylesheet" href="$AppPath/javascript/kendo/v2017.2.504/kendo-ui/styles/kendo.bootstrap.mobile.min.css?$CleanAppVersion" />
<link rel="stylesheet" href="$AppPath/external/gla.css?$CleanAppVersion" />
##
## Pivotal Modules
##
<script src="$AppPath/javascript/pivotalutils.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/psApp.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/logger.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/workflow.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/postcode.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/dateutils.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/pattern.js?$CleanAppVersion" ></script>
<script src="$AppPath/javascript/modules/login.js?$CleanAppVersion" ></script>
##
<!-- ********************** -->
<!-- End of Pivotal Scripts -->
<!-- ********************** -->
##
