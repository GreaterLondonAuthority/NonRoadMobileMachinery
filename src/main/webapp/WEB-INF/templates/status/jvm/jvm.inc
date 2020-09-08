#parse("$PageName/macros.inc")

    #macroTabButtons([
        "status.jvm.gauges.title",
        "status.jvm.environment.title"
    ])

    #macroStartTabs()

        #macroStartTab("status.jvm.gauges.title", true)
            #macroStartFieldset("status.jvm.gauges.title")

                <div class="row">
                    <div class="col-md-12">

                            #macroShowStatusGauge("heap" "status.jvm.heap.title" "status.jvm.heap.template")
                            #macroShowStatusGauge("permgen" "status.jvm.permgen.title" "status.jvm.permgen.template")
                            #macroShowStatusGauge("cpu" "status.jvm.cpu.title")
                            #if (!$OS.Name.contains("Windows"))
                                #macroShowStatusGauge("fileDescriptors" "status.jvm.file_descriptors.title" "status.jvm.file_descriptors.template")
                            #end
                            #foreach($FileSystem in $FileSystems)
                                #macroShowStatusGauge("fileSystem-$FileSystem" "$FileSystem" "status.jvm.filesystem.template")
                            #end
                    </div>
                </div>

            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("status.jvm.environment.title")
            #macroStartStatusTable("status.jvm.environment.title")
                #macroShowStatusTableRow("status.jvm.vendor" "$Management.SpecVendor<br>$Management.VmName")
                #macroShowStatusTableRow("status.jvm.version" $Management.VmVersion)
                #macroShowStatusTableRow("status.jvm.operating_system" "$OS.Name ($OS.Arch) v$OS.Version")
                #macroShowStatusTableRow("status.jvm.processors" $Runtime.availableProcessors())
                #macroShowStatusTableRow("status.jvm.start_time" $utils.dateFormat($Management.StartTime, "dd MMMM yyyy HH:mm:ss"))
                #macroShowStatusTableRow("status.jvm.up_time" $utils.getDuration($Management.Uptime))
                #macroShowStatusTableRow("status.jvm.process_id" $Management.Name)
                #macroShowStatusTableRow("status.jvm.command_line" $utils.join($Management.InputArguments," "))
            #macroEndStatusTable()
        #macroEndTab()
    #macroEndTabs()

<script type="application/javascript">

    #**
     * Create all the progress bars and kick-off the refresh
     *#
    $(document).ready(function() {

        var statusGauges = {
            gaugesAvailable: false,
            updateGauges : function () {
                var results=PivotalUtils.getJsonContent('$AppPath/$PageName/$SubPageName/metrics');
                for (var name in results) {
                    try {
                        $(document.getElementById(name)).data('kendoProgressBar').value(results[name].percent);
                        var templateEle = $(document.getElementById(name + "_text"));
                        if (templateEle) {
                            var template = templateEle.attr("template");
                            if (template) {
                                templateEle.html(template.replace("x", results[name].used).replace("y", results[name].total));
                            }
                        }
                    }
                    catch(X){}
                }
            }
        }

        $(".statusProgress").each(function () {
            $(this).kendoProgressBar({
                type: "percent",
                animation: {
                    duration: 600
                }
            }).data("kendoProgressBar");
            statusGauges.gaugesAvailable = true;
        });
        setInterval(statusGauges.updateGauges, 10000);
        statusGauges.updateGauges();
    });
</script>
