#macroStartForm("scheduledtaskentity" "$AppPath/$PageName/$SubPageName/refreshoption" )
    #macroStartFieldset("admin.log.datarange.title")
        #set($DateRange= [{"value":"1","text":"Today"},{"value":"2","text":"This Week"},{"value":"3","text":"This Month"}, {"value":"4","text":"This Year"}])
        #macroInputSelect("logentity.range" $NULL $NULL $DateRange {"Placeholder":"logentity.choose.range","onChange":"logTaskHandler.changeDateRange"})
     #macroEndFieldset()
     #macroGrid("data-grid" $Config { "readonly" : "true"})
#macroEndForm()


<script>
    var logTaskHandler = {

        ## Called when range  is changed it will filter the data based on that
        changeDateRange: function(el) {
            refreshTable = function(){
                $('#data-grid').data('kendoGrid').dataSource.read();
                $('#data-grid').data('kendoGrid').refresh();
            }
            PivotalUtils.getContentAsync("$AppPath/$PageName/$SubPageName/refreshoption", "daterange="+ el.val(),refreshTable)
        },

        ## Called first time page load to clear the datarange in session
        change: function() {
            PivotalUtils.getContentAsync("$AppPath/$PageName/$SubPageName/refreshoption" )
        }
    }
    logTaskHandler.change();
</script>

