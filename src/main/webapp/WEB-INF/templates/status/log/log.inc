#parse("$PageName/macros.inc")

<ul id="menu" style="display:none">
    <li>
        #I18N("status.log.file.list.title")
        <ul id="logFileList">
            <li>
                #I18N("status.log.file.current.title")
                <ul>
                    #macroShowLogMenuItem($AppLog)
                </ul>
            </li>
            <li>
                #I18N("status.log.file.app.list.title")
                <ul>
                    #foreach($Log in $AppLogs)
                        #if ($foreach.count < 10)
                            #macroShowLogMenuItem($Log)
                        #end
                    #end
                </ul>
            </li>
            <li>
                #I18N("status.log.file.other.list.title")
                <ul>
                    #foreach($Name in $Logs.keySet())
                        <li>
                            $Name
                            <ul>
                                #foreach($Log in $Logs.get($Name))
                                    #if ($foreach.count < 10)
                                        #macroShowLogMenuItem($Log)
                                    #end
                                #end
                            </ul>
                        </li>
                    #end
                </ul>
            </li>
        </ul>
    </li>
    <li>
        #I18N("status.log.settings.title")
        <ul>
            <li>
                #I18N("status.log.settings.font.size.title")
                <ul>
                    #macroLogFontSizeMenus([100,70,50])
                </ul>
            </li>
            <li actionType="settailing" id="autoTailingElement">
                #I18N("status.log.auto.tail.title")
            </li>
        </ul>
    </li>
    <li actionType="editconfig">
        #I18N("status.log.edit.configuration.title")
    </li>
    <li disabled="disabled" style="position: absolute;right:40px;overflow:hidden;width:400px;text-align:right" id="selectedLogFile">
        $Preferences.get("selectedLogName", "$!AppLog.Name")
    </li>
</ul>

<script type="application/javascript">

    var logSettings = {
        fontSize: $Preferences.get("fontSize", 70),
        autoTail: $Preferences.get("autoTail", false),
        tailSize: $Preferences.get("tailSize", 5000),
        defaultFile: '$Preferences.get("selectedLogName", "$!AppLog.Name")',
        resizeTimer: false,
        tailTimer: false,
        lastAction: false,

        excel: function(filename) {
            $("#display").prop('src', "$AppPath/status/logcontent/xls?filename=" + filename);
        },
        download: function(filename) {
            $("#display").prop('src', "$AppPath/status/logcontent?attached&filename=" + filename);
        },
        view: function(filename) {
            this.defaultFile = filename;
            this.lastAction = "view";
            $("#display").prop('src', "$AppPath/status/logcontent?size=" + this.fontSize + "&filename=" + this.defaultFile);
        },
        tail: function(filename) {
            this.defaultFile = filename;
            this.lastAction = "tail";
            $("#display").prop('src', "$AppPath/status/logcontent?tail&auto=" + this.autoTail + "&size=" + this.fontSize + "&length=" + this.tailSize + "&filename=" + this.defaultFile);
        },
        setfontsize: function(notused, element) {
            this.fontSize = $(element).attr("fontsize");
            $(element.parentNode).find(".k-state-selected").removeClass("k-state-selected");
            $(element).addClass("k-state-selected");
            this[this.lastAction](this.defaultFile);
        },
        settailing: function(notused, element) {
            if (this.autoTail) {
                this.autoTail = false;
                clearInterval(this.tailTimer);
                $(element).removeClass("k-state-selected");
            }
            else {
                this.autoTail = true;
                this.tailTimer = setInterval(function () {
                    logSettings.tail(logSettings.defaultFile);
                }, 5000);
                $(element).addClass("k-state-selected");
                this.tail(this.defaultFile);
            }
        },
        resize: function() {
            clearTimeout(this.resizeTimer);
            this.resizeTimer = setTimeout(function() {
                var frame = $("#display");
                frame.width($(window).width() - frame.position().left - 25);
                frame.height($(window).height() - frame.position().top - 20);
                frame.css("visibility", "visible");
            }, 300);
        },
        editconfig: function() {
            psApp.windowManager.open({title:"#I18N("system.log.properties.editor.title")", width:"80%", height:"80%", content:"$AppPath/$PageName/logconfig", useIFrame:true, customActions:[
                                    "Maximize","Refresh","Close",
                                    {
                                        name:"pencil",
                                        handler: function(modalWindow) {
                                            var content = this.window.MediaEditorHelper.getContent();
                                            PivotalUtils.getContent("$AppPath/$PageName/logconfig", "logconfig=" + encodeURIComponent(content), true);
                                            PivotalUtils.heartbeat("$AppPath");
                                            logSettings[logSettings.lastAction](logSettings.defaultFile);
                                            psApp.windowManager.close();
                                        }
                                    }
                                ]});

        }
    };

    $(document).ready(function() {
        $("#menu").kendoMenu({
         open: function(e) {

            // Remove k-state-selected  to fix the issue of sub menu font is not displaying

              $("#logFileList").find(".k-state-selected").removeClass("k-state-selected");
         },
            select: function(e) {
                var type = $(e.item).attr('actionType');
                var filename = $(e.item).attr('filename');
                if (type=="tail" || type=="view") {
                    logSettings.autoTail = false;
                    clearInterval(logSettings.tailTimer);
                    $("#autoTailingElement").removeClass("k-state-selected");
                    $("#logFileList").find(".k-state-selected").removeClass("k-state-selected");
                    $("[id='" + filename + "']").addClass("k-state-selected");
                    $("#selectedLogFile").text(filename);
                    console.log(filename + ": " + $("#" + filename).length)
                }
                logSettings[type](filename, e.item);
            }
        });
        $("#menu").show();
        $(window).resize(logSettings.resize);
        logSettings.resize();

        #if ($Preferences.get("autoTail", false))
            logSettings.autoTail = false;
            logSettings.settailing(null, $("#autoTailingElement")[0]);

        #elseif ($Preferences.SelectedLog && $Preferences.SelectedLog.length() > $MAX_LENGTH)
            logSettings.tail(logSettings.defaultFile);

        #else
            logSettings.view(logSettings.defaultFile);
        #end
    });
</script>

<iframe id="display" style="visibility: hidden"></iframe>
