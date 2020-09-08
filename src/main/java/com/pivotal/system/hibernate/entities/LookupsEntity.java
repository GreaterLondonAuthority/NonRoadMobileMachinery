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
@Table(name = "lookups")
public class LookupsEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LookupsEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private String name;
    private String description;
    private String type;
    private String tag;
    private boolean disabled;


    @Id
    @Column(name = "id", nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        logger.debug("Setup LookupsEntity.name = {}", name);
        this.name = name;
    }

    @Basic
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "type", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Basic
    @Column(name = "tag")
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Basic
    @Column(name = "disabled", nullable = false)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LookupsEntity)) return false;
        final LookupsEntity other = (LookupsEntity) obj;
        return  Objects.equals(this.name, other.name) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.type, other.type) &&
                Objects.equals(this.tag, other.tag) &&
                Objects.equals(this.disabled, other.disabled);
    }

    @Override
    public String toString() {
        return "LookupsEntity{" +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", tag='" + tag + '\'' +
                ", disabled='" + disabled + '\'' +
                 '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, tag, disabled);
    }

}
