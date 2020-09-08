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
@Table(name = "site_users")
@AssociationOverrides({
		@AssociationOverride(name = "primaryKey.site",
			joinColumns = @JoinColumn(name = "site_id")),
		@AssociationOverride(name = "primaryKey.user",
			joinColumns = @JoinColumn(name = "users_id")) })

public class SiteUsersEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SiteUsersEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private SiteUsersIdEntity primaryKey = new SiteUsersIdEntity();
    private RoleEntity role;

    public SiteUsersEntity() {}

    @EmbeddedId
    public SiteUsersIdEntity getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(SiteUsersIdEntity primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Transient
    public SiteEntity getSite() {
        return getPrimaryKey().getSite();
    }

    public void setSite(SiteEntity site) {
        getPrimaryKey().setSite(site);
    }

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    @Transient
    public UserEntity getUser() {
        return getPrimaryKey().getUser();
    }

    public void setUser(UserEntity user) {
        getPrimaryKey().setUser(user);
    }

    @Override
    public String toString() {

            return "SiteUserEntity{" +
                    "role='" + role + '\'' +
                    "primaryKey='" + primaryKey + '\'' +
                    "}";
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (!(obj instanceof SiteUsersEntity)) return false;
        final SiteUsersEntity other = (SiteUsersEntity) obj;

        if (getPrimaryKey() != null ? getPrimaryKey().equals(other.getPrimaryKey()): other.getPrimaryKey() != null)
            return false;

        return  true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrimaryKey());
    }
}
