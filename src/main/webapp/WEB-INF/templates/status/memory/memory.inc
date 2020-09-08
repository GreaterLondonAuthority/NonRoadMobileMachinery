#parse("$PageName/macros.inc")

<h1>#I18N("performance.memory.title")</h1>

#macroPerformanceGraph([
                {"name":"heap.percent.used","max":100},
                {"name":"permgen.percent.used","max":100},
                {"name":"permgen.percent.used.codecache","max":100},
                {"name":"permgen.percent.used.psedenspace","max":100},
                {"name":"permgen.percent.used.psoldgen","max":100},
                {"name":"permgen.percent.used.pssurvivorspace","max":100}
              ],
              {"name":"users.count","height":50})
