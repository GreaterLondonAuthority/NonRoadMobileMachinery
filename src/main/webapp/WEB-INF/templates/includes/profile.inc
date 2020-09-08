#if ($displayGlobalHeader)
<div class="profile">
    <div>
##        <a class="tooltipRequired" style="display:block" id="editProfile" href="javascript:void(0)" title="#I18N("mainnav.profile.edit")" tooltip="#I18N("mainnav.profile.edit")" data-role="tooltip">
            <i class="fa fa-user" id="profile_icon"></i>
            <span style="margin-left: 4px">$!CurrentUser.Name</span>
##        </a>
    </div>
    <div>
        <a class="tooltipRequired" href="$AppPath/logout" title="#I18N("mainnav.logout")" tooltip="#I18N("mainnav.logout")" data-role="tooltip">
            <i class="fa fa-sign-out" id="logout_icon"></i>
            <span style="margin-left: 4px">#I18N("mainnav.logout")</span>
        </a>
    </div>
</div>

<script>
/**
 * Launches the page for a User to edit his profile in a modal window.
 */
$("#editProfile").click(function(event) {
    event.stopPropagation();
    psApp.windowManager.open({title:"#I18N("profile.edit.title")", width:700, height:600, content:"$AppPath/profile/edit", useIFrame:true});
})
</script>
#end
