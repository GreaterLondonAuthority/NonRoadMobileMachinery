#if ($CurrentUser)
<nav class="navbar navbar-expand-sm bg-light navbar-light">
    <div class="">
        <ul class="navbar-nav list-unstyled">
            #if ("$!PageToDisplay" != "siteregister" && $CurrentUser.hasAccess($PRIVILEGES_VIEW_SITE_REGISTER))
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/dashboard/register/site">
                        <i class="fa fa-search"></i>
                        #I18N('site.view.register.title')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            #if ("$!PageToDisplay" != "machineryregister" && $CurrentUser.hasAccess($PRIVILEGES_VIEW_MACHINERY_REGISTER))
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/dashboard/register/machinery">
                        <i class="fa fa-search"></i>
                        #I18N('machinery.view.register.title')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            #if ("$!PageName/$!SubPageName" != "site/register" && $CurrentUser.hasAccess($PRIVILEGES_REGISTER_SITE))
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/site/register">
                        <i class="fa fa-plus"></i>
                        #I18N('site.register.title')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            #if ($CurrentUser.hasAccess($PRIVILEGES_HAS_ADMIN_ACCESS))
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/admin" title="Admin">
                        <i class="fa fa-cog"></i>
                        #I18N('menu.toolbar.settings')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            #if ($CurrentUser.hasAccess($PRIVILEGES_PERFORMANCE_ADMIN))
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/status/jvm">
                        <i class="fa fa-tachometer-alt"></i>
                        #I18N('menu.toolbar.system')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            #if("$!PageName/$!SubPageName" != "profile/edit")
                <li class="nav-item">
                    <a class="nav-link" href="$AppPath/profile/edit">
                        <i class="fa fa-user"></i>
                        #I18N('profile.edit.pagetitle')
                        <i class="fa fa-angle-right"></i>
                    </a>
                </li>
            #end

            <li class="nav-item">
                <a class="nav-link" href="$AppPath/login/logout">
                    <i class="fa fa-sign-out-alt"></i>
                    #I18N('menu.toolbar.signout')
                    <i class="fa fa-angle-right"></i>
                </a>
            </li>
        </ul>
    </div>
</nav>
#end
