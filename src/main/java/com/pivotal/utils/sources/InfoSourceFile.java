/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.sources;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VFSUtils;
import org.apache.commons.vfs2.*;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class represents the concept of a file or attachment within an email
 * It also represents the contents of a Zip/7z archive file
 */
public class InfoSourceFile {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InfoSourceFile.class);
    private static final int BUFFER = 2048;

    Map<String, String> properties=new LinkedCaseInsensitiveMap<>();
    String name;
    String shortName;
    String extension;
    long size;
    Date lastModified;
    List<File> localTempFiles=new ArrayList<>();
    File localFile;
    String lastError = "";
    InfosourceEntity infosourceEntity;

    /**
     * Default constructor to allow extending this class
     */
    protected InfoSourceFile() {}

    /**
     * Creates a new file object
     *
     * @param localFile Locally stored file
     * @param properties map of useful type specific information
     * @param name Name of the file
     * @param shortName Short name of the file
     * @param extension Extension of the file
     * @param size Length in bytes of the file
     * @param lastModified Date the file was last modified
     * @param infosourceEntity The file's infosource entity
     */
    public InfoSourceFile(File localFile, Map<String, String> properties, String name, String shortName, String extension, long size, Date lastModified, InfosourceEntity infosourceEntity) {
        this.localFile = localFile;
        this.properties = properties;
        this.name = name;
        this.shortName = shortName;
        this.extension = extension;
        this.size = size;
        this.lastModified = lastModified;
        this.infosourceEntity = infosourceEntity;
    }

    /**
     * Returns a date representation of the specified property
     *
     * @param property Name of the property
     *
     * @return Date object
     */
    public Date getDateProperty(String property) {
        Date returnValue=null;
        if (properties.containsKey(property))
            returnValue= Common.parseDate(properties.get(property));
        return returnValue;
    }

    /**
     * Returns the string value of the property
     *
     * @param property Name of the property
     *
     * @return String value
     */
    public String getProperty(String property) {
        String returnValue=null;
        if (properties.containsKey(property))
            returnValue=properties.get(property);
        return returnValue;
    }

    /**
     * Returns a map of all the properties available from the source item
     *
     * @return Map of properties reduced to strings
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns a long representation of the specified property
     *
     * @param property Name of the property
     *
     * @return Long value or 0 if it doesn't exist
     */
    public long getLongProperty(String property) {
        long returnValue=0;
        if (properties.containsKey(property))
            returnValue=Common.parseLong(properties.get(property));
        return returnValue;
    }

    /**
     * Returns a boolean representation of the specified property
     *
     * @param property Name of the property
     *
     * @return boolean value or false if it doesn't exist
     */
    public boolean getBooleanProperty(String property) {
        boolean returnValue=false;
        if (properties.containsKey(property))
            returnValue=Common.isYes(properties.get(property));
        return returnValue;
    }

    /**
     * Returns the name of the file
     *
     * @return String name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the short name of the file
     *
     * @return String short name
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Returns the extension part of the filename
     *
     * @return String
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns the size of the file in bytes
     *
     * @return Length of file
     */
    public long getSize() {
        return size;
    }

    /**
     * Get the date that the file was last modified
     *
     * @return Date object
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Returns true if the extension matches any of the values passed
     *
     * @param extArray Array of strings to check for
     * @return True if a match is found
     */
    public boolean isExtension(String... extArray) {
        boolean returnValue=false;
        for (int i=0; !Common.isBlank(extArray) && !returnValue && i<extArray.length; i++) {
            returnValue=Common.doStringsMatch(extArray[i], extension);
        }
        return returnValue;
    }

    /**
     * Removes any local temporary files used by this source file
     */
    protected void cleanLocalTempFiles() {
        logger.debug("Deleting local storage " + (Common.isBlank(localTempFiles)?"empty":localTempFiles.size() + " files"));
        if (!Common.isBlank(localTempFiles)) {
            for (File file : localTempFiles) {
                try {
                    if (file.exists()) Common.deleteDir(file);
                }
                catch (Exception e) {
                    logger.warn("Problem deleting local temporary file [" + file.getAbsolutePath() + "] - " + PivotalException.getErrorMessage(e));
                }
            }
        }
        localTempFiles=null;
    }

    /**
     * Returns true if there is an outstanding error on this object
     *
     * @return boolean
     */
    public boolean isInError() {
        return !Common.isBlank(lastError);
    }

    /**
     * Returns the last error tat occurred on this object
     *
     * @return String
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Returns the underlying temporary file for this atachment
     *
     * @return Local file
     */
    public File getLocalFile() {
        return localFile;
    }

    public String getTextContent() {
        return Common.readTextFile(localFile);
    }


    /**
     * Copy the file to a new location
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be copied to
     *
     * @return true if success
     */
    public boolean copy(String newFile) {
        return copy(newFile,false,false,null,null,null);
    }

    /**
     * Copy the file to a new location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be copied to
     * @param username The username for the authentication
     * @param password The password for the authentication
     *
     * @return true if success
     */
    public boolean copy(String newFile, String username, String password) {
        return copy(newFile,false,false,username,password,null);
    }

    /**
     * Copy the file to a new location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *        file is going to be copied to
     * @param username The username for the authentication
     * @param password The password for the authentication
     * @param sshKey SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     *
     * @return true if success
     */
    public boolean copy(String newFile, String username, String password, File sshKey) {
        return copy(newFile,false,false,username,password,sshKey);
    }

    /**
     * Zip the file and copy it to a new vfs location
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be copied to
     *
     * @return true if success
     */
    public boolean zipAndCopy(String newFile) {
        return copy(newFile,true,false,null,null,null);
    }

    /**
     * Zip the file and copy it to new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be copied to
     * @param username The username for the authentication
     * @param password The password for the authentication
     *
     * @return true if success
     */
    public boolean zipAndCopy(String newFile, String username, String password) {
        return copy(newFile,true,false,username,password,null);
    }

    /**
     * Zip the file and copy it to new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *        file is going to be copied to
     * @param username The username for the authentication
     * @param password The password for the authentication
     * @param sshKey SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     *
     * @return true if success
     */
    public boolean zipAndCopy(String newFile, String username, String password, File sshKey) {
        return copy(newFile,true,false,username,password,sshKey);
    }

    /**
     * Move the file to a new vfs location
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be moved to
     *
     * @return true if success
     */
    public boolean move(String newFile) {
        return copy(newFile,false,true,null,null,null);
    }

    /**
     * Move the file to a new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be moved to
     * @param username The username for the authentication
     * @param password The password for the authentication
     *
     * @return true if success
     */
    public boolean move(String newFile, String username, String password) {
        return copy(newFile,false,true,username,password,null);
    }


    /**
     * Move the file to a new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *        file is going to be moved to
     * @param username The username for the authentication
     * @param password The password for the authentication
     * @param sshKey SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     *
     * @return true if success
     */
    public boolean move(String newFile, String username, String password, File sshKey) {
        return copy(newFile,false,true,username,password,sshKey);
    }

    /**
     * Zip the file and move it to a new vfs location
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be moved to
     *
     * @return true if success
     */
    public boolean zipAndMove(String newFile) {
        return copy(newFile,true,true,null,null,null);
    }

    /**
     * Zip the file and move it to a new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be moved to
     * @param username The username for the authentication
     * @param password The password for the authentication
     *
     * @return true if success
     */
    public boolean zipAndMove(String newFile, String username, String password) {
        return copy(newFile,true,true,username,password,null);
    }

    /**
     * Zip the file and move it to a new vfs location, and includes authentication information
     * It makes sense for vfs like sftp.
     *
     * @param newFile - full path (including file name) where the
     *        file is going to be moved to
     * @param username The username for the authentication
     * @param password The password for the authentication
     * @param sshKey SSH Key file to be used in authentication. The key NEEDS to be in openSSH format
     *
     * @return true if success
     */
    public boolean zipAndMove(String newFile, String username, String password, File sshKey) {
        return copy(newFile,true,true,username,password,sshKey);
    }

    /**
     * Generic method to handle all the following operations:
     * copy, zipAndCopy, move, zipAndMove
     *
     * @param newFile - full path (including file name) where the
     *           file is going to be moved or copied to
     * @param zip - if the file has to be zipped before moving or copying
     * @param delete - if the file has to be deleted after the copy (it means move)
     * @param username Username to use for connecting to server
     * @param password Password to use for connecting to server
     *
     * @return true if success
     */
    private boolean copy(String newFile, boolean zip, boolean delete, String username, String password, File sshKey) {

        FileSystemManager fsManager = null;
        FileObject dest;
        FileObject deleteFile;

        try {
            logger.debug("Attempting to copy [" + name + "] to ["+newFile+ ']');

            // Connect to the server

            fsManager = VFSUtils.getManager();
            FileSystemOptions opts = setUpConnection(username, password, sshKey);

            // Check that the file exists

            FileObject file = fsManager.resolveFile(localFile.getAbsolutePath());
            if (file.exists()) {

                // if starts with / its local file, if it contains :/ it's a full vfs uri
                // otherwise it's a file in the same folder as the infosource.

                if (newFile.startsWith("/") || newFile.contains(":/") ||
                    newFile.startsWith("\\") || newFile.contains(":\\")) {
                    dest = fsManager.resolveFile(newFile, opts);
                }
                else {
                    dest = fsManager.resolveFile(name.substring(0, name.lastIndexOf('/') + 1) + newFile, opts);
                }

                if (zip) {
                    createZipFile(file, dest);
                }
                else {
                    dest.copyFrom(file, Selectors.SELECT_ALL);
                }

                logger.debug("Copied file  [" + file.getName() + "] to [" + newFile + ']');

                if (delete) {

                    // Configure connection to the infosource again to delete the file
                    File tmpKey = null;
                    if (!Common.isBlank(infosourceEntity.getSshKey())) {
                        tmpKey = new File(Common.getTemporaryFilename());
                        Common.writeTextFile(tmpKey, new String(infosourceEntity.getSshKey(), "UTF-8"));
                    }
                    opts = setUpConnection(infosourceEntity.getUsername(),infosourceEntity.getPassword(),tmpKey);
                    deleteFile = fsManager.resolveFile(name, opts);
                    if (deleteFile.exists()) {
                        deleteFile.delete();
                        logger.debug("Deleted file [" + file.getName() + ']');
                    }

                }
            }
            else {
                lastError="Source file does not exist ["+file.getName()+ ']';
            }
        }
        catch (Exception e) {
            lastError="Problem copying the VFS file  [" +name + "] to ["+newFile+"] - " + PivotalException.getErrorMessage(e);
            logger.error(lastError);
        }
        finally {

            // close connection to the destination VFS
            VFSUtils.closeManager(fsManager);
        }

        return !isInError();
    }

    /**
     * Creates a zipped version of the file
     *
     * @param orig Original file to add to the zip
     * @param dest The VFS file where the zipped file is going to be saved
     *
     * @throws IOException when it can't close the output stream (it means it was unable to save the file)
     */
    private void createZipFile(FileObject orig, FileObject dest) throws IOException {
        ZipOutputStream out = null;
        BufferedInputStream origin = null;
        byte[] data = new byte[BUFFER];

        try {
            out = new ZipOutputStream(new BufferedOutputStream(dest.getContent().getOutputStream()));
            origin = new BufferedInputStream(orig.getContent().getInputStream(), BUFFER);
            out.putNextEntry(new ZipEntry(shortName));
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
        }
        catch (Exception e) {
            throw new PivotalException(e.getMessage());
        }
        finally {
            Common.close(origin);

            // Not catching as it means we were unable to save the file

            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Helper method to configure all the connection parameters
     *
     * @param username Username for VFS connection
     * @param password Password for VFS connection
     * @param sshKeyFile File with the Private key for authentication in OpenSsh format
     *
     * @return The object containing all the connection parameters
     *
     * @throws FileSystemException if it fails to configure it
     */
    private FileSystemOptions setUpConnection(String username, String password, File sshKeyFile) throws FileSystemException {

        //when no username is provided, it should be either a local file or a file in the same
        //infosource, so using the same authentication information

        if (Common.isBlank(username)) {
            username = infosourceEntity.getUsername();
            password = infosourceEntity.getPassword();
        }

        return VFSUtils.setUpConnection(username, password, infosourceEntity.isUserDirIsRoot(),sshKeyFile);
    }


    /**
     * Gets the byte[] representation of this file.
     * @return byte[] with the file data. Null if something when wrong
     */
    public byte[] getResourceAsBytes()
    {
        byte[] res=null;
        if(localFile!=null){
            try {
                res = Common.readBinaryFile(localFile);
            }
            catch (Exception e) {
                lastError="Problem getting the byte[] from the file " +name + " - " + PivotalException.getErrorMessage(e);
                logger.error(lastError);
            }
        }
        return res;
    }


    public byte[] createChecksum() throws Exception {
        InputStream fis =  null;
        MessageDigest complete = null;
        try {
            fis = new FileInputStream(localFile);
            byte[] buffer = new byte[1024];
            complete = MessageDigest.getInstance("MD5");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
        }
        finally {
            Common.close(fis);
        }

        return complete==null?null:complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public String getMD5Checksum() throws Exception {
        byte[] b = createChecksum();
        String result = "";

        for (byte aB : b) {
            result += Integer.toString((aB & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }






}
