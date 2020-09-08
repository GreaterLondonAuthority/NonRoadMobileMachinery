<h1>#I18N("admin.action_type.edit.title")</h1>
#macroStartForm("actiontypeentity" "$AppPath/$PageName/$SubPageName/edit")
   #macroStartTabs()
      #macroStartTab("admin.action_type.edit.title", true)
        #macroStartFieldset("admin.action_type.edit.title")
            #macroInputHidden("actiontypeentity.id")
            #macroInputText("actiontypeentity.name" $NULL $NULL {"focus":true})
            #macroInputTextArea("actiontypeentity.description")
            #macroInputHidden("clearafterupdate")

            #set($ExtraFilter = "")
            #macroSetExtraFilter("not searchEntity.disabled=true" "or searchEntity.id=$!actiontypeentity.workflow.id" [$actiontypeentity, $actiontypeentity.workflow])
            #macroInputEntitySelect("actiontypeentity.workflow" $NULL $NULL "WorkflowEntity" {"IsRequired":true,"ShowEmptyItem":true,"Tooltip":"actiontypeentity.choose.workflow","ExtraFilter":"$!ExtraFilter","Width":"455px"})

            #macroInputSwitch("actiontypeentity.disabled")

        #macroEndFieldset()
      #macroEndTab()
   #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})
