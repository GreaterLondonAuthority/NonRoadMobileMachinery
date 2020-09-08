
##<div style="margin-left: 10px; margin-top: 10px">
##    <span class="insert" style="margin-right: 10px;padding: 4px">Insert</span>
##    <span class="delete" style="padding: 4px;margin-right:10px">Delete</span>
##    Comparing v$First.Id with
##    #if ($utils.parseInt($Second.Id) == 0)
##        v$Second.Id
##    #else
##        LATEST
##    #end
##</div>

<!--split for audit-->
<div class="table-responsive">
    <table class="table table-striped">
        <tbody>

        ## Now output the rows

        #if ("$!First.previousValues" == "")
            #set ($Fields = $Utils.splitToList($Changes,"(^<span [^>]>@)|(<br>@)"))
            #foreach($Field in $Fields)
                #set ($Parts=$Field.split(": *",2))
                #set ($FieldName = $Parts.get(0).replaceFirst("^@",""))
                #set ($FieldValue = "")
                #if ($Parts.size()>1)
                    #set ($FieldValue = $Parts.get(1))
                #end
                <tr>
                    <th class="text-nowrap">$FieldName</th>
                    <td width="100%">
                        <span class="insert">$FieldValue.replaceAll("&para;","")</span>
                    </td>
                </tr>
            #end
        #else
            #set ($Fields = $Utils.splitToList($Changes,"(^<span>@)|(<br>@)"))
            #foreach($Field in $Fields)
                #set ($Parts=$Field.split(": *",2))
                #set ($FieldName = $Parts.get(0).replaceFirst("^@",""))
                #set ($FieldValue = "")
                #if ($Parts.size()>1)
                    #set ($FieldValue = $Parts.get(1))
                #end
                <tr>
                    <th>$FieldName</th>
                    <td width="100%">
                        $FieldValue.replaceAll("&para;","")
                    </td>
                </tr>
            #end
        #end
        </tbody>
    </table>
</div>
<!--split for audit-->
