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

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the concept of a file to the loading processes
 * In the case of an email info source, this would be a mime message
 * but in the case of VFS this would be a file of some sort
 */
public abstract class InfoSourceItem extends InfoSourceFile {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InfoSourceItem.class);

    List<InfoSourceFile> files=new ArrayList<>();
    String lastError;

    /**
     * Returns a list of the files for this item
     * In the case of an email message this will be the attachments in the case of
     * a VFS source then this will only return a single file
     *
     * @return List of entities
     */
    public List<InfoSourceFile> getFiles() {
        return files;
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
     * Returns true if there is an outstanding error on this object
     *
     * @return boolean
     */
    public boolean isInError() {
        return !Common.isBlank(lastError);
    }

    /**
     * This method is responsible for deleting this object from wherever
     * it is stored
     *
     * @return Returns true if the item was successfully deleted
     */
    public abstract boolean delete();

    /**
     * Returns true if there are no items in the store
     *
     * @return boolean true if empty
     */
    public boolean isEmpty() {
        return Common.isBlank(files);
    }

    /**
     * Returns the number of items in the list
     *
     * @return Number of items
     */
    public int getCount() {
        return isEmpty()?0:files.size();
    }

}
