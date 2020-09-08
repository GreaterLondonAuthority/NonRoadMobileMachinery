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
import com.pivotal.utils.workflow.WorkflowHelper;
import com.pivotal.web.controllers.utils.JsonResponse;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.pivotal.utils.Common.isBlank;

@Entity
@Table(name = "machinery")
public class MachineryEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MachineryEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private Integer id;
    private SiteEntity site;
    private LookupsEntity type;
    private String contractor;
    private String typeOther;
    private Timestamp timeAdded;
    private Timestamp timeModified;
    private UserEntity addedBy;
    private UserEntity modifiedBy;
    private UserEntity adminUser;
    private Timestamp startDate;
    private Timestamp endDate;
    private String machineId;
    private String supplier;
    private String engineManufacturer;
    private String machineryManufacturer;
    private Double powerRating;
    private String typeApprovalNumber;
    private LookupsEntity euStage;
    private LookupsEntity retrofitModel;
    private String retrofitModelOther;
    private String retrofitId;
    private LookupsEntity exemptionReason;
    private String exemptionReasonText;
    private String exemptionStatus;
    private Timestamp exemptionStatusDate;
    private Timestamp exemptionStatusExpiryDate;
    private LookupsEntity exemptionStatusReason;
    private LookupsEntity exemptionStatusCode;
    private String exemptionId;

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

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    public LookupsEntity getType() {
        return type;
    }

    public void setType(LookupsEntity type) {
        this.type = type;
    }

    @Transient
    public String getDisplayName() {
        if (isBlank(this.typeOther))
            return this.type.getName();
        else
            return this.typeOther;
    }

    @Basic
    @Column(name = "type_other")
    public String getTypeOther() {
        return typeOther;
    }

    public void setTypeOther(String typeOther) {
        this.typeOther = typeOther;
    }

    @Basic
    @Column(name = "contractor")
    public String getContractor() {
        return contractor;
    }

    public void setContractor(String contractor) {
        this.contractor = contractor;
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

    @ManyToOne
    @JoinColumn(name = "admin_user_id")
    public UserEntity getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(UserEntity adminUser) {
        this.adminUser = adminUser;
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

    @Basic
    @Column(name = "machine_id", nullable = false)
    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    @Basic
    @Column(name = "supplier")
    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    @Basic
    @Column(name = "engine_manufacturer")
    public String getEngineManufacturer() {
        return engineManufacturer;
    }

    public void setEngineManufacturer(String engineManufacturer) {
        this.engineManufacturer = engineManufacturer;
    }

    @Basic
    @Column(name = "machinery_manufacturer")
    public String getMachineryManufacturer() {
        return machineryManufacturer;
    }

    public void setMachineryManufacturer(String machineryManufacturer) {
        this.machineryManufacturer = machineryManufacturer;
    }

    @Basic
    @Column(name = "power_rating")
    public Double getPowerRating() {
        return powerRating;
    }

    public void setPowerRating(Double powerRating) {
        this.powerRating = powerRating;
    }

    @Basic
    @Column(name = "type_approval_number")
    public String getTypeApprovalNumber() {
        return typeApprovalNumber;
    }

    public void setTypeApprovalNumber(String typeApprovalNumber) {
        this.typeApprovalNumber = typeApprovalNumber;
    }

    @ManyToOne
    @JoinColumn(name = "eu_stage_id", referencedColumnName = "id")
    public LookupsEntity getEuStage() {
        return euStage;
    }

    public void setEuStage(LookupsEntity euStage) {
        this.euStage = euStage;
    }

    @ManyToOne
    @JoinColumn(name = "retrofit_model_id", referencedColumnName = "id")
    public LookupsEntity getRetrofitModel() {
        return retrofitModel;
    }

    public void setRetrofitModel(LookupsEntity retrofitModel) {
        this.retrofitModel = retrofitModel;
    }

    @Basic
    @Column(name = "retrofit_model_other")
    public String getRetrofitModelOther() {
        return retrofitModelOther;
    }

    public void setRetrofitModelOther(String retrofitModelOther) {
        this.retrofitModelOther = retrofitModelOther;
    }

    @Transient
    public String getDisplayRetrofitName() {
        if (isBlank(this.retrofitModelOther))
            return this.retrofitModel.getName();
        else
            return this.retrofitModelOther;
    }

    @Basic
    @Column(name = "retrofit_id")
    public String getRetrofitId() {
        return retrofitId;
    }

    public void setRetrofitId(String retrofitId) {
        this.retrofitId = retrofitId;
    }

    @Basic
    @Column(name = "exemption_reason_text")
    public String getExemptionReasonText() {
        return exemptionReasonText;
    }

    public void setExemptionReasonText(String exemptionReasonText) {
        this.exemptionReasonText = exemptionReasonText;
    }

    @Basic
    @Column(name = "exemption_status")
    public String getExemptionStatus() {
        return exemptionStatus;
    }

    public void setExemptionStatus(String exemptionStatus) {
        this.exemptionStatus = exemptionStatus;
    }

    @Basic
    @Column(name = "exemption_status_date")
    public Timestamp getExemptionStatusDate() {
        return exemptionStatusDate;
    }

    public void setExemptionStatusDate(Timestamp exemptionStatusDate) {
        this.exemptionStatusDate = exemptionStatusDate;
    }

    @Basic
    @Column(name = "exemption_status_expiry_date")
    public Timestamp getExemptionStatusExpiryDate() {
        return exemptionStatusExpiryDate;
    }

    public void setExemptionStatusExpiryDate(Timestamp exemptionStatusExpiryDate) {
        this.exemptionStatusExpiryDate = exemptionStatusExpiryDate;
    }

    @ManyToOne
    @JoinColumn(name = "exemption_status_reason_id", referencedColumnName = "id")
    public LookupsEntity getExemptionStatusReason() {
        return exemptionStatusReason;
    }

    public void setExemptionStatusReason(LookupsEntity exemptionStatusReason) {
        this.exemptionStatusReason = exemptionStatusReason;
    }

    @ManyToOne
    @JoinColumn(name = "exemption_status_code_id", referencedColumnName = "id")
    public LookupsEntity getExemptionStatusCode() {
        return exemptionStatusCode;
    }

    public void setExemptionStatusCode(LookupsEntity exemptionStatusCode) {
        this.exemptionStatusCode = exemptionStatusCode;
    }

    @ManyToOne
    @JoinColumn(name = "exemption_reason_id", referencedColumnName = "id")
    public LookupsEntity getExemptionReason() {
        return exemptionReason;
    }

    public void setExemptionReason(LookupsEntity exemptionReason) {
        this.exemptionReason = exemptionReason;
    }

    @Basic
    @Column(name = "exemption_id")
    public String getExemptionId() {
        return exemptionId;
    }

    public void setExemptionId(String exemptionId) {
        this.exemptionId = exemptionId;
    }

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    public SiteEntity getSite() {
        return site;
    }

    public void setSite(SiteEntity site) {
        this.site = site;
    }

    /**
     * Usees the workflow script to check if the current user has access to this site
     *
     * @return true if user has access
     */
    @Transient
    public Boolean checkUserAccess() {

        if (this.getSite() == null)
            return true;

        else if (this.getSite().checkUserAccess(true)) {
            Map<String, Object> settings = new HashMap<>();
            settings.put("machineryentity", this);

            JsonResponse wfResponse = WorkflowHelper.executeWorkflow("MACHINERY_SECURITY", settings, false);

            return wfResponse.getData().containsKey("ACCESS") && "OK".equalsIgnoreCase(wfResponse.getDataItem("ACCESS").toString());
        }
        else
            return false;
    }

    @Override
    public String toString() {
        return "MachineryEntity{" +
                    "id='" + id  + '\'' +
                    ", contractor='" + contractor + '\'' +
                    ", timeAdded='" + timeAdded + '\'' +
                    ", addedBy='" + addedBy + '\'' +
                    ", timeModified='" + timeModified + '\'' +
                    ", modifiedBy='" + modifiedBy + '\'' +
                    ", supplier='" + supplier + '\'' +
                    ", engineManufacturer='" + engineManufacturer + '\'' +
                    ", machineryManufacturer='" + machineryManufacturer + '\'' +
                    ", powerRating='" + powerRating + '\'' +
                    ", typeApprovalNumber='" + typeApprovalNumber + '\'' +
                    ", euStage='" + euStage + '\'' +
                    ", retrofitModel='" + retrofitModel + '\'' +
                    ", retrofitModelOther='" + retrofitModelOther + '\'' +
                    ", retrofitId='" + retrofitId + '\'' +
                    ", exemptionReason='" + exemptionReason + '\'' +
                    ", exemptionReasonText='" + exemptionReasonText + '\'' +
                    ", exemptionStatus='" + exemptionStatus + '\'' +
                    ", exemptionStatusReason='" + exemptionStatusReason + '\'' +
                    ", exemptionStatusCode='" + exemptionStatusCode + '\'' +
                    ", exemptionStatusDate='" + exemptionStatusDate + '\'' +
                    ", exemptionStatusExpiryDate='" + exemptionStatusExpiryDate + '\'' +
                    ", exemptionId='" + exemptionId + '\'' +
                    ", adminUser='" + adminUser + '\'' +
                    ", type='" + type + '\'' +
                    ", typeOther='" + typeOther + '\'' +
                    "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MachineryEntity)) return false;
        final MachineryEntity other = (MachineryEntity) obj;

        return  Objects.equals(this.type, other.type) &&
                Objects.equals(this.typeOther, other.typeOther) &&
                Objects.equals(this.contractor, other.contractor) &&
                Objects.equals(this.timeAdded, other.timeAdded) &&
                Objects.equals(this.timeModified, other.timeModified) &&
                Objects.equals(this.startDate, other.startDate) &&
                Objects.equals(this.endDate, other.endDate) &&
                Objects.equals(this.machineId, other.machineId) &&
                Objects.equals(this.supplier, other.supplier) &&
                Objects.equals(this.engineManufacturer, other.engineManufacturer) &&
                Objects.equals(this.machineryManufacturer, other.machineryManufacturer) &&
                Objects.equals(this.powerRating, other.powerRating) &&
                Objects.equals(this.typeApprovalNumber, other.typeApprovalNumber) &&
                Objects.equals(this.euStage, other.euStage) &&
                Objects.equals(this.retrofitModel, other.retrofitModel) &&
                Objects.equals(this.retrofitModelOther, other.retrofitModelOther) &&
                Objects.equals(this.retrofitId, other.retrofitId) &&
                Objects.equals(this.exemptionReason, other.exemptionReason) &&
                Objects.equals(this.exemptionReasonText, other.exemptionReasonText) &&
                Objects.equals(this.exemptionStatus, other.exemptionStatus) &&
                Objects.equals(this.exemptionStatusDate, other.exemptionStatusDate) &&
                Objects.equals(this.exemptionStatusExpiryDate, other.exemptionStatusExpiryDate) &&
                Objects.equals(this.exemptionStatusReason, other.exemptionStatusReason) &&
                Objects.equals(this.exemptionStatusCode, other.exemptionStatusCode) &&
                Objects.equals(this.exemptionId, other.exemptionId) &&
                Objects.equals(this.adminUser, other.adminUser) &&
                Objects.equals(this.modifiedBy, other.modifiedBy) &&
                Objects.equals(this.addedBy, other.addedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type,
                            typeOther,
                            contractor,
                            timeAdded,
                            timeModified,
                            startDate,
                            endDate,
                            machineId,
                            supplier,
                            engineManufacturer,
                            machineryManufacturer,
                            powerRating,
                            typeApprovalNumber,
                            euStage,
                            retrofitModel,
                            retrofitModelOther,
                            retrofitId,
                            exemptionReason,
                            exemptionReasonText,
                            exemptionStatus,
                            exemptionStatusDate,
                            exemptionStatusExpiryDate,
                            exemptionStatusReason,
                            exemptionStatusCode,
                            exemptionId,
                            adminUser,
                            modifiedBy,
                            addedBy);
    }
}
