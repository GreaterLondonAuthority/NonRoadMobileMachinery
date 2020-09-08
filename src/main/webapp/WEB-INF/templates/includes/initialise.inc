## Useful shorthand for Velocity
#set ($LSQUARE_CHAR='[')
#set ($RSQUARE_CHAR=']')
#set ($LCURLY_CHAR='{')
#set ($RCURLY_CHAR='}')
#set ($DOT_CHAR='\.')
#set ($NEWLINE="
")
#set($HASH = "#")
#set($DOLLAR = "$")
#set($DQUOTE = '"')
#set($DEFAULT_DATE_FORMAT = "dd MMM yyyy")
##
## List of report types supported by the system
#set ($ReportTypesList=[{"value":"python","text":"admin.report.type.python"},
                        {"value":"sqldump","text":"admin.report.type.sql.dump"},
                        {"value":"velocity","text":"admin.report.type.velocity"}])
##
## List of distribution list types supported by the system
#set ($DistributionTypesList=[{"value":"email","text":"reporting.distribution_list.type.email"},
                              {"value":"emaillink","text":"reporting.distribution_list.type.email.link"},
                              {"value":"vfs","text":"reporting.distribution_list.type.vfs"},
                              {"value":"sms","text":"reporting.distribution_list.type.sms"},
                              {"value":"deadend","text":"reporting.distribution_list.type.deadend"}])
##
## Map of compression types
#set ($CompressionTypeList=[{"value":"gzip","text":"GZIP (.gz)"},
                            {"value":"compress","text":"ZLIB (.Z)"}])
##
## Map of priority types
#set ($PriorityTypeList=[{"value":"3","text":"reporting.distribution_list.priority.normal"},
                         {"value":"1","text":"reporting.distribution_list.priority.highest"},
                         {"value":"2","text":"reporting.distribution_list.priority.high"},
                         {"value":"4","text":"reporting.distribution_list.priority.low"},
                         {"value":"5","text":"reporting.distribution_list.priority.lowest"}])
##
## Map of importance types
#set ($ImportanceTypeList=[{"value":"normal","text":"reporting.distribution_list.importance.normal"},
                           {"value":"high","text":"reporting.distribution_list.importance.high"},
                           {"value":"low","text":"reporting.distribution_list.importance.low"}])
##
## Map of sensitivity types
#set ($SensitivityTypeList=[{"value":" ","text":"reporting.distribution_list.sensitivity.normal"},
                            {"value":"personal","text":"reporting.distribution_list.sensitivity.personal"},
                            {"value":"private","text":"reporting.distribution_list.sensitivity.private"},
                            {"value":"company-confidential","text":"reporting.distribution_list.sensitivity.confidential"}])
##
## Map of schedule types
#set ($ScheduleTypeList=[{"value":"0","text":"admin.scheduled_task.schedule.every.x.minutes"},
                         {"value":"1","text":"admin.scheduled_task.schedule.every.day"},
                         {"value":"2","text":"admin.scheduled_task.schedule.every.week"},
                         {"value":"3","text":"admin.scheduled_task.schedule.every.month"},
                         {"value":"4","text":"admin.scheduled_task.schedule.never"},
                         {"value":"5","text":"admin.scheduled_task.schedule.once"}])
##
## Map of day names
#set ($ScheduleDayList=[{"value":"1","text":"admin.scheduled_task.day.monday"},
                         {"value":"2","text":"admin.scheduled_task.day.tuesday"},
                         {"value":"3","text":"admin.scheduled_task.day.wednesday"},
                         {"value":"4","text":"admin.scheduled_task.day.thursday"},
                         {"value":"5","text":"admin.scheduled_task.day.friday"},
                         {"value":"6","text":"admin.scheduled_task.day.saturday"},
                         {"value":"7","text":"admin.scheduled_task.day.sunday"}])
