<h1>#I18N("admin.lookups.edit.title")</h1>
#macroStartForm("lookupsentity" "$AppPath/$PageName/$SubPageName/edit")
   #macroStartTabs()
      #macroStartTab("admin.lookups.edit.title", true)
        #macroStartFieldset("admin.lookups.edit.title")
            #macroInputHidden("lookupsentity.id")
            #macroInputText("lookupsentity.name" $NULL $NULL {"focus":true})
            #macroInputHidden("clearafterupdate")
            #macroInputTextArea("lookupsentity.description")
            #macroInputHidden("lookupsentity.type" $Request.getParameter("type"))
            #macroInputText("lookupsentity.tag")
            #macroInputSwitch("lookupsentity.disabled")
        #macroEndFieldset()
      #macroEndTab()
   #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})