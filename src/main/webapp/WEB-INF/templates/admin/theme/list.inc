#parse("includes/initialise.inc")

#macro(macroShowSelectedTheme $ThemeName)
    #if ($Theme==$ThemeName)
        <i class="fa fa-check-square-o"></i>
    #else
        <a class="set-theme" href="javascript:void(0)" theme="$ThemeName" tooltip="#I18N("admin.theme.set.tooltip")">
            <i class="fa fa-square-o"></i>
        </a>
    #end
#end

<table class="table table-hover table-striped" style="width:600px">
    <thead>
        <tr>
            <th style="width:20px">#I18N("admin.theme.list.selected")</th>
            <th style="width:150px">#I18N("admin.theme.list.name")</th>
            <th colspan="2" style="width:430px">#I18N("admin.theme.list.description")</th>
        </tr>
    </thead>
    <tr>
        <td align="middle">#macroShowSelectedTheme("default")</td>
        <td>default</td>
        <td>#I18N("admin.theme.default.description")</td>
        <td><a class="k-button" href="$AppPath/$PageName/theme/default" tooltip="#I18N("admin.theme.download.default.tooltip")"><i class="fa fa-cloud-download"></i></a></td>
    </tr>
    #foreach ($CustomTheme in $CustomThemes)
        <tr>
            <td align="middle">#macroShowSelectedTheme($CustomTheme.Name)</td>
            <td>$CustomTheme.Name</td>
            <td>$!CustomTheme.Description</td>
            <td>
                <a class="k-button" href="$AppPath/media/download/$CustomTheme.Id" tooltip="#I18N("admin.theme.download.tooltip")"><i class="fa fa-cloud-download"></i></a>&nbsp;
                <a class="k-button delete-theme" href="javascript:void(0)" tooltip="#I18N("admin.theme.delete.tooltip")" theme="$CustomTheme.Name"><i class="fa fa-trash-o"></i></a>&nbsp;
                <a class="k-button edit-theme" href="javascript:void(0)" tooltip="#I18N("admin.theme.edit.tooltip")" theme="$CustomTheme.Name"><i class="fa fa-pencil"></i></a>
            </td>
        </tr>
    #end
</table>

