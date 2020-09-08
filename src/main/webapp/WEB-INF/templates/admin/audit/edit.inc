<h1>#I18N("admin.audit.edit.title")</h1>
#macroStartForm("changelogentity" "$AppPath/$PageName/$SubPageName/edit")

    #macroTabButtons([
        "admin.audit.edit.title",
        "admin.audit.edit.changes.title"
    ])

    #macroStartTabs()
        #macroStartTab("admin.audit.edit.title", true)
            #macroStartFieldset("admin.audit.edit.title")
                #macroInputHidden("changelogentity.id")
                #macroInputDate("changelogentity.timeAdded" $NULL $NULL {"focus":true})
                #macroInputHidden("clearafterupdate")
                #macroInputText("changelogentity.userFullName")
                #macroInputText("changelogentity.changeType")
                #macroInputText("changelogentity.tableAffected")
                #macroInputText("changelogentity.rowAffected")
            #macroEndFieldset()
        #macroEndTab()
        #macroStartTab("admin.audit.edit.changes.title")
            #macroStartFieldset("admin.audit.edit.changes.title")
            <div class="input-line view-line textarea">
                #macroShowLabel("changelogentity.previousValues")
                <div id="differences"></div>
            </div>
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()

#macroEndForm({"editButton":{"hide":true}})

<script>
    $(document).ready(function() {
        var content = PivotalUtils.getContent("$AppPath/admin/history/compare", "first=$Entity.Id&second=$!NextLogId");
        var contents = content.split("<!--split for audit-->");
        if (contents && contents.length>2) {
            content = contents[1];
        }
        $('#differences').html(content)
    });
</script>
