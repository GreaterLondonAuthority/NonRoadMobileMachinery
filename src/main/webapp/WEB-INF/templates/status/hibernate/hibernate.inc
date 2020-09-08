<button id='resetStats' class="k-button" title='#I18N("status.hibernate.stats.reset.tooltip")'>#I18N("status.hibernate.stats.reset")</button>
<button id='enableStats' class="k-button" title=''></button>

<div id="hibernateStats"></div>

<script type="application/javascript">

    var statusHibernateActions = {
        statsEnabled: #if($Statistics.StatisticsEnabled)true#{else}false#end,

        #**
         * Initialises the refreshing of the stats
         *#
        init: function() {
            setInterval(statusHibernateActions.redraw, 10000);
            statusHibernateActions.redraw();

            $("#resetStats").click(function () {
                PivotalUtils.getContentAsync("$AppPath/$PageName/hibernate/reset", "" , function() {
                    statusHibernateActions.redraw();
                });
            });

            $("#enableStats").click(function () {
                statusHibernateActions.statsEnabled = !statusHibernateActions.statsEnabled;
                PivotalUtils.getContentAsync("$AppPath/$PageName/hibernate/stats", "enable=" + (statusHibernateActions.statsEnabled?"true":"false") , function() {
                    statusHibernateActions.redraw();
                });
            });
        },

        #**
         * Redraws the stats from the server
         *#
        redraw: function() {
            if (statusHibernateActions.statsEnabled) {
                $("#enableStats").attr('title','#I18N("status.hibernate.stats.disable.tooltip")');
                $("#enableStats").text('#I18N("status.hibernate.stats.disable")');
            }
            else {
                $("#enableStats").attr('title','#I18N("status.hibernate.stats.enable.tooltip")');
                $("#enableStats").text('#I18N("status.hibernate.stats.enable")');
            }
            $('#hibernateStats').html(PivotalUtils.getContent("$AppPath/$PageName/hibernate/statistics"))
        }
    }

    #**
     * Initialise the page
     *#
    $(document).ready(function() {
        statusHibernateActions.init();
    });

</script>

