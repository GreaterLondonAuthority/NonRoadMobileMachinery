<div id="threadList"></div>

<script type="application/javascript">

    var statusThreadsActions = {

        init: function() {
            setInterval(statusThreadsActions.redraw, 10000);
            statusThreadsActions.redraw();
        },

        redraw: function() {
            $('#threadList').html(PivotalUtils.getContent("$AppPath/$PageName/threads/list.vm"))
            $(".actionButton").click(function () {
                var action = $(this);

                // Kill the task

                if (action.attr('actionType')=="killTask")
                    statusThreadsActions.killTask(action);

                // Kill the thread

                else if (action.attr('actionType')=="killThread")
                    statusThreadsActions.killThread(action);
            });

            $(".showStackTrace").kendoTooltip({
                iframe: true,
                content: function(e) {
                    return PivotalUtils.getContent("$AppPath/$PageName/threads/stacktrace.vm", "id=" + $(e.target).attr('threadId'));
                }
            });
            PivotalUtils.showFormTooltips("#threadList")
        },

        killTask: function(button) {
            if (confirm('#I18N("status.threads.killtask.brutal")\n\n' + button.attr('threadName') + '\n\n#I18N("status.threads.are_you_sure")\n')) {
                document.body.style.cursor="progress";
                PivotalUtils.getContentAsync("$AppPath/task/killtask", "id=" + button.attr('taskId') + "&threadid=" + button.attr('threadId'), function(response) {
                    document.body.style.cursor="default";
                    alert('#I18N("status.threads.killtask.confirmation")');
                })
            }
        },

        killThread: function(button) {
            if (confirm('#I18N("status.threads.killthread.brutal")\n\n' + button.attr('threadName') + '\n\n#I18N("status.threads.are_you_sure")\n')) {
                document.body.style.cursor="progress";
                PivotalUtils.getContentAsync("$AppPath/$PageName/killthread", "id=" + button.attr('threadId'), function(response) {
                    document.body.style.cursor="default";
                    alert('#I18N("status.threads.killthread.confirmation")');
                })
            }
        }
    }

    #**
     * Initialise the page
     *#
    $(document).ready(function() {
        statusThreadsActions.init();
    });

</script>