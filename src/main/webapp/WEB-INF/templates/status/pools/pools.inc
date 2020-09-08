#parse("$PageName/macros.inc")

<h1>#I18N("performance.pools.title")</h1>

#macroPerformanceGraph([
                {"name":"cache.stats.hitrate"},
                {"name":"jdbc.pool.active"},
                {"name":"velocity.template.cache.hitrate"},
                {"name":"velocity.template.cache.missrate"},
                {"name":"velocity.template.cache.size"}
              ],
              {"name":"users.count","height":50})
