#parse("$PageName/macros.inc")

#macroStartStatusTable("status.servlet.container.title")
    #macroShowStatusTableRow("status.servlet.container" $Application.ServerInfo)
    #macroShowStatusTableRow("status.servlet.api" "v${Application.MajorVersion}.$Application.MinorVersion")
#macroEndStatusTable()

#macroStartStatusTable("status.servlet.environment.title")
    #foreach ($Item in $ApplicationAttributes.keySet())
        #set($Value=$ApplicationAttributes.get($Item))
        #if ("$Value.Class.SimpleName"=="File")
        #elseif ("$Value.Class.SimpleName"=="String")
            #set($Value = $Value.replaceAll("([,;:])","$1 "))
        #elseif ("$Value.Class.SimpleName"=="String[]")
            #set($Value = $utils.join($Value,"; "))
        #elseif ("$Value.Class.SimpleName"=="Boolean")
        #else
            #set($Value = $Value.Class)
        #end
        #macroShowStatusTableRowNoTranslation($utils.justifyLeft($Item,50,"<br>") $Value)
    #end
#macroEndStatusTable()
