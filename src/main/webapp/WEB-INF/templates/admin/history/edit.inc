
<table class="formHeader">
    <tr>
        <th nowrap>Entity:&nbsp;</th>
        <td width="100%">$Entity.tableAffected version $Entity.id</td>
    </tr>
    <tr>
        <th nowrap>Action:&nbsp;</th>
        <td width="100%">$Entity.changeType</td>
    </tr>
    <tr>
        <th nowrap>Change Date:&nbsp;</th>
        <td>$Utils.dateFormat($Entity.timeAdded, "E d MMMM yyyy HH:mm:ss")</td>
    </tr>
    <tr>
        <th nowrap>Author:&nbsp;</th>
        <td>$Entity.userFullName</td>
    </tr>
</table>

<table class="historyView">

    ## Now output the rows
    #set ($Fields = $Utils.splitToList($Entity.previousValues,"(^@)|(\n@)"))

    #foreach($Field in $Fields)
        #if ($velocityCount>1)
            #set ($Parts=$Field.split(": *",2))
            #set ($FieldName = $Parts.get(0))
            #set ($FieldValue = "")
            #if ($Parts.size()>1)
                #set ($FieldValue = $Parts.get(1))
            #end
            #if ($FieldValue!="")
                <tr>
                    <th nowrap>$FieldName&nbsp;</th>
                    <td width="100%">
                        $Utils.encodeHTML($FieldValue).replaceAll(" ","&nbsp;")
                    </td>
                </tr>
            #end
        #end
    #end
    
</table>
