#if ($displayGlobalHeader)
<div class="topbar">
    <script>
        $(document).ready(function () {

            $('.tooltipRequired').kendoTooltip({
                position: "right",
                width: 100,
                iframe: false,
                content: function (e) {
                    var target = e.target;
                    return $(target).attr("tooltip");
                }
            }).data("kendoTooltip");

            $("#search").change(function(e){
                PivotalUtils.getContentAsync("$AppPath/admin/search/prefs/session/save", "globalsearch.criteria=" + $(this).val());
                console.log(e);
            });

            $( "#search" ).keydown(function( event ) {
                if ( event.which == 13 && $(this).val() !== '') {
                    var tmp = PivotalUtils.getJsonContent("$AppPath/admin/search/numberofresults", "criteria=" + $(this).val())
                    if (tmp.inError) {
                        alert(tmp.error);
                        $(this).focus();
                    }
                    else if (tmp.count == 0) {
                        alert("Nothing found")
                        $(this).focus();
                    }
                    else {
                        psApp.windowManager.open({title:"#I18N("globalsearch.results.title")", width:"70%", height:"70%", content:"$AppPath/admin/search/results?criteria=" + $(this).val(), useIFrame: true});
                        PivotalUtils.getContentAsync("$AppPath/admin/search/prefs/session/save", "globalsearch.criteria=" + $(this).val());
                        PivotalUtils.getContentAsync("$AppPath/admin/search/history/save", "criteria=" + $(this).val(), function() {
                        });
                    }
                    return false;
                }
            });

            $('#search-input-group').on('show.bs.dropdown', function () {
                $.ajax({
                    url : '$AppPath/admin/search/history/get',
                    success : function(sArray) {
                        if (sArray && sArray.length > 0) {
                            $(".topbar-search .dropdown-menu-item").unbind();

                            var elements = [];
                            for (var i = 0; i < sArray.length; i++) {
                                elements.push($('<li />').append($('<a />', {
                                            href: '#',
                                            class: 'dropdown-menu-item',
                                            text: sArray[i]
                                        }))
                                );
                            }

                            $('#search-input-history').html(elements);

                            $(".topbar-search .dropdown-menu-item").click(function () {
                                $('#search').val($(this).html());
                                alert($("#search").val());
                                psApp.windowManager.open({title:"#I18N("globalsearch.results.title")", width:"70%", height:"70%", content:"$AppPath/admin/search/results?criteria=" + $("#search").val(), useIFrame: true});
                            });
                        } else {
                            $('#search-input-history').html($('<li />', { text: 'No previous searches', class: 'empty' }));
                        }
                    }
                });
            });
        });
    </script>
</div>

#end

