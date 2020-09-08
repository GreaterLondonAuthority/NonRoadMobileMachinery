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
@Table(name = "user_role")
public class UserRoleEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserRoleEntity.class);
    private static final long serialVersionUID = -8525448347234966540L;
    private Integer id;
    private RoleEntity role;
    private UserEntity user;

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return a {@link Integer} object.
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
     * @param id a {@link Integer} object.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @ManyToOne
    @JoinColumn(name = "users_id", referencedColumnName = "id", nullable = false)
    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserRoleEntity)) return false;
        final UserRoleEntity other = (UserRoleEntity) obj;
        return Objects.equals(this.role, other.role) &&
               Objects.equals(this.user, other.user);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "UserRoleEntity{" +
               "id='" + id + '\'' +
               "role='" + role + '\'' +
               ", user='" + user + '\'' +
               '}';
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(role, user);
    }
}
