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
@Table(name = "action_type")
public class ActionTypeEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ActionTypeEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private String name;
    private String description;
    private boolean disabled;
    private WorkflowEntity workflow;

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
    @Column(name = "disabled", length = 0, precision = 0)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @ManyToOne
    @JoinColumn(name = "workflow_id", referencedColumnName = "id", nullable = false)
    public WorkflowEntity getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowEntity workflow) {
        this.workflow = workflow;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ActionTypeEntity{" +
               ", id='" + id + '\'' +
               ", description='" + description + '\'' +
               ", name='" + name + '\'' +
               ", disabled='" + disabled + '\'' +
               ", workflow='" + workflow + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (!(obj instanceof ActionTypeEntity)) return false;
        final ActionTypeEntity other = (ActionTypeEntity) obj;
        return Objects.equals(this.description, other.description) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.disabled, other.disabled) &&
               Objects.equals(this.workflow, other.workflow);
        }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, disabled, workflow);
    }
}