##
#set ($CompressionList=$utils.getLinkedCaseInsensitiveMap($utils.sortMapByKey({"":"","gzip":"GZIP (.gz)","compress":"ZLIB (.Z)"})))
#set ($ReportTypes=$utils.getLinkedCaseInsensitiveMap($utils.sortMap({"Python" : "Python", "SQLDump" : "SQL Dump", "Velocity" : "Velocity"})))
#set ($PriorityList={"3":"Normal", "1":"Highest", "2":"High", "4":"Low", "5":"Lowest"})
#set ($ImportanceList={"normal":"Normal", "high":"High", "low":"Low"})
#set ($SensitivityList={"":"Normal", "personal":"Personal", "private":"Private", "company-confidential":"Confidential"})
#set ($ReportOutputTypes=$utils.getLinkedCaseInsensitiveMap($utils.sortMap({"":"","txt":"Plain Text","html":"HTML","xml":"XML","json":"JSON","pdf":"Adobe PDF","xls":"Excel (97-2007)","xlsx":"Excel (2010)","csv":"Delimited (comma)","tsv":"Delimited (tab)","rtf":"Rich Text Format (rtf)"})))
#set ($ScheduleTypes=$utils.sortMapByKey({"":"","0":"Every X Minutes","1":"Every Day","2":"Every Week","3":"Every Month","4":"Never","5":"Once"}))
##
##
## Sizes of the dialogs
#set($TaskWidth=510)
#set($TaskHeight=560)
#set($WebserviceTaskWidth=510)
#set($WebserviceTaskHeight=460)
#set($ListWidth=550)
#set($ListHeight=530)
#set($SourceWidth=500)
#set($SourceHeight=550)
#set($ReportWidth=420)
#set($ReportHeight=340)
#set($SettingsWidth=640)
#set($SettingsHeight=520)
#set($InfosourceWidth=540)
#set($InfosourceHeight=410)
#set($TransformerWidth=420)
#set($TransformerHeight=320)
#set($HistoryWidth=800)
#set($HistoryHeight=600)
#set($HistoryViewWidth=700)
#set($HistoryViewHeight=450)
##
#set($roleentity_type-EditHeight = 350)
#set($roleentity_type-EditWidth = 750)
#set($roleentity_type-Title = "roleentity.type.addnew.title")
##
## Work out some default values - these would normally come from the
## application but just in case they haven't been set
##
#set($FontSize=$Preferences.get("system.fontsize"))
#if ("$!FontSize"=="")
    #set($FontSize=16)
#end
#set($FontFace=$Preferences.get("system.fontfamily"))
#if ("$!FontFace"=="")
    #set($FontFace="Open Sans")
#end
#if ("$!HEARTBEAT_PERIOD_MILLISECONDS"=="")
    #set ($HEARTBEAT_PERIOD_MILLISECONDS = 60000)
#end
##
#set ($AppDate = $Utils.formatDate($APPLICATION_BUILD_DATE,"EEE, d MMM yyyy H:mm:ss"))
#if ("$!AppDate" == "")
    #set ($AppDate = "Dev")
#end
##
#if ($EditState.is("VIEWING"))
    #set ($ReadOnlyMode = true)
#else
    #set ($ReadOnlyMode = false)
#end
##
## Running values
## Check to see if we have already been in here or that these values have been created
## elsewhere already
#if ("$!PageName" == "")
    #set($DefaultContentInclude = "default.inc")
    #set($PageName="")
    #set($SubPageName="")
    #set($ActionPageName="")
    #set($SubActionPageName="")
    #set($AppPath="$!Request.getContextPath()")
    #set($Pages=$utils.splitToList($Request.RequestURI, "$AppPath/"))
    #if ($AppPath=="")
        #if ($Pages.size()>1)
            #set($PageName=$Pages.get(1).split(";").get(0))
            #if ($Pages.size()>2) #set($SubPageName=$Pages.get(2)) #end
            #if ($Pages.size()>3) #set($ActionPageName=$Pages.get(3)) #end
            #if ($Pages.size()>4) #set($SubActionPageName=$Pages.get(4)) #end
        #end
    #else
        #if ($Pages.size()>1)
            #set($Pages=$utils.splitToList($Pages.get(1), "/"))
            #set($PageName=$Pages.get(0).split(";").get(0))
            #if ($Pages.size()>1) #set($SubPageName=$Pages.get(1)) #end
            #if ($Pages.size()>2) #set($ActionPageName=$Pages.get(2)) #end
            #if ($Pages.size()>3) #set($SubActionPageName=$Pages.get(3)) #end
        #end
    #end
    ##
    ## This should be used when including other templates within a page. This allows a plugin to override the value to
    ## point to their load folder without it affecting any other page reference.
    #set($LoadPageName=$PageName)
    #set($Server=$utils.splitToList("$Request.requestURL", "$Request.requestURI").get(0))
    #set($Action=$Request.getParameter("action"))
    ##
#end
#set ($displayGlobalHeader = true)
#set($ReportParagraphNumber = 0)
## Ensure the global header setting isnt getting overwritten
#parse("includes/has-global-header.inc")
##
## Standard colours
##
#set($HIGHLIGHT_COLOUR = "#9e0059")
##
##
