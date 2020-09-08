#macroGrid("data-grid" $Config {"buttons": [
    {"tooltip":"admin.workflow.action.edit",
     "imageClassName":"fa fa-table",
     "callback":"workflowControls.editScript"
    }
]})

#macroInputHidden("workflow-edit-content")

<script>
    var workflowControls = {
        /**
         * Edit the content of the report
         * @param id The ID of the report
         */
        editScript: function(id) {
            workflowControls.id = id;
            psApp.windowManager.open({
                title:"#I18N("workflowentity.script.editor.title")",
                width:"80%",
                height:"80%",
                content:"$AppPath/$PageName/$SubPageName/script?id=" + id,
                useIFrame:true,
                customActions:[
                    "Maximize","Refresh",
                    {
                        name:"save",
                        handler: function(modalWindow) {
                            var helper = this.window.MediaEditorHelper;
                            PivotalUtils.getContent("$AppPath/$PageName/$SubPageName/edit", "id=" + helper.contentId + "&script=" + encodeURIComponent(helper.getContent()), true);
                            PivotalUtils.heartbeat("$AppPath");
                            return false;
                        }
                    },
                    "Close"
                ]

            });
            return false;
        }
    }
</script>
