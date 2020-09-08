#macroGrid("data-grid" $Config {"buttons": [
    {"tooltip":"admin.report.action.edit",
     "imageClassName":"fa fa-table",
     "callback":"reportControls.editReport"
    },
    {"tooltip":"admin.report.action.run",
     "imageClassName":"fa fa-play-circle-o",
     "callback":"reportControls.launchReport"
    }
]})

<script>
    var reportControls = {

        id:false,

        /**
         * Edit the content of the report
         * @param id The ID of the task
         */
        editReport: function(id) {
            psApp.windowManager.open({title:"#I18N("scheduledtaskentity.report.editor.title")", width:"80%", height:"80%", content:"$AppPath/$PageName/report/taskreport?id=" + id, useIFrame:true, customActions:[
                                    "Maximize","Refresh","Close",
                                    {
                                        name:"pencil",
                                        handler: function(modalWindow) {
                                            var helper = this.window.MediaEditorHelper;
                                            PivotalUtils.getContent("$AppPath/$PageName/report/edit", "id=" + helper.contentId + "&fileString=" + encodeURIComponent(helper.getContent()), true);
                                            PivotalUtils.heartbeat("$AppPath");
                                            psApp.windowManager.close();
                                        }
                                    },
                                    {
                                        name:"save",
                                        handler: function(modalWindow) {
                                            var helper = this.window.MediaEditorHelper;
                                            PivotalUtils.getContent("$AppPath/$PageName/report/edit", "id=" + helper.contentId + "&fileString=" + encodeURIComponent(helper.getContent()), true);
                                            PivotalUtils.heartbeat("$AppPath");
                                            return false;
                                        }
                                    }
                                ]});
            return false;
        },

        /**
         * Launch the report manually
         * @param id The ID of the task
         */
        launchReport: function(id) {
            var results = PivotalUtils.getJsonContent("$AppPath/$PageName/$SubPageName/launch", "id=" + id);
            if (results.inError) {
                alert(results.error)
            }
            else {
                PivotalUtils.heartbeat("$AppPath");
                $('#data-grid').data('kendoGrid').dataSource.read();
                $("#data-grid").data("kendoGrid").refresh();
            }
            return false;
        }
    }
</script>
