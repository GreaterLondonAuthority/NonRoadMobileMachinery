<h1>#I18N("admin.role_type.edit.title")</h1>
#macroStartForm("roletypeentity" "$AppPath/$PageName/$SubPageName/edit")
   #macroStartTabs()
      #macroStartTab("admin.role_type.edit.title", true)
        #macroStartFieldset("admin.role_type.edit.title")
            #macroInputHidden("roletypeentity.id")
            #macroInputText("roletypeentity.name" $NULL $NULL {"focus":true})
            #macroInputHidden("clearafterupdate")
            #macroInputTextArea("roletypeentity.description")
            #macroInputSwitch("roletypeentity.disabled")
        #macroEndFieldset()
      #macroEndTab()
   #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})
