/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.SystemPropertyUtils;

import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * <p>Some general purpose methods involving classes.</p>
 *
*/
public class ClassUtils {

    // Get access to the logger
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ClassUtils.class);

    /**
     * Stop the class from being instantiated.
     */
    private ClassUtils() {
    }

    /**
     * Uses Spring to scan the specific package for classes of the required type.
     *
     * @param basePackage The base package to scan
     * @param findClass   The class that we are looking for
     * @param <T>         The inferred type expecting
     * @return The list of known classes with the specified type
     */
    @SuppressWarnings("unchecked")
    public static <T> List<Class<T>> findAllClasses(String basePackage, final Class<T> findClass) {

        // Create a candidate scanner for getting the class type

        final CandidateScanner scanner = new CandidateScanner() {
            @Override
            boolean isCandidate(MetadataReader metadataReader) {
                try {
                    Class c = Class.forName(metadataReader.getClassMetadata().getClassName());
                    if (findClass.isAssignableFrom(c)) {
                        return true;
                    }
                }
                catch (ClassNotFoundException e) {
                    logger.debug("The class [%s] could not be found", e, metadataReader.getClassMetadata().getClassName());
                }
                return false;
            }
        };

        // Finalise this by converting the list to the type we are expecting

        return Lists.transform(findAllClasses(basePackage, scanner), new Function<Class<?>, Class<T>>() {

            @Override
            public Class<T> apply(@Nullable Class<?> input) {

                // Convert to the type required

                return (Class<T>) input;
            }
        });
    }

    /**
     * Uses Spring to scan the specific package for classes with the specified annotation.
     *
     * @param basePackage The base package to scan
     * @param annotation  The annotation class that we are looking for
     * @return The list of known classes with the specified type
     */
    @SuppressWarnings("unused")
    public static List<Class<?>> findAllAnnotatedClasses(String basePackage, final Class<? extends Annotation> annotation) {

        // Create a candidate scanner for getting the annotated type

        CandidateScanner scanner = new CandidateScanner() {
            @Override
            boolean isCandidate(MetadataReader metadataReader) {
                try {
                    Class<?> c = Class.forName(metadataReader.getClassMetadata().getClassName());
                    if (c.getAnnotation(annotation) != null) {
                        return true;
                    }
                }
                catch (ClassNotFoundException e) {
                    logger.debug("The class [%s] could not be found", e, metadataReader.getClassMetadata().getClassName());
                }
                return false;
            }
        };

        // Finalise this by converting the list to the type we are expecting

        return findAllClasses(basePackage, scanner);
    }

    /**
     * All the classes within the package will be returned.
     *
     * @param basePackage      The base package to scan
     * @param candidateScanner The scanner to determine if the class is required
     * @return The classes within the package
     */
    private static List<Class<?>> findAllClasses(String basePackage, CandidateScanner candidateScanner) {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        List<Class<?>> candidates = Lists.newArrayList();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + "/" + "**/*.class";
        try {
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    if (candidateScanner.isCandidate(metadataReader)) {
                        try {
                            candidates.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
                        }
                        catch (ClassNotFoundException e) {
                            logger.debug("Could not find the class [%s] within the package [%s]", e, metadataReader.getClassMetadata().getClassName(), basePackage);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            logger.debug("Could not load the resources for the package [%s]", e, basePackage);
        }
        return candidates;
    }

    /**
     * Can resolve the package to the base package name.
     *
     * @param basePackage The The base package to scan
     * @return The resolved base packaeg
     */
    private static String resolveBasePackage(String basePackage) {
        return org.springframework.util.ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    /**
     * A {@code CandidateScanner} is used to filter a list of classes by determining if a class is required.
     */
    abstract static class CandidateScanner {

        /**
         * Will return true if the specified class specified by the {@code MetadataReader} is required.
         *
         * @param metadataReader The meta data reader
         * @return true if required or false otherwise
         */
        abstract boolean isCandidate(MetadataReader metadataReader);
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    public static List<Class> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String fileName = resource.getFile();
            String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
            dirs.add(new File(fileNameDecoded));
        }
        List<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files!=null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isDirectory()) {
                    assert !fileName.contains(".");
                    classes.addAll(findClasses(file, packageName + '.' + fileName));
                } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                    Class _class;
                    try {
                        _class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6));
                    } catch (ExceptionInInitializerError e) {
                        // happen, for example, in classes, which depend on
                        // Spring to inject some beans, and which fail,
                        // if dependency is not fulfilled
                        _class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6),
                                false, Thread.currentThread().getContextClassLoader());
                    }
                    classes.add(_class);
                }
            }
        }
        return classes;
    }

    /**
     * Returns true if the property exists
     * @param clazz Class to look for
     * @param name Name of the property e.g. ggg.yyy.jjjj
     * @return True if the class is not null, the name not empty and a property of this name exists
     */
    public static boolean propertyExists(Class clazz, String name) {
        boolean returnValue = false;
        if (clazz!=null && !Common.isBlank(name)) {
            try {
                returnValue = ClassUtils.getPropertyDescriptor(clazz, name) != null;
            }
            catch (Exception e) {
                logger.debug("[%s] definitely doesn't exist in [%s]", name, clazz);
            }
        }
        return returnValue;
    }

    /**
     * Recursively gets the property description of any method however deeply nested
     *
     * @param clazz Class of the bean to interrogate
     * @param name Name of the property e.g. ggg.yyy.jjjj
     * @return Property descriptor of null if not found
     */
    public static PropertyDescriptor getPropertyDescriptor(Class clazz, String name) throws BeansException {
        PropertyDescriptor returnValue;
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(clazz);
        if (name.contains(".")) {
            String className = name.split("\\.", 2)[0];
            String methodName = name.split("\\.", 2)[1];
            PropertyDescriptor tmp = beanWrapper.getPropertyDescriptor(className);
            returnValue = getPropertyDescriptor(tmp.getPropertyType(), methodName);
        }
        else
            returnValue = beanWrapper.getPropertyDescriptor(name);
        return returnValue;
    }

    /**
     * Exercises the getter method to retrieve the bean value
     * @param object Object to use
     * @param name Method name (nested is allowed)
     * @return Object value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPropertyValue(Object object, String name) {

        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(object);
        T returnValue = null;
        try {
            returnValue = (T)beanWrapper.getPropertyValue(name);
        }
        catch (Exception e) {
            logger.debug("Field [%s] has produced a null value", name);
        }
        return returnValue;
    }

    /**
     * Exercises the setter method to set the bean value
     * @param object Object to use
     * @param name Method name (nested is allowed)
     * @param value Value to apply to the property
     * @return Previous value
     */
    @SuppressWarnings("unchecked")
    public static <T> T setPropertyValue(Object object, String name, Object value) {

        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(object);
        T returnValue = null;
        try {
            returnValue = (T)beanWrapper.getPropertyValue(name);
            beanWrapper.setPropertyValue(new PropertyValue(name, value));
        }
        catch (Exception e) {
            logger.debug("Field [%s] has produced a null value", name);
        }
        return returnValue;
    }

    /**
     * Safely invokes a method without throwing an error if the method fails
     * or doesn't exist
     * @param object Object to run the method on
     * @param methodName Name of the method
     * @return Value from the method
     */
    public static <T> T invokeMethod(Object object, String methodName) {
        return invokeMethod(object, methodName, (Object[])null);
    }

    /**
     * Safely invokes a method without throwing an error if the method fails
     * or doesn't exist
     * @param object Object to run the method on
     * @param methodName Name of the method
     * @param args Any arguments to use
     * @return Value from the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T  invokeMethod(Object object, String methodName, Object... args) {
        T returnValue = null;
        if (object!=null) {

            // Get the classes of the arguments to the method

            Class[] classes = null;
            if (!Common.isBlank(args)) {
                List<Class> tmp = new ArrayList<>();
                for (Object arg : args) {
                    tmp.add(arg.getClass());
                }
                classes = tmp.toArray(new Class[tmp.size()]);
            }

            // Find and execute the method

            try {
                Method method;
                if (object instanceof String) {
                    method = Class.forName((String)object).getMethod(methodName, classes);
                    returnValue = (T)method.invoke(null, args);
                }
                else if (object instanceof Class) {
                    method = ((Class)object).getMethod(methodName, classes);
                    returnValue = (T)method.invoke(null, args);
                }
                else {
                    method = object.getClass().getMethod(methodName, classes);
                    returnValue = (T)method.invoke(object, args);
                }
            }
            catch (Exception e) {
                logger.debug("Cannot invoke [%s] on [%s]", methodName, object);
            }
        }
        return returnValue;
    }

    /**
     * Gets a map of all the constant fields in the class
     * These are fields that are final, static and their name is in all upper case
     * @param clazz Class to interrogate
     * @param pattern The pattern to match the name with
     * @return Map of field names and values
     */
    public static Map<String, Object> getFields(Class clazz, String pattern) {
        Map<String, Object> returnValue = new HashMap<>();
        List<Field> objFields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        for (Field objField : objFields) {
            if (Modifier.isFinal(objField.getModifiers()) && Modifier.isStatic(objField.getModifiers()) && objField.getName().equals(objField.getName().toUpperCase())) {
                if (Common.isBlank(pattern) || objField.getName().matches(pattern)) {
                    try {
                        objField.setAccessible(true);
                        returnValue.put(objField.getName(), objField.get(objField.getClass()));
                    }
                    catch (Exception e) {
                        logger.error("Problem outputting static field values - " + e.getMessage());
                    }
                }
            }
        }
        return returnValue;
    }


    /**
     * Gets a list of all the constant fields in the class
     * These are fields that are final, static, their name is in all upper case
     * and they have the annotation specified
     * @param clazz Class to interrogate
     * @param annotation The annotation that the field must have
     * @return List of fields
     */
    public static List<Field> getFields(Class clazz, Class<? extends Annotation>annotation) {
        List<Field> returnValue = new ArrayList<>();
        List<Field> objFields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        for (Field objField : objFields) {
            if (Modifier.isFinal(objField.getModifiers()) && Modifier.isStatic(objField.getModifiers()) && objField.getName().equals(objField.getName().toUpperCase())) {
                if (objField.isAnnotationPresent(annotation)) {
                    try {
                        objField.setAccessible(true);
                        returnValue.add(objField);
                    }
                    catch (Exception e) {
                        logger.error("Problem outputting static field values - " + e.getMessage());
                    }
                }
            }
        }
        return returnValue;
    }

}
