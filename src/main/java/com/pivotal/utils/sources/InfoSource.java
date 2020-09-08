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
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides the boiler plate class that all implementations of an information source must extend
 */
public abstract class InfoSource {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InfoSource.class);

    Map<String, String> properties=new LinkedCaseInsensitiveMap<>();
    com.pivotal.utils.sources.InfosourceEntity infosourceEntity;
    List<com.pivotal.utils.sources.InfoSourceItem> items=new ArrayList<>();

    /**
     * Creates an instance of the this information source for reading
     *
     * @param infosourceEntity Database entity for this source
     */
    public InfoSource(com.pivotal.utils.sources.InfosourceEntity infosourceEntity) {
        this.infosourceEntity=infosourceEntity;
    }

    /**
     * Opens the actual source
     * Implementors should make sure that they cache this list so that there
     * is a consistent set of entities to work between open/close calls
     *
     * @throws PivotalException Errors if the source can't be opened
     */
    abstract public void open() throws PivotalException;

    /**
     * Closes the source - doesn't do anything if the source
     * is not open
     *
     */
    abstract public void close();

    /**
     * Returns a list of the entities associated with this information source
     * For example, if this source is an email account, then this will return
     * a list of all the emails in the mailbox
     *
     * @return List of entities
     */
    public List<com.pivotal.utils.sources.InfoSourceItem> getItems() {
        return items;
    }

    /**
     * Returns true if there are no items in the store
     *
     * @return boolean true if empty
     */
    public boolean isEmpty() {
        return Common.isBlank(items);
    }

    /**
     * Returns the number of items in the list
     *
     * @return Number of items
     */
    public int getCount() {
        return isEmpty()?0:items.size();
    }

}
