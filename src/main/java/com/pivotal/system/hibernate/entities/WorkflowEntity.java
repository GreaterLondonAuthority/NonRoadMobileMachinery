/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "workflow")
public class WorkflowEntity extends AbstractEntity implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private String name;
    private String code;
    private String description;
    private String script;
    private boolean disabled;

    @Id
    @Column(name = "id", nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        logger.debug("Id set to {}", id);
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
    @Column(name = "code", nullable = false, length = 250)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Basic
    @Column(name = "description", length = 250)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "script")
    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
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
        if (!(obj instanceof WorkflowEntity)) return false;
        final WorkflowEntity other = (WorkflowEntity) obj;
        return  Objects.equals(this.name, other.name) &&
                Objects.equals(this.code, other.code) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.disabled, other.disabled) &&
                Objects.equals(this.script, other.script);
    }

    @Override
    public String toString() {
        return "WorkflowEntity{" +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", disabled='" + disabled + '\'' +
                ", script='" + script + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code, description, disabled, script);
    }
}
