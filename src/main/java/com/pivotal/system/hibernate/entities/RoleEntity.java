/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * <p>RoleEntity class.</p>
 */
@Entity
@Table(name = "role")
public class RoleEntity extends AbstractEntity implements Serializable {

    // Special ROLE names used for initialisation elsewhere

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RoleEntity.class);
    private static final long serialVersionUID = -8525448347234966540L;

    private Integer id;
    private String name;
    private String description;
    private Boolean administrator;
    private String privileges;
    private String privilegeAccess;
    private Boolean disabled;
    private RoleTypeEntity type;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id a {@link java.lang.Integer} object.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Basic
    @Column(name = "name", unique = true, nullable = false, length = 100, precision = 0)
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Basic
    @Column(name = "description", length = 65535, precision = 0)
    public String getDescription() {
        return description;
    }

    /**
     * <p>Setter for the field <code>description</code>.</p>
     *
     * @param description a {@link java.lang.String} object.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>isAdministrator.</p>
     *
     * @return a Boolean.
     */
    @Basic
    @Column(name = "administrator", nullable = false, length = 0, precision = 0)
    public Boolean getAdministrator() {
        return administrator;
    }

    /**
     * <p>Setter for the field <code>administrator</code>.</p>
     *
     * @param administrator a Boolean.
     */
    public void setAdministrator(Boolean administrator) {
        this.administrator = administrator;
    }

    @Transient
    public Boolean isAdministrator() {
        return administrator;
    }
    /**
     * <p>isDisabled.</p>
     *
     * @return a Boolean.
     */
    @Basic
    @Column(name = "disabled", nullable = false, length = 0, precision = 0)
    public Boolean getDisabled() {
        return disabled;
    }

    /**
     * <p>Setter for the field <code>disabled</code>.</p>
     *
     * @param disabled a Boolean.
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }


    @Transient
    public Boolean isDisabled() {
        return disabled;
    }

    /**
     * <p>Getter for the field <code>privileges</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Basic
    @Column(name = "privileges", length = 150, precision = 0)
    public String getPrivileges() {
        return privileges;
    }

    /**
     * <p>Setter for the field <code>privileges</code>.</p>
     *
     * @param privileges a {@link java.lang.String} object.
     */
    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    /**
     * <p>Getter for the field <code>privileges</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Basic
    @Column(name = "privilege_access", length = 150, precision = 0)
    public String getPrivilegeAccess() {
        return privilegeAccess;
    }

    /**
     * <p>Setter for the field <code>privileges</code>.</p>
     *
     * @param privilegeAccess a {@link java.lang.String} object.
     */
    public void setPrivilegeAccess(String privilegeAccess) {
        this.privilegeAccess = privilegeAccess;
    }

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = true)
    public RoleTypeEntity getType() {
        return type;
    }

    public void setType(RoleTypeEntity roleType) {
        this.type = roleType;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RoleEntity)) return false;
        final RoleEntity other = (RoleEntity) obj;
        return Objects.equals(this.administrator, other.administrator) &&
               Objects.equals(this.description, other.description) &&
               Objects.equals(this.disabled, other.disabled) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.type, other.type) &&
               Objects.equals(this.privileges, other.privileges) &&
               Objects.equals(this.privilegeAccess, other.privilegeAccess);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "RoleEntity{" +
               "administrator='" + administrator + '\'' +
               ", description='" + description + '\'' +
               ", disabled='" + disabled + '\'' +
               ", id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", privileges='" + privileges + '\'' +
               ", privilegeAccess='" + privilegeAccess + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(administrator, description, disabled, name, type, privileges, privilegeAccess);
    }
}
