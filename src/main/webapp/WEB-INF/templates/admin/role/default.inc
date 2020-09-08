#macroGrid("data-grid" $Config
 { "hidedelete" : true,
   "buttons": [
    {"tooltip":"system.grid.command.delete",
     "imageClassName":"fa fa-trash-o",
     "noInternal" : true,
     "callback":"roleControls.deleteval"
    }
]})

<script>
    var roleControls = {
        deleteval: function(id) {
            var response = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/action", "id=" + id);
            if (response.inError) {
                alert(response.error);
              }
            else {
                if(response.information != ""){
                  var temp =  "#I18N("system.grid.command.role.delete.confirm")[" + response.information + "] ?" ;
                    if(confirm(temp)) {
                        PivotalUtils.getContent('$AppPath/$PageName/$SubPageName/delete?id=' + id);
                        PivotalUtils.heartbeat("$AppPath");
                        $("#data-grid").data("kendoGrid").dataSource.read();
                    }
                }
                else if(confirm("#I18N("system.grid.command.delete.confirm")")) {
                    PivotalUtils.getContent('$AppPath/$PageName/$SubPageName/delete?id=' + id);
                    PivotalUtils.heartbeat("$AppPath");
                    $("#data-grid").data("kendoGrid").dataSource.read();
                }

            }
        }
    }
</script>
