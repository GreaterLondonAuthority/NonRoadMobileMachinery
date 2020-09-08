#macroStartForm("distributionlistentity" "$AppPath/$PageName/$SubPageName/edit" {"EncType":"multipart/form-data"})
    #macroStartTabs()
        #macroStartTab("reporting.distribution_list.edit.title", true)
            #macroStartFieldset("reporting.distribution_list.edit.title")
                #macroInputHidden("distributionlistentity.id")
                #macroInputText("distributionlistentity.name")
                #macroInputTextArea("distributionlistentity.description")
                #macroInputSelect("distributionlistentity.type" $NULL $NULL $DistributionTypesList {"Placeholder":"distributionlistentity.choose.report","onChange":"distributionListHandler.change","Width":"455px"})

                <div class="form-subtitle deadend">#I18N("reporting.distributionlistentity.edit.deadend.subtitle")</div>
                <div class="form-subtitle email">#I18N("reporting.distributionlistentity.edit.email.recipients.subtitle")</div>
                <div class="form-subtitle emaillink">#I18N("reporting.distributionlistentity.edit.emaillink.recipients.subtitle")</div>
                <div class="form-subtitle vfs">#I18N("reporting.distributionlistentity.edit.vfs.recipients.subtitle")</div>
                <div class="form-subtitle sms">#I18N("reporting.distributionlistentity.edit.sms.recipients.subtitle")</div>

            #macroEndFieldset()

            <span id="transientForm">
                <span class="dist-settings deadend">
                    #macroInputHidden("distributionlistentity.content" "empty")
                </span>

                <span class="dist-settings email">
                    #macroStartFieldset("reporting.distributionlistentity.edit.email.recipients.title")
                        #macroInputTextArea("distributionlistentity.content" $NULL $NULL {"Rows":"5", "Placeholder":"Testing&lt;tm@pivotal-solutions.co.uk&gt;"})
                        #macroInputSwitch("distributionlistentity.foreach")
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.email.content.title")
                        #macroInputText("distributionlistentity.emailFrom" $NULL $NULL {"Placeholder":"test@pivotal-solutions.co.uk"})
                        #macroInputText("distributionlistentity.emailCc")
                        #macroInputText("distributionlistentity.emailBcc")
                        #macroInputSelect("distributionlistentity.emailImportance" $NULL $NULL $ImportanceTypeList)
                        #macroInputSelect("distributionlistentity.emailSensitivity" $NULL $NULL $SensitivityTypeList)
                        #macroInputSelect("distributionlistentity.emailPriority" $NULL $NULL $PriorityTypeList)
                        #macroInputText("distributionlistentity.emailSubject")
                        #macroInputTextArea("distributionlistentity.emailBody" $NULL $NULL {"Rows":"5"})
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.email.attachment.title")
                        #macroInputText("distributionlistentity.emailAttachmentName")
                        #macroInputSelect("distributionlistentity.compression" $NULL $NULL $CompressionTypeList {"ShowEmptyItem":true})
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.email.security.title")
                        #macroInputText("distributionlistentity.emailHost" $NULL $NULL {"Placeholder":"pivotal-solutions.co.uk / 192.168.0.1"})
                        #macroInputText("distributionlistentity.username")
                        #macroInputPassword("distributionlistentity.password")
                    #macroEndFieldset()
                </span>

                <span class="dist-settings emaillink">
                    #macroStartFieldset("reporting.distributionlistentity.edit.emaillink.recipients.title")
                        #macroInputTextArea("distributionlistentity.content" $NULL $NULL {"Rows":"5", "Placeholder":"Testing&lt;tm@pivotal-solutions.co.uk&gt;"})
                        #macroInputSwitch("distributionlistentity.foreach")
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.emaillink.content.title")
                        #macroInputText("distributionlistentity.emailFrom" $NULL $NULL {"Placeholder":"test@pivotal-solutions.co.uk"})
                        #macroInputText("distributionlistentity.emailCc")
                        #macroInputText("distributionlistentity.emailBcc")
                        #macroInputSelect("distributionlistentity.emailImportance" $NULL $NULL $ImportanceTypeList)
                        #macroInputSelect("distributionlistentity.emailSensitivity" $NULL $NULL $SensitivityTypeList)
                        #macroInputSelect("distributionlistentity.emailPriority" $NULL $NULL $PriorityTypeList)
                        #macroInputText("distributionlistentity.emailSubject")
                        #macroInputTextArea("distributionlistentity.emailBody" $NULL $NULL {"Rows":"5"})
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.emaillink.security.title")
                        #macroInputText("distributionlistentity.emailHost" $NULL $NULL {"Placeholder":"pivotal-solutions.co.uk / 192.168.0.1"})
                        #macroInputText("distributionlistentity.username")
                        #macroInputPassword("distributionlistentity.password")
                    #macroEndFieldset()
                    #macroStartFieldset("reporting.distributionlistentity.edit.recipients.title")
                        #macroInputText("distributionlistentity.secondaryContent")
                        #macroInputSwitch("distributionlistentity.userDirIsRoot")
                        #macroInputSelect("distributionlistentity.compression" $NULL $NULL $CompressionTypeList {"ShowEmptyItem":true})
                        #macroInputText("distributionlistentity.secondaryUsername")
                        #macroInputPassword("distributionlistentity.secondaryPassword")
                    #macroEndFieldset()
                </span>

                <span class="dist-settings vfs">
                    #macroStartFieldset("reporting.distributionlistentity.edit.vfs.recipients.title")
                        #macroInputTextArea("distributionlistentity.content" "distributionlistentity.content.vfs" $NULL {"Tooltip":"distributionlistentity.content.vfs.description", "Placeholder":"ftp;//reports.pivotal-solutions.co.uk"})
                        #macroInputSwitch("distributionlistentity.userDirIsRoot")
                        #macroInputSelect("distributionlistentity.compression" "distributionlistentity.comprssion.vfs" $NULL $CompressionTypeList {"ShowEmptyItem":true,"Tooltip":"distributionlistentity.comprssion.vfs.description"})
                        #macroInputText("distributionlistentity.username" $NULL $NULL {"Tooltip":"distributionlistentity.username.vfs.description"})
                        #macroInputPassword("distributionlistentity.password" $NULL $NULL {"Tooltip":"distributionlistentity.password.vfs.description"})
                    #macroEndFieldset()
                </span>

                <span class="dist-settings sms">
                    #macroStartFieldset("reporting.distributionlistentity.edit.sms.url.title")
                        #macroInputText("distributionlistentity.secondaryContent" "distributionlistentity.sms.link-url" $ServletHelper.getFullAppPath() $NULL {"Tooltip" : "distributionlistentity.sms.link-url.description"})
                        #macroInputText("distributionlistentity.content" "distributionlistentity.sms.url" $NULL $NULL {"Tooltip" : "distributionlistentity.sms.url.description"})
                    #macroEndFieldset()

                    #macroStartFieldset("reporting.distributionlistentity.edit.sms.parameters.title")
                    #if($Entity.getSettings())
                        #macroDefinitionEdit("transducerentity.address.title" $Entity.getSettings())
                    #else
                        #macroDefinitionEdit("transducerentity.address.title" $Entity.getSettings("sms"))
                    #end
                    #macroEndFieldset()
                </span>

            </span>
        #macroEndTab()
    #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})

<script>
    var distributionListHandler = {
        change: function(el) {
            var selected = $("#distributionlistentity\\.type").val();

            $(".dist-settings,.form-subtitle").hide();
            $(".dist-settings").appendTo("body");

            if (selected!="") {
                $(".dist-settings." + selected).appendTo("#transientForm");
                $("." + selected).show();
            }

            PivotalUtils.showFormTooltips("#transientForm");
        }
    }
    distributionListHandler.change();
</script>
