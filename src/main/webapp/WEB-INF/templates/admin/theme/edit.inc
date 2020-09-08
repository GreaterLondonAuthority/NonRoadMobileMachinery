#parse("includes/initialise.inc")

#macroStartForm("theme-upload" "$AppPath/$PageName/$SubPageName/edit" {"EncType":"multipart/form-data","onSubmit":"themeEditUtils.fileCheck"})
    #macroStartFieldset("admin.theme.upload")
        #macroInputFile("file" "admin.theme.file" {"NoRemove":true,"IsRequired":false})
        #if ($Utils.isBlank($Themes))
            #macroInputText("name" "admin.theme.name" $Theme.Name {"IsRequired":true,"Placeholder":"admin.theme.name.tooltip"})
        #else
            #macroInputComboBox("" "" $SelectedTheme, [{
                "InputName":"name",
                "InputLabel":"admin.theme.name",
                "Placeholder":"admin.theme.name.tooltip",
                "Tooltip":"admin.theme.name.tooltip",
                "DataSource": $Themes,
                "CascadeFrom":""
            }] {"IsRequired":true,"Tooltip":"admin.theme.name.tooltip","Placeholder":"admin.theme.name.tooltip"})
        #end
        #macroInputTextArea("description" "admin.theme.description" $Theme.Description)
    #macroEndFieldset()
#macroEndForm({"saveAndCloseButton":{}})
<script>
    var themeEditUtils = {
        fileCheck : function() {
            var upload=$("#file").data("kendoUpload");
            var files = upload.getFiles();
            if (files.length < 1) {
                alert("#I18N("admin.theme.upload.missing.file")");
                return false;
            }
            else
                return true;
        }
    };
</script>