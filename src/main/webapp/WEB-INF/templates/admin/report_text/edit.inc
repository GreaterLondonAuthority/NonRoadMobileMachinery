<h1>#I18N("admin.report_text.edit.title")</h1>
#macroStartForm("editReportTextForm" "$AppPath/$PageName/$SubPageName/edit")
   #macroStartTabs()
      #macroStartTab("admin.report_text.edit.title", true)
        #macroStartFieldset("admin.report_text.edit.title")
            #macroInputHidden("reporttextentity.id")
            #macroInputText("reporttextentity.name" $NULL $NULL {"focus":true})
            #macroInputHidden("clearafterupdate")
            #macroInputTextArea("reporttextentity.description")

            #set($LayoutTypes=[])
            #set($tmp=$LayoutTypes.add({"text":"#I18N($!REPORTTEXTENTITY_LAYOUT_PORTRAIT)","value":"$!REPORTTEXTENTITY_LAYOUT_PORTRAIT"}))
            #set($tmp=$LayoutTypes.add({"text":"#I18N($!REPORTTEXTENTITY_LAYOUT_LANDSCAPE)","value":"$!REPORTTEXTENTITY_LAYOUT_LANDSCAPE"}))
            #macroInputSelect("reporttextentity.layout" $NULL $NULL $LayoutTypes  {"Placeholder":"reporttextentity.choose.layout","Width":"455px"})

            ##
            ## Report text type
            ##
            #set($ExtraFilter = "")
            #macroSetExtraFilter("not searchEntity.disabled=true" "or searchEntity.id=$!reporttextentity.type.id" [$reporttextentity, $reporttextentity.type])
            #macroInputSelect("reporttextentity.type" $NULL $NULL "$AppPath/$PageName/$SubPageName/display/ReportTextTypeEntity" {"IsRequired":true,"ShowEmptyItem":true,"Tooltip":"reporttextentity.choose.type","ExtraFilter":"$!ExtraFilter","Width":"455px"})

            ##
            ## Disabled flag
            ##
            #macroInputSwitch("reporttextentity.disabled")
            <input type="hidden" name="text" id="reporttextentity.text">
            <div id="reportText">
            $!reporttextentity.text
            </div>

        #macroEndFieldset()
      #macroEndTab()
   #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})


<script>
    $(document).ready(function () {
    #if (!$EditState.is("VIEWING"))
        #macroFroalaEditor("div#reportText" "#reporttextentity\\.text" {
                                "height":"200px"
                                ,"width":"100%"
                                ,"imageManagerLoadURL": "$AppPath/$PageName/$SubPageName/imagebrowser/read"
                                ,"imageManagerDeleteURL": "$AppPath/$PageName/$SubPageName/imagebrowser/delete"
                                ,"imageUploadURL": "$AppPath/$PageName/$SubPageName/imagebrowser/upload"
                            })

        $("#editReportTextForm").bind("submit",psApp.froalaHelper.finish);
    #end
    });
</script>
