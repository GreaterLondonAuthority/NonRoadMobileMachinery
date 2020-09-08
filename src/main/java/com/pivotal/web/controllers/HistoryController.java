/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.security.UserManager;
import com.pivotal.system.data.dao.DatabaseHibernate;
import com.pivotal.system.hibernate.entities.ChangeLogEntity;
import com.pivotal.system.hibernate.entities.ReportEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.*;
import com.pivotal.web.controllers.utils.GridResults;
import com.pivotal.web.controllers.utils.JsonResponse;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides the display and editing of the audit trail of changes to NRMM components
 */
@Controller
@RequestMapping(value="/admin/history")
public class HistoryController extends AbstractAdminController{

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HistoryController.class);

    /**
     * Shows the grid for the specified entity and id
     *
     * @param model      Model to populate with the datasource objects
     * @param entityType Entity type of the element we're checking
     * @param id         Entity id of the element we're checking
     *
     * @return The template to use
     */
    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String getList(Model model, @RequestParam(value = "entity", required = true) String entityType, @RequestParam(value = "id", required = true) int id) {
        // Get a list of the components we need from the query string etc.
        if (!Common.isBlank(entityType) && !Common.isBlank(id)) {
            try {
                String whereClause = String.format("(tableAffected = '%s' and rowAffected = %d) or parentRow = '%s'", entityType, id, entityType + ":" + id);
                GridResults config = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(getNamespace()), whereClause);
                config.savePreferences();
                model.addAttribute("Prefs", UserManager.getCurrentUser().getPreferences(getNamespace()));
                model.addAttribute("Config", config);
                model.addAttribute("RefEntityTableName", entityType);
                model.addAttribute("RefId", id);
                model.addAttribute("HideSideMenu", Common.isYes(ServletHelper.getParameter("hidesidemenu")));
            }
            catch (Exception e) {
                logger.error("Problem showing grid - {}", PivotalException.getErrorMessage(e));
            }
        }
        return getRootTemplate();
    }

    /**
     * Manages the sending of data to the grid
     * Takes care of any sorting and filtering that might be in place
     *
     * @param model         Model to use
     * @param entityType    entity grid data is for
     * @param id            Id of entity data is for
     *
     * @return Grid results object
     */
    @RequestMapping(value = "/historygrid", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public GridResults getGridData(Model model, @RequestParam(value = "entity", required = true) String entityType, @RequestParam(value = "id", required = true) int id) {

        GridResults gridResults = null;
        // Get a list of the components we need from the query string etc.
        String whereClause = "";
        if (!Common.isBlank(entityType) && !Common.isBlank(id)) {
            try {
                whereClause = String.format("(tableAffected = '%s' and rowAffected = %d) or parentRow = '%s'", entityType, id, entityType + ":" + id);

                gridResults = new GridResults(getEntityClass(), UserManager.getCurrentUser().getPreferences(getNamespace()), whereClause);
                gridResults.executeQuery();
                gridResults.savePreferences();

            }
            catch (Exception e) {
                logger.error("Problem getting history grid results - {}", PivotalException.getErrorMessage(e));
            }
        }
        return gridResults;
    }

    /**
     * Returns a comparison of 2 changes
     *
     * @param model Model to populate with the datasource objects
     * @param first Id of the first change log entry
     * @param second Id of the second change log entry
     *
     * @return Comparison of 2 changes
     */
    @RequestMapping(value="/compare", method=RequestMethod.GET)
    public String getComparison(Model model, @RequestParam(value="first", required=false) Integer first, @RequestParam(value="second", required=false) Integer second) {

        // Always set the order as oldest first

        if (first!=null && second!=null) {
            if (first > second) {
                int tmp=first;
                first=second;
                second=tmp;
            }
        }
        else if (first==null) {
            first = second;
            second = null;
        }

        // Get the entities

        ChangeLogEntity firstChange= HibernateUtils.getEntity(ChangeLogEntity.class, first);
        if (firstChange!=null) {

            // If we don't have a second change, then we must be comparing with the current

            ChangeLogEntity secondChange;
            if (second==null) {

                // Find out the entity type we're dealing with and create a dummy change

                secondChange=new ChangeLogEntity();
                String changes = HibernateUtils.serializeEntity(HibernateUtils.getEntityByTable(firstChange.getTableAffected(), firstChange.getRowAffected()));
                secondChange.setChangeType(ChangeLogEntity.ChangeTypes.EDITED.getDescription());
                secondChange.setPreviousValues(changes);
                secondChange.setUserFullName(firstChange.getUserFullName());
                secondChange.setRowAffected(firstChange.getRowAffected());
                secondChange.setTableAffected(firstChange.getTableAffected());
                secondChange.setTimeAdded(firstChange.getTimeAdded());
                secondChange.setParentRow(firstChange.getParentRow());
            }
            else
                secondChange= HibernateUtils.getEntity(ChangeLogEntity.class, second);

            if (secondChange!=null) {
                model.addAttribute("First", firstChange);
                model.addAttribute("Second", secondChange);

                // Create a set of diffs

                String firstValues= getDecodedValues(firstChange.getPreviousValues());
                String secondValues= getDecodedValues(secondChange.getPreviousValues());
                DiffMatchPatch differ=new DiffMatchPatch();
                LinkedList<DiffMatchPatch.Diff> diffs = differ.diff_main(firstValues==null?"":firstValues, secondValues==null?"":secondValues);
                differ.diff_cleanupSemantic(diffs);
                model.addAttribute("Changes", diffPrettyHtml(diffs));
            }
        }
        return getRootTemplate();
    }

    /**
     * Reverts a change to be the latest version
     *
     * @param id Id of the change log entry
     * @return a {@link com.pivotal.web.controllers.utils.JsonResponse} object.
     */
    @RequestMapping(value="/revert", method=RequestMethod.GET)
    public @ResponseBody
    JsonResponse revertChange(@RequestParam("id") Integer id) {
        JsonResponse returnValue = new JsonResponse();
        ChangeLogEntity changeEntity= HibernateUtils.getEntity(ChangeLogEntity.class, id);
        if (changeEntity!=null) {

            // Get the useful values

            boolean isReport = false;
            String change=changeEntity.getPreviousValues();
            String table=changeEntity.getTableAffected();
            int tableId=changeEntity.getRowAffected();
            String entityName= getEntityNameFromTable(table);
            if (Common.doStringsMatch(entityName, ReportEntity.class.getSimpleName())) {
                entityName = ReportEntity.class.getSimpleName();
                isReport = true;
            }

            // Get a list of the values and turn them into a map

            if (!Common.isBlank(change)) {
                List<String> fields=Common.splitToList(change,"(^@)|(\n@)");
                Map<String,Object> valueMap=new LinkedHashMap<>();
                for (int i=1; i<fields.size(); i++) {
                    String[] parts=fields.get(i).split(": *", 2);
                    String fieldName=parts[0];
                    if (!fieldName.startsWith("[")) {
                        String fieldValue = null;
                        if (parts.length > 1) fieldValue = parts[1].replaceAll("'", "''");

                        // Check to see if we need to decode the data

                        if (isReport && Common.doStringsMatch(fieldName, "file")) {
                            try {
                                fieldValue = new String(Base64.decode(fieldValue), "UTF-8");
                            }
                            catch (Exception e) {
                                logger.error("Problem decoding change value - {}", PivotalException.getErrorMessage(e));
                            }
                        }

                        Object val;
                        //is it boolean
                        if (Common.doStringsMatch(fieldName, "disabled") || Common.doStringsMatch(fieldName, "internal")) {
                            Boolean tmp = Common.parseBoolean(fieldValue);
                            if (tmp) {
                                fieldValue = "1";
                            }
                            else {
                                fieldValue = "0";
                            }
                        }

                        valueMap.put(fieldName, fieldValue);
                    }
                }

                // Determine the type of statement to create

                boolean isInsertion=false;
                String serializedVersion=null;

                // Determine where to get the entity from

                DatabaseHibernate db=new DatabaseHibernate();
                Object entity=HibernateUtils.getEntity(entityName, tableId);
                if (entity==null) {
                    HibernateUtils.getEntity(entityName);
                    if (db.addRecord(table, valueMap))
                        returnValue.setInformation("Re-instated [" + entityName + "] record as version [" + id + "] successfully");
                    else
                        returnValue.setError("Failed to re-instate [" + entityName + "] record as version [" + id + "]\n\n" + db.getLastError());
                    isInsertion=true;
                }
                else {
                    serializedVersion=HibernateUtils.serializeEntity(entity);
                    if (db.updateRecord(table, "id=" + tableId, valueMap, false)) {
                        returnValue.setInformation("Reverted [" + entityName + "] record to version [" + id + "] successfully");
                        HibernateUtils.evict(entity);
                    }
                    else
                        returnValue.setError("Failed to revert [" + entityName + "] record to version [" + id + "]\n\n" + db.getLastError());
                }

                // Save the value to the database

                if (!db.isInError()) {
                    HibernateUtils.addChangeLog(entity, serializedVersion, isInsertion?ChangeLogEntity.ChangeTypes.ADDED:ChangeLogEntity.ChangeTypes.EDITED);
                }
                db.close();
            }
        }
        return returnValue;
    }

    /**
     * Convert a Diff list into a pretty HTML report
     *
     * @param diffs LinkedList of Diff objects
     *
     * @return HTML representation.
     */
    private String diffPrettyHtml(List<DiffMatchPatch.Diff> diffs) {
        StringBuilder html = new StringBuilder();
        int i = 0;
        for (DiffMatchPatch.Diff aDiff : diffs) {
            String text = aDiff.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "&para;<br>");
            switch (aDiff.operation) {
                case INSERT:
                    html.append("<span class='diffinsert'>").append(text).append("</span>");
                    break;
                case DELETE:
                    html.append("<span class='diffdelete'>").append(text).append("</span>");
                    break;
                case EQUAL:
                    html.append(text);
                    break;
            }
            if (aDiff.operation != DiffMatchPatch.Operation.DELETE) {
                i += aDiff.text.length();
            }
        }
        return html.toString();
    }

    /**
     * Returns the entity name of the table
     *
     * @param table Table name
     *
     * @return Entity name
     */
    private static String getEntityNameFromTable(String table) {
        String entityName=table.replaceAll("_","") + "entity";
        try {
            List<Class> classes= ClassUtils.getClasses(ChangeLogEntity.class.getPackage().getName());
            for (Class entityClass : classes) {
                if (Common.doStringsMatch(entityClass.getSimpleName(), entityName))
                    entityName=entityClass.getSimpleName();
            }
        }
        catch (Exception e) {
            logger.error("Problem enumerating entity classes - {}", PivotalException.getErrorMessage(e));
        }

        return entityName;
    }

    /**
     * Convenience method for cleaning up any special column values
     *
     * @param change Change content
     *
     * @return Modified change content
     */
    private static String getDecodedValues(String change) {

        String returnValue = null;
        if (!Common.isBlank(change)) {

            // Split the change into it's constituents

            List<String> fields=Common.splitToList(change,"(^@)|(\n@)");
            Map<String,Object> valueMap=new LinkedHashMap<>();

            // Manage each part

            for (int i=1; i<fields.size(); i++) {
                String[] parts=fields.get(i).split(": *", 2);
                String fieldName=parts[0];
                String fieldValue=null;
                if (parts.length>1) fieldValue=parts[1].replaceAll("'","''");

                // If this is the file object of a report, then decode it

                if (Common.doStringsMatch(fieldName, "file")) {
                    try {
                        fieldValue = new String(Base64.decode(fieldValue),"UTF-8");
                    }
                    catch (Exception e) {
                        logger.error("Problem decoding change value - {}", PivotalException.getErrorMessage(e));
                    }
                }
                valueMap.put(fieldName, fieldValue);
            }

            // Re-assemble the change

            for (String key : valueMap.keySet()) {
                returnValue=(returnValue==null?"":returnValue + '\n') + '@' + key + ": " + valueMap.get(key);
            }
        }

        return returnValue;
    }

    /**
     * Returns the entity to use as the sour4ce of the list data
     *
     * @return Entity e.g. UserEntity
     */
    @Override
    public Class<?> getEntityClass() {
        return ChangeLogEntity.class;
    }
}
