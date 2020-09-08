/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.system.hibernate.entities.RoleEntity;
import com.pivotal.system.hibernate.entities.UserEntity;
import com.pivotal.system.hibernate.entities.UserRoleEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.CaseManager;
import com.pivotal.system.security.Privileges;
import com.pivotal.system.security.UserManager;
import com.pivotal.system.security.ViewSecurity;
import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.controllers.utils.GridResults;
import com.pivotal.web.controllers.utils.JsonResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import static com.pivotal.utils.Common.isBlank;
import static com.pivotal.utils.Common.splitToList;

/**
 * Handles requests for storing and managing Roles
 */
@Authorise
@Controller
@RequestMapping(value = "/admin/role")
public class RoleController extends AbstractAdminController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RoleController.class);

    /** {@inheritDoc} */
    @Override
    public Class getEntityClass() {
        return RoleEntity.class;
    }

    /** {@inheritDoc} */
    @Override
    public GridResults getGridData() {
        GridResults returnValue = super.getGridData();
        if (!isBlank(returnValue.getData())) {
            for (Map<String, Object> row : returnValue.getData()) {
                if (row.containsKey("privileges")) {
                    row.put("privileges", Common.join(Privileges.getLabels((String)row.get("privileges")), ", "));
                }
            }
        }
        return returnValue;
    }

    /**
     * Adds any controller specific objects to the model
     * It should be overridden for this to happen
     *
     * @param model     Model to add to
     * @param entity    Entity currently under edit
     * @param id        The ID of the entity under edit
     * @param editState The type of edit
     */
    protected void addAttributesToModel(Model model, Object entity, Integer id, EditStates editState) {

        // add CaseManager to allow getting of lookup lists

        model.addAttribute("CaseManager",CaseManager.class);

        // add UserManager to check user have right to create role of right admin

        model.addAttribute("UserManager", UserManager.class);

        if (!isBlank(id)) {
            List<Object> userIds = ViewSecurity.userRoleSearch("distinct users_id", null, String.valueOf(id));

            if (!isBlank(userIds)) {
                List<UserEntity> allUsers = HibernateUtils.selectEntitiesNamedParameters("From UserEntity where Id in (:userIds) order by name", "userIds", userIds);
                model.addAttribute("RoleUsers", allUsers);
            }
        }
    }

    /**
     * This will check if there is any user with this role and send back the names to view
     *
     * @param id ID of the data collector entity
     * @return JSON response
     */
    @RequestMapping(value = "/action", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse actionCollector(@RequestParam(value = "id") int id) {


        RoleEntity roleEntity = HibernateUtils.getEntity(RoleEntity.class, id);
        JsonResponse returnValue = new JsonResponse();
        if (roleEntity == null) {
            logger.error("Role entity for id [{}] - doesn't exist", id);
            returnValue.setError(I18n.getString("roleentity.id.error"));
        }
        else {


            String result = "";
            int count = 0;
            List<UserRoleEntity> userRoleEntities = HibernateUtils.selectEntities("from UserRoleEntity where role = ?", roleEntity);
            for (UserRoleEntity userRole : userRoleEntities) {
                if (count != 0) {
                    result += " , ";
                }
                if (count >= 9) {
                    result += userRole.getUser().getName() + "...";
                    break;
                }
                result += userRole.getUser().getName();
                count++;
            }
            returnValue.setInformation(result);
        }
        return returnValue;
    }
    /**
     * This will check if there is any user with this role and send back the names to view
     *
     * @param id ID of the data collector entity
     * @param subRoles role ids to check
     * @return JSON response
     */
    @RequestMapping(value = "/checksubroles", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonResponse checkSubRoles(@RequestParam(value = "id") int id, @RequestParam(value = "subroles") String subRoles) {

        JsonResponse returnValue = new JsonResponse();

        if (!Common.isBlank(subRoles)) {

            String roleName;

            // Prevent self referencing roles

            boolean subRolesOk = true;
            List<String> subRoleList = splitToList(subRoles, ",");
            for (int index = 0; index < subRoleList.size() && subRolesOk; index++)
                subRolesOk = !String.valueOf(id).equals(subRoleList.get(index));

            if (!subRolesOk) {
                RoleEntity roleEntity = HibernateUtils.getEntity(RoleEntity.class, id);
                if (roleEntity != null)
                    roleName = roleEntity.getName();
                else
                    roleName = "id=" + id;

                returnValue.setError(String.format(I18n.getString("roleentity.error.self_reference"), roleName));
            }
            else {

                // Check for circular reference, build up query
                StringBuilder query = new StringBuilder();
                for (int index = 0; index < subRoleList.size(); index++)
                    query.append(String.format("%s (parent_role_id=%s and sub_role_id=%d)", index == 0 ? "" : " or ", subRoleList.get(index), id));

                List<Object> results = HibernateUtils.selectSQLEntities(String.format("select name from role where id in (select parent_role_id from role_search where %s)", query));
                if (results.size() > 0) {
                    roleName = results.get(0).toString();
                    returnValue.setError(String.format(I18n.getString("roleentity.error.circular_reference"), roleName));
                }
            }
        }

        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public void beforeSave(HttpSession session, HttpServletRequest request, Model model, Object entityObject, Integer id, EditStates editState, BindingResult result) {

        RoleEntity entity = (RoleEntity)entityObject;

        if (!result.hasErrors()) {

            // Split out the privileges from the form

            Map<String, String> formPrivs = UserManager.extractPrivilegesFromForm(request);

            entity.setPrivileges(Common.join(Common.join(formPrivs.keySet(), ","), ","));
            entity.setPrivilegeAccess(Common.join(Common.join(formPrivs.values(), ","), ","));

            logger.debug("Privileges set to {}", entity.getPrivileges());

        }
    }
}
