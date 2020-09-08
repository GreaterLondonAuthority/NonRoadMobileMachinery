#macroStartForm("scheduledtaskentity" "$AppPath/$PageName/$SubPageName/edit" {"EncType":"multipart/form-data"})

    #macroTabButtons([
        "admin.scheduled_task.edit.title",
        "admin.scheduled_task.edit.report.title",
        "admin.scheduled_task.schedule.title"
    ])

    #macroStartTabs()
        #macroStartTab("admin.scheduled_task.edit.title", true)
            #macroStartFieldset("admin.scheduled_task.edit.title")
                #macroInputHidden("scheduledtaskentity.id")
                #macroInputHidden("scheduledtaskentity.taskType" "report")
                #macroInputText("scheduledtaskentity.name")
                #macroInputTextArea("scheduledtaskentity.description")
                #macroInputSelect("scheduledtaskentity.distributionList" $NULL $NULL "$AppPath/$PageName/$SubPageName/display/DistributionListEntity" {"Placeholder":"scheduledtaskentity.choose.recipients","ShowEmptyItem":true,"IsRequired":true,"AddNewLink":"$AppPath/$PageName/distribution_list/edit","Width":"455px"})
            #macroEndFieldset()
        #macroEndTab()


        #macroStartTab("admin.scheduled_task.edit.report.title")
            #macroStartFieldset("admin.scheduled_task.edit.report.title")
                #macroInputSelect("scheduledtaskentity.report" $NULL $NULL "$AppPath/$PageName/$SubPageName/display/ReportEntity" {"Placeholder":"scheduledtaskentity.choose.report","onChange":"taskHandler.changeReport","ShowEmptyItem":true,"IsRequired":true,"AddNewLink":"$AppPath/$PageName/report/edit","Width":"455px"})
                <span id="report-parameters">
                    #if ($ScheduledTaskEntity.report)
                        #parse("$PageName/$SubPageName/report.vm")
                    #end
                </span>
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.scheduled_task.schedule.title")
            #macroStartFieldset("admin.scheduled_task.schedule.title")
                #macroInputSelect("scheduledtaskentity.schedType" $NULL $NULL $ScheduleTypeList {"Placeholder":"scheduledtaskentity.choose.schedule","onChange":"taskHandler.change"})

                <span id="transientForm">
                    <span class="task-settings type-0">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedRunEveryMinutes.subtitle")</div>
                        #macroInputNumber("scheduledtaskentity.schedRunEveryMinutes" $NULL $NULL {"IsRequired":true,"Min":1,"Max":1439})
                        #macroInputTime("scheduledtaskentity.schedRunEveryMinutesFrom" $NULL $NULL {"Format":"HH:mm","TimeFormat":"HH:mm"})
                        #macroInputTime("scheduledtaskentity.schedRunEveryMinutesTo" $NULL $NULL {"Format":"HH:mm","TimeFormat":"HH:mm"})
                        #macroInputSwitch("scheduledtaskentity.schedRunEveryDayExcludeWeekends")
                        #macroInputSwitch("scheduledtaskentity.schedRunEveryDayExcludeHolidays")
                    </span>

                    <span class="task-settings type-1">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedRunEveryDayAt.subtitle")</div>
                        #macroInputTime("scheduledtaskentity.schedRunEveryDayAt" $NULL $NULL {"IsRequired":true,"Format":"HH:mm","TimeFormat":"HH:mm"})
                        #macroInputSwitch("scheduledtaskentity.schedRunEveryDayExcludeWeekends")
                        #macroInputSwitch("scheduledtaskentity.schedRunEveryDayExcludeHolidays")
                    </span>

                    <span class="task-settings type-2">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedRunEveryWeekDays.subtitle")</div>
                        #macroInputSelect("scheduledtaskentity.schedRunEveryWeekDays" $NULL $NULL $ScheduleDayList {"IsRequired":true,"IsMultiSelect":true})
                        #macroInputTime("scheduledtaskentity.schedRunEveryWeekDaysAt" $NULL $NULL {"IsRequired":true,"Format":"HH:mm","TimeFormat":"HH:mm"})
                    </span>

                    <span class="task-settings type-3">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedRunEveryMonthOn.subtitle")</div>
                        #macroInputNumber("scheduledtaskentity.schedRunEveryMonthOn" $NULL $NULL {"IsRequired":true,"Min":1,"Max":28})
                        #macroInputTime("scheduledtaskentity.schedRunEveryMonthAt" $NULL $NULL {"IsRequired":true,"Format":"HH:mm","TimeFormat":"HH:mm"})
                    </span>

                    <span class="task-settings type-4">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedNever.subtitle")</div>
                    </span>

                    <span class="task-settings type-5">
                        <div class="form-subtitle">#I18N("scheduledtaskentity.schedRunOnceOn.subtitle")</div>

                        #set($date ="")
                        #if(!$Utils.isBlank($scheduledtaskentity.schedRunOnceOn))
                            #set($date =  $Utils.dateFormat($scheduledtaskentity.schedRunOnceOn ,"dd MMM yyyy HH:mm"))
                        #end
                        #if($Utils.isBlank($date)) #set($date = "")#end
                        #macroInputDateTime("scheduledtaskentity.schedRunOnceOn"  $NULL  "$date", {"IsRequired":true,"Format":"dd MMM yyyy HH:mm", "TimeFormat":"HH:mm"})
                        ###macroInputDateTime("scheduledtaskentity.schedRunOnceOn" $NULL $NULL {"IsRequired":true})
                    </span>
                </span>
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})

<script>
    var taskHandler = {

        ## Called when the report is selected
        changeReport: function(el) {
            $("#report-parameters").html(PivotalUtils.getContent("$AppPath/$PageName/$SubPageName/report", "id=" + el.val()));
            PivotalUtils.showFormTooltips("#report-parameters");
        },

        ## Called when the schedule type is selected
        change: function(el) {
            var selected = $("#scheduledtaskentity\\.schedType").val();

            $(".task-settings").hide();
            $(".task-settings").appendTo("body");

            if (selected!="") {
                $(".task-settings.type-" + selected).appendTo("#transientForm");
                $(".type-" + selected).show();
                $("#transientForm").closest("fieldset").find(".legendContent").html($("#transientForm .form-subtitle").html());
            }
            else {
                $("#transientForm").closest("fieldset").find(".legendContent").html("");
            }
            PivotalUtils.showFormTooltips("#transientForm");
        }
    }
    taskHandler.change();
</script>
