/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.security;

import com.pivotal.utils.Common;
import com.pivotal.utils.I18n;

import java.util.*;

/**
 * Privileges defines the list of Privileges allowed in the system
 * The values are represented by ID and Label, both of which must be unique
 */
public enum Privileges {

    //** List of privileges available in the system
    NONE(0, "system.privilege.none.name", "system.privilege.none.description", "system.privilege.section.profile"),
    EDIT_OWN_EMAIL_ADDRESS(1, "system.privilege.edit_own_email_address.name", "system.privilege.edit_own_email_address.description", "system.privilege.section.profile"),
    MANAGE_OWN_ACCOUNT(2, "system.privilege.manage_own_account.name", "system.privilege.manage_own_account.description", "system.privilege.section.profile"),

    REGISTER_SITE(20, "system.privilege.register_site.name", "system.privilege.register_site.description", "system.privilege.section.site"),
    EDIT_SITE_ADDRESS(21, "system.privilege.edit_site_address.name", "system.privilege.register_site_address.description", "system.privilege.section.site"),
    EDIT_SITE_END_DATE(22, "system.privilege.edit_site_end_date.name", "system.privilege.register_site_end_date.description", "system.privilege.section.site"),
    EDIT_SITE_CONTACT(23, "system.privilege.edit_site_contact.name", "system.privilege.register_site_contact.description", "system.privilege.section.site"),
    EDIT_SITE_PLANNING_APP_NUMBER(24, "system.privilege.edit_site_planning_app_number.name", "system.privilege.edit_site_planning_app_number.description", "system.privilege.section.site"),
    EDIT_SITE_ADMIN_MA(25, "system.privilege.edit_site_admin_ma.name", "system.privilege.edit_site_admin_ma.description", "system.privilege.section.site"),
    EDIT_SITE_ADMIN_SC(26, "system.privilege.edit_site_admin_sc.name", "system.privilege.edit_site_admin_sc.description", "system.privilege.section.site"),
    EDIT_SITE_ADMIN_SA(27, "system.privilege.edit_site_admin_sa.name", "system.privilege.edit_site_admin_sa.description", "system.privilege.section.site"),
    VIEW_SITE_REGISTER(28, "system.privilege.view_site_register.name", "system.privilege.view_site_register.description", "system.privilege.section.site"),
    SITE_REGISTER_MULTI_BOROUGH(29, "system.privilege.site_register_multi_borough.name", "system.privilege.site_register_multi_borough.description", "system.privilege.section.site"),

    REGISTER_MACHINERY(40, "system.privilege.register_machinery.name", "system.privilege.register_machinery.description", "system.privilege.section.machinery"),
    EDIT_MACHINERY(41, "system.privilege.edit_machinery.name", "system.privilege.edit_machinery.description", "system.privilege.section.machinery"),
    OFFSITE_MACHINERY(42, "system.privilege.offsite_machinery.name", "system.privilege.offiste_machinery.description", "system.privilege.section.machinery"),
    ACCEPT_PENDING_MACHINERY(43, "system.privilege.accept_pending_machinery.name", "system.privilege.accept_pending_machinery.description", "system.privilege.section.machinery"),
    REJECT_PENDING_MACHINERY(44, "system.privilege.reject_pending_machinery.name", "system.privilege.reject_pending_machinery.description", "system.privilege.section.machinery"),
    VIEW_ALL_MACHINERY(45, "system.privilege.view_all_machinery.name", "system.privilege.view_all_machinery.description", "system.privilege.section.machinery"),
    VIEW_MACHINERY_FILES(46, "system.privilege.view_machinery_files.name", "system.privilege.view_machinery_files.description", "system.privilege.section.machinery"),
    DELETE_MACHINERY(47, "system.privilege.delete_machinery.name", "system.privilege.delete_machinery.description", "system.privilege.section.machinery"),
    VIEW_MACHINERY_REGISTER(48, "system.privilege.view_machinery_register.name", "system.privilege.view_machinery_register.description", "system.privilege.section.machinery"),
    MACHINERY_REGISTER_MULTI_BOROUGH(49, "system.privilege.machinery_register_multi_borough.name", "system.privilege.machinery_register_multi_borough.description", "system.privilege.section.machinery"),

    APPLICATION_ADMIN(60, "system.privilege.application_admin.name", "system.privilege.application_admin.description", "system.privilege.section.system"),
    SYSTEM_ADMIN(61, "system.privilege.system_admin.name", "system.privilege.system_admin.description", "system.privilege.section.system"),
    PERFORMANCE_ADMIN(62, "system.privilege.performance_admin.name", "system.privilege.performance_admin.description", "system.privilege.section.system");


    //** Array of privileges any of which means the user can see the admin section
    /**
     * Constant <code>HAS_ADMIN_ACCESS</code>
     */
    @SuppressWarnings("unused")
    public static final Privileges[] HAS_ADMIN_ACCESS = {
            APPLICATION_ADMIN,
            SYSTEM_ADMIN
    };

    private String description;
    private String section;
    private String label;
    private int id;

