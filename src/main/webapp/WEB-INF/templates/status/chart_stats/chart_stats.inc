#parse("$PageName/macros.inc") <div id="example">

    <table style="width: 100%">
       <tr>
           <td style="width: 50%"> #macroDonutChart("Browser" ,"performance.preference.browser.usage" "top" false $Results[0] $Results[1])</td>
           <td style="width: 50%"> #macroDonutChart("Operating" ,"performance.preference.os.usage" "top" false $Results[2] $Results[3]) </td>
       </tr>
       <tr>
           <td style="width: 50%"> #macroSingleChart("Screen" ,"performance.preference.colour.depth.usage" "top" true $Results[4])</td>
           <td style="width: 50%">#macroSingleChart("Colour" ,"performance.preference.screen.resolution.usage" "top" false $Results[5])</td>
       </tr>
   </table>

    #*
    @function	Create donut chart
    @params		$Name (string) - Unique name of the chart
    			$Title (string) - Title to give the chart
    			$LegendPosition (string) - Legend position
    			$LegendVisible (boolean) - Display or not
                $OuterData (Map) - Giving series to show outer donut
                $InnerData (Map) - Giving series to show inner donut
    *#
    #macro(macroDonutChart $Name $Title $LegendPosition $LegendVisible $OuterData  $InnerData )
        #set ($Title = "#I18N($Title)")
        #if (!$LegendPosition)
            #set($LegendPosition = "top")
        #end
        #if (!$LegendVisible)
            #set($LegendVisble = "false")
        #else
            #set($LegendVisble = "true")
        #end
        <div id="example">
              <div class="demo-section k-content">
                  <div id="$Name"></div>
              </div>
              <script>
                  function createChart$Name() {
                      $("#$Name").kendoChart({
                          title: {text: "$Title"},
                          legend: {visible: $LegendVisible, position: "$LegendPosition"},
                          chartArea: {background: ""},
                          seriesDefaults: {type: "donut"},
                          series: [{
                                    name: "Browser",
                                    data: [
                                            #foreach($mapEntry in $InnerData.entrySet())
                                           {
                                             "category": $DQUOTE $mapEntry.key$DQUOTE,
                                              value: $mapEntry.value
                                           }
                                           #if( $foreach.hasNext ),#end
                                         #end
                                        ]
                                    },
                                    {
                                         name: "version",
                                         data: [
                                                  #foreach($mapEntry in  $OuterData.entrySet())
                                                   {
                                                       "category": $DQUOTE $mapEntry.key$DQUOTE,
                                                       value: $mapEntry.value
                                                   }#if( $foreach.hasNext ),#end
                                                   #end
                                                ],
                                         labels: {
                                              visible: true,
                                              font :"7.5px Arial,Helvetica,sans-serif",
                                              background: "transparent",
                                              position: "outsideEnd",
                                              template: "#= category #: #= value#%"
                                         }
                                    }],
                          tooltip: {
                              visible: true,
                              template: "#= category # : #= value #%"
                          }
                      });
                  }
                  $(document).ready(createChart$Name);
                  $(document).bind("kendo:skinChange", createChart$Name);
              </script>
        </div>
    #end
    #*
    @function	Create donut chart
    @params		$Name (string) - Unique name of the chart
    			$Title (string) - Title to give the chart
    			$LegendPosition (string) - Legend position
    			$LegendVisible (boolean) - Display or not
                $Data (Map) - Giving series to show  donut
    *#
    #macro(macroSingleChart $Name $Title $LegendPosition $LegendVisible $data)
        #set ($Title = "#I18N($Title)")
        #if (!$LegendPosition)
            #set($LegendPosition = "top")
        #end
        #if (!$LegendVisible)
            #set($LegendVisble = "false")
        #else
            #set($LegendVisble = "true")
        #end
        <div id="example">
           <div class="demo-section k-content">
               <div id="$Name"></div>

           </div>
           <script>
               function createChart$Name() {
                   $("#$Name").kendoChart({
                       title: { text: "$Title"},
                       legend: {visible: $LegendVisible, position: "$LegendPosition"},
                       seriesDefaults: {
                           labels: {
                               template: "#= category # : #= kendo.format('{0:P}', percentage)#",
                               position: "outsideEnd",
                               visible: true,
                               background: "transparent"
                           }
                       },
                       series: [{
                           type: "donut",
                           data: [
                              #foreach($mapEntry in $data.entrySet())
                                {
                                    "category": $DQUOTE $mapEntry.key$DQUOTE,
                                    "value": $mapEntry.value
                                }#if( $foreach.hasNext ),#end
                                #end
                               ]
                       }],
                       tooltip: {
                           visible: true,
                               template: "#= kendo.format('{0:P}', percentage) #"
                       }
                   });
               }
               $(document).ready(function() {
                   createChart$Name();
                   $(document).bind("kendo:skinChange", createChart$Name);
               });
           </script>
        </div>
     #end
