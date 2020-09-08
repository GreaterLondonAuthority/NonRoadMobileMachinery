<div class="footer"></div>

## If we're logged then enable the heartbeat and show any notifications
#if ("$!CurrentUser"!="")
    <script type="application/javascript">
        $(document).ready(function() {
            PivotalUtils.startHeartbeat("$AppPath", "$AppPath/$PageName/$SubPageName/$ActionPageName", $HEARTBEAT_PERIOD_MILLISECONDS, "$!FooterProperties.HeartbeatExtra");
        });
    </script>

    #macroShowNotifications()
#end

</body>
</html>
