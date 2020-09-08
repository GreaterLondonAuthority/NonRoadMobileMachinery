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

@Entity
@Table(name = "role_type")
public class RoleTypeEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RoleTypeEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String name;
    private String description;

    private Integer id;

    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    @Basic
    @Column(name = "name", nullable = false, insertable = true, updatable = true, length = 250)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 250)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private boolean disabled;

    @Basic
    @Column(name = "disabled", nullable = false, insertable = true, updatable = true)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    private boolean internal;

    @Basic
    @Column(name = "internal", nullable = true, insertable = true, updatable = true)
    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RoleTypeEntity)) return false;
        final RoleTypeEntity other = (RoleTypeEntity) obj;
        return  Objects.equals(this.description, other.description) &&
                Objects.equals(this.disabled, other.disabled) &&
                Objects.equals(this.internal, other.internal) &&
                Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return "RoleTypeEntity{" +
                ", id='" + id + '\'' +
                ", disabled='" + disabled + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", internal='" + internal + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(disabled, internal,  name, description);
    }

}
