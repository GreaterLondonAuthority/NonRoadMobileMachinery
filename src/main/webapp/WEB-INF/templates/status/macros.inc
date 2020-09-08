#**
 * Create the table start tag
 * @param Title title of the group to put the table in
 *#
#macro(macroStartStatusTable $Title)
    #macroStartFieldset($Title)
        <table class="table table-striped table-hover">
#end

#**
 * Createa the table end tag
 *#
#macro(macroEndStatusTable)
        </table>
    #macroEndFieldset()
#end

#**
 * Displays a line of information as a table entry
 * @param Name I18N name to display
 * @param Value Value to show
 *#
#macro(macroShowStatusTableRow $Name $Value)
    #macroShowStatusTableRowNoTranslation("#I18N($Name)" $Value)
#end

#**
 * Displays a line of information as a table entry but without
 * translating the name
 * @param Name name to display
 * @param Value value to show
 *#
#macro(macroShowStatusTableRowNoTranslation $Name $Value)
    <tr>
        <td>$Name</td>
        <td>$!Value</td>
    </tr>
#end

#**
 * Shows a progress bar for the specified ID with a label
 * and an optional template for the description
 * @param Id Id to give the progress bar
 * @param Label Label to give the progress bar
 * @param Template for any description
 *#
#macro(macroShowStatusGauge $Id $Label $Template)
    <div class="status-gauge">
        <label>#I18N($Label)
            #if ("$!Template"!="")
                <span id="${Id}_text" template="#I18N($Template)"></span>
            #end
        </label>
        <div id="$Id" class="statusProgress"></div>
    </div>
#end

#**
 * Shows a menu item for the log file
 * @param Log log file to interrogate
 *#
#macro(macroShowLogMenuItem $Log)
    #set($MAX_LENGTH=1000000)
    #set($MAX_EXCEL_LENGTH=5000000)

    #if ($Log.isFile() && !$Log.isHidden() && $Log.length() > 0)
        <li id="$Log.Name" #if ($Log.Name==$Preferences.get("selectedLogName", "$AppLog.Name")) class="k-state-selected" #end >
            #if ($Log.Name.matches(".+[0-9]{4}-[0-9]{2}-[0-9]{2}.*"))
                $Log.Name.replaceAll("[^0-9\\-]+","")
            #else
                $Log.Name
            #end
            <span>
                #if ($Log.length() > 1024)
                    #if ($Log.length() > 1048576)
                        #set ($Length=$Log.length() / 1048576)
                        #set ($Length="${Length}M")
                    #else
                        #set ($Length=$Log.length() / 1024)
                        #set ($Length="${Length}K")
                    #end
                #else
                    #set ($Length="$Log.length()")
                #end
                &nbsp;&nbsp;($Length)
            </span>
            <ul>
                #if ($Log.length() < $MAX_EXCEL_LENGTH)
                    <li actionType="excel" filename="$Log.Name">
                        <img src="$AppPath/graphics/common/associations/xlsx.gif"> Export to Excel
                    </li>
                #end
                #if ($Log.length() > $MAX_LENGTH)
                    <li actionType="download" filename="$Log.Name">
                        <img src="$AppPath/graphics/common/associations/txt.gif"> Download
                    </li>
                #else
                    <li actionType="view" filename="$Log.Name">
                        <img src="$AppPath/graphics/common/associations/html.png"> View
                    </li>
                #end
                <li actionType="tail" filename="$Log.Name">
                    <img src="$AppPath/graphics/common/associations/htm.gif"> Tail
                </li>
            </ul>
        </li>
    #end
#end

#**
 * Shows the font size option with selection
 * @param FontSizes List of font sizes
 *#
#macro(macroLogFontSizeMenus $FontSizes)
    #foreach($FontSize in $FontSizes)
        <li actionType="setfontsize" fontsize="$FontSize"#if ($Preferences.get("fontSize", 70) == $FontSize) class="k-state-selected"#end>
            #I18N("status.log.font.size.$FontSize")
        </li>
    #end
#end

#**
 * Creates a stock chart with an optional navigator
 * @param PanesToUse List of pane defintions
 * @param NavigatorToUse definitio of the navigator pane
 *#
