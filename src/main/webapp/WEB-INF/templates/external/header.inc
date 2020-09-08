#parse("includes/initialise.inc")
#if($Utils.isBlank($PageTitle))
    #set($PageTitle = $WorkflowResult.getDataItem("PageTitle"))
#end
#if($Utils.isBlank($PageTitle))
    #set($PageTitle = $WordUtils.capitalize($PageName))
#end
#set($PageTitle = "$!PageTitle - NRMM | London City Hall")
##
#set($PageToDisplay = $WorkflowResult.getDataItem("PageToDisplay"))
#set($PageToDisplayDescription = $WorkflowResult.getDataItem("PageToDisplayDescription"))
#set($BreadCrumbList = $WorkflowResult.getDataItem("BreadCrumbList"))
#if(!$BreadCrumbList)
    #set($BreadCrumbList = [{"name":"NRMM","path":"$AppPath/dashboard"}])
#end
#set($LastBreadCrumb = $WorkflowResult.getDataItem("LastBreadCrumb"))
##
#set($IncludePath = "$AppPath/external")
##
#parse("external/template_header.inc")
##
<!-- ************************* -->
<!-- Start of Pivotal Template -->
<!-- ************************* -->
