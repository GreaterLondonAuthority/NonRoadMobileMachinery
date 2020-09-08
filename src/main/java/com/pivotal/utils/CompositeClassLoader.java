/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>A CompositeClassLoader that will try to load classes/resources using multiple classloaders.  It will try to load
 * using the order in which classloaders where supplied and will return the first match.</p>
 * <p>Borrowed from the Atlassian CompositeClassLoader provided within their template plugin. But, it basically proxies
 * each call to each class loader and whichever returns first.</p>
 *
*/
public class CompositeClassLoader extends ClassLoader {

    // Get access to the logger
    private static final Logger logger = LoggerFactory.getLogger(CompositeClassLoader.class);

    // The class loaders to load from
    private final Set<ClassLoader> classLoaders;

    /**
     * Creates a new composite class loader.  At least one class loader needs to be supplied. Please note that order of
     * the supplied class loaders is the order in which they will be queried.
     *
     * @param classLoaders A list of class loaders to query.
     */
    public CompositeClassLoader(final ClassLoader... classLoaders) {
        // ATR-27: Ensure the parent ClassLoader is null so we don't leak classes from main ClassLoader
        super(null);
        if (classLoaders == null || classLoaders.length == 0) {
            throw new IllegalArgumentException("At least one classLoader must be supplied!");
        }
        this.classLoaders = new LinkedHashSet<>(Arrays.asList(classLoaders));
    }


    @Override
    public void clearAssertionStatus() {
        for (ClassLoader classLoader : classLoaders) {
            classLoader.clearAssertionStatus();
        }
    }

    @Override
    public URL getResource(final String name) {
        for (ClassLoader classLoader : classLoaders) {
            final URL resource = classLoader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        for (ClassLoader classLoader : classLoaders) {
            final InputStream resource = classLoader.getResourceAsStream(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        IOException ioe = null;
        for (ClassLoader classLoader : classLoaders) {
            final Enumeration<URL> resources;
            try {
                resources = classLoader.getResources(name);
                if (resources != null && resources.hasMoreElements()) {
                    return resources;
                }
            }
            catch (IOException e) {
                logger.debug("Underlying classloader '" + classLoader + "' threw IOException", e);
                ioe = e;
            }
        }
        if (ioe != null) {
            throw ioe;
        }
        return null;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            final Class<?> aClass;
            try {
                aClass = classLoader.loadClass(name);
                if (aClass != null) {
                    return aClass;
                }
            }
            catch (ClassNotFoundException e) {
                // don't log a stacktrace here, as some logging frameworks will try to load the classes in the stacktrace
                // to determine the version of the class. This loadClass call can trigger yet another ClassNotFoundException
                // here, triggering more logging and finally resulting in a StackOverflowError.
                logger.debug("Underlying classloader '" + classLoader + "' couldn't find class: " + e.getMessage());
            }

        }
        throw new ClassNotFoundException("Class '" + name + "' could not be found!");
    }

    @Override
    public void setClassAssertionStatus(final String className, final boolean enabled) {
        for (ClassLoader classLoader : classLoaders) {
            classLoader.setClassAssertionStatus(className, enabled);
        }
    }

    @Override
    public void setDefaultAssertionStatus(final boolean enabled) {
        for (ClassLoader classLoader : classLoaders) {
            classLoader.setDefaultAssertionStatus(enabled);
        }
    }

    @Override
    public void setPackageAssertionStatus(final String packageName, final boolean enabled) {
        for (ClassLoader classLoader : classLoaders) {
            classLoader.setPackageAssertionStatus(packageName, enabled);
        }
    }
}
