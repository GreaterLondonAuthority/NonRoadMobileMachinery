/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SiteUsersIdEntity implements Serializable {

    private static final long serialVersionUID = 4550422780535557785L;

    private SiteEntity site;
    private UserEntity user;

    @ManyToOne
    public SiteEntity getSite() {
        return site;
    }

    public void setSite(SiteEntity site) {
        this.site = site;
    }

    @ManyToOne
    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    @Override
    public String toString() {

            return "SiteUsersIdEntity{" +
                    "site='" + site + '\'' +
                    "user='" + user + '\'' +
                    "}";
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (!(obj instanceof SiteUsersIdEntity)) return false;
        final SiteUsersIdEntity other = (SiteUsersIdEntity) obj;

        return  Objects.equals(this.site, other.site) &&
                Objects.equals(this.user, other.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(site, user);
    }
}
