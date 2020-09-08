#macroGrid("data-grid" $Config {"buttons": [
    {"tooltip":"admin.report.action.edit",
     "imageClassName":"fa fa-table",
     "callback":"reportControls.editReport"
    }
]})

#macroInputHidden("report-edit-content")

<script>
    var reportControls = {

        id: false,

        /**
         * Edit the content of the report
         * @param id The ID of the report
         */
        editReport: function(id) {
            reportControls.id = id;
            psApp.windowManager.open({title:"#I18N("reportentity.filestring.editor.title")", width:"80%", height:"80%", content:"$AppPath/$PageName/$SubPageName/definition?id=" + id, useIFrame:true, customActions:[
                                    "Maximize","Refresh","Close",
                                    {
                                        name:"pencil",
                                        handler: function(modalWindow) {
                                            var content = this.window.MediaEditorHelper.getContent();
                                            PivotalUtils.getContent("$AppPath/$PageName/$SubPageName/edit", "id=" + reportControls.id + "&fileString=" + encodeURIComponent(content), true);
                                            PivotalUtils.heartbeat("$AppPath");
                                            psApp.windowManager.close();
                                        }
                                    }
                                ]});
            return false;
        }
    }
</script>
