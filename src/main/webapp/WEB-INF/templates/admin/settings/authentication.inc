#parse("$PageName/$SubPageName/macros.inc")

<div class="body-padding">

#macroStartForm("settings" "$AppPath/$PageName/$SubPageName" {"noAutoComplete":true})

##  Added these two inputs just to prevent auto fill for the user/name fields as chrome
##  seems to ignore the autocomplete tag
##  Added ignore attribute so the auto validation doesn't expect the passwords to be the same
    <input style="display:none" ignore="true" type="text" name="fakeusernameremembered"/>
    <input style="display:none" ignore="true" type="password" name="fakepasswordremembered"/>

    #macroTabButtons([
            "admin.settings.authentication.type.title",
            "admin.settings.authentication.ldap.title",
            "admin.settings.authentication.saml.title"
            ])

    #macroStartTabs()

    #set ($AuthTypes = [{"value" : "none", "text" : "admin.settings.authentication.none" },
                        {"value" : "simple", "text" : "admin.settings.authentication.simple" },
                        {"value" : "adfs", "text" : "admin.settings.authentication.adfs" },
                        {"value" : "ldap", "text" : "admin.settings.authentication.ldap" },
                        {"value" : "saml", "text" : "admin.settings.authentication.saml" }
                        ])

    #macroStartTab("admin.settings.authentication.type.title", true)
        #macroStartFieldset("admin.settings.authentication.type.title")
            #macroShowSettingsInput("r" ,$SETTING_AUTHENTICATION_TYPE, $SETTING_AUTHENTICATION_TYPE, $AuthTypes {"Width":"400px"})
            #macroShowSettingsInput("c" ,$SETTING_LOGINASOTHERUSER_ENABLED)
        #macroEndFieldset()
    #macroEndTab()

    #macroStartTab("admin.settings.authentication.ldap.title")
        #macroStartFieldset("admin.settings.authentication.ldap.title" "" "" "<button id='adminSettingsSetLdapDefaults' class='adminSettingsSetDefaults k-button' title='#I18N('system.settings.authentication.set.ldap')'><i class='fa fa-bullseye'></i></button>")
            #macroShowSettingsInput("s" ,$SETTING_LDAP_HOST)
            #macroShowSettingsInput("t" ,$SETTING_LDAP_PRINCIPLE_DN)
            #macroShowSettingsInput("p" ,$SETTING_LDAP_PRINCIPLE_PASSWORD)
            #macroShowSettingsInput("t" ,$SETTING_LDAP_USER_SEARCH)
        #macroEndFieldset()
	#macroEndTab()

    #macroStartTab("admin.settings.authentication.saml.title")
        #macroStartFieldset("admin.settings.authentication.saml.title" "" "" "<button id='adminSettingsSetSamlDefaults' class='adminSettingsSetDefaults k-button' title='#I18N('system.settings.authentication.set.saml')'><i class='fa fa-bullseye'></i></button>")
            #macroShowSettingsInput("s" ,$SETTING_SAML_IDP)
            #macroShowSettingsInput("c", $SETTING_SAML_TYPE)
            #macroShowSettingsInput("s", $SETTING_SAML_PROVIDER_NAME)
            #macroShowSettingsInput("s", $SETTING_SAML_AUTH_ID)
            #macroShowSettingsInput("s", $SETTING_SAML_CERTIFICATE)
        #macroEndFieldset()
	#macroEndTab()

	#macroEndTabs()

    #macroButtonSubmit("submit" "global.button.save")

#macroEndForm({"saveAndCloseButton":{}})


</div>

<script type="application/javascript">
    $(".adminSettingsSetDefaults").click(function() {
        if (this.id=="adminSettingsSetLdapDefaults") {
            $('[id="$SETTING_LDAP_HOST"]').val('pivotal-solutions.co.uk');
            $('[id="$SETTING_LDAP_PRINCIPLE_DN"]').val('test@pivotal-solutions.co.uk');
            $('[id="$SETTING_LDAP_PRINCIPLE_PASSWORD"]').val('');
            $('[id="$SETTING_LDAP_USER_SEARCH"]').val('CN=Users,DC=pivotal-solutions,DC=co,DC=uk??sub?(sAMAccountName=%user_uid%)');
        }
        else {
            $('[id="$SETTING_SAML_IDP"]').val('https://idp.ssocircle.com:443/sso/SSORedirect/metaAlias/ssocircle');
            $('[id="$SETTING_SAML_PROVIDER_NAME"]').val('https://idp.ssocircle.com');
            $('[id="$SETTING_SAML_AUTH_ID"]').val('nrmm');
            $('[id="$SETTING_SAML_CERTIFICATE"]').val('');
        }
    });
</script>
