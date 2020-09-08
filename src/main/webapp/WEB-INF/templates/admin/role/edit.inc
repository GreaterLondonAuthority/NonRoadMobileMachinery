#macroEditHeading("userentity.id" "admin.role.add" "admin.role.edit")
#macroStartForm("roleentity" "$AppPath/$PageName/$SubPageName/edit"  {"onSubmit":"roleUtils.checkSubRoles"})

    #macroTabButtons([
        "admin.role.edit"
        ,"admin.role.access"
    ])

    #macroStartTabs()
        #macroStartTab("admin.role.edit", true)
            #macroStartFieldset("admin.role.general")
                #macroInputHidden("roleentity.id")
                #macroInputText("roleentity.name" $NULL $NULL {"focus":true})
                #macroInputHidden("clearafterupdate")
                #macroInputTextArea("roleentity.description")

                #set($AddNewLink="")
                #if($CurrentUser.hasAccess($PRIVILEGE_ROLE_TYPE_MANAGEMENT))
                    #set($AddNewLink = "$AppPath/admin/role_type/edit")
                #end

                #set($ExtraFilter = "")
                #macroSetExtraFilter("not searchEntity.disabled=true" "or searchEntity.id=$!roleentity.type.id" [$roleentity,$roleentity.type])
                #macroInputSelect("roleentity.type" $NULL $NULL "$AppPath/$PageName/$SubPageName/display/RoleTypeEntity" {"IsRequired":false,"ShowEmptyItem":true,"Tooltip":"roleentity.choose.type","ExtraFilter":"$!ExtraFilter","AddNewLink":"$AddNewLink","Width":"455px"})

                #macroInputSwitch("roleentity.disabled")
            #macroEndFieldset()
        #macroEndTab()

        #macroStartTab("admin.role.access")
            #macroStartFieldset("admin.role.access")

            <div style="overflow: auto">
            <table id="privilegeGrid">
                <colgroup>
                    <col/>
                    <col style="width:75px"/>
                    <col style="width:75px"/>
                </colgroup>
                <thead>
                    <tr>
                        <th class="privilegeTitle" data-field="privilege">#I18N("admin.privilegeAccess.privilege")</th>
                        <th>#I18N("admin.privilegeAccess.no.name")</th>
                        <th>#I18N("admin.privilegeAccess.yes.name")</th>
                    </tr>
                </thead>
                <tbody>
                    #set($AllPrivileges = $Privileges.getGroupedSortedList())
                    #set($rolePrivilegeAccessMap = $UserManager.getPrivilegeAccessMap($roleentity.privileges, $roleentity.privilegeAccess))

                    #foreach($PrivSection in $AllPrivileges.keySet())
                        <tr>
                            <td><b>$PrivSection</b></td>
                            <td/>
                            <td/>
                        </tr>
                        #foreach($Privilege in $AllPrivileges.get($PrivSection))
                            <tr>
                                #set($CurrentVal = $rolePrivilegeAccessMap.containsKey($Privilege.name))
                                <td class="privilegeLabel">$Privilege.label</td>
                                <td>
                                    <input type="radio" name="access_${Privilege.id}" id="${Privilege.id}_no" value="no"
                                        #if(!$CurrentVal)checked="true"#end
                                    >
                                </td>
                                <td>
                                    <input type="radio" name="access_${Privilege.id}" id="${Privilege.id}_yes" value="yes"
                                           #if($CurrentVal)checked="true"#end
                                    >
                                </td>
                            </tr>
                        #end
                    #end
                </tbody>
            </table>
            </div>
            #macroEndFieldset()
        #macroEndTab()
    #macroEndTabs()
#macroEndForm({"saveAndCloseButton":{}})



<script>
    $(document).ready(function() {
        $("#privilegeGrid").kendoGrid({
            height: 520,
            sortable: false
        });
    });

    var roleUtils = {

        editUser: function (userId) {
            psApp.windowManager.open({
                title: "$Utils.formatString("#I18N('system.grid.command.edit.title')", " User")",
                content: "$AppPath/$PageName/user/edit?id=" + userId,
                useIFrame: true
            });
        },

        checkSubRoles: function() {

            var returnValue = false;

            var results = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/checksubroles", "id=" + $("#roleentity\\.id").val() + "&subroles=" + $("#roleentity\\.subRoles").val(), true);
            if (results && results.inError)
                alert(results.error);
            else
                returnValue = true;

            return returnValue;
        }
    }

</script>
