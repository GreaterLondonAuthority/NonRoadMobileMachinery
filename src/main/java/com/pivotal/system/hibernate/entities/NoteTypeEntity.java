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
@Table(name = "note_type")
public class NoteTypeEntity extends AbstractEntity implements Serializable {

    /** Constant <code>NOTE_TYPE_GENERAL = "system.note_type.general.name"</code> */
    public static final String NOTE_TYPE_GENERAL = "system.note_type.general.name";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NoteTypeEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private String name;
    private String description;
    private boolean internal;
    private boolean disabled;

    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        logger.debug("Set id {}", id);
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
    @Column(name = "description", nullable = true, insertable = true, updatable = true, length = 250)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (!(obj instanceof NoteTypeEntity)) return false;
        final NoteTypeEntity other = (NoteTypeEntity) obj;
        return  Objects.equals(this.internal, other.internal) &&
                Objects.equals(this.disabled, other.disabled) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.name, other.name);
        }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, internal);
    }
}
