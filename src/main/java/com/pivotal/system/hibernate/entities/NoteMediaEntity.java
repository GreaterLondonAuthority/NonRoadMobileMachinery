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
@Table(name = "note_media")
public class NoteMediaEntity extends AbstractEntity implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(NoteMediaEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private NoteEntity note;
    private MediaEntity media;

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
    @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false)
    public NoteEntity getNote() {
        return note;
    }

    public void setNote(NoteEntity note) {
        this.note = note;
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
        if (!(o instanceof NoteMediaEntity)) return false;
        final NoteMediaEntity other = (NoteMediaEntity) o;

        return Objects.equals(this.media, other.media) &&
               Objects.equals(this.note, other.note) &&
               Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(note, media);
    }
}
