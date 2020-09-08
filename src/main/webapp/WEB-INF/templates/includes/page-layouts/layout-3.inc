#macroPageHead($PageTitle)

<div id="pgcontainer">
    ## Parse the full top bar
    #parse("includes/full-global-header.inc")

    <div style="clear:both;"></div>
    <aside>
        <nav>
            #parse("includes/global-header.inc")
            #parse("$LoadPageName/secondary-content.inc")
        </nav>
        <button id="menu-btn" type="button" class="btn btn-primary btn-sm" data-toggle="button"><i class="fa fa-bars"></i></button>
    </aside>

    ## Parse the main body
    #parse("includes/full-global-main.inc")
</div><!-- @end #pgcontainer -->
<script>
    // See if there is a deeplink in the session for processing
    $(document).ready(function() {
        #if($Session.getAttribute("DeepLink"))

            try {
                var deepLink = $!Session.getAttribute("DeepLink");

                if (deepLink.Path[0] == "cases") {

                    var caseId = deepLink.Path.length > 1?deepLink.Path[1]:"";
                    var openTabName = deepLink.Path.length > 2?deepLink.Path[2]:"";
                    psApp.popups.caseEdit("$AppPath", caseId, {"title":"#I18N("case.admin.title")","opentab":openTabName});
                }
            }
            catch(X) {
                alert("#18N('global.error.message.problem_processing_deeplink')");
            }

            ## Clear out deeplink setting so we don't open it again

            #set($tmp = $Session.removeAttribute("DeepLink"))
        #end
    });

</script>
## parse the main footer
#parse("includes/full-global-footer.inc")
