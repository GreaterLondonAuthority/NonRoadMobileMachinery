/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.data.dao.Database;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.servlet.Initialisation;
import com.pivotal.web.servlet.ServletHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.util.*;

/**
 * Handles requests for status information
 */
@Authorise
@Controller
@RequestMapping(value = {"/status"})
public class PerformanceController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PerformanceController.class);

    private static final String DEFAULT_HOURS_TO_GET = "6";
    private static final String USER_PREFERENCE_STATUS_SETTINGS = "UserPreferenceStatusSettings";

    /**
     * Populates the context with information about the running tasks
     *
     * @param model Context to populate
     * @param hoursToGet Hours of data to retrieve
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value={"","/stats","/pools","/memory"})
    public String getChartInfo(Model model, @RequestParam(value="hours", defaultValue= DEFAULT_HOURS_TO_GET) int hoursToGet) {
        Database db = new DatabaseHibernate();
        List<Map<String, Object>> servers = db.find("select distinct(server_id) as \"server\" from log where date_added>'" + getDateTime(hoursToGet) + '\'');
        db.close();

        // Try and reduce the name to a prefix

        if (!Common.isBlank(servers)) {
            List<String> tmp=new ArrayList<>();
            for (Map<String, Object> row : servers) {
                String name = (String)row.get("server");
                row.put("alias", name.replaceAll("[^a-zA-Z0-9_]", "_"));
                if (!name.matches("[0-9.]+"))
                    tmp.add(StringUtils.reverse(name));
            }
            String postFix = StringUtils.reverse(StringUtils.getCommonPrefix(tmp.toArray(new String[tmp.size()])));
            for (Map<String, Object> row : servers) {
                String name = (String)row.get("server");
                if (Common.isBlank(postFix) || Common.doStringsMatch(postFix, name))
                    row.put("name", name);
                else
                    row.put("name", name.split(postFix)[0]);
            }
        }
        model.addAttribute("Servers", servers);
        model.addAttribute("HoursToGet",hoursToGet);
        model.addAttribute("Events", getEventsData(hoursToGet));
        return "status";
    }

    /**
     * This will Fetch user preference and show data in different charts in
     * user preference section
     *
     * @param model Context to populate
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value = "/chart_stats")
    public String getChartStatistics(Model model) {
        List<Map<String, Integer>> results = new ArrayList<>();
        Database db = new DatabaseHibernate();

        // Fetch browser Statistics  from user_log

        List<Map<String, Object>> rows = db.find("select  user_log.browser, count(distinct user_log.sessionid) as counter  from user_log group by user_log.browser");
        if (!Common.isBlank(rows)) {
            Integer firefox, internetExplorer, chrome, safari;
            firefox = internetExplorer = chrome = safari = 0;
            for (Map<String, Object> row : rows) {

                if (!Common.isBlank(row.get("browser")) && !Common.isBlank(row.get("counter"))) {
                    if (Common.doStringsMatch(row.get("browser").toString(), "Firefox")) {
                        firefox = Common.parseInt(row.get("counter").toString());
                    }
                    if (Common.doStringsMatch(row.get("browser").toString(), "Internet Explorer")) {
                        internetExplorer = Common.parseInt(row.get("counter").toString());
                    }
                    if (Common.doStringsMatch(row.get("browser").toString(), "Chrome")) {
                        chrome = Common.parseInt(row.get("counter").toString());
                    }
                    if (Common.doStringsMatch(row.get("browser").toString(), "Safari")) {
                        safari = Common.parseInt(row.get("counter").toString());
                    }
                }
            }
            Map<String, Integer> result = new TreeMap<>();
            Integer total = firefox + internetExplorer + chrome + safari;
            if (total > 0) {
                chrome = (chrome * 100) / total;
                firefox = (firefox * 100) / total;
                internetExplorer = (internetExplorer * 100) / total;
                safari = (safari * 100) / total;
            }
            result.put("Chrome", chrome);
            result.put("Firefox", firefox);
            result.put("Internet Explorer", internetExplorer);
            result.put("Safari", safari);

            // First result is for  browser Statistics

            results.add(result);

            // Fetch browser_version Statistics  from user_log

            rows = db.find("select user_log.browser,user_log.browser_version, count(distinct user_log.sessionid) as counter  from user_log group by user_log.browser,browser_version order by browser");
            result = new TreeMap<>();
            if (!Common.isBlank(rows)) {
                Map<String, Integer> tempResult = new TreeMap<>();
                total = 0;
                for (Map<String, Object> row : rows) {
                    String browser = "";
                    String browser_version = "";
                    int counter = 0;

                    if (!Common.isBlank(row.get("browser"))) { browser = row.get("browser").toString();}
                    if (!Common.isBlank(row.get("browser_version"))) { browser_version = row.get("browser_version").toString();}
                    if (!Common.isBlank(row.get("counter"))) {counter = Common.parseInt(row.get("counter").toString());}

                    tempResult.put(browser + " " + browser_version, counter);
                    total += counter;
                }

                firefox = internetExplorer = chrome = safari = 0;
                for (Map.Entry<String, Integer> entry : tempResult.entrySet()) {
                    if (total > 0) {
                        result.put(entry.getKey(), (entry.getValue() * 100) / total);
                    }
                    else {
                        result.put(entry.getKey(), 0);
                    }

                    // check browser is in the list

                    if (entry.getKey().contains("Firefox")) { firefox = 1;}
                    if (entry.getKey().contains("Internet Explorer")) { internetExplorer = 1; }
                    if (entry.getKey().contains("Chrome")) { chrome = 1;}
                    if (entry.getKey().contains("Safari")) {safari = 1;}
                }
                if (firefox == 0) { result.put("Firefox ", 0);}
                if (internetExplorer == 0) {result.put("Internet Explorer  ", 0);}
                if (chrome == 0) {result.put("Chrome ", 0);}
                if (safari == 0) {result.put("Safari ", 0);}

            }
            results.add(result);

             // Fetch operating system Statistics  from user_log

            rows = db.find("select  user_log.os, count(distinct user_log.sessionid) as counter  from user_log group by user_log.os");
            result = new TreeMap<>();
            total = 0;
            if (!Common.isBlank(rows)) {
                for (Map<String, Object> row : rows) {
                    if (!Common.isBlank(row.get("os")) && !Common.isBlank(row.get("counter"))) {
                        String thisOSName = row.get("os").toString();
                        if (result.containsKey(thisOSName))
                            result.put(thisOSName, result.get(thisOSName) + Common.parseInt(row.get("counter").toString()));
                        else
                            result.put(thisOSName, Common.parseInt(row.get("counter").toString()));

                        total += Common.parseInt(row.get("counter").toString());
                    }
                }

                for (String osName : result.keySet()) {
                    int value = (result.get(osName) * 100) / total;
                    if (value == 0) value = 1;
                    result.put(osName, value);
                }
            }
            results.add(result);

            // Fetch operating system architecture Statistics  from user_log

            rows = db.find("select  user_log.os,user_log.os_architecture, count(distinct user_log.sessionid) as counter  from user_log group by user_log.os,os_architecture order by os");
            result = new TreeMap<>();
            if (!Common.isBlank(rows)) {
                Map<String, Integer> tempResult = new TreeMap<>();
                total = 0;
                for (Map<String, Object> row : rows) {
                    String os, osArchitecture;
                    os = osArchitecture = "";
                    int counter = 0;
                    if (!Common.isBlank(row.get("os"))) {
                        os = row.get("os").toString();
                    }
                    if (!Common.isBlank(row.get("os_architecture"))) {
                        osArchitecture = row.get("os_architecture").toString();
                    }
                    if (!Common.isBlank(row.get("counter"))) {
                        counter = Common.parseInt(row.get("counter").toString());
                    }
                    tempResult.put(os + " (" + osArchitecture + ")", counter);
                    total += counter;
                }
                for (Map.Entry<String, Integer> entry : tempResult.entrySet()) {
                    if (total > 0) {
                        int value = (entry.getValue() * 100) / total;
                        if (value == 0) value = 1;
                        result.put(entry.getKey(), value);
                    }
                    else {
                        result.put(entry.getKey(), 0);
                    }

                }
            }
            results.add(result);

            // Fetch colours Statistics  from user_log

            rows = db.find("select  user_log.colours, count(distinct user_log.sessionid) as counter  from user_log group by user_log.colours");
            result = new TreeMap<>();
            if (!Common.isBlank(rows)) {
                Map<String, Integer> tempResult = new TreeMap<>();
                total = 0;
                for (Map<String, Object> row : rows) {
                    String colours = "";
                    int counter = 0;
                    if (!Common.isBlank(row.get("colours"))) {
                        colours = row.get("colours").toString();
                    }
                    if (!Common.isBlank(row.get("counter"))) {
                        counter = Common.parseInt(row.get("counter").toString());
                    }
                    tempResult.put(colours, counter);
                    total += counter;
                }
                for (Map.Entry<String, Integer> entry : tempResult.entrySet()) {
                    if (total > 0) {
                        result.put(entry.getKey(), (entry.getValue() * 100) / total);
                    }
                    else {
                        result.put(entry.getKey(), 0);
                    }
                }
            }
            results.add(result);

            // Fetch screen_resolution Statistics  from user_log

            rows = db.find("select  user_log.screen_resolution, count(distinct user_log.sessionid) as counter  from user_log group by user_log.screen_resolution");
            result = new TreeMap<>();
            if (!Common.isBlank(rows)) {
                Map<String, Integer> tempResult = new TreeMap<>();
                total = 0;
                for (Map<String, Object> row : rows) {
                    String screenResolution = "";
                    int counter = 0;
                    if (!Common.isBlank(row.get("screen_resolution"))) {
                        screenResolution = row.get("screen_resolution").toString();
                    }
                    if (!Common.isBlank(row.get("counter"))) {
                        counter = Common.parseInt(row.get("counter").toString());
                    }
                    tempResult.put(screenResolution, counter);
                    total += counter;
                }
                for (Map.Entry<String, Integer> entry : tempResult.entrySet()) {
                    if (total > 0) {
                        result.put(entry.getKey(), (entry.getValue() * 100) / total);
                    }
                    else {
                        result.put(entry.getKey(), 0);
                    }
                }
            }
            results.add(result);

            model.addAttribute("Results", results);

        }
        db.close();
        return "status";

    }

    /**
     * This will clear the user session entry and logout it from the browser
     *
     * @param request The current request
     */
    @RequestMapping(value = "/users/signout", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void signOut(HttpServletRequest request) {
        String id = request.getParameter("id");
        Map<String, HttpSession> sessions = Initialisation.getSessionMap();
        if (sessions.containsKey(id)) {
            UserManager.logout(null, id);
            try {
                sessions.get(id).invalidate();
            }
            catch(IllegalStateException e) {
                logger.debug("Session invalidation failed " + PivotalException.getErrorMessage(e));
            }
        }
    }

    /**
     *  This will load data from the login user table
     *
     * @param params Query String parameters
     *
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value = "/users/data", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> reportTableData(@RequestParam Map<String, Object> params) {

        if (params.containsKey("fields")) {
            UserManager.getCurrentUser().getPreferences().put(USER_PREFERENCE_STATUS_SETTINGS, ((String)(params.get("fields"))).replaceAll(" ",""));
        }

        Database db = new DatabaseHibernate();
        List<Map<String, Object>> useStatus = db.find("select u.* from user_status u join users on userid=email order by lastname,firstname");

        // Try and reduce the name to a prefix

        if (!Common.isBlank(useStatus)) {
            for (Map<String, Object> row : useStatus) {
                row.put("id", "\"" + row.get("sessionid") + "\"");
                row.put("login_time", Common.formatDate((Timestamp) row.get("login_time"), "yyyy-MM-dd HH:mm:ss"));
                Timestamp last_access = (Timestamp) row.get("last_access");
                row.put("last_access", Common.formatDate(last_access, "yyyy-MM-dd HH:mm:ss"));
                Timestamp last_heartbeat = (Timestamp) row.get("last_heartbeat");
                row.put("last_heartbeat", Common.formatDate(last_heartbeat, "yyyy-MM-dd HH:mm:ss"));

                long diff = 0;
                if (!Common.isBlank(last_access) && !Common.isBlank(last_heartbeat)) {
                    diff = last_heartbeat.getTime() - last_access.getTime();
                    diff = diff / (60 * 1000) % 60;
                    if (diff < 0) {
                        diff = 0L;
                    }
                }
                row.put("idle", diff + " min");
            }
        }
        db.close();

        return useStatus;
    }

    /**
     * Fetch the column for the table
     *
     * @param model Context to populate
     * @return Returns an appropriate template to use
     */
    @RequestMapping(value = "/users")
    public String getUserInfo(Model model) {
        List<Map<String, Object>>userStatusColumns = DataSourceUtils.getColumns("user_status");
        String selectedColumns = UserManager.getCurrentUser().getPreferences().get(USER_PREFERENCE_STATUS_SETTINGS, "");
        if (Common.isBlank(selectedColumns))
            model.addAttribute("UserStatusColumnsSelected", userStatusColumns);
        else {

            List<String>colList = Common.splitToList(selectedColumns, ",");
            Map<String, String>colMap = new LinkedHashMap<>();
            for(String col : colList)
                colMap.put(Common.getItem(col,":",0), col);

            model.addAttribute("UserStatusColumnsSelected", colMap);
        }

        model.addAttribute("UserStatusColumns", userStatusColumns);

        return "status";
    }

    /**
     * Populates the context with information about the historical performance
     *
     * @param hoursToGet Hours of data to retrieve
     * @param cats List of report names to get data for
     * @param anchor Report name to be the left most join
     * @return Returns an appropriate template to use
     */
    @ResponseBody
    @RequestMapping(value="/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String,Object>> getData(@RequestParam(value="hours", defaultValue= DEFAULT_HOURS_TO_GET) int hoursToGet,
                                            @RequestParam(value="cats") String[] cats,
                                            @RequestParam(value="anchor", defaultValue = "heap.percent.used") String anchor) {
        Database db = new DatabaseHibernate();
        String query = createQuery(anchor, hoursToGet, cats);
        List<Map<String,Object>> rows = db.find(query, false);
        db.close();
        return flattenTable(rows);
    }

    /**
     * Returns a list of events that may have occurred within the time period
     *
     * @param hoursToGet Hours of data to retrieve
     *
     * @return List of events
     */
    private List<Map<String,Object>> getEventsData(int hoursToGet) {
        Database db = new DatabaseHibernate();
        List<Map<String, Object>> returnValue = db.find("select a.date_added as \"date\"," +
                "case when status='" + LogEntity.STATUS_SERVER_STARTED + "' then 'S' " +
                "when status='" + LogEntity.STATUS_SERVER_STOPPED + "' then 'S' " +
                "else 'D' " +
                "end as \"text\",message as \"description\"\n" +
                "from \n" +
                "log a \n" +
                "where\n" +
                "a.date_added>'" + getDateTime(hoursToGet) + "' and\n" +
                "a.status in ('" + LogEntity.STATUS_SERVER_STARTED + "','" +
                                   LogEntity.STATUS_SERVER_STOPPED+ "') and\n" +
                "a.duration<>0 and\n" +
                "a.server_id='" + ServletHelper.getAppIdentity() + "'\n" +
                "order by a.date_added\n" +
                ";\n", false);
        for (Map<String, Object> row : returnValue) {
            row.put("date", Common.formatDate((Date)row.get("date"), "yyyy/MM/dd HH:mm:ss"));
        }
        db.close();
        return returnValue;
    }

    /**
     * Reduces the rows by flattening the rows into a join based around the date
     *
     * @param rows Results set to flatten
     *
     * @return Results set
     */
    private static List<Map<String, Object>> flattenTable(List<Map<String, Object>> rows) {

        // We now need to flatten this out into a table with the date as the key
        // and sets of data where the column names are preceded by the server name

        Map<Timestamp,Map<String,Object>> returnValue = new LinkedHashMap<>();
        for (Map<String,Object> row : rows) {

            // Get the key elements

            Timestamp date=(Timestamp)row.get("date");
            String server=((String)row.get("server")).replaceAll("[^a-zA-Z0-9_]", "_");
            row.put("server", server);

            // Get the row from the table if it already exists

            Map<String,Object> displayRow = returnValue.get(date);
            if (displayRow==null) displayRow = new HashMap<>();

            // Update the row with the data we have

            displayRow.put("date", date);
            for (String key : row.keySet()) {
                if (!Common.doStringsMatch(key, "date", "server"))
                    displayRow.put(key + (Common.doStringsMatch(key, "users_count")?"":('_' + server)), row.get(key));
            }

            // Put the display row back into the table

            returnValue.put(date, displayRow);
        }
        return new ArrayList<>(returnValue.values());
    }

    /**
     * Returns the formatted timestamp comparison string
     *
     * @param hoursToGet Number of hours to take away from now
     * @return Formatted SQL string
     */
    private String getDateTime(int hoursToGet) {
        return Common.formatDate(Common.addDate(new Date(), Calendar.HOUR, -hoursToGet), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Creates an aggregating query to collect stats based on the report names
     * sent to us
     * @param anchorReport Report to anchor the query to
     * @param hoursToGet Number of hours to get
     * @param reportNames List of names to collect stats for
     * @return A query
     */
    private String createQuery(String anchorReport, int hoursToGet, String... reportNames) {
        StringBuilder query = new StringBuilder();

        query.append("select zz.date_added as \"date\",zz.server_id as \"server\"");
        for (int i=0; i<reportNames.length; i++) {
            String reportName = reportNames[i];
            String table = (char)(i + 'a') + "";
            if (Common.doStringsMatch(reportName, "load.average")) {
                query.append(String.format(",\n%s.duration / 1000 as \"%s\"", table, reportName.replace(".", "_")));
            }
            else {
                query.append(String.format(",\n%s.duration as \"%s\"", table, reportName.replace(".", "_")));
            }
        }
        query.append(" from\nlog zz\n");
        for (int i=0; i<reportNames.length; i++) {
            String reportName = reportNames[i];
            String table = (char)(i + 'a') + "";
            query.append(String.format("left join log %s on (zz.date_added=%s.date_added and zz.server_id=%s.server_id and %s.report_name='%s')\n",
                                       table, table, table, table, reportName));
        }
        query.append(String.format("where zz.report_name='%s' and zz.date_added>'%s'\n", anchorReport, getDateTime(hoursToGet)));
        query.append("order by zz.date_added;\n");
        return query.toString();
    }

}
