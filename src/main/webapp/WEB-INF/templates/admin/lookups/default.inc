#macroGrid("data-grid" $Config {
    "extraFilter":"type=\'$Request.getParameter('type')\'",
    "createCallback":"lookupsAdminUtils.openEdit"
})

<script>
var lookupsAdminUtils = {

    openEdit: function(id) {

        id = id?id:"";
        var readOnly = false;

        var settings = {};

        var fullPath = "$AppPath/$PageName/lookups/edit/" + id + "?type=$Request.getParameter('type')";

        // Set default dimensions
        psApp.popups.putSetting(settings, "height", "40%");
        psApp.popups.putSetting(settings, "width", "850px");
        psApp.popups.putSetting(settings, "name", "lookups-edit");
        psApp.popups.putSetting(settings, "title", readOnly == "1" ? "#I18N("admin.lookups.edit.title")" : (id=="" ? "#I18N("admin.lookups.edit.title")":"#I18N("admin.lookups.edit.title")"));
        psApp.popups.putSetting(settings, "readOnly", readOnly == "1");
        psApp.popups.putSetting(settings, "closeFunction", function() {
            PivotalUtils.heartbeat("$AppPath");
            $("$GridElement").data("kendoGrid").dataSource.read();
        });
        psApp.popups.openModalWindow(fullPath, settings);
    }
};
</script>
