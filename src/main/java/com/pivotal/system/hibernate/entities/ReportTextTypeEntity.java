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
@Table(name = "report_text_type")
public class ReportTextTypeEntity extends AbstractEntity implements Serializable {

    //TODO These id constants should be names as per the roleentity

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportTextTypeEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String name;
    private boolean internal;
    private boolean disabled;
//    private MediaEntity icon;

    private Integer id;

    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        logger.debug("Set id to " + id);
    }

    @Basic
    @Column(name = "name", nullable = false, insertable = true, updatable = true, length = 100)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "disabled", nullable = true, insertable = true, updatable = true)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Basic
    @Column(name = "internal", nullable = true, insertable = true, updatable = true)
    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "icon", referencedColumnName = "id")
//    public MediaEntity getIcon() {
//        return icon;
//    }
//
//    public void setIcon(MediaEntity media) {
//        this.icon = media;
//    }
//
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReportTextTypeEntity)) return false;
        final ReportTextTypeEntity other = (ReportTextTypeEntity) obj;
        return  Objects.equals(this.internal, other.internal) &&
                Objects.equals(this.disabled, other.disabled) &&
                Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return "ReportTextTypeEntity{" +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", internal='" + internal + '\'' +
                ", disabled='" + disabled + '\'' +
               '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(internal, disabled, name);
    }
}
