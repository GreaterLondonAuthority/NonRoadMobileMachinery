<div class="body-padding">
    #if ("$!SubPageName" == "")
        <script>window.location.href="$AppPath/$PageName/$SystemNav[0].SectionLinks[0].Href"</script>
    #else
        #if ($SubPageName == "simulator")
            #if ("$ActionPageName" != "")
                #parse("$LoadPageName/$SubPageName/${ActionPageName}.inc")
            #else
                #parse("$LoadPageName/$SubPageName/overview.inc")
            #end
        #elseif ("$SubPageName" != "")
            #parse("$LoadPageName/$SubPageName/${SubPageName}.inc")
        #else
            #parse("$LoadPageName/$SubPageName/$DefaultContentInclude")
        #end
    #end
</div>

