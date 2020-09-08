<div class="body">
    #if ("$!SubPageName" == "")
        <script>window.location.href="$AppPath/$PageName/$SystemNav[0].SectionLinks[0].Href"</script>
    #elseif ("$ActionPageName" != "")
        #parse("$LoadPageName/$SubPageName/${ActionPageName}.inc")
    #else
        #parse("$LoadPageName/$SubPageName/$DefaultContentInclude")
    #end
</div>