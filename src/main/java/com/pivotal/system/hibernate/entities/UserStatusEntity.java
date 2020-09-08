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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "user_status")
public class UserStatusEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserStatusEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String userid;

    @Basic
    @Column(name = "userid", nullable = false, insertable = true, updatable = true, length = 50)
    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    private String sessionid;

    @Id
    @Column(name = "sessionid", nullable = false, insertable = true, updatable = true, length = 50)
    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    private String ipAddress;

    @Basic
    @Column(name = "ip_address", nullable = true, insertable = true, updatable = true, length = 100)
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    private String appPath;

    @Basic
    @Column(name = "app_path", nullable = true, insertable = true, updatable = true, length = 100)
    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    private String userAgent;

    @Basic
    @Column(name = "user_agent", nullable = true, insertable = true, updatable = true, length = 1000)
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private String browserLocale;

    @Basic
    @Column(name = "browser_locale", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowserLocale() {
        return browserLocale;
    }

    public void setBrowserLocale(String browserLocale) {
        this.browserLocale = browserLocale;
    }

    private String browser;

    @Basic
    @Column(name = "browser", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    private String browserVersion;

    @Basic
    @Column(name = "browser_version", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    private String os;

    @Basic
    @Column(name = "os", nullable = true, insertable = true, updatable = true, length = 100)
    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    private String osArchitecture;

    @Basic
    @Column(name = "os_architecture", nullable = true, insertable = true, updatable = true, length = 100)
    public String getOsArchitecture() {
        return osArchitecture;
    }

    public void setOsArchitecture(String osArchitecture) {
        this.osArchitecture = osArchitecture;
    }

    private String screenResolution;

    @Basic
    @Column(name = "screen_resolution", nullable = true, insertable = true, updatable = true, length = 100)
    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    private String colours;

    @Basic
    @Column(name = "colours", nullable = true, insertable = true, updatable = true, length = 100)
    public String getColours() {
        return colours;
    }

    public void setColours(String colours) {
        this.colours = colours;
    }

    private String region;

    @Basic
    @Column(name = "region", nullable = true, insertable = true, updatable = true, length = 100)
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    private boolean mobile;

    @Basic
    @Column(name = "mobile", nullable = false, insertable = true, updatable = true)
    public Boolean getMobile() {
        return mobile;
    }

    public void setMobile(Boolean mobile) {
        this.mobile = Common.isYes(mobile);
    }

    private Timestamp loginTime;

    @Basic
    @Column(name = "login_time", nullable = false, insertable = true, updatable = true)
    public Timestamp getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Timestamp loginTime) {
        this.loginTime = loginTime;
    }

    private Timestamp lastAccess;

    @Basic
    @Column(name = "last_access", nullable = true, insertable = true, updatable = true)
    public Timestamp getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(Timestamp lastAccess) {
        this.lastAccess = lastAccess;
    }

    private Timestamp lastHeartbeat;

    @Basic
    @Column(name = "last_heartbeat", nullable = true, insertable = true, updatable = true)
    public Timestamp getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Timestamp lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    private String locale;

    @Basic
    @Column(name = "locale", nullable = true, insertable = true, updatable = true, length = 100)
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    private String infoMessage;

    @Basic
    @Column(name = "info_message", nullable = true, insertable = true, updatable = true, length = 10000)
    public String getInfoMessage() {
        return infoMessage;
    }

    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }

     @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserStatusEntity)) return false;
        final UserStatusEntity other = (UserStatusEntity) obj;
        return Objects.equals(this.mobile, other.mobile) &&
                Objects.equals(this.appPath, other.appPath) &&
                Objects.equals(this.browser, other.browser) &&
                Objects.equals(this.browserLocale, other.browserLocale) &&
                Objects.equals(this.browserVersion, other.browserVersion) &&
                Objects.equals(this.colours, other.colours) &&
                Objects.equals(this.infoMessage, other.infoMessage) &&
                Objects.equals(this.ipAddress, other.ipAddress) &&
                Objects.equals(this.lastAccess, other.lastAccess) &&
                Objects.equals(this.lastHeartbeat, other.lastHeartbeat) &&
                Objects.equals(this.locale, other.locale) &&
                Objects.equals(this.loginTime, other.loginTime) &&
                Objects.equals(this.os, other.os) &&
                Objects.equals(this.osArchitecture, other.osArchitecture) &&
                Objects.equals(this.loginTime, other.loginTime) &&
                Objects.equals(this.region, other.region) &&
                Objects.equals(this.screenResolution, other.screenResolution) &&
                Objects.equals(this.sessionid, other.sessionid) &&
                Objects.equals(this.userAgent, other.userAgent) &&
                Objects.equals(this.userid, other.userid);
    }
  @Override
    public String toString() {
        return "UserStatusEntity{" +
                ", userid='" + userid + '\'' +
                ", sessionid='" + sessionid + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", appPath='" + appPath + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", browserLocale='" + browserLocale + '\'' +
                ", browser='" + browser + '\'' +
                ", browserVersion='" + browserVersion + '\'' +
                ", os='" + os + '\'' +
                ", osArchitecture='" + osArchitecture + '\'' +
                ", screenResolution='" + screenResolution + '\'' +
                ", colours='" + colours + '\'' +
                ", region='" + region + '\'' +
                ", mobile='" + mobile + '\'' +
                ", loginTime='" + loginTime + '\'' +
                ", lastAccess='" + lastAccess + '\'' +
                ", lastHeartbeat='" + lastHeartbeat + '\'' +
                ", locale='" + locale + '\'' +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(userid, sessionid,  ipAddress, appPath, userAgent, browserLocale,
                            browser, browserVersion, os,osArchitecture, screenResolution, colours,
                            region, mobile, loginTime, lastAccess, lastHeartbeat, locale, infoMessage) ;
    }
}
