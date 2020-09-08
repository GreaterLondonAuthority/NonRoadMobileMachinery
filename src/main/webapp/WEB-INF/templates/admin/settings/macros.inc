#**
 * Shows an input that is tied to a setting from the system
 * Takes care of the binding and transalation
 *
 * @param Type Type of input to use
 * @param Name Name and ID to give the input
 * @param Label Label to display - defaults to name
 * @param Options Any options required for radio/dropdowns
 * @param Properties Any properties to be passed to a select box
 *#
#macro(macroShowSettingsInput $Type $Name $Label $Options $Properties)
    #set ($Value = "$!Context.get($Name)")
    #set ($DefaultValue = "$!Context.get('${Name}_DEFAULT')")
    #if ($Value == "" && $DefaultValue != "")
        #set ($Value = $DefaultValue)
    #end

    #if(!$Properties)
        #set($Properties = {})
    #end

    #set($tmp=$Properties.put("Tooltip", "${Name}.description"))

    #if($Label)
        #set($InputLabel = $Label)
    #else
        #set($InputLabel = $Name)
    #end
    #if ($Type=="s")
        #macroInputText($Name, $InputLabel, $Value $Properties)
    #elseif ($Type=="t")
        #macroInputTextArea($Name, $InputLabel, $Value $Properties)
    #elseif ($Type=="p")
        #macroInputPassword($Name, $InputLabel, $Value)
    #elseif ($Type=="n")
        #macroInputNumber($Name, $InputLabel, $Value)
    #elseif ($Type=="c")
        #macroInputSwitch($Name, $InputLabel, $Value)
    #elseif ($Type=="r")
        #macroInputSelect($Name, $InputLabel, $Value, $Options, $Properties)
    #end
#end

#**
 * Displays a line of database information
 * @param Name I18N name to display
 * @param Value Value to show
 *#
#macro(macroShowDatabaseEntry $Name $Value)
    <tr>
        <td>#I18N($Name)</td>
        <td>$!Value</td>
    </tr>
#end

#**
 * Save the form values to the server using AJAX
 *#
<script type="application/javascript">
    $(document).ready(function() {
        $("#settings").submit(function(event) {
            event.preventDefault();
            var result = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName", $(this).serialize(), true);
            if (result.inError)
                alert(result.error);
            else
                alert('#I18N("admin.settings.action.save.successful")');
        });
    });
</script>