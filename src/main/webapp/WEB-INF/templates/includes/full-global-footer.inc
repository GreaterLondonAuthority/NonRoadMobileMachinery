<script>
    $(document).ready(function() {
        /**
         * Listen for when the main sidebar menu is toggled open and closed and save the setting as a user preference.
         * */

        $('#menu-btn').click(function(event) {
            var result = $('body').toggleClass('minified');

            try {
                if ($("#data-grid"))
                    $("#data-grid").data("resizer").setTimer();
            }
            catch(X) {}

            PivotalUtils.getContentAsync('$AppPath/admin/user/prefs/save', 'minified=' + result.hasClass('minified'));
        });

        /**
         * Listen for when the search bar is used
         * */
        $( '.search input[type="search"]' ).keydown( globalSearch );

        $("#searchDocuments").change(function(event) {
        var value = ($("#searchDocuments")[0].checked)?"1":"0";
            PivotalUtils.getContent("$AppPath/$PageName/preferences/save", "searchDocuments=" + value, true);
        });

        /**
         * Listen for when the top menu is toggled open and closed and save the setting as a user preference.
         * */
        $('#menu-btn-top').click(function(event) {
            $('#pgcontainer').css('background-color', $('.body').css('background-color'))
            PivotalUtils.getContentAsync('$AppPath/admin/user/prefs/save', 'top-minified=' + $('.menu').hasClass('active'));
            setTimeout(function() {
                $(window).resize();
            },1000);
        });
    });


function globalSearch(event) {
   if ( event.which == 13 && $(this).val() !== '') {
       var tmp = PivotalUtils.getJsonContent("$AppPath/admin/search/test", "searchdocuments=" + (($("#searchDocuments")[0].checked)?"1":"0") + "&criteria=" + $(this).val())
       if (tmp.inError) {
           alert(tmp.error);
           $(this).focus();
       }
       else if (tmp.count == 0) {
           alert("Nothing found")
           $(this).focus();
       }
       else {
           psApp.windowManager.open({title:"#I18N("globalsearch.results.title")", width:"70%", height:"70%", content:"$AppPath/admin/search/results?searchdocuments=" + (($("#searchDocuments")[0].checked)?"1":"0") + "&criteria=" + escape($(this).val()), useIFrame: true});
           PivotalUtils.getContentAsync("$AppPath/admin/search/prefs/session/save", "globalsearch.criteria=" + $(this).val());
           PivotalUtils.getContentAsync("$AppPath/admin/search/history/save", "criteria=" + $(this).val(), function() {
           });
       }
       return false;
   }
}

</script>
<div id="c-mask" class="c-mask"></div>
#macroFooter()
