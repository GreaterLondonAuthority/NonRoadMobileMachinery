/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.entities;

import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.web.email.Email;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "email_queue")
public class EmailQueueEntity {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailQueueEntity.class);

    private Integer id;
    private String emailObject;
    private String emailTo;
    private String emailFrom;
    private String emailSubject;
    private Timestamp timeAdded;
    private int sendAttempts;

    @Column(name = "id", nullable = false, length = 10)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "email_object")
    public String getEmailObject() {
        return emailObject;
    }

    public void setEmailObject(String emailObject) {
        this.emailObject = emailObject;
    }

    @Basic
    @Column(name = "email_to")
    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    @Basic
    @Column(name = "email_from")
    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    @Basic
    @Column(name = "email_subject")
    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    @Basic
    @Column(name = "time_added")
    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    @Basic
    @Column(name = "send_attempts")
    public int getSendAttempts() {
        return sendAttempts;
    }

    public void setSendAttempts(int sendAttempts) {
        this.sendAttempts = sendAttempts;
    }

    @Transient
    /*
     * Save email object to queue
     *
     * email Email object to save to queue
     *
     */
    public static void saveEmail(Email email) {

        EmailQueueEntity emailQueueEntity = HibernateUtils.getEntity(EmailQueueEntity.class);
        emailQueueEntity.setEmailTo(email.getToList().get(0).getName());
        emailQueueEntity.setEmailFrom(email.getFromAddress());
        emailQueueEntity.setEmailSubject(email.getSubject());
        emailQueueEntity.setSendAttempts(0);
        emailQueueEntity.setEmailObject(Common.serialize(email));

//        HibernateUtils.save(emailQueueEntity);

    }

    @Override
    public int hashCode() {
            return Objects.hash(emailObject, emailTo,  emailFrom, emailSubject, timeAdded, sendAttempts);
        }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EmailQueueEntity)) return false;
        final EmailQueueEntity other = (EmailQueueEntity) obj;
        return  Objects.equals(this.emailObject, other.emailObject) &&
                Objects.equals(this.emailTo, other.emailTo) &&
                Objects.equals(this.emailFrom, other.emailFrom) &&
                Objects.equals(this.emailSubject, other.emailSubject) &&
                Objects.equals(this.timeAdded, other.timeAdded) &&
                Objects.equals(this.sendAttempts, other.sendAttempts);
    }

    @Override
    public String toString() {
        return "EmailQueueEntity{" +
                ", id='" + id + '\'' +
                ", emailTo='" + emailTo + '\'' +
                ", emailFrom='" + emailFrom + '\'' +
                ", emailSubject='" + emailSubject + '\'' +
                ", timeAdded='" + timeAdded + '\'' +
                ", sendAttempts='" + sendAttempts + '\'' +
               '}';
    }

}
