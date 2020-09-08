/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.data.cache.CacheEngine;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.system.security.Preferences;
import com.pivotal.system.security.Privileges;
import com.pivotal.system.security.UserManager;
import com.pivotal.utils.Common;
import com.pivotal.web.controllers.AbstractController;
import com.pivotal.web.servlet.ServletHelper;
import org.hibernate.validator.constraints.Email;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import static com.pivotal.utils.Common.isBlank;

/**
 *
 */
@Table(name = "users")
@Entity
public class UserEntity extends AbstractEntity implements Serializable {

    public static final String DEFAULT_USER_NAME = "admin";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserEntity.class);
    private static final long serialVersionUID = 1745547653677932714L;

    private Integer id;
    private String firstname;
    private String lastname;
    private String password;
    private String previousPasswords;
    private String address;
    private Timestamp expires;
    private String email;
    private String phoneNumber;
    private String preferencesXML;
    private boolean disabled;
    private Boolean sendEmails;
    private Boolean receiveEmails;
    private RoleEntity role;
    private Timestamp validFrom;
    private Timestamp lastLoggedIn;
    private Integer loginFailCount;
    private boolean confirmed;
    private String newEmail;
    private Set<SiteUsersEntity> siteUsers = new HashSet<>(0);
    private BoroughEntity borough;

    @Column(name = "id", nullable = false, length = 10)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Transient
    public String getName() {

        String returnValue = firstname;
        return isBlank(returnValue)?(isBlank(lastname)?"": lastname):(returnValue + (isBlank(lastname)?"":(" " + lastname)));
    }

    @Column(name = "firstname")
    @Basic
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @Column(name = "lastname")
    @Basic
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }


    @Column(name = "send_emails", length = 5)
    @Basic
    public Boolean getSendEmails() {
        return sendEmails;
    }

    public void setSendEmails(Boolean sendEmails) {
        this.sendEmails = sendEmails;
    }

    @Column(name = "receive_emails", length = 5)
    @Basic
    public Boolean getReceiveEmails() {
        return receiveEmails;
    }

    public void setReceiveEmails(Boolean receiveEmails) {
        this.receiveEmails = receiveEmails;
    }

    @Column(name = "password")
    @Basic
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name = "previous_passwords")
    @Basic
    public String getPreviousPasswords() {
        return previousPasswords;
    }

    public void setPreviousPasswords(String previousPasswords) {
        this.previousPasswords = previousPasswords;
    }

    @Column(name = "address")
    @Basic
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Column(name = "disabled", length = 5)
    @Basic
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Column(name = "confirmed", length = 5)
    @Basic
    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    @Column(name = "expires")
    @Basic
    public Timestamp getExpires() {
        return expires;
    }

    public void setExpires(Timestamp expires) {
        this.expires = expires;
    }

    @Email
    @Column(name = "email")
    @Basic
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Email
    @Column(name = "new_email")
    @Basic
    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    @Column(name = "phone_number")
    @Basic
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Column(name = "valid_from")
    @Basic
    public Timestamp getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Timestamp validFrom) {
        this.validFrom = validFrom;
    }

    @Transient
    public boolean isValid() {
        return isBlank(validFrom) || validFrom.before(Common.getDateTime());
    }

    @Column(name = "login_fail_count", nullable = false, length = 10)
    @Basic
    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    @Column(name = "last_logged_in")
    @Basic
    public Timestamp getLastLoggedIn() {
        return lastLoggedIn;
    }

    public void setLastLoggedIn(Timestamp lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    @Transient
    public void setLastLoggedInNow() {
        setLastLoggedIn(new Timestamp(new Date().getTime()));
    }

    @Column(name = "preferences", length = 2147483647)
    @Basic
    public String getPreferencesXML() {
        return preferencesXML;
    }

    public void setPreferencesXML(String preferencesXML) {
        this.preferencesXML = preferencesXML;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "primaryKey.user")
    public Set<SiteUsersEntity> getSiteUsers() {
        return siteUsers;
    }

    public void setSiteUsers(Set<SiteUsersEntity> siteUsers) {
        this.siteUsers = siteUsers;
    }

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @ManyToOne
    @JoinColumn(name = "borough_id", referencedColumnName = "id")
    public BoroughEntity getBorough() {
        return borough;
    }

    public void setBorough(BoroughEntity borough) {
        this.borough = borough;
    }

    /**
     * Returns the preferences object to use to get at their preferences etc.
     * This method works out the default namespace to use by calling the Spring
     * request mapping service to match the current request up to a suitable class
     *
     * @param <T> The value type of the preferences
     * @return Preferences object
     */
    @Transient
    public <T> Preferences<T> getPreferences() {

        // Create a namespace based on the current request if there is one

        String namespace = null;
        if (ServletHelper.getRequest() != null) {
            namespace = (String) ServletHelper.getRequest().getAttribute(AbstractController.NAMESPACE_ATTRIBUTE);
        }
        return getPreferences(namespace);
    }

    /**
     * Returns the preferences object to use to get as their preferences etc
     *
     * @param <T>   The value type of the preferences
     * @param namespace Namespace to use
     *
     * @return Preferences object
     */
    @Transient
    public <T> Preferences<T> getPreferences(String namespace) {

        // Get the preferences from the cache

        Preferences<T> cachedPreferences = CacheEngine.get(email + "-preferences");
        if (cachedPreferences == null) {
            cachedPreferences = new Preferences<>(this, namespace);
            CacheEngine.put(email + "-preferences", 600, cachedPreferences);
        }

        // Return a clone of the preferences
        return cachedPreferences.clone(namespace);
    }

    /**
     * Returns true if user is admin
     *
     * @return isAdmin flag
     */
    @Transient
    public Boolean isAdministrator() {
        return false;
    }

    /**
     * Looks for privilege id in list
     * @param privileges list of privileges to check
     *
     * @return true if uer has access to any of the privileges
     */
    @Transient
    public Boolean hasAccess(Privileges... privileges) {

        boolean returnValue = false;

        if (privileges != null && privileges.length > 0 && this.role != null) {
            Map<Integer, String>privMap = new HashMap<>();
            for(String privId : Common.splitToList(this.role.getPrivileges(), ","))
                privMap.put(Common.parseInt(privId), privId);

            int index=0;
            while(!returnValue && index<privileges.length)
                returnValue = privMap.containsKey(privileges[index++].getId());
        }

        return returnValue;
    }

    /**
     * Looks for privilege name in list
     * @param names list of privilege names to check
     *
     * @return true if uer has access to any of the privileges
     */
    @Transient
    public Boolean hasAccess(String... names) {

        boolean returnValue = isAdministrator();
        if (!returnValue && !Common.isBlank(names)) {
            for (String name : names) {
                returnValue = getPrivilegesMap(this.role).containsKey(name);
                if (returnValue) break;
            }
        }
        return returnValue;
    }


    /**
     * Looks for privilege id in list
     * @param siteEntity Site to check against
     * @param privileges list of privileges to check
     *
     * @return true if user has access to any of the privileges
     */
    @Transient
    public Boolean hasAccess(SiteEntity siteEntity, Privileges... privileges) {

         boolean returnValue = hasAccess(privileges);

        if (!returnValue && privileges != null && privileges.length > 0 && this.role != null) {
            Map<Integer, String>privMap = new HashMap<>();
            Integer siteId = siteEntity.getId();
            Integer userId = this.getId();
            List<RoleEntity> roleEntities = HibernateUtils.selectEntities("SELECT su.role From SiteUsersEntity su where su.primaryKey.site.id = ? and su.primaryKey.user.id = ?", siteId, userId);

            if (roleEntities != null && roleEntities.size() > 0) {
                for (String privId : Common.splitToList(roleEntities.get(0).getPrivileges(), ","))
                    privMap.put(Common.parseInt(privId), privId);

                int index = 0;
                while (!returnValue && index < privileges.length)
                    returnValue = privMap.containsKey(privileges[index++].getId());
            }
        }

        return returnValue;
    }

    /**
     * Looks for privilege name in list
     * @param siteEntity Site to check against
     * @param names list of privilege names to check
     *
     * @return true if uer has access to any of the privileges
     */
    @Transient
    public Boolean hasAccess(SiteEntity siteEntity, String... names) {

        boolean returnValue = hasAccess(names);

        if (!returnValue && !Common.isBlank(names)) {
            List<RoleEntity> roleEntities = HibernateUtils.selectEntities("SELECT su.role From SiteUserEntity su where su.site = ? and su.user = ?", siteEntity, this);

            if (roleEntities != null && roleEntities.size() > 0) {
                Map<String, Privileges> privilegesMap = getPrivilegesMap(roleEntities.get(0));
                for (String name : names) {
                    returnValue = privilegesMap.containsKey(name);
                    if (returnValue) break;
                }
            }
        }
        return returnValue;
    }

    /**
     * Returns a map of privileges this user has via the role
     *
     * @return Map of privileges
     */
    @Transient
    public Map<String, Privileges> getPrivilegesMap(RoleEntity roleEntity) {

        Map<String, Privileges> privilegesMap = new HashMap<>();

        if (roleEntity != null) {
            privilegesMap = UserManager.getPrivilegeMap(roleEntity.getPrivileges());
        }

        return privilegesMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserEntity)) return false;
        final UserEntity other = (UserEntity) obj;
        return  Objects.equals(this.address, other.address) &&
                Objects.equals(this.disabled, other.disabled) &&
                Objects.equals(this.confirmed, other.confirmed) &&
                Objects.equals(this.email, other.email) &&
                Objects.equals(this.newEmail, other.newEmail) &&
                Objects.equals(this.expires, other.expires) &&
                Objects.equals(this.validFrom, other.validFrom) &&
                Objects.equals(this.loginFailCount, other.loginFailCount) &&
                Objects.equals(this.lastLoggedIn, other.lastLoggedIn) &&
                Objects.equals(this.firstname, other.firstname) &&
                Objects.equals(this.lastname, other.lastname) &&
                Objects.equals(this.password, other.password) &&
                Objects.equals(this.phoneNumber, other.phoneNumber) &&
                Objects.equals(this.preferencesXML, other.preferencesXML) &&
                Objects.equals(this.borough, other.borough) &&
                Objects.equals(this.previousPasswords, other.previousPasswords) &&
                Objects.equals(this.receiveEmails, other.receiveEmails);
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "address='" + address + '\'' +
                ", disabled='" + disabled + '\'' +
                ", confirmed='" + confirmed + '\'' +
                ", email='" + email + '\'' +
                ", newEmail='" + newEmail + '\'' +
                ", expires='" + expires + '\'' +
                ", validFrom='" + validFrom + '\'' +
                ", lastLoggedIn='" + lastLoggedIn + '\'' +
                ", loginFailCount='" + loginFailCount + '\'' +
                ", id='" + id + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", password='" + password + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", preferencesXML='" + preferencesXML + '\'' +
                ", borough='" + borough + '\'' +
                ", previousPasswords='" + previousPasswords + '\'' +
                ", receiveEmails='" + receiveEmails + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, disabled, confirmed, email, newEmail, expires, validFrom, loginFailCount, lastLoggedIn, firstname, lastname, password, phoneNumber, preferencesXML, borough, previousPasswords, receiveEmails);
    }
}