    /**
     * Constructs the enum with a suitable description
     *
     * @param id          Internal ID of the privilege
     * @param label       English label of the privilege
     * @param description English description of the privilege
     * @param section     Section this privilege is in
     */
    Privileges(int id, String label, String description, String section) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.section = section;
    }

    /**
     * Retrieves the internationalised description of the privilege
     *
     * @return Internationalised description
     */
    public String getDescription() {
        return I18n.translate(description);
    }

    /**
     * Retrieves the internationalised section of the privilege
     *
     * @return Internationalised section
     */
    public String getSection() {
        return I18n.translate(section);
    }

    /**
     * Retrieves the internationalised label of the privilege
     *
     * @return Internationalised label
     */
    public String getLabel() {
        return I18n.translate(label);
    }

    /**
     * Retrieves the ID of the privilege
     *
     * @return ID of the object
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the name of the privilege (same as name())
     *
     * @return Name of the object
     */
    public String getName() {
        return name();
    }

    /**
     * Creates a privilege type using a name
     *
     * @param name Textual label of the type
     * @return Privilege
     */
    public static Privileges get(String name) {
        Privileges returnValue = null;
        for (Privileges value : values()) {
            if (Common.doStringsMatch(name, value.name())) {
                returnValue = value;
                break;
            }
        }
        return returnValue;
    }

    /**
     * Creates a privilege type using the id
     *
     * @param id Internal ID of the privilege
     * @return Privilege
     */
    public static Privileges get(int id) {
        Privileges returnValue = null;
        for (Privileges value : values()) {
            if (id == value.id) {
                returnValue = value;
                break;
            }
        }
        return returnValue;
    }

    /**
     * Return a map of privileges keyed on the name
     *
     * @return map of privileges
     */
    public static Map<String, Privileges> getMap() {
        Map<String, Privileges> tmp = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Privileges privileges : values()) {
            if (!privileges.equals(NONE)) {
                tmp.put(privileges.name(), privileges);
            }
        }
        return tmp;
    }

    /**
     * Return a map of privileges for the specified list of IDs
     *
     * @param idList Array of privilege IDs
     * @return List of privileges or null if none known
     */
    public static Map<String, Privileges> getMap(String... idList) {
        Map<String, Privileges> returnValue = new TreeMap<>();
        if (!Common.isBlank(idList)) {
            for (String id : idList) {
                Privileges privilege = get(Common.parseInt(id));
                if (privilege != null)
                    returnValue.put(privilege.name(), privilege);
            }
        }
        return returnValue.isEmpty() ? null : returnValue;
    }

    /**
     * Return a map of privileges for the specified list of IDs keyed on the name
     *
     * @param idList Array of privilege IDs
     * @return List of privileges or null if none known
     */
    @SuppressWarnings("unused")
    public static Map<String, Privileges> getMap(String idList) {
        if (Common.isBlank(idList))
            return null;
        else
            return getMap(idList.split(" *, *"));
    }

    /**
     * Return a map of privileges for the specified list of IDs keyed on the label
     *
     * @param idList Array of privilege IDs
     * @return List of privileges or null if none known
     */
    @SuppressWarnings("unused")
    public static Map<String, Privileges> getLabelMap(String idList) {
        if (Common.isBlank(idList))
            return null;
        else {
            Map<String, Privileges> returnValue = null;
            Map<String, Privileges> tmp = getMap(idList.split(" *, *"));
            if (!Common.isBlank(tmp)) {
                returnValue = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (Privileges privilege : tmp.values()) {
                    returnValue.put(privilege.getLabel(), privilege);
                }
            }
            return returnValue;
        }
    }

    /**
     * Return a map of privileges for the specified list of IDs keyed on the label
     *
     * @param idList Array of privilege IDs
     * @return List of privileges or null if none known
     */
    public static List<String> getLabels(String idList) {
        if (Common.isBlank(idList))
            return null;
        else {
            List<String> returnValue = null;
            Map<String, Privileges> tmp = getMap(idList.split(" *, *"));
            if (!Common.isBlank(tmp)) {
                returnValue = new ArrayList<>();
                for (Privileges privilege : tmp.values()) {
                    returnValue.add(privilege.getLabel());
                }
                Common.sortList(returnValue);
            }
            return returnValue;
        }
    }

    /**
     * Return a list of privileges
     *
     * @return List of privileges
     */
    public static Collection<Privileges> getList() {
        return getMap().values();
    }

    /**
     * Return a list of privileges
     *
     * @return List of privileges
     */
    public static Map<String, TreeMap<String, Privileges>> getGroupedSortedList() {

        TreeMap<String, TreeMap<String, Privileges>> returnValue = new TreeMap<>();

        for (Privileges privileges : getList()) {

            if (!returnValue.containsKey(privileges.getSection())) {
                returnValue.put(privileges.getSection(), new TreeMap<String, Privileges>());
            }
            returnValue.get(privileges.getSection()).put((privileges.getId() + 100) + "|" + privileges.getLabel(), privileges);
        }
        return returnValue;
    }

    /**
     * Return a list of privileges in a format suitable for display in a form
     *
     * @return List of privileges
     */
    @SuppressWarnings("unused")
    public static Collection<Map<String, String>> getListDisplay() {
        List<Map<String, String>> returnValue = new ArrayList<>();

        // if current user has admin Privileges then can see all privileges

        if (UserManager.isCurrentUserAdministrator()) {
            for (Privileges privileges : values()) {
                returnValue.add(Common.getMapFromPairs("text", privileges.getLabel(), "value", privileges.id + ""));
            }
        }
        else {

            // Second case only those privileges shows what current user have

//            for (Privileges privileges : values()) {
//                if(UserManager.getCurrentUser().hasPrivilege(privileges)){
//                    returnValue.add(Common.getMapFromPairs("text", privileges.getLabel(), "value", privileges.id + ""));
//                }
//            }
        }
        return Common.sortList(returnValue, "text");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s [%s][%d] - %s", getLabel(), name(), getId(), getDescription());
    }

}
