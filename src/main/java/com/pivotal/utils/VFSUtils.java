/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.io.File;

/**
 * Class used to create VFS manager as Java object instead
 * of using it as singleton (VFS.getManager).
 *
 * This way it's less harmful to handle
 * opening/closing sessions as there are no shared
 * connections.
 *
 */
public class VFSUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VFSUtils.class);

    /**
     * Creates a new VFS manager.
     *
     * It should be used instead of VFS.getManager() as it can be harmful to
     * manage opening/closing sessions from a singleton instance
     *
     * @return the DefaultFileSystemManager
     */
    public static FileSystemManager getManager() {

        StandardFileSystemManager manager = new StandardFileSystemManager();

        try {
            manager.init();
        } catch (FileSystemException e) {
            logger.error("Unable to configure VFS connection", e);
            throw new PivotalException(e);
        }

        return manager;
    }

    /**
     * Receives a FileSysteManager instance.
     *
     * If it is A StandardFileSystemManager created by getManager()
     * it will be able to close it. (closing all opened connections)
     *
     * FileSystemManager does not provide a close method.
     *
     * @param manager The FileSystemManager to be closed
     */
    public static void closeManager(FileSystemManager manager) {
        if (manager != null && manager instanceof StandardFileSystemManager) {
            ((StandardFileSystemManager) manager).close();
        }
    }

    /**
     * Configure all VFS connection parameters
     * and returns it as a FileSystemOptions object,
     * which can be used directly in methods from the
     * VFS manager, e.g. resolveFile.
     *
     * @param username username for authentication
     * @param password password for authentication
     * @param isUserDirIsRoot if the path is specified from the user dir
     *
     * @return a FileSystemOptions object, which can be used with FileSystemManager.resolveFile()
     */
    public static FileSystemOptions setUpConnection(String username, String password, boolean isUserDirIsRoot) {
        return setUpConnection(username, password, isUserDirIsRoot, null, null, null);
    }

    /**
     * Configure all VFS connection parameters
     * and returns it as a FileSystemOptions object,
     * which can be used directly in methods from the
     * VFS manager, e.g. resolveFile.
     *
     * @param username username for authentication
     * @param password password for authentication
     * @param isUserDirIsRoot if the path is specified from the user dir
     * @param sshKeyFile SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     *
     * @return a FileSystemOptions object, which can be used with FileSystemManager.resolveFile()
     */
    public static FileSystemOptions setUpConnection(String username, String password, boolean isUserDirIsRoot, File sshKeyFile) {
        return setUpConnection(username, password, isUserDirIsRoot, sshKeyFile, null, null);
    }

    /**
     * Configure all VFS connection parameters
     * and returns it as a FileSystemOptions object,
     * which can be used directly in methods from the
     * VFS manager, e.g. resolveFile.
     *
     * @param username username for authentication
     * @param password password for authentication
     * @param isUserDirIsRoot if the path is specified from the user dir
     * @param sshKeyFile SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     * @param timeout Timeout for connection in milliseconds
     * @param proxy Proxy server to use
     *
     * @return a FileSystemOptions object, which can be used with FileSystemManager.resolveFile()
     */
    public static FileSystemOptions setUpConnection(String username, String password, boolean isUserDirIsRoot, File sshKeyFile, Integer timeout, String proxy) {
        FileSystemOptions opts = new FileSystemOptions();

        try {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(null, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);

            FtpsFileSystemConfigBuilder.getInstance().setFtpsType(opts, "implicit");
            FtpsFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true);
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");

            FtpsFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, isUserDirIsRoot);
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, isUserDirIsRoot);
            FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, isUserDirIsRoot);

            // Set the timeout if something supplied

            if (timeout!=null) {
                FtpsFileSystemConfigBuilder.getInstance().setDataTimeout(opts, timeout);
                SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, timeout);
                FtpFileSystemConfigBuilder.getInstance().setSoTimeout(opts, timeout);
                FtpFileSystemConfigBuilder.getInstance().setDataTimeout(opts, timeout);
            }

            // Sort the proxy out for HTTP

            if (!Common.isBlank(proxy)) {
                String server = proxy.split(" *:")[0];
                HttpFileSystemConfigBuilder.getInstance().setProxyHost(opts, server);
                if (proxy.contains(":")) {
                    HttpFileSystemConfigBuilder.getInstance().setProxyHost(opts, proxy.replaceAll("^[^:]+:",""));
                }
            }

            // Add on the SSH key authentication if we have been sent a key

            if (sshKeyFile!=null && sshKeyFile.exists()) {
                File[] keys=new File[1];
                //final File privateKeyFile = new File(sshKeyFile, "id_rsa");
                keys[0]=sshKeyFile;
                SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, keys);
            }
        }
        catch (FileSystemException e) {
            logger.error("Unable to configure VFS connection", e);
            throw new PivotalException(e);
        }
        return opts;
    }

}
