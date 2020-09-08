<div id="poolStats"></div>

<script type="application/javascript">

    var statusPoolActions = {

        #**
         * Initialises the refreshing of the stats
         *#
        init: function() {
            setInterval(statusPoolActions.redraw, 10000);
            statusPoolActions.redraw();
        },

        #**
         * Redraws the stats from the server
         *#
        redraw: function() {
            $('#poolStats').html(PivotalUtils.getContent("$AppPath/$PageName/pool/statistics"))
            $(".formButton").click(function () {
                var type = $(this).attr('actionType');
                var poolName = $(this).attr('poolName');
                var poolCount = $(this).attr('poolCount');
                statusPoolActions[type](poolName, poolCount);
                statusPoolActions.redraw();
            });
        },

        #**
         * Kills the pool after checking with the user first
         * @param poolName Name of the pool to kill
         * @param activeConns Number of active connections in the pool
         *#
        killPool: function(poolName, activeConns) {
           var	confirmed = false;
           var force = false;
           if (activeConns == 0) {
               confirmed = window.confirm('#I18N("status.pool.kill.confirm")');
           }
           else{
               confirmed = window.confirm('#I18N("status.pool.kill.active.confirm")');
               force=true;
           }
           if (confirmed){
               PivotalUtils.getContentAsync("$AppPath/$PageName/pool/kill", "poolname=" + poolName + "&forcekill=" + force , function(response) {
                   var data = eval('(' + response.responseText + ')');
                   if (data.inError)
                       alert(data.error)
                   else
                       alert('#I18N("status.pool.kill.acknowledge")');
               });
           }
       },

        #**
         * Clears the pool of all connections
         * @param poolName Name of the pool to clear
         *#
       clearPool: function(poolName) {
           if (window.confirm('#I18N("status.pool.clear.confirm")')) {
               PivotalUtils.getContentAsync("$AppPath/$PageName/pool/clear", "poolname=" + poolName, function(response) {
                   alert('#I18N("status.pool.clear.acknowledge")');
               });
           }
       },

        #**
         * Clears the velocity cache of templates
         *#
       clearVelocityCache: function() {
           if (window.confirm('#I18N("status.cache.velocity.clear.confirm")')) {
               PivotalUtils.getContentAsync("$AppPath/$PageName/velocity/clear", null, function(response) {
                   alert('#I18N("status.cache.velocity.clear.acknowledge")');
               });
           }
       },

        #**
         * Clears the velocity cache stats
         *#
       clearVelocityCacheStats: function() {
           PivotalUtils.getContentAsync("$AppPath/$PageName/velocity/clearstats", null, function(response) {
               alert('#I18N("status.cache.velocity.clear.stats.acknowledge")');
           });
       },

        #**
         * Clears the velocity cache stats
         *#
        clearConnectionProviderStata: function() {
           PivotalUtils.getContentAsync("$AppPath/$PageName/connection/clearstats", null, function(response) {
               alert('#I18N("status.pool.connection.clear.stats.acknowledge")');
           });
       },

        #**
         * Clears the general purpose cache
         *#
       clearCache: function() {
           if (window.confirm('#I18N("status.cache.clear.confirm")')) {
               PivotalUtils.getContentAsync("$AppPath/$PageName/cache/clear", null, function(response) {
                   alert('#I18N("status.cache.clear.acknowledge")');
               });
           }
       },

        #**
         * Clears the general purpose cache stats
         *#
       clearCacheStats: function() {
           PivotalUtils.getContentAsync("$AppPath/$PageName/cache/clearstats", null, function(response) {
               alert('#I18N("status.cache.clear.stats.acknowledge")');
           });
       }
    }

    #**
     * Initialise the page
     *#
    $(document).ready(function() {
        statusPoolActions.init();
    });

</script>

