#macroStartForm("reportentity" "$AppPath/$PageName/$SubPageName/edit" {"EncType":"multipart/form-data"})

    #macroStartTabs()
        #macroStartTab("admin.report.edit.title", true)
            #macroStartFieldset("admin.report.edit.title")
                #macroInputHidden("reportentity.id")
                #macroInputText("reportentity.name")
                #macroInputTextArea("reportentity.description")
                #macroInputSelect("reportentity.type" $NULL $NULL $ReportTypesList {"Placeholder":"reportentity.choose.report","Width":"455px"})
                #macroInputCodeEditor("reportentity.fileString" "$!ReportEntity.Type")
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()
#macroEndForm()