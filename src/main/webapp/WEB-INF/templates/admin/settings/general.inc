#parse("$PageName/$SubPageName/macros.inc")
<div class="body-padding">

    #macroTabButtons([
        "admin.settings.general.application.title",
        "admin.settings.general.scheduling.title",
        "admin.settings.general.cache.title",
        "admin.settings.general.theme.title",
        "admin.settings.general.email.title"
        ])

    #macroStartForm("settings" "$AppPath/$PageName/$SubPageName")
        #macroStartTabs()

            #macroStartTab("admin.settings.general.application.title", true)

                #macroStartFieldset("system.setting.app.general.title")
                    #macroShowSettingsInput("n" ,$SETTING_APP_GENERAL_AUTOSAVE_PERIOD)
                    #macroShowSettingsInput("s" ,$SETTING_APP_GENERAL_FILE_UPLOAD_TYPES)
                    #macroShowSettingsInput("n" ,$SETTING_APP_GENERAL_FILE_UPLOAD_MAXSIZE)
                    #macroShowSettingsInput("s" ,$SETTING_APP_GENERAL_TAN_GUIDE_LINK)
                #macroEndFieldset()

            #macroEndTab()

            #macroStartTab("admin.settings.general.scheduling.title")
                #macroStartFieldset("admin.settings.general.scheduling.title")
                    #macroShowSettingsInput("c" ,$SETTING_SCHEDULING_ENABLED)
                    #macroInputHidden("_$SETTING_SCHEDULING_ENABLED" "")
                    #macroShowSettingsInput("c" ,$SETTING_PUBLISHING_ENABLED)
                    #macroInputHidden("_$SETTING_PUBLISHING_ENABLED" "")
                    #macroShowSettingsInput("s" ,$SETTING_PUBLISHING_SERVER)
                    #macroShowSettingsInput("s" ,$SETTING_ERROR_EMAIL)
                    #macroShowSettingsInput("s" ,$SETTING_DEFAULT_EMAIL_PUBLISHER_ADDRESS)
                #macroEndFieldset()
            #macroEndTab()

        #macroStartTab("admin.settings.general.cache.title")
            #macroStartFieldset("admin.settings.general.cache.title")
                #macroShowSettingsInput("r" ,$SETTING_CACHE_ENGINE, $SETTING_CACHE_ENGINE, [{"value":"ehcache","text":"EHcache"},{"value":"memcached","text":"Memcached"}], {"IsRequired":true})
                #macroShowSettingsInput("n", $SETTING_CACHE_MAX_SIZE)
                #macroShowSettingsInput("s", $SETTING_CACHE_MEMCACHED_SERVERS)
                #macroShowSettingsInput("n", $SETTING_CACHE_EXPIRATION)
            #macroEndFieldset()
            #macroStartFieldset("admin.settings.general.session.title")
                #macroShowSettingsInput("n", $SETTING_SESSION_TIMEOUT)
                #macroShowSettingsInput("n", $SETTING_SESSION_TIMEOUT_WARNING)
                #macroShowSettingsInput("n", $SETTING_SESSION_UPLOAD_FILE_RETENTION)
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.general.theme.title")
            #macroStartFieldset("admin.settings.general.theme.title")
                <div id="themes"></div>
                #macroButtonAction("upload" "admin.settings.theme.action.upload" {"onClick":"ThemeActions.launchThemeUpload"})
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.general.email.title")
            #macroStartFieldset("admin.settings.general.email.title")
                #macroShowSettingsInput("s", $SETTING_EMAIL_SERVER_HOST)
                #macroShowSettingsInput("s", $SETTING_EMAIL_SERVER_USERNAME)
                #macroShowSettingsInput("p", $SETTING_EMAIL_SERVER_PASSWORD)
                #macroShowSettingsInput("s", $SETTING_EMAIL_SERVER_PORT)
                #macroShowSettingsInput("c", $SETTING_EMAIL_SERVER_SSL)
                #macroInputHidden("_$SETTING_EMAIL_SERVER_SSL" "")
                #macroShowSettingsInput("c", $SETTING_EMAIL_SERVER_DEBUG)
                #macroInputHidden("_$SETTING_EMAIL_SERVER_DEBUG" "")
                #macroShowSettingsInput("s", $SETTING_EMAIL_SERVER_FROM)
            #macroEndFieldset()
        #macroEndTab()

        #macroEndTabs()

        <div class="button-wrap">
            #macroButtonSubmit("submit" "global.button.save")
        </div>

    #macroEndForm({"saveAndCloseButton":{}})
