#macroStartForm("reportlogentity" "$AppPath/$PageName/$SubPageName/edit")

    #macroStartTabs()
        #macroStartTab("admin.log.title", true)
            #macroStartFieldset("admin.log.title")
                 ##$LogEntity
                #macroInputHidden("logentity.id")
                #macroInputText("logentity.dateAdded")
                #macroInputText("logentity.duration")
                #macroInputTextArea("logentity.message")
                #macroInputText("logentity.parameterValues")
                #macroInputText("logentity.recipients")
                #macroInputText("logentity.reportName")
                #macroInputText("logentity.serverId")
                #macroInputText("logentity.status")
                #macroInputText("logentity.taskName")
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()
#macroEndForm()
