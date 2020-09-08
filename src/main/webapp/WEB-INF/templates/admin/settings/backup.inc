#parse("$PageName/$SubPageName/macros.inc")

<div class="body-padding">

#macroTabButtons([
        "admin.settings.backup.download.title",
        "admin.settings.backup.applytestdata.title",
        "admin.settings.backup.applysettingsdata.title",
        "admin.settings.backup.workflow.title",
        "admin.settings.backup.reload.title",
        "admin.settings.backup.clear.title"
        ])



<span id="backup-settings">

    #macroStartTabs()

        #macroStartTab("admin.settings.backup.download.title", true)
            #macroStartFieldset("admin.settings.backup.download.title")
                <p><strong>#I18N("system.settings.backup.download.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.download.narrative")</p>
                <p>#macroInputSelect("admin.settings.backup.table-list" $NULL $Utils.join($TablesDefault) $Tables {"IsMultiSelect":true})</p>
                <p>#macroInputSwitch("admin.settings.backup.compress")</p>
                <button class="adminSettingsBackup k-button" action_type="download">
                    <span class='fa fa-download'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.download")</span>
                </button>
                <div id="adminSettingsDownloadProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.backup.applytestdata.title")
            #macroStartFieldset("admin.settings.backup.applytestdata.title")
                <p><strong>#I18N("system.settings.backup.applytestdata.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.applytestdata.narrative")</p>
                <button class="adminSettingsBackup k-button" action_type="applytestdata">
                    <span class='fa fa-check'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.applytestdata")</span>
                </button>
                <div id="adminSettingsApplyTestDataProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.backup.applysettingsdata.title")
            #macroStartFieldset("admin.settings.backup.applysettingsdata.title")
                <p><strong>#I18N("system.settings.backup.applysettingsdata.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.applysettingsdata.narrative")</p>
                <button class="adminSettingsBackup k-button" action_type="applysettingsdata">
                    <span class='fa fa-check'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.applysettingsdata")</span>
                </button>
                <div id="adminSettingsApplySettingsDataProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.backup.workflow.title")
            #macroStartFieldset("admin.settings.backup.workflow.settings.title")
                <p><strong>#I18N("system.settings.backup.workflow.settings.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.workflow.settings.narrative")</p>
                <p>#macroInputTextArea("admin.settings.backup.workflow.settings" $NULL $Context.get($SETTING_BACKUP_WORKFLOW_SETTINGS) {"Cols":75,"Rows":10})</p>
                <button class="adminSettingsBackup k-button" onclick="AdminBackupActions.saveWorkflowSettings()">
                    <span class='fa fa-check'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.workflow.settings")</span>
                </button>
                <div id="adminSettingsWorkflowSettingsProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
            #macroStartFieldset("admin.settings.backup.workflow.dump.title")
                <p><strong>#I18N("system.settings.backup.workflow.dump.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.workflow.dump.narrative")</p>
                <button class="adminSettingsBackup k-button" action_type="dumpworkflow">
                    <span class='fa fa-check'>&nbsp;&nbsp;</span><span class="buttonText">#I18N("system.settings.backup.workflow.dump")</span>
                </button>
                <div id="adminSettingsWorkflowDumpProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
            #macroStartFieldset("admin.settings.backup.workflow.load.title")
                <p><strong>#I18N("system.settings.backup.workflow.load.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.workflow.load.narrative")</p>
                <form enctype="multipart/form-data" method="post" action="$AppPath/$PageName/$SubPageName/job/start/loadworkflow" id="adminWorkflowReloadForm">
                    <input type="file" name="file">
                </form>
                <button class="adminSettingsBackup k-button" action_type="loadworkflow">
                    <span class='fa fa-check'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.workflow.load")</span>
                </button>
                <div id="adminSettingsWorkflowLoadProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.backup.reload.title")
            #macroStartFieldset("admin.settings.backup.reload.title")
                <p><strong>#I18N("system.settings.backup.reload.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.reload.narrative")</p>
                <form enctype="multipart/form-data" method="post" action="$AppPath/$PageName/$SubPageName/job/start/reload" id="adminSettingsReloadForm">
                    <input type="file" name="file">
                </form>
                <br>
                <button class="adminSettingsBackup k-button" action_type="reload">
                    <span class='fa fa-repeat'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.reload")</span>
                </button>
                <div id="adminSettingsReloadProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.settings.backup.clear.title")
            #macroStartFieldset("admin.settings.backup.clear.title")
                <p><strong>#I18N("system.settings.backup.clear.subtitle")</strong></p>
                <p>#I18N("system.settings.backup.clear.narrative")</p>
                <button class="adminSettingsBackup k-button" action_type="clear">
                    <span class='fa fa-trash-o'></span><span class="buttonText">&nbsp;&nbsp;#I18N("system.settings.backup.clear")</span>
                </button>
                <div id="adminSettingsClearProgress" class="adminSettingsProgress"></div>
            #macroEndFieldset()
        #macroEndTab()

    #macroEndTabs()
</span>

</div>