</div>

<div id="hiddenRefreshHolidaysButton">
    <button type="button" id="RefreshHolidaysButton" class="btn btn-primary" tooltip="#I18N("system.setting.app.holiday_refresh.description")">
        <i class="fa fa-refresh"></i> #I18N("system.setting.app.holiday_refresh")
    </button>
</div>

<script>
    $("#adminClearLog").click(function() {
        if (confirm("#I18N('${SETTING_LOG_MONTHS}.confirm')")) {
            $(this).addClass("icon-looping");
            var results = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/logs/truncate");
            if (results.inError)
                alert(results.error);
            else
                alert("#I18N('${SETTING_LOG_MONTHS}.success')")
            $(this).removeClass("icon-looping");
        }
        return false;
    });

    $(document).ready(function () {
        generalSettings.loadButton("RefreshHolidays", generalSettings.loadHolidays);
    });

    var generalSettings  = {
        loadButton: function(buttonName, clickFunction) {
                $("#visible" + buttonName + "Button").html($("#hidden" + buttonName + "Button").html());
                $("#hidden" + buttonName + "Button").html("");

                if (clickFunction) {
                    $("#" + buttonName + "Button").bind("click", function (e) {
                        clickFunction();
                        return false;
                    });
                }
            },

            loadHolidays: function() {

                var result = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/loadholidays");
                if (result.inError)
                    alert(result.error);
                else
                    $("#system\\.setting\\.app\\.holidays").val(result.information);

            }
    };
    var ThemeActions = {

        /**
         * Called to show the edit dialog
         */
        launchThemeUpload : function(event, theme) {
            psApp.windowManager.open({title:'#I18N("admin.theme.upload.dialog.title")', height:500, width:750, content:"$AppPath/$PageName/theme/edit" + (theme?"?name=" + theme:""), useIFrame:true, closeFunction:ThemeActions.closeDialog});
            return false;
        },

        /**
         * Called after the dialog is closed
         * @param cancelled True if the dialog was cancelled
         */
        closeDialog : function(cancelled) {
            if (!cancelled) {
                ThemeActions.refreshList();
            }
        },

        refreshList: function() {
            $('#themes').html(PivotalUtils.getContent("$AppPath/$PageName/theme/list"));
            PivotalUtils.showFormTooltips("#themes");

            $(".delete-theme").click(function() {
                var theme = $(this).attr("theme");
                if (confirm("#I18N("admin.theme.upload.dialog.confirm")" + theme + "?\n")) {
                    PivotalUtils.getJsonContent("$AppPath/$PageName/theme/remove", "name=" + theme);
                    ThemeActions.refreshList();
                }
            });

            $(".edit-theme").click(function() {
                var theme = $(this).attr("theme");
                ThemeActions.launchThemeUpload(null, theme);
            });

            $(".set-theme").click(function() {
                var theme = $(this).attr("theme");
                PivotalUtils.getJsonContent("$AppPath/$PageName/theme/set", "name=" + theme);
                document.location.reload();
            });
        }
    }

    $(document).ready(ThemeActions.refreshList);

</script>

## Set the heartbeat to run faster for these pages

#set ($HEARTBEAT_PERIOD_MILLISECONDS = 10000)
