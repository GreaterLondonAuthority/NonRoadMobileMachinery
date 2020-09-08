
#macroGrid("history-grid" $Config {
    "source" : "$AppPath/$PageName/$SubPageName/historygrid?entity=$RefEntityTableName&id=$RefId",
    "readonly" : true, "buttons": [
    {"tooltip":"history.action.compare",
     "condition":"",
     "imageClassName":"fa fa-columns",
     "callback":"historyManager.compare"
    }
]})
##,{"tooltip":"history.action.revert",
## "condition":"!data.revert",
## "imageClassName":"fa fa-retweet",
## "callback":"historyManager.revert"
##}

<script>

    historyManager = {
        compare : function() {
            var id = $(this).closest("tr").next().find(".fa-columns").parent().attr("entity-id");
            if(id){
                psApp.windowManager.open({title:"#I18N('history.action.compare.title')", width:"$AdminPopupWidth", height:"$AdminPopupHeight", content:"$AppPath/$PageName/$SubPageName/compare?first=" + id, useIFrame:true, function() {
                    PivotalUtils.heartbeat("$AppPath");
                    $("#history-grid").data("kendoGrid").dataSource.read();
                }});
            }
        },
        revert : function(){
            var id = $(this).closest("tr").next().find(".fa-retweet").parent().attr("entity-id");
            PivotalUtils.showConfirmationDialog(
                "revert_convert",
                "Revert", {
                    messageText: "Are you sure you want to revert to v" + id + "?",
                    confirmText: "Revert",
                    cancelText: "Cancel",
                    showCancelButton: true,
                    confirmFunction: function () {
                        var status = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/revert?id=" + id);
                        if (status.inError) {
                            PivotalUtils.showNotification("Failed to revert","error",{});
                            if(console) console.error("Failed to revert : " + status.error,"error",{});
                        }
                        else {
                            PivotalUtils.showNotification("Reverted Successfully","info");
                            PivotalUtils.heartbeat("$AppPath");
                            $("#history-grid").data("kendoGrid").dataSource.read();
                        }
                    }.bind(this),
                    cancelFunction: function() {
                    }
                }
            );
            return false;
        }

    }

    $(document).ready(function () {
        var grid = $("#history-grid").data("kendoGrid");
        grid.bind("dataBound", function () {
            //Hide the actions of the last version. the datasource is ordered so hide it from the last row
            $("#history-grid").find("tr").last().children().last().find("a").hide();
        });
    });


</script>

