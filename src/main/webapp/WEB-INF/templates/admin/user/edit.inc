#macroStartForm("userentity" "$AppPath/$PageName/$SubPageName/edit" {"onValidationSuccessful":"userProfileUtils.encryptData"})

    #macroTabButtons([
        "admin.user.personal.details"
    ,"admin.user.security"
    ])

    #macroStartTabs()
        #macroStartTab("admin.user.personal.details", true)
            #macroStartFieldset("admin.user.personal.details")
                #macroInputHidden("userentity.id")
                #macroInputHidden("clearafterupdate")

                #macroInputEmail("userentity.email" $NULL $NULL {"focus":true})
                #macroInputText("userentity.firstname" $NULL $NULL {"Autocomplete":"given-name","ExtraInputAtt":"data-role=${DQUOTE}PERSON_FIRSTNAME${DQUOTE}"})
                #macroInputText("userentity.lastname" $NULL $NULL {"Autocomplete":"family-name","ExtraInputAtt":"data-role=${DQUOTE}PERSON_LASTNAME${DQUOTE}"})

                #macroInputPassword("userentity.password" $NULL $NULL {"Autocomplete":"new-password","ExtraInputAtt":"data-role=${DQUOTE}USER_PASSWORD${DQUOTE} blank-ok=${DQUOTE}true${DQUOTE}"})
                #macroInputPassword("userentity.password.confirm" $NULL $NULL {"Autocomplete":"new-password","ExtraInputAtt":"data-role=${DQUOTE}USER_PASSWORD${DQUOTE} blank-ok=${DQUOTE}true${DQUOTE}"})

                ###macroInputTextArea("userentity.address")
                #macroInputText("userentity.phoneNumber" $NULL $NULL {"Autocomplete":"tel-national","ExtraInputAtt":"data-role=${DQUOTE}PHONE_NUMBER${DQUOTE}"})
                #macroInputSwitch("userentity.sendEmails")
                #macroInputSwitch("userentity.receiveEmails")
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.user.security")
            #macroStartFieldset("admin.user.security")
                #macroInputEntitySelect("userentity.role" $NULL $NULL "RoleEntity" {"ShowEmptyItem":"true","Width":"455px","ExtraFilter":"type.name='Application User'"} $NULL)
                #macroInputEntitySelect("userentity.borough" $NULL $NULL "BoroughEntity" {"ShowEmptyItem":"true","Width":"455px"} $NULL)

                #set($dateValidFrom ="")
                #if(!$Utils.isBlank($userentity.validFrom))
                    #set($dateValidFrom =  $Utils.dateFormat($userentity.validFrom ,"dd MMM yyyy HH:mm"))
                #end
                #if($Utils.isBlank($dateValidFrom)) #set($dateValidFrom = "")#end
                #macroInputDateTime("userentity.validFrom"  $NULL  "$dateValidFrom", {"Format":"dd MMM yyyy HH:mm", "TimeFormat":"HH:mm"})

                #set($dateExpires ="")
                #if(!$Utils.isBlank($userentity.expires))
                    #set($dateExpires =  $Utils.dateFormat($userentity.expires ,"dd MMM yyyy HH:mm"))
                #end
                #if($Utils.isBlank($dateExpires)) #set($dateExpires = "")#end
                #macroInputDateTime("userentity.expires"  $NULL  "$dateExpires", {"Format":"dd MMM yyyy HH:mm", "TimeFormat":"HH:mm"})

                #set($lastLoggedIn ="")
                #if(!$Utils.isBlank($userentity.lastLoggedIn))
                    #set($lastLoggedIn =  $Utils.dateFormat($userentity.lastLoggedIn ,"dd MMM yyyy HH:mm"))
                #end
                #if($Utils.isBlank($lastLoggedIn)) #set($lastLoggedIn = "")#end
                #macroInputDateTime("userentity.lastLoggedIn"  $NULL  "$lastLoggedIn", {"Format":"dd MMM yyyy HH:mm", "TimeFormat":"HH:mm", "IsDisabled":true})

                #macroInputText("userentity.loginFailCount" $NULL $NULL {"Width":"100px"})
                #macroInputSwitch("userentity.confirmed")

                ## stop user from disabling themself
                #if(!$Utils.doStringsMatch($UserManager.getCurrentUser().getEmail(), $userentity.email))
                    #macroInputSwitch("userentity.disabled")
                #end
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()

    #set($RightButtons = [{"id":"resetLogin","icon":"fa-unlock","label":"userentity.resetlogin.button","onClick":"userProfileUtils.resetLogin"}])
    #set($EndFormProperties = {"saveAndCloseButton":{},"rightButtons":$RightButtons})

#macroEndForm($EndFormProperties)

<script>


    $(document).ready(function () {
        $("#privilegeGrid").kendoGrid({});
    });

    var userProfileUtils = {

        /**
         * In the call to the kendoValidator we get the event when the form is validated.
         * When the validation is successful, just before submitting the form to the server
         * the code checks for a function called onValidationSuccessful and, if present,
         * calls it.
         *
         * Here, just before submission, we want to encrypt any non-empty passwords. We encrypt both
         * of them as they will both contain the password as plaintext.
         */
        encryptData: function () {
            // loop through all password inputs
            $("[type=password]").each(function (index) {
                // get original value
                var value = $(this).val();
                if (value != "") {
                    // encrypt and base64-encode
                    var sessionId = PivotalUtils.getCookie("JSESSIONID");
                    var encrypted = PivotalUtils.xorEncode(value, sessionId);
                    var encoded = PivotalUtils.base64Encode(encrypted);
                    // set the value back
                    $(this).val(encoded);
                }
            });
        },

        resetLogin: function() {
            if (confirm("#I18N("userentity.resetlogin.confirm")")) {

                var userEmail =  $("#userentity\\.email").val();

                if ((userEmail && userEmail !== "") || confirm("#I18N("userentity.resetlogin.noemail_confirm")")) {

                    $("#userentity\\.loginFailCount").val("0");
                    $("#userentity\\.expires").val("");
                    $("#userentity\\.validFrom").val("");
                    $("#userentity\\.confirmed").data("kendoMobileSwitch").check(true);
                    $("#userentity\\.disabled").data("kendoMobileSwitch").check(false);

                    if (userEmail && userEmail !== "") {
                        psApp.workflow.execute("$AppPath", "PASSWORD_RESET_REQUEST", "email=" + userEmail);
                        PivotalUtils.showNotification('#I18N("userentity.resetlogin.email_sent")');
                    }

                    $("#submit").click();
                }
            }
        }
    }
</script>
