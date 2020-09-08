#parse("$PageName/macros.inc")

<h1>#I18N("performance.statistics.title")</h1>

#macroPerformanceGraph([
                {"name":"cpu.percent","max":800},
                {"name":"threads.count"},
                {"name":"file.descriptors","max":100},
                {"name":"load.average","max":5}
              ],
              {"name":"users.count","height":50})

