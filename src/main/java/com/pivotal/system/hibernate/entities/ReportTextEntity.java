/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.utils.Common;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "report_text")
public class ReportTextEntity extends AbstractEntity implements Serializable {

    /** Constant <code>LAYOUT_PORTRAIT = "system.report_text.layout.portrait"</code> */
    public static final String LAYOUT_PORTRAIT = "system.report_text.layout.portrait";
    /** Constant <code>LAYOUT_LANDSCAPE = "system.report_text.layout.landscape"</code> */
    public static final String LAYOUT_LANDSCAPE = "system.report_text.layout.landscape";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReportTextEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String name;
    private String description;
    private boolean disabled;
    private ReportTextTypeEntity type;
    private String text;
    private String layout;
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
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "layout")
    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
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
    @Column(name = "text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Transient
    public String getTextStart() {
        String retValue = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(text);

        if (!Common.isBlank(retValue)) {

            if (retValue.length() > 100)
                retValue = retValue.substring(0,97) + "...";
        }

        return retValue;
    }


    @Basic
    @Column(name = "disabled", nullable = true, insertable = true, updatable = true)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    public ReportTextTypeEntity getType() {
        return type;
    }

    public void setType(ReportTextTypeEntity type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, type, text, disabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReportTextEntity)) return false;
        final ReportTextEntity other = (ReportTextEntity) obj;
        return  Objects.equals(this.name, other.name) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.type, other.type) &&
                Objects.equals(this.text, other.text) &&
                Objects.equals(this.disabled, other.disabled);
    }

    @Override
    public String toString() {
        return "ReportTextEntity{" +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", text='" + text + '\'' +
                ", disabled='" + disabled + '\'' +
               '}';
    }
}
