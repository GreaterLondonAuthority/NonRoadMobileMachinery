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
@Table(name = "machinery_media")
public class MachineryMediaEntity extends AbstractEntity implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(MachineryMediaEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private MachineryEntity machinery;
    private MediaEntity media;
    private Integer id;

    @Id
    @Column(name = "id", nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
        logger.debug("Set id {}", id);
    }


    @ManyToOne
    @JoinColumn(name = "machinery_id", referencedColumnName = "id", nullable = false)
    public MachineryEntity getMachinery() {
        return machinery;
    }

    public void setMachinery(MachineryEntity machinery) {
        this.machinery = machinery;
    }

    @ManyToOne
    @JoinColumn(name = "media_id", referencedColumnName = "id", nullable = false)
    public MediaEntity getMedia() {
        return media;
    }

    public void setMedia(MediaEntity media) {
        this.media = media;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof MachineryMediaEntity)) return false;
        final MachineryMediaEntity other = (MachineryMediaEntity) o;

        return Objects.equals(this.media, other.media) &&
               Objects.equals(this.machinery, other.machinery) &&
               Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machinery, media);
    }
}
