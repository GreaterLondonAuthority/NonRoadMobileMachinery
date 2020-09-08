#parse("$PageName/macros.inc")

#macroStartStatusTable("status.system.environment.title")
    #macroShowStatusTableRow("status.system.classpath" $Management.ClassPath)
    ###macroShowStatusTableRow("status.system.boot_classpath" $Management.BootClassPath.replaceAll("([,;:])","$1 "))
    #macroShowStatusTableRow("status.system.library_path" $Management.LibraryPath.replaceAll("([,;:])","$1 "))

    #foreach ($Item in $SystemProperties.keySet())
        #macroShowStatusTableRowNoTranslation($utils.justifyLeft($Item,50,"<br>") $SystemProperties.get($Item).replaceAll("([,;:])","$1 "))
    #end
#macroEndStatusTable()

