<!-- *********************** -->
<!-- End of Pivotal Template -->
<!-- *********************** -->
##
#if ("$!CurrentUser"!="")
    <script type="application/javascript">
        $(document).ready(function() {
            var extra = "$!FooterProperties.HeartbeatExtra";
            PivotalUtils.startHeartbeat("$AppPath", "$AppPath/$PageName/$SubPageName/$ActionPageName", $HEARTBEAT_PERIOD_MILLISECONDS, extra);
        });
    </script>
    <div class="modal fade" id="TimeoutWarning" role="dialog">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h4 class="modal-title">#I18N("system.timeout_warning.title")</h4>
                </div>
                <div class="modal-body">
                    <p>#I18N("system.timeout_warning.message")</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" id="TimeoutButtonYes" data-dismiss="modal" onclick="PivotalUtils.keepSessionAlive('$AppPath')">Yes</button>
                    <button type="button" class="btn btn-default" onclick="location.href='$AppPath/login/logout'">No</button>
                </div>
            </div>
        </div>
    </div>

    #macroShowNotifications()
#end
##
#parse("external/template_footer.inc")
