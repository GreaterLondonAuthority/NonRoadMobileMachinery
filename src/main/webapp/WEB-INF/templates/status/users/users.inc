#parse("$PageName/macros.inc")

#set ($Properties = {"buttons": [{"tooltip":"performance.users.logout",
                                "imageClassName":"fa fa-sign-out",
                                "callback":"userControls.signOut"
                                }
                                ]})
#set ($Actions = [])
    #foreach($button in $Properties.buttons)
        #set ($ToolTip = "#I18N($button.tooltip)")
        #if ("$!button.condition"!="")
            #set ($tmp = $Actions.add("# if ($button.condition) { #<a class='k-button k-button-icontext $!button.className' title='$ToolTip' href='javasctipt:void(0)' onclick='${button.callback}(#: id #);return false'><span class=' fa $!button.imageClassName'></span></a>#} #"))
        #else
            #if("$!button.link"!="")
                #set ($tmp = $Actions.add("<a  target='_blank' class='k-button k-button-icontext $!button.className' title='$ToolTip' href='$!button.link #: id #' ><span class=' fa $!button.imageClassName'></span></a>"))
            #else
                #set ($tmp = $Actions.add("<a class='k-button k-button-icontext $!button.className' title='$ToolTip' href='javasctipt:void(0)' onclick='${button.callback}(#: id #);return false'><span class=' fa $!button.imageClassName'></span></a>"))
            #end
        #end
    #end
#set ($ActionWidth = $Actions.size() * 142)
#set ($Actions = $Utils.join($Actions, " "))
#set ($Actions = "$DQUOTE<div style='white-space:nowrap'>$Actions</div>$DQUOTE")

<div id="example">
   <div id="grid"></div>
    <script>
       $(document).ready(function() {
           $("#grid").kendoGrid({
               dataSource: {
                   type: "json",
                   transport: {
                       read: "$AppPath/status/users/data"
                   },
                   schema: {
                       model: {
                           fields: {
                                  #foreach($Col in $UserStatusColumnsSelected)
                                      #if ($Col.class.simpleName == "LinkedHashMap")
                                          #set($FieldName = $Utils.getItem($Col, ":", 0))
                                      #else
                                          #set($FieldName = $Col.Field)
                                      #end
                                      $FieldName: { type: "string" },
                                  #end
                                  idle: { type: "string" }
                           }
                       }
                   },
               },
               height: 550,
               sortable: true,
               columnMenu: true,
               pageable: {
                       refresh: true,
                       #if ($!PageSize)
                               pageSize:$PageSize,
                       #else
                               pageSize:10,
                       #end
                       pageSizes: [10, 50, 100],
                       buttonCount: 5,
                       input: true,
                       messages: {
                           itemsPerPage: "#I18N("system.grid.page.items.per.page")",
                           next: "#I18N("system.grid.page.next")",
                           refresh: "#I18N("system.grid.page.refresh")",
                           morePages: "#I18N("system.grid.page.more.pages")",
                           previous: "#I18N("system.grid.page.previous")",
                           first: "#I18N("system.grid.page.first")",
                           last: "#I18N("system.grid.page.last")",
                           of: "#I18N("system.grid.page.of")",
                           display: "#I18N("system.grid.page.display")",
                           empty: "#I18N("system.grid.page.empty")",
                           page: "#I18N("system.grid.page.page")"
                       }
                   },
               resizable: true,
               columnReorder: function(e) {userControls.saveGridSettings(this, false)},
               columnResize: function(e) {userControls.saveGridSettings(this, false)},
               columnShow: function(e) {userControls.saveGridSettings(this, true)},
               columnHide: function(e) {userControls.saveGridSettings(this, true)},
               columns: [
                       #foreach($Col in $UserStatusColumns)
                           #set($Hidden = "false")
                           #set($Width = "200")
                           #if($UserStatusColumnsSelected.class.simpleName == "LinkedHashMap")
                               #if($UserStatusColumnsSelected.containsKey($Col.Field))
                                   #set($Width = $Utils.getItem($UserStatusColumnsSelected.containsKey($Col.Field), ":", 1))
                               #else
                                   #set($Hidden = "true")
                               #end
                           #end
                        {
                             field: $DQUOTE #I18N("$Col.Field")$DQUOTE,
                             title: $DQUOTE #I18N("performance.users.$Col.Field")$DQUOTE,
                             width: $Width,
                            hidden: $Hidden
                         },
                        #end
                        {
                          field: "idle",
                          title:  $DQUOTE #I18N("performance.users.idle")$DQUOTE,
                          width: 200
                        },
                        {
                           width: $ActionWidth,
                           menu: false,
                           sortable: false,
                           filterable: false,
                           name: "edit-actions",
                           template: $Actions  ,
                           title: "#I18N("system.grid.column.actions")"
                       }
                    ]
           });
       });
       var userControls = {
                id:0,
                /**
                 * Save grid settings to server
                 */
                saveGridSettings: function(grid, refresh) {
                            setTimeout(function() {
                                var columns="fields=";
                                var first=true;
                                for (var i = 0; i < grid.columns.length; i++) {
                                    if (!grid.columns[i].hidden && grid.columns[i].field) {
                                        columns+=(!first?",":"") + grid.columns[i].field+":"+grid.columns[i].width;
                                        first=false;
                                    }
                                }

                                // Refresh the grid using a URL that contains the column list
                                if (refresh) {
                                    var tmp = grid.dataSource.transport.options.read.url;
                                    grid.dataSource.transport.options.read.url += (tmp.indexOf("?")>-1?"&":"?") + columns;
                                    grid.dataSource.read();
                                    grid.dataSource.transport.options.read.url = tmp;
                                }
                                else {
                                    PivotalUtils.getContentAsync("$AppPath/$PageName/$SubPageName/settings", columns)
                                }
                            }, 100);
                        },
                /**
                    * Launch the report manually
                    * @param id The ID of the task
                    */
                signOut: function(id) {
                    PivotalUtils.getJsonContent("$AppPath/status/users/signout", "id=" + id);
                    PivotalUtils.heartbeat("$AppPath");
                    $('#grid').data('kendoGrid').dataSource.read();
                    $("#grid").data("kendoGrid").refresh();
                       return false;
                   }
           }

   </script>

</div>
