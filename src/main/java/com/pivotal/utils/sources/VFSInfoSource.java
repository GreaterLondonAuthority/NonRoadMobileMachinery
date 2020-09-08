/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.sources;

import com.pivotal.utils.*;
import com.googlecode.compress_j2me.lzc.LZCInputStream;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.vfs2.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Implements a virtual file system info source
 */
public class VFSInfoSource extends InfoSource {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VFSInfoSource.class);

    private Integer timeout = null;
    private String proxyServer = null;

    /**
     * Creates an instance of the this information source for reading
     *
     * @param infosourceEntity Database entity for this source
     */
    public VFSInfoSource(InfosourceEntity infosourceEntity) {
        super(infosourceEntity);
    }

    /**
     * Opens the actual source
     * Implementors should make sure that they cache this list so that there
     * is a consistent set of entities to work between open/close calls
     *
     * @throws PivotalException Errors if the source can't be opened
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void open() throws PivotalException {

        logger.debug("Opening VFS information source " + infosourceEntity);

        FileObject srcFolder;
        FileSystemManager fsManager = null;
        File tmpKey=null;

        try {
            // Setup security just in case we need it

            fsManager = VFSUtils.getManager();
            if (!Common.isBlank(infosourceEntity.getSshKey())) {
                tmpKey = new File(Common.getTemporaryFilename());
                Common.writeTextFile(tmpKey, new String(infosourceEntity.getSshKey(), "UTF-8"));
            }
            FileSystemOptions opts = VFSUtils.setUpConnection(infosourceEntity.getUsername(), infosourceEntity.getPassword(), infosourceEntity.isUserDirIsRoot(), tmpKey, timeout, proxyServer);

            // Attempt to resolve the source

            srcFolder = fsManager.resolveFile(infosourceEntity.getServer(), opts);
            if (!srcFolder.exists()) {
                logger.error("The VFS location does not exist for " + infosourceEntity);
            }

            // Is it a folder

            else if (srcFolder.getType().equals(FileType.FOLDER)) {
                logger.debug("Resolved VFS file to a folder " + srcFolder.getName());

                // Enumerate the files into a local cache

                FileObject[] fileList = srcFolder.getChildren();
                if (!Common.isBlank(fileList)) {
                    logger.debug("Found " + fileList.length + " files to possibly download");
                    for (int i = 0; i < fileList.length; i++) {
                        if (!fileList[i].getType().equals(FileType.FOLDER) && !fileList[i].isHidden()) {
                            logger.debug("Downloading " + i + " of " + fileList.length + " files");
                            items.add(new InfosourceItem(fileList[i], infosourceEntity, fsManager));
                        }
                        else if(fileList[i].getType().equals(FileType.FOLDER)){
                            logger.debug("Not downloading folder " + fileList[i].getName());
                        }
                        else {
                            logger.debug("Not downloading hidden file " + i + " of " + fileList.length);
                        }
                    }
                }
                else {
                    logger.debug("The folder is empty");
                }
            }

            // Is it actually a file

            else if (srcFolder.getType().equals(FileType.FILE)) {
                logger.debug("Resolved VFS file to a file");
                items.add(new InfosourceItem(srcFolder, infosourceEntity, fsManager));
            }

        }
        catch (Exception e) {
            logger.error("Failed to resolve VFS file location");
            throw new PivotalException(e);
        }
        finally {
            // disconnect from VFS source
            VFSUtils.closeManager(fsManager);
            if (tmpKey!=null && tmpKey.exists()) tmpKey.delete();
        }
    }

    /**
     * Closes the source - doesn't do anything if the source
     * is not open
     */
    @Override
    public void close() {
        logger.debug("Closing VFS [" + infosourceEntity + ']');

        // Clean up the temporary files used by this source

        if (!Common.isBlank(items)) {
            for (InfoSourceItem item : items) {
                item.cleanLocalTempFiles();
            }
        }

        // Clear the list of captured items

        items=new ArrayList<>();
    }

    /**
     * Implementation of a VFS item
     */
    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    protected class InfosourceItem extends InfoSourceItem {

        FileObject file;

        /**
         * Creates a file object to use
         *
         * @param file             VFS file to use
         * @param infosourceEntity The item's infosource entity
         * @throws FileSystemException Errors
         */
        InfosourceItem(FileObject file, InfosourceEntity infosourceEntity, FileSystemManager fsManager) throws FileSystemException {

            logger.debug("Working on file " + file.getName());
            //check if infosourceEntity is valid
            if (infosourceEntity == null) {
                String err = "InfosourceEntity cannot by null. Please check your parameters.";
                logger.error(err);
                throw new FileSystemException(err);
            }

            // Enumerate the properties of the file

            this.file = file;
            this.infosourceEntity = infosourceEntity;
            properties.put("path", file.getName().getPath());
            properties.put("uri", file.getName().getFriendlyURI());
            properties.put("type", file.getType().getName());
            properties.put("hidden", file.isHidden() ? "true" : "false");
            properties.put("readable", file.isReadable() ? "true" : "false");
            properties.put("writeable", file.isWriteable() ? "true" : "false");

            // Set the easy values

            name = file.getName().getURI();
            shortName = file.getName().getBaseName();
            size = file.getContent().getSize();
            extension = file.getName().getExtension();
            //HTTP Info sources may not contain Last Modified Time. For those cases we will use current_date as last modified.
            lastModified = null;
            try{
                lastModified = new Date(file.getContent().getLastModifiedTime());
            }
            catch(Exception e){
                lastModified = new Date();
            }


            // Download the file to a local copy for processing later

            File tmpFile = new File(Common.getTemporaryFilename(file.getName().getBaseName().replaceFirst("^[^.]+\\.", "")));
            localFile = tmpFile;
            FileObject destFile = fsManager.resolveFile(tmpFile.getAbsolutePath());
            logger.debug("Downloading/copying file " + file.getName() + " to " + destFile.getName());
            destFile.copyFrom(file, Selectors.SELECT_ALL);
            localTempFiles.add(tmpFile);
            String fileName = file.getName().getURI();
            String fileExtension = file.getName().getExtension();


            // Check for a supported compressed file (GZIP or COMPRESS)

            if (Common.doStringsMatch(fileExtension, "z", "gz")) {
                tmpFile = Z_UncompressFile(tmpFile);
                fileName = file.getName().getURI().replaceFirst("(?is)\\.((z)|(gz))$", "");
                fileExtension = Common.getFilenameExtension(tmpFile.getName());
            }

            // Check for a ZIP compressed archive
            // We will unpack everything and add each file to the list

            if (Common.doStringsMatch(fileExtension, "zip")) {
                unZipFiles(tmpFile, infosourceEntity);
            }

            //Check if the source files are PGP encoded.
            else if (infosourceEntity.getHasPgpEncodedFiles() && Common.doStringsMatch(fileExtension, "pgp", "gpg", "asc")) {
                decodePgpFile(tmpFile, infosourceEntity, file.getName().getBaseName());
            }
            else {

                // Add this file to the list

                logger.debug("Added file " + destFile.getName() + " to temporary list");
                files.add(new InfoSourceFile(tmpFile, properties, fileName,
                        Common.getFilename(fileName), fileExtension,
                        file.getContent().getSize(), lastModified, infosourceEntity));
            }
        }


        /**
         * Decodes PGP Files and adds the decoded file to files list
         *
         * @param encryptedFile    Encrypted Input File
         * @param infosourceEntity Infosource Entity
         * @param fileName pgp encrypted original file name
         * @throws FileSystemException if it fails to decode the file
         */
        private void decodePgpFile(File encryptedFile, InfosourceEntity infosourceEntity, String fileName) throws FileSystemException {
            logger.debug("Decoding PGP File");
            String err = "Problem decoding file [%1s] - %2s";

            //Buffer declarations
            InputStream in = null;

            //Validate required fields
            //key location
            if (Common.isBlank(infosourceEntity.getPgpPrivateKey())) {
                String formatedErr = String.format(err, file.getName(), "PGP key not found. Please check your configuration.");
                logger.error(formatedErr);
                throw new FileSystemException(formatedErr);
            }

            try {
                //Adding pgp able provider to java Security.
                Security.addProvider(new BouncyCastleProvider());

                InputStream keyIn = new ByteArrayInputStream(infosourceEntity.getPgpPrivateKey());
                in = new FileInputStream(encryptedFile);

                //Get and passphrase and make sure it is never null
                String passphrase = infosourceEntity.getPgpPassphrase();
                if (passphrase == null) passphrase = "";

                //Decrypt task...
                String outFile = PgpUtils.decryptFile(in, Common.getTemporaryDirectory() + '/', keyIn,
                        passphrase.toCharArray(), fileName.replaceFirst("\\.((gpg)|(pgp)|(asc))$", ""));

                //Adding to tmp files
                File newTmpFile = new File(outFile);
                localTempFiles.add(newTmpFile);

                //Adding to files List
                files.add(new InfoSourceFile(newTmpFile, properties, outFile,
                        Common.getFilename(outFile), Common.getFilenameExtension(outFile),
                        newTmpFile.length(), new Date(newTmpFile.lastModified()),
                        infosourceEntity));
            }
            catch (Exception e) {
                logger.error(err, file.getName(), e.getMessage());
                throw new FileSystemException(String.format(err, file.getName(), e.getMessage()));
            }
            finally {
                Common.close(in);
            }
            logger.debug("Done");

        }


        /**
         * Uncompresses the file into a local temp file
         *
         * @param file Downloaded file object to decompress
         * @return Temporary decompressed file
         * @throws FileSystemException Errors if decompression fails
         */
        private File Z_UncompressFile(File file) throws FileSystemException {

            // Work out what the name of the file will be when it's decompressed

            String newExtension = Common.getFilenameExtension(file.getName().replaceFirst("(?is)\\.((z)|(gz))$", ""));
            File tmpFile = new File(Common.getTemporaryFilename(newExtension));
            logger.debug("Decompressing [" + file.getName() + "] to [" + tmpFile + ']');
            InputStream in = null;
            OutputStream tmpFileStream = null;

            // Use the file extension to determine the file decompression to use
            // We're only supporting gzip and Unix compress

            try {
                if (Common.doStringsMatch(Common.getFilenameExtension(file.getName()), "z")) {
                    in = new LZCInputStream(new FileInputStream(file));
                }
                else if (Common.doStringsMatch(Common.getFilenameExtension(file.getName()), "gz")) {
                    in = new GZIPInputStream(new FileInputStream(file));
                }
                else {
                    throw new Exception("Unsupported compression format");
                }
                tmpFileStream = new FileOutputStream(tmpFile);
                Common.pipeInputToOutputStream(in, tmpFileStream);

                // Add the file to the temporary files store so that it gets cleaned up later

                localTempFiles.add(tmpFile);
            }
            catch (Exception e) {
                throw new FileSystemException("Problem decompressing file [" + file.getName() + "] - " + e.getMessage());
            }
            finally {
                Common.close(in, tmpFileStream);
            }
            return tmpFile;
        }

        /**
         * Unzips the file into its constituents and adds them to the list of file items
         * within the specified info source item
         * This method uses the rather nifty zip libraRy from Srikanth Reddy Lingala rather
         * than the built in Java versions because his version supports encryption
         *
         * @param file             Zip file to un-archive
         * @param infosourceEntity Info source entity
         * @throws FileSystemException Errors if cannot unpack
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void unZipFiles(File file, InfosourceEntity infosourceEntity) throws FileSystemException {
            try {
                // Initiate ZipFile object with the path/name of the zip file.

                ZipFile zipFile = new ZipFile(file);

                // Check to see if the zip file is password protected

                if (zipFile.isEncrypted()) zipFile.setPassword(infosourceEntity.getEncPassword());

                // Get the list of file headers from the zip file

                @SuppressWarnings("unchecked")
                List<FileHeader> fileHeaderList = zipFile.getFileHeaders();

                // Loop through the file headers

                for (FileHeader fileHeader : fileHeaderList) {

                    // Extract the file to the specified destination

                    if (!fileHeader.isDirectory()) {
                        File extractedFile = new File(Common.getTemporaryFilename());
                        zipFile.extractFile(fileHeader, extractedFile.getAbsolutePath());
                        localTempFiles.add(extractedFile);

                        // This will have possibly created a folder, so copy the file to an actual
                        // top level place

                        extractedFile = new File(extractedFile.getAbsolutePath() + File.separator + fileHeader.getFileName());
                        File newTmpFile = new File(Common.getTemporaryFilename(Common.getFilenameExtension(fileHeader.getFileName())));
                        extractedFile.renameTo(newTmpFile);
                        localTempFiles.add(newTmpFile);

                        // Add this file to the list

                        logger.debug("Added file " + newTmpFile.getName() + " to temporary list");
                        files.add(new InfoSourceFile(newTmpFile, properties, fileHeader.getFileName().replaceAll("[\\/]", "_"),
                                Common.getFilename(fileHeader.getFileName()), Common.getFilenameExtension(fileHeader.getFileName()),
                                fileHeader.getUncompressedSize(), dosToJavaTime(fileHeader.getLastModFileTime()), infosourceEntity));
                    }
                }
            }
            catch (Exception e) {
                logger.error("Problem unarchiving file [" + file.getName() + "] - " + e.getMessage());
                throw new FileSystemException("Problem unarchiving file [" + file.getName() + "] - " + e.getMessage());
            }
        }

        /**
         * Convert the ZIP time to java time
         *
         * @param dosTime Time in DOS format
         * @return Date object
         */
        private Date dosToJavaTime(int dosTime) {

            int sec = 2 * (dosTime & 0x1f);
            int min = dosTime >> 5 & 0x3f;
            int hrs = dosTime >> 11 & 0x1f;
            int day = dosTime >> 16 & 0x1f;
            int mon = (dosTime >> 21 & 0xf) - 1;
            int year = (dosTime >> 25 & 0x7f) + 1980;

            Calendar cal = Calendar.getInstance();
            cal.set(year, mon, day, hrs, min, sec);

            return new Date(cal.getTime().getTime());
        }

        /**
         * This method is responsible for deleting this object from wherever
         * it is stored
         *
         * @return Returns true if the item was successfully deleted
         */
        @Override
        public boolean delete() {

            FileSystemManager fsManager = null;
            File tmpKey=null;
            try {
                logger.debug("Attempting to delete [" + file.getName() + ']');

                // Let's create a manager ourselves so we can control session opening/closing
                fsManager = VFSUtils.getManager();
                if (!Common.isBlank(infosourceEntity.getSshKey())) {
                    tmpKey = new File(Common.getTemporaryFilename());
                    Common.writeTextFile(tmpKey, new String(infosourceEntity.getSshKey(), "UTF-8"));
                }
                FileSystemOptions opts = VFSUtils.setUpConnection(infosourceEntity.getUsername(), infosourceEntity.getPassword(), infosourceEntity.isUserDirIsRoot(), tmpKey, timeout, proxyServer);

                // Let's resolve a new FileObject, using this manager

                FileObject fileRef = fsManager.resolveFile(file.getName().getURI(), opts);

                // deletes the file if it exists

                if (fileRef.exists()) {
                    fileRef.delete();
                    logger.debug("Deleted file [" + fileRef.getName() + ']');
                }
            }
            catch (Exception e) {
                lastError = "Problem deleting the VFS file [" + file.getName() + "] - " + PivotalException.getErrorMessage(e);
                logger.error(lastError);
            }
            finally {
                VFSUtils.closeManager(fsManager);
            }

            // Clean off any local files we have used


            cleanLocalTempFiles();
            return !isInError();
        }

    }

    /**
     * Get the current timeout value
     * @return Timeout value - can be null for default
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout to use for the connection
     * @param timeout Time out in millieseconds
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    /**
     * Get the proxy server in "server:port" mode
     * @return Server to use
     */
    public String getProxyServer() {
        return proxyServer;
    }

    /**
     * Sets the proxy server in "server:port" format
     * @param proxyServer Proxy name
     */
    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }
}
