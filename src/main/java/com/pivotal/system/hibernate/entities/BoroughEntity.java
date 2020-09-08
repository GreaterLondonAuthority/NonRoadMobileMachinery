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
@Table(name = "borough")
public class BoroughEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BoroughEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String name;
    private boolean disabled;

    private Integer id;

    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        logger.debug("Setting id to {}", id);
    }

    @Basic
    @Column(name = "name", nullable = false, length = 250)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "disabled", length = 0, precision = 0)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BoroughEntity)) return false;
        final BoroughEntity other = (BoroughEntity) obj;
        return  Objects.equals(this.name, other.name) &&
                Objects.equals(this.disabled, other.disabled);
    }

    @Override
    public String toString() {
        return "BoroughEntity{" +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", disabled='" + disabled + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, disabled);
    }
}
