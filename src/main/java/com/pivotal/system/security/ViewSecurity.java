/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.security;

import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;

import java.util.List;

/**
 *
 */
public class ViewSecurity {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ViewSecurity.class);

    final private static int TIME_TO_LIVE = 10;
    final private static String CACHE_KEY_DELIMITER = "|";

    /**
     * UserNoteSearch for notes
     * @param ownerId       Owner of note
     * @param whereClause   Restriction
     *
     * @return List of objects either fresh or cached
     */
    public static List<Object> userNoteSearch(Integer ownerId, String whereClause) {

        List<Object>returnValue;

        // Check cache

        String cacheKey = buildCacheKey("userNoteSearch", ownerId, whereClause);

        returnValue = CacheEngine.get(cacheKey);
        if (Common.isBlank(returnValue) && !Common.isBlank(ownerId)) {

            String query = "select note_id from user_note_search where owner_id = " + ownerId + (Common.isBlank(whereClause)?"":" and " + whereClause);

            logger.debug("Searching user_note_search with query {}", query);

            returnValue = HibernateUtils.selectSQLEntities(query);

            CacheEngine.put(cacheKey, TIME_TO_LIVE, returnValue);
        }
        else
            logger.debug("UserNoteSearch Data loaded from cache");

        return returnValue;
    }

    /**
     * caseUserAccessSearch for Case access
     *
     * @param returnField   Field to return in list
     * @param userId        User to check for
     * @param caseId        Case to check for
     * @param roleIds       List of Roles to check for
     *
     * @return List of objects either fresh or cached
     */
    public static List<Object> caseUserAccessSearch(String returnField, Integer userId, Integer caseId, String roleIds) {

        List<Object>returnValue = null;

        if (!Common.isBlank(returnField)) {
            // Check cache
            String cacheKey = buildCacheKey("caseUserAccessSearch", returnField, userId, caseId, roleIds);
            returnValue = CacheEngine.get(cacheKey);

            if (Common.isBlank(returnValue)) {

                String query = "";

                if (!Common.isBlank(userId))
                    query += (Common.isBlank(query) ? "" : " and ") + "users_id = " + userId;

                if (!Common.isBlank(caseId))
                    query += (Common.isBlank(query) ? "" : " and ") + "case_id = " + caseId;

                if (!Common.isBlank(roleIds))
                    query += (Common.isBlank(query) ? "" : " and ") + "role_id in (" + roleIds + ")";

                query = "select " + returnField + " from case_user_access_search where " + query;

                logger.debug("Searching case_user_access_search with query " + query);

                returnValue = HibernateUtils.selectSQLEntities(query);

                CacheEngine.put(cacheKey, TIME_TO_LIVE, returnValue);
            }
            else
                logger.debug("CaseUserAccessSearch Data loaded from cache");
        }
        else
            logger.error("Unable to query data as return field not specified");

        return returnValue;
    }

    /**
     * meetingUserAccessSearch for Meeting access
     *
     * @param returnField   Field to return in list
     * @param userId        User to check for
     * @param meetingId     Meeting to check for
     * @param roleIds       Roles to check for
     *
     * @return List of objects either fresh or cached
     */
    public static List<Object> meetingUserAccessSearch(String returnField, Integer userId, Integer meetingId, String roleIds) {

        List<Object>returnValue = null;

        if (!Common.isBlank(returnField)) {

            // Check cache

            String cacheKey = buildCacheKey("meetingUserAccessSearch", returnField, userId, meetingId);
            returnValue = CacheEngine.get(cacheKey);

            if (Common.isBlank(returnValue)) {

                String query = "";

                if (!Common.isBlank(userId))
                    query += (Common.isBlank(query)?"":" and ") + "users_id = " + userId;

                if (!Common.isBlank(meetingId))
                    query += (Common.isBlank(query)?"":" and ") + "meeting_id = " + meetingId;

                if (!Common.isBlank(roleIds))
                    query += (Common.isBlank(query)?"":" and ") + "role_id in ( " + meetingId + ")";

                query = "select " + returnField + " from meeting_user_access_search where " + query;

                logger.debug("Searching meeting_user_access_search with query " + query);

                returnValue = HibernateUtils.selectSQLEntities(query);

                CacheEngine.put(cacheKey, TIME_TO_LIVE, returnValue);
            }
            else
                logger.debug("MeetingUserAccessSearch Data loaded from cache");
        }
        else
            logger.error("Unable to query data as return field not specified");
        return returnValue;
    }

    /**
     * userRoleSearch for user roles
     *
     * @param returnField   Field to return in list
     * @param userId        User to check for
     * @param roleIds       List of Roles to check for
     *
     * @return List of objects either fresh or cached
     */
    public static List<Object> userRoleSearch(String returnField, Integer userId, String roleIds) {

        List<Object>returnValue = null;

        if (!Common.isBlank(returnField)) {
            // Check cache
            String cacheKey = buildCacheKey("userRoleSearch", returnField, userId, roleIds);
            returnValue = CacheEngine.get(cacheKey);

            if (Common.isBlank(returnValue)) {

                String query = "";

                if (!Common.isBlank(userId))
                    query += (Common.isBlank(query) ? "" : " and ") + "users_id = " + userId;

                if (!Common.isBlank(roleIds))
                    query += (Common.isBlank(query) ? "" : " and ") + "role_id in (" + roleIds + ")";

                query = "select " + returnField + " from user_role_search where " + query;

                logger.debug("Searching user_role_search with query " + query);

                returnValue = HibernateUtils.selectSQLEntities(query);

                CacheEngine.put(cacheKey, TIME_TO_LIVE, returnValue);
            }
            else
                logger.debug("UserRoleSearch Data loaded from cache");
        }
        else
            logger.error("Unable to query data as return field not specified");

        return returnValue;
    }

    /**
     * Builds a key for use in the cache from the components
     *
     * @param components    Items to be used in building the key
     *
     * @return Cache Key
     */
    private static String buildCacheKey(Object... components) {

        String returnValue = "";
        for(Object element : components) {
            if (!Common.isBlank(returnValue)) returnValue += CACHE_KEY_DELIMITER;
            if (Common.isBlank(element))
                returnValue += "NULL";
            else
                returnValue += String.valueOf(element);
        }

        return returnValue;
    }
}
