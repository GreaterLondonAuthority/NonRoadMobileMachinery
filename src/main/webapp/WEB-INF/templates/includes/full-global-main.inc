<div id="main">
    <header>
        #set($found=false)
        #set($pagetitle="")
        #set($pagesubtitle="")
        #set($pagelinks=false)
        #foreach($AdminNavSection in $SystemNav)
            #foreach($AdminNavLink in $AdminNavSection.SectionLinks)
                #set ($href = "$AdminNavLink.Href")
                #macroAdminHrefMatches($href)
                #if($AdminHrefMatches)
                ###if($Utils.doStringsMatch($href, $SubPageName, "$SubPageName/$ActionPageName"))
                    #set($found=true)
                    #set($pagetitle=$AdminNavSection.SectionTitle)
                    #set($pagelinks=$AdminNavSection.SectionLinks)
                #end
            #end
            #if($found)
                #break
            #end
        #end

        #foreach($AdminNavLink in $pagelinks)
            #macroAdminHrefMatches($AdminNavLink.Href)
            #if($AdminHrefMatches)
            ###if($Utils.doStringsMatch($AdminNavLink.Href, $SubPageName, "$SubPageName/$ActionPageName"))
                #set($pagesubtitle=$AdminNavLink.LinkText)
            #end
        #end

        <ol class="breadcrumb" style="float:left;">
            #if ($pagetitle != "")<li><a href="#">#I18N($pagetitle)</a></li>#end
            #if ($pagesubtitle != "")<li class="active">#I18N($pagesubtitle)</li>#end
        </ol>

        #set ($SelectedHref = "")

        <ul class="navigation">
            ## Find the last matching link
            #set ($SelectedHref = "")
            #set ($SelectedLabel = "")
            #foreach($AdminNavLink in $pagelinks)
                #macroAdminHrefMatches($AdminNavLink.Href)
                #if($AdminHrefMatches)
                ###if($Utils.doStringsMatch($AdminNavLink.Href, $SubPageName, "$SubPageName/$ActionPageName", "$SubPageName/$ActionPageName/$SubActionPageName"))
                    #set ($SelectedHref = $AdminNavLink.Href)
                #end
            #end
            #foreach($AdminNavLink in $pagelinks)
                <li
                    #if ($Utils.doStringsMatch($AdminNavLink.Href, $SelectedHref))
                            class="active"
                    #end
                        >
                    <a href="$AppPath/$PageName/$AdminNavLink.Href">#I18N($AdminNavLink.LinkText)</a>
                </li>
            #end
        </ul>
        <div style="clear:both;"></div>
    </header>

    #parse("$LoadPageName/main-content.inc")
</div>
