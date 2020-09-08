/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.sources;

/**
 * Provides meta data to all the InfoSources
 */
public class InfosourceEntity {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String encUsername;

    public String getEncUsername() {
        return encUsername;
    }

    public void setEncUsername(String encusername) {
        this.encUsername = encusername;
    }

    private String encPassword;

    public String getEncPassword() {
        return encPassword;
    }

    public void setEncPassword(String encpassword) {
        this.encPassword = encpassword;
    }

    private String server;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    private boolean userDirIsRoot;

    public boolean isUserDirIsRoot() {
        return userDirIsRoot;
    }

    public void setUserDirIsRoot(boolean user_dir_is_root) {
        this.userDirIsRoot = user_dir_is_root;
    }

    private String emailType;

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    @Override
    public String toString() {
        return "InfosourceEntity{" +
               "description='" + description + '\'' +
               ", emailType='" + emailType + '\'' +
               ", encPassword='" + encPassword + '\'' +
               ", encUsername='" + encUsername + '\'' +
               ", hasPgpEncodedFiles='" + hasPgpEncodedFiles + '\'' +
               ", name='" + name + '\'' +
               ", password='" + password + '\'' +
               ", pgpPassphrase='" + pgpPassphrase + '\'' +
               ", server='" + server + '\'' +
               ", sshPassphrase='" + sshPassphrase + '\'' +
               ", sshUserAuthentication='" + sshUserAuthentication + '\'' +
               ", type='" + type + '\'' +
               ", userDirIsRoot='" + userDirIsRoot + '\'' +
               ", username='" + username + '\'' +
               '}';
    }

    private boolean hasPgpEncodedFiles;

    public boolean getHasPgpEncodedFiles() {
        return hasPgpEncodedFiles;
    }

    public void setHasPgpEncodedFiles(boolean hasPgpEncodedFiles) {
        this.hasPgpEncodedFiles = hasPgpEncodedFiles;
    }

    private byte[] pgpPrivateKey;
    public byte[] getPgpPrivateKey() {
        return pgpPrivateKey;
    }

    public void setPgpPrivateKey(byte[] pgpPrivateKey) {
        this.pgpPrivateKey = pgpPrivateKey;
    }

    private String pgpPassphrase;
    public String getPgpPassphrase() {
        return pgpPassphrase;
    }

    public void setPgpPassphrase(String pgpPassphrase) {
        this.pgpPassphrase = pgpPassphrase;
    }

    private boolean sshUserAuthentication;
    public boolean isSshUserAuthentication() {
        return sshUserAuthentication;
    }

    public void setSshUserAuthentication(boolean sshUserAuthentication) {
        this.sshUserAuthentication = sshUserAuthentication;
    }

    private byte[] sshKey;
    public byte[] getSshKey() {
        return sshKey;
    }

    public void setSshKey(byte[] sshKey) {
        this.sshKey = sshKey;
    }

    private String sshPassphrase;
    public String getSshPassphrase() {
        return sshPassphrase;
    }

    public void setSshPassphrase(String sshPassphrase) {
        this.sshPassphrase = sshPassphrase;
    }

}
