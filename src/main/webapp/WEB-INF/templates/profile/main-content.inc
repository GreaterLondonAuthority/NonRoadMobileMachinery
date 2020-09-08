<div id="profile">
    #macroStartForm("userentity" "$AppPath/$PageName/$SubPageName/edit" {"noResize":true,"onSubmit":"profileTools.submitForm","onValidationFailed":"profileTools.validationFailed"})
        <h1>#I18N('profile.title')</h1>
        #macroStartFieldset("#I18N('profile.edit.pagetitle')")
            #if(!$CurrentUser.hasAccess($PRIVILEGES_EDIT_OWN_EMAIL_ADDRESS))
                #set($EmailReadOnly = "readonly")
                #set($EmailNote = "#I18N('profile.readonly.email.message')")
            #end

            #if(!$CurrentUser.hasAccess($PRIVILEGES_MANAGE_OWN_ACCOUNT))
                #set($PersonalDetailsReadOnly = "readonly")
            #end

            #macroInputText("firstname" "profile.edit.firstname" $CurrentUser.firstname {"Autocomplete":"given-name","IsRequired":true,"ExtraInputAtt":"data-role=${DQUOTE}PERSON_FIRSTNAME${DQUOTE} $!PersonalDetailsReadOnly"})
            #macroInputText("lastname" "profile.edit.lastname" $CurrentUser.lastname {"Autocomplete":"family-name","IsRequired":true,"ExtraInputAtt":"data-role=${DQUOTE}PERSON_LASTNAME${DQUOTE} $!PersonalDetailsReadOnly"})
            #macroInputEmail("email" "profile.edit.email" $profileUser.email {"note":"$!EmailNote","IsRequired":true,"Autocomplete":"email","ExtraInputAtt":"data-email-msg=${DQUOTE}#I18N('login.register.bad.email.format')${DQUOTE} $!EmailReadOnly"})
            #macroInputText("telephone" "profile.edit.telephone" $profileUser.phoneNumber {"Autocomplete":"tel-national","ExtraInputAtt":"data-role=${DQUOTE}PHONE_NUMBER${DQUOTE} $!PersonalDetailsReadOnly"})
        #macroEndFieldset()

        #macroStartFieldset("profile.edit.password.change")
            #macroInputPassword("password" "profile.edit.password" "" {"Autocomplete":"new-password","hideOptional":true,"ExtraInputAtt":"data-role=${DQUOTE}USER_PASSWORD${DQUOTE} blank-ok=${DQUOTE}true${DQUOTE}"})
            #macroInputPassword("confirmPassword" "profile.edit.confirmPassword" "" {"Autocomplete":"new-password","hideOptional":true,"ExtraInputAtt":"ignore=${DQUOTE}true${DQUOTE}","ExtraHTML":"<div id='passwordMatch'></div>"})
            <div class="form-group">
                <div id="passwordRules">
                    $!WorkflowHelper.executeWorkflow("PASSWORD_RULES", $NULL, true).information
                </div>
            </div>
        #macroEndFieldset()

        #set($RightButtons = [{"id":"localSave","icon":"save","label":"Save","onClick":"profileTools.saveProfile"}])
        #set($EndFormProperties = {"cancelButton":{"hide":true},"saveButton":{"hide":true},"rightButtons":$RightButtons})
    #macroEndForm($EndFormProperties)
</div>

<script>
$(document).ready(function () {

    $('#registerForm').submit(function (e) {
        return registerUtils.saveform(e);
    });

    profileTools.showPasswordMatch();

    $('#password').on("input", function() {
       profileTools.showPasswordMatch();
    });
    $('#confirmPassword').on("input", function() {
       profileTools.showPasswordMatch();
    });
});

var profileTools = {

    submitForm: function() {
        return false;
    },

    validationFailed: function () {

         ## PivotalUtils.showNotification("#I18N("site.edit.error.validationfailed")", "error");
         $(".k-invalid:first").focus();
         $('#password').val("");
         $('#confirmPassword').val("");
         profileTools.showPasswordMatch();
     },

     showPasswordMatch: function() {
         if ($('#confirmPassword').val() === "")
             $("#passwordMatch").html("");
         else {
             if (psApp.login.passwordsMatch())
                 $("#passwordMatch").html("#I18N('global.error.password.match_yes')");
             else
                 $("#passwordMatch").html("#I18N('global.error.password.match_no')");
         }
     },

    /**
     * Performs the save.
     */
    saveProfile: function() {

        if (!psApp.login.passwordsMatch()) {
            alert("#I18N('global.error.password.missmatch')");
            $('#password').val("");
            $('#confirmPassword').val("");
            $('#password').focus();
        }
        else {

            var formChanged = PivotalUtils.isFormDirty;
            $("#userentity").submit();

            // alert($(".k-invalid").length);
            if ($(".k-invalid").length == 0) {
                // Create the object containing all the form parameters.
                var paramObject = {
                    firstname: $("#firstname").val(),
                    lastname: $("#lastname").val(),
                    telephone: $("#telephone").val(),
                    email: $("#email").val(),
                    password: psApp.login.encryptPassword($("#password").val()),
                    theme: $("#theme").val()
                };
                // Post the form object to the Controller and check the values.
                var status = PivotalUtils.getJsonContent("$AppPath/$PageName/update", $.param(paramObject), true);
                if (status.inError) {
                    PivotalUtils.showNotification(status.error, "error");
                } else {
                    PivotalUtils.showNotification('#I18N("profile.edit.saved.successfully")');
                    if (formChanged) {
                        if ($("#password").val() !== "")
                            psApp.workflow.execute("$AppPath", "USER_PASSWORD_CHANGE", $("#userentity").serialize());
                        else
                            psApp.workflow.execute("$AppPath", "USER_DETAIL_CHANGE", $("#userentity").serialize());
                    }
                    PivotalUtils.clearAllDirtyFlags();
                    $("#password").val("");
                    $("#confirmPassword").val("");
                }
            }
        }
    },

}
</script>
