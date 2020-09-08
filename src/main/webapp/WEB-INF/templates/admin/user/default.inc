#set($buttons = [])
#set($tmp=$buttons.add({"tooltip":"admin.user.button.preferences","imageClassName":"fa fa-wrench","callback":"userGridUtils.editPreferences"}))
#macroGrid("data-grid" $Config { "PopupWidth" : "900px", "buttons":$buttons})

<script>
var userGridUtils = {
    editPreferences: function (userId) {

        var fullPath = "$AppPath/$PageName/$SubPageName/preferences/" + userId;
        var settings = {};
        psApp.popups.putSetting(settings, "title", "Edit User Preferences", false);
        psApp.popups.putSetting(settings, "width", "90%", false);
        psApp.popups.putSetting(settings, "height", "90%", false);

        psApp.popups.openModalWindow(fullPath, settings);

    }
}
</script>