#macro(macroPerformanceGraph $PanesToUse $NavigatorToUse)

    #set ($Cats = [])
    #set ($Panes = [])
    #foreach($Pane in $PanesToUse)
        #set ($tmp = $Cats.add($Pane.name))
        #set ($tmp = "performance.chart.$Pane.name")
        #set ($Pane.title = "#I18N($tmp)")
        #set ($Pane.name = $Pane.name.replace(".","_"))
        #set ($tmp = $Panes.add($Pane))
    #end
    #if ("$!NavigatorToUse"!="")
        #set ($tmp = $Cats.add($NavigatorToUse.name))
        #set ($tmp = "performance.chart.$NavigatorToUse.name")
        #set ($Navigator = $NavigatorToUse)
        #set ($Navigator.title = "#I18N($tmp)")
        #set ($Navigator.name = $NavigatorToUse.name.replace(".","_"))
    #end
    #set ($Cats = $Utils.join($Cats))
    #set ($ChartName = "StockChart$Utils.getDate().Time")
    #set ($ChartNameVar = "StockChartDiv$Utils.getDate().Time")

    <div id="$ChartName"></div>

    <script type="application/javascript">
        var $ChartNameVar = {

            resizeTime: false,
            refreshTimer: false,

            resize: function() {
                clearTimeout(this.resizeTime);
                this.resizeTime = setTimeout(${ChartNameVar}.draw, 500);
            },

            draw: function() {
                clearTimeout(this.refreshTimer);
                this.refreshTimer = setTimeout(${ChartNameVar}.draw, 30000);

                var height = $(window).height() - $("#$ChartName").position().top - 20 ;
                $("#$ChartName").height(height);

                $("#$ChartName").kendoStockChart({
                    dataSource: {
                        transport: {
                            read: {
                                url: "$AppPath/$PageName/data?cats=$Cats",
                                dataType: "json",
                                cache: false
                            }
                        }
                    },
                    dateField: "date",
                    panes: [
                            #foreach($Pane in $Panes)
                                {
                                    name: "$Pane.name",
                                    #if ($Pane.height.contains("%"))
                                        #if ("$!Navigator" != "")
                                            height: (height - $Navigator.height) * ($Pane.height.replace("%","") / 100),
                                        #else
                                            height: height * ($Pane.height.replace("%","") / 100),
                                        #end
                                    #elseif ("$!Pane.height" != "")
                                        height: $Pane.height,
                                    #else
                                        #if ("$!Navigator" != "")
                                            height: (height - $Navigator.height) / $Panes.size(),
                                        #else
                                            height: height / $Panes.size(),
                                        #end
                                    #end
                                    margin: 5,
                                    padding: 5,
                                    background: "#5EBDE6",
                                    opacity: 0.1,
                                    border: {
                                        width: 1,
                                        color: "black"
                                    }
                                },
                            #end
                    ],

                    valueAxes: [
                        #foreach($Pane in $Panes)
                            {
                                name: "$Pane.name",
                                pane: "$Pane.name",
                                min: 0,
                                #if ("$!Pane.max" != "")
                                    max: $Pane.max,
                                    #set ($Unit = $Pane.max / 5)
                                    majorUnit: $Unit,
                                #end
                                title: {
                                    rotation: -90,
                                    text: "$Pane.title",
                                    font: "10px Arial"
                                },
                                labels: {
                                    format: "",
                                    margin: 5,
                                    font: "10px Arial"
                                },
                            },
                        #end
                    ],

                    series: [
                        #foreach($Pane in $Panes)
                            #foreach($Server in $Servers)
                                {
                                    field: "${Pane.name}_$Server.alias",
                                    name: "$Server.name.replace(".","_")",
                                    axis: "$Pane.name",
                                    #if ($Pane.title.contains("%"))
                                        tooltip: {
                                            template: "#= series.name #: #= value #%"
                                        },
                                    #end
                                },
                            #end
                        #end
                    ],

                    categoryAxis: [
                        #foreach($Pane in $Panes)
                            {
                                name: "$Pane.name",
                                pane: "$Pane.name",
                                field: "date",
                                labels: {
                                    font: "10px Arial"
                                }
                            },
                        #end
                    ],

                    seriesDefaults: {
                        type: "line",
                        style: "smooth",
                        markers: {
                            type: "square",
                            rotation: 45,
                            size: 3
                        },
                        tooltip: {
                            visible: true,
                            template: "#= series.name #: #= value #"
                        },
                    },

                    #if ("$!Navigator" != "")
                        navigator: {
                            pane: {
                                name: "$Navigator.name",
                                height: $Navigator.height,
                                margin: 5,
                                padding: 0,
                                background: "#c0c0c0",
                                opacity: 0.4
                            },
                            series: {
                                type: "area",
                                missingValues: "interpolate",
                                style: "smooth",
                                field: "$Navigator.name"
                            },
                            tooltip: {
                                visible: true,
                                template: "#= series.name #: #= value #"
                            },
                            categoryAxis: {
                                field: "date",
                                visible: false,
                                notes: {
                                    data: [
                                        #foreach($Event in $Events)
                                            {
                                                value: "$Event.date",
                                                label: {
                                                    text: "$Event.text"
                                                }
                                            },
                                        #end
                                    ]
                                }
                            },
                        },
                    #end

                    #if ($Servers.size() > 1)
                        legend: {
                          visible: true,
                          position: "bottom"
                        },
                    #end
                });
            }
        }

        $(document).ready(function() {
            ${ChartNameVar}.draw();
        });
        $(window).resize(function() {
            ${ChartNameVar}.resize();
        });

    </script>

#end
