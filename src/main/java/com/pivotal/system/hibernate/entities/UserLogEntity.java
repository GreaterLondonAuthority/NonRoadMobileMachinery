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
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "user_log")
public class UserLogEntity extends AbstractEntity implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserLogEntity.class);
    private static final long serialVersionUID = 4550422780535557785L;

    private String userid;
    private String sessionid;
    private String ipAddress;
    private String userAgent;
    private String browserLocale;
    private String browser;
    private String browserVersion;
    private String os;
    private String osArchitecture;
    private String screenResolution;
    private String colours;
    private String region;
    private byte mobile;
    private Timestamp accessTime;
    private String locale;
    private String path;

    private Integer id;

    @Id
    @Column(name = "id", nullable = false, length = 10, precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Basic
    @Column(name = "userid", nullable = false, insertable = true, updatable = true, length = 50)
    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    @Basic
    @Column(name = "sessionid", nullable = false, insertable = true, updatable = true, length = 50)
    public String getSessionid() {
        return sessionid;
    }

    public void setSessionid(String sessionid) {
        this.sessionid = sessionid;
    }

    @Basic
    @Column(name = "ip_address", nullable = true, insertable = true, updatable = true, length = 100)
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Basic
    @Column(name = "user_agent", nullable = true, insertable = true, updatable = true, length = 1000)
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Basic
    @Column(name = "browser_locale", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowserLocale() {
        return browserLocale;
    }

    public void setBrowserLocale(String browserLocale) {
        this.browserLocale = browserLocale;
    }

    @Basic
    @Column(name = "browser", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    @Basic
    @Column(name = "browser_version", nullable = true, insertable = true, updatable = true, length = 100)
    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    @Basic
    @Column(name = "os", nullable = true, insertable = true, updatable = true, length = 100)
    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Basic
    @Column(name = "os_architecture", nullable = true, insertable = true, updatable = true, length = 100)
    public String getOsArchitecture() {
        return osArchitecture;
    }

    public void setOsArchitecture(String osArchitecture) {
        this.osArchitecture = osArchitecture;
    }

    @Basic
    @Column(name = "screen_resolution", nullable = true, insertable = true, updatable = true, length = 100)
    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    @Basic
    @Column(name = "colours", nullable = true, insertable = true, updatable = true, length = 100)
    public String getColours() {
        return colours;
    }

    public void setColours(String colours) {
        this.colours = colours;
    }

    @Basic
    @Column(name = "region", nullable = true, insertable = true, updatable = true, length = 100)
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Basic
    @Column(name = "mobile", nullable = false, insertable = true, updatable = true)
    public byte getMobile() {
        return mobile;
    }

    public void setMobile(byte mobile) {
        this.mobile = mobile;
    }

    @Basic
    @Column(name = "access_time", nullable = false, insertable = true, updatable = true)
    public Timestamp getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Timestamp accessTime) {
        this.accessTime = accessTime;
    }

    @Basic
    @Column(name = "locale", nullable = true, insertable = true, updatable = true, length = 100)
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Basic
    @Column(name = "path", nullable = true, insertable = true, updatable = true, length = 10000)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserLogEntity that = (UserLogEntity) o;

        if (id != that.id) return false;
        if (mobile != that.mobile) return false;
        if (accessTime != null ? !accessTime.equals(that.accessTime) : that.accessTime != null) return false;
        if (browser != null ? !browser.equals(that.browser) : that.browser != null) return false;
        if (browserLocale != null ? !browserLocale.equals(that.browserLocale) : that.browserLocale != null)
            return false;
        if (browserVersion != null ? !browserVersion.equals(that.browserVersion) : that.browserVersion != null)
            return false;
        if (colours != null ? !colours.equals(that.colours) : that.colours != null) return false;
        if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
        if (locale != null ? !locale.equals(that.locale) : that.locale != null) return false;
        if (os != null ? !os.equals(that.os) : that.os != null) return false;
        if (osArchitecture != null ? !osArchitecture.equals(that.osArchitecture) : that.osArchitecture != null)
            return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (region != null ? !region.equals(that.region) : that.region != null) return false;
        if (screenResolution != null ? !screenResolution.equals(that.screenResolution) : that.screenResolution != null)
            return false;
        if (sessionid != null ? !sessionid.equals(that.sessionid) : that.sessionid != null) return false;
        if (userAgent != null ? !userAgent.equals(that.userAgent) : that.userAgent != null) return false;
        if (userid != null ? !userid.equals(that.userid) : that.userid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userid, sessionid, ipAddress, userAgent, browserLocale, browser, browserVersion,
                            os, getOsArchitecture(), screenResolution, colours, region, mobile, accessTime, locale, path);
    }
}
