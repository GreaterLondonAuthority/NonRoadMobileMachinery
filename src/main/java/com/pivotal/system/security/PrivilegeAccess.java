/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.system.security;

/**
 *
 */
public class PrivilegeAccess {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrivilegeAccess.class);

    private Privileges privilege;
    private String access = "";

    /**
     * Set privilege and what access we have
     *
     * @param privilege    Privilege
     * @param access        Access for this privilege
     */
    PrivilegeAccess (Privileges privilege, String access) {

        this.setPrivilege(privilege);
        this.setAccess(access);
    }

    /**
     * Getter for Privileges
     *
     * @return Privileges
     */
    public Privileges getPrivilege() {
        return privilege;
    }

    /**
     * Setter for Privileges
     *
     * @param privilege to set
     */
    private void setPrivilege(Privileges privilege) {
        this.privilege = privilege;
    }

    /**
     * Getter for access access
     *
     * @return accessType
     */
    public String getAccess() {
        return access;
    }

    private void setAccess(String access) {
        this.access = access;
    }
}
