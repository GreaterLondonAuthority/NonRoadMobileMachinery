#macroImage("$AppPath/themes/$Theme/imgs/logo.png" "Ver: $APPLICATION_VERSION ($AppDate)", "logo")

<div class="global-header">
	<div class="links">

            #if ($CurrentUser.hasAccess($PRIVILEGES_HAS_ADMIN_ACCESS) || $CurrentUser.hasAccess($PRIVILEGES_PERFORMANCE_ADMIN))
                <div#if($Utils.doStringsMatch("dashboard", "$PageName")) class="active"#end><a tooltip="#I18N("mainnav.home")" href="$AppPath/dashboard"><i class="fa fa-home"></i></a></div>
            #end

            #if ($CurrentUser.hasAccess($PRIVILEGES_HAS_ADMIN_ACCESS))
                <div#if($Utils.doStringsMatch("admin", "$PageName")) class="active"#end><a tooltip="#I18N("mainnav.admin")" href="$AppPath/admin"><i class="fa fa-cog"></i></a></div>
            #end

            #if ($CurrentUser.hasAccess($PRIVILEGES_PERFORMANCE_ADMIN))
                <div#if($Utils.doStringsMatch("status", "$PageName")) class="active"#end><a tooltip="#I18N("mainnav.performance")" href="$AppPath/status/jvm"><i class="fa fa-tachometer-alt"></i></a></div>
            #end

	</div>
    <script>
        PivotalUtils.showFormTooltips(".links", "bottom")
    </script>
</div>
