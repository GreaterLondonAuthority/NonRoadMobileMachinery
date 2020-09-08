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
import com.pivotal.utils.JsonMapper;
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.utils.workflow.WorkflowJob;
import com.pivotal.web.controllers.utils.JsonResponse;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

import static com.pivotal.utils.Common.isBlank;

@Entity
@Table(name = "site")
public class SiteEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SiteEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private String name;
    private String description;
    private Timestamp timeAdded;
    private Timestamp timeModified;
    private UserEntity addedBy;
    private UserEntity modifiedBy;
    private Timestamp startDate;
    private Timestamp endDate;
    private String address;
    private String postcode;
    private String longitude;
    private String latitude;
    private BoroughEntity borough;
    private String zone;
    private String planningAppNumber;
    private String contactFirstName;
    private String contactLastName;
    private String contactEmail;
    private String contactPhoneNumber;

    private Set<SiteUsersEntity> siteUsers = new HashSet<SiteUsersEntity>(0);


    @Id
    @Column(name = "id", nullable = false, length = 10)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        logger.debug("Id set to {}", id);
        this.id = id;
    }

    @Basic
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne
    @JoinColumn(name = "added_by", referencedColumnName = "id", nullable = false)
    public UserEntity getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UserEntity addedBy) {
        this.addedBy = addedBy;
    }

    @ManyToOne
    @JoinColumn(name = "modified_by", referencedColumnName = "id", nullable = false)
    public UserEntity getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(UserEntity modifiedBy) {
        this.modifiedBy = modifiedBy;
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
    @Column(name = "contact_first_name")
    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    @Basic
    @Column(name = "contact_last_name")
    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    @Transient
    public String getContactName() {

        if (isBlank(this.contactFirstName) && isBlank(this.contactLastName))
            return "";

        else if (isBlank(this.contactLastName))
            return this.contactFirstName;

        else if (isBlank(this.contactFirstName))
            return this.contactLastName;

        else
            return this.contactFirstName + " " + this.contactLastName;
    }

    @Basic
    @Column(name = "contact_email")
    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Basic
    @Column(name = "contact_phone_number")
    public String getContactPhoneNumber() {
        return contactPhoneNumber;
    }

    public void setContactPhoneNumber(String phoneNumber) {
        this.contactPhoneNumber = phoneNumber;
    }

    @Basic
    @Column(name = "address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Basic
    @Column(name = "postcode")
    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    @Basic
    @Column(name = "longitude")
    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    @Basic
    @Column(name = "latitude")
    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    @Basic
    @Column(name = "time_added", nullable = false, length = 19)
    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    @Transient
    public void setTimeAddedNow() {
        setTimeAdded(Common.getTimestamp());
    }

    @Basic
    @Column(name = "time_modified", length = 19)
    public Timestamp getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(Timestamp timeModified) {
        this.timeModified = timeModified;
    }

    @Transient
    public void setTimeModifiedNow() {
        setTimeModified(Common.getTimestamp());
    }

    @Transient
    public List<NoteEntity> getNotes() {

        return NoteEntity.getNotes(SiteEntity.class.getSimpleName(), id, "");
    }

    @Basic
    @Column(name = "start_date", nullable = false, length = 19)
    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    @Basic
    @Column(name = "end_date", nullable = false, length = 19)
    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    @ManyToOne
    @JoinColumn(name = "borough_id", referencedColumnName = "id")
    public BoroughEntity getBorough() {
        return borough;
    }

    public void setBorough(BoroughEntity borough) {
        this.borough = borough;
    }

    @Basic
    @Column(name = "zone")
    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    @Basic
    @Column(name = "planning_app_number")
    public String getPlanningAppNumber() {
        return planningAppNumber;
    }

    public void setPlanningAppNumber(String planningAppNumber) {
        this.planningAppNumber = planningAppNumber;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "primaryKey.site")
    public Set<SiteUsersEntity> getSiteUsers() {
        return siteUsers;
    }

    public void setSiteUsers(Set<SiteUsersEntity> siteUsers) {
        this.siteUsers = siteUsers;
    }

    @Transient
    public JsonResponse sendInvitation(String firstname, String lastname, String email, String roleName) {
        return sendInvitation(firstname, lastname, email, roleName, this);
    }

    @Transient
    public static JsonResponse sendInvitation(String firstname, String lastname, String email, String roleName, Integer siteId) {

        SiteEntity siteEntity = HibernateUtils.getEntity(SiteEntity.class, siteId);
        return sendInvitation(firstname, lastname, email, roleName, siteEntity);
    }

    @Transient
    public static JsonResponse sendInvitation(String firstname, String lastname, String email, String roleName, SiteEntity siteEntity) {

        Map<String, String>settings = new HashMap<>();
        settings.put("firstname", firstname);
        settings.put("lastname", lastname);
        settings.put("email", email);
        settings.put("roleName", roleName);

        WorkflowJob workflowJob = new WorkflowJob("SITE_USER_SEND_INVITATION");
        workflowJob.putSetting("SettingsMap", settings);
        workflowJob.putSetting("SettingsString", JsonMapper.serializeItem(settings));
        workflowJob.putSetting("Site", siteEntity);

        return WorkflowHelper.executeWorkflow(workflowJob);
    }

    /**
     * Usees the workflow script to check if the current user has access to this site
     *
     * @return true if user has access
     */
    @Transient
    public Boolean checkUserAccess(boolean viewAccess) {

        Map<String, Object>settings = new HashMap<>();
        if (viewAccess)
            settings.put("ACCESS_TYPE", "VIEW");

        settings.put("siteentity", this);

        JsonResponse wfResponse = WorkflowHelper.executeWorkflow("SITE_SECURITY", settings, false);

        return wfResponse.getData().containsKey("ACCESS") && "OK".equalsIgnoreCase(wfResponse.getDataItem("ACCESS").toString());
    }

    @Override
     public String toString() {
         return "SiteEntity{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", postcode='" + postcode + '\'' +
                    ", longitude='" + longitude + '\'' +
                    ", latitude='" + latitude + '\'' +
                    ", contactFirstName='" + contactFirstName + '\'' +
                    ", contactLastName='" + contactLastName + '\'' +
                    ", contactEmail='" + contactEmail + '\'' +
                    ", contactPhoneNumber='" + contactPhoneNumber + '\'' +
                    "}";
     }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (!(obj instanceof SiteEntity)) return false;
        final SiteEntity other = (SiteEntity) obj;

        return  Objects.equals(this.name, other.name) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.timeAdded, other.timeAdded) &&
                Objects.equals(this.timeModified, other.timeModified) &&
                Objects.equals(this.startDate, other.startDate) &&
                Objects.equals(this.endDate, other.endDate) &&
                Objects.equals(this.address, other.address) &&
                Objects.equals(this.postcode, other.postcode) &&
                Objects.equals(this.longitude, other.longitude) &&
                Objects.equals(this.latitude, other.latitude) &&
                Objects.equals(this.borough, other.borough) &&
                Objects.equals(this.zone, other.zone) &&
                Objects.equals(this.planningAppNumber, other.planningAppNumber) &&
                Objects.equals(this.contactFirstName, other.contactFirstName) &&
                Objects.equals(this.contactLastName, other.contactLastName) &&
                Objects.equals(this.contactPhoneNumber, other.contactPhoneNumber) &&
                Objects.equals(this.contactEmail, other.contactEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name,
                            description,
                            timeAdded,
                            timeModified,
                            startDate,
                            endDate,
                            address,
                            postcode,
                            longitude,
                            latitude,
                            borough,
                            zone,
                            planningAppNumber,
                            contactFirstName,
                            contactLastName,
                            contactPhoneNumber,
                            contactEmail);
    }
}