<script type="application/javascript">

    PivotalUtils.showFormTooltips("#backup-settings");

    var AdminBackupActions = {
        checkTimer:0,
        button:false,
        type:false,
        serverJobId:false,
        progressBar:false,
        text:false,
        originalHtml:"",

        /**
         * Run an action based on the current state of the buttons
         * @param button Button pressed
         */
        action: function(button) {
            if (this.type)
                this.stop(true);
            else
                this.start(button);
        },

        saveWorkflowSettings: function() {

            var result = PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/saveworkflowsettings', "settings=" + encodeURIComponent($("#admin\\.settings\\.backup\\.workflow\\.settings").val()), true);
            if (result) {
                if (result.inError)
                    alert(result.error);
                else {
                    alert('#I18N("admin.settings.action.save.successful")');
                }
            }
            else
                alert("Unknown response from server");
        },

        /**
         * Starts a job on the server based on an attribute of the button pressed
         * @param button Button pressed
         */
        start: function(button) {
            this.progressBar = false;
            this.button = $(button);
            this.type = this.button.attr('action_type');
            this.text = this.button.find('.buttonText');
            this.originalHtml = this.text.html();

            if (this.type=='download') {
                this.text.html('#I18N("system.settings.backup.download.stop")')
                this.progressBar = $("#adminSettingsDownloadProgress");
                var params = "tables=" + $("#admin\\.settings\\.backup\\.table-list").val();
                if ($("#admin\\.settings\\.backup\\.compress").prop("checked")) {
                    params+="&compress";
                }
                this.serverJobId=PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/start/download', params, true).id;
            }
            else if (this.type=='reload') {
                if (confirm('#I18N("system.settings.backup.reload.confirm")')) {
                    this.text.html('#I18N("system.settings.backup.reload.stop")')
                    this.progressBar = $("#adminSettingsReloadProgress");
                    AIM.submit($('#adminSettingsReloadForm')[0], {
                        onComplete : function(frame, response) {
                            var results = eval('(' + response + ')');
                            AdminBackupActions.serverJobId=results.id;
                        }
                    });
                }
            }
            else if (this.type=='clear') {
                if (confirm('#I18N("system.settings.backup.clear.confirm")')) {
                    this.text.html('#I18N("system.settings.backup.clear.stop")');
                    this.progressBar = $('#adminSettingsClearProgress');
                    this.serverJobId = PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/start/clear', null, true).id;
                }
            }
            else if (this.type=='applytestdata') {
                this.text.html('#I18N("system.settings.backup.applytestdata.stop")');
                this.progressBar = $('#adminSettingsApplyTestDataProgress');
                this.serverJobId = PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/start/applytestdata', null, true).id;
            }
            else if (this.type=='applysettingsdata') {
                this.text.html('#I18N("system.settings.backup.applysettingsdata.stop")');
                this.progressBar = $('#adminSettingsApplySettingsDataProgress');
                this.serverJobId = PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/start/applysettingsdata', null, true).id;
            }
            else if (this.type=='loadworkflow') {
                if (confirm('#I18N("system.settings.backup.reload.confirm")')) {
                    this.text.html('#I18N("system.settings.backup.workflow.load.stop")');
                    this.progressBar = $('#adminSettingsWorkflowLoadProgress');
                    AIM.submit($('#adminWorkflowReloadForm')[0], {
                        onComplete : function(frame, response) {
                            var results = eval('(' + response + ')');
                            AdminBackupActions.serverJobId=results.id;
                        }
                    });
                }
            }
            else if (this.type=='dumpworkflow') {
                this.text.html('#I18N("system.settings.backup.workflow.dump.stop")');
                this.progressBar = $('#adminSettingsWorkflowDumpProgress');
                this.serverJobId = PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/start/dumpworkflow', "fileext=dat", true).id;
            }
            if (this.progressBar) {
                $('.adminSettingsBackup').prop('disabled', true);
                this.button.prop('disabled', false);
                this.progressBar.data('kendoProgressBar').value(0);
                this.progressBar.show();
                this.checkTimer = setInterval(this.checkProgress, 500);
            }
            else {
                this.progressBar = false;
                this.button = false;
                this.type = false;
                this.text = false;
            }
        },

        /**
         * Stops the currently running job
         */
        stop: function(userInstigated) {
            if (userInstigated) PivotalUtils.getContentAsync('$AppPath/$PageName/$SubPageName/job/kill', 'id=' + AdminBackupActions.serverJobId)
            $('.adminSettingsBackup').prop('disabled', true);
            clearTimeout(this.checkTimer);
            this.text.html(this.originalHtml);
            this.progressBar.hide();
            this.progressBar.data('kendoProgressBar').value(0);
            this.progressBar = null;
            $('.adminSettingsBackup').prop('disabled', false);
            this.type = null;
        },

        /**
         * Check the status of the curently running job
         */
        checkProgress: function () {
            var results=PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/job/check', 'id=' + AdminBackupActions.serverJobId);
            if (results.completed) {
                if (AdminBackupActions.type=="download")
                    window.location.href='$AppPath/$PageName/$SubPageName/job/download?fileext=sql&id=' + AdminBackupActions.serverJobId;
                else if (AdminBackupActions.type=="dumpworkflow")
                    window.location.href='$AppPath/$PageName/$SubPageName/job/download?fileext=dat&filename=workflow&&id=' + AdminBackupActions.serverJobId;

                AdminBackupActions.stop();
            }
            else {
                AdminBackupActions.progressBar.data('kendoProgressBar').value(results.count);
            }
        },
    };

    #**
     * Create all the progress bars
     *#
    $(".adminSettingsProgress").each(function () {
        $(this).hide();
        $(this).kendoProgressBar({
            type: "percent",
            animation: {
                duration: 600
            }
        }).data("kendoProgressBar");
    });
    $(".adminSettingsBackup").click(function() {
        AdminBackupActions.action(this);
    });
</script>
