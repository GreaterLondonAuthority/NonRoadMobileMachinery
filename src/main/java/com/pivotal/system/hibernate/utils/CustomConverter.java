/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.utils;

import com.pivotal.system.hibernate.entities.LogEntity;
import com.pivotal.system.hibernate.entities.MediaEntity;
import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides conversion from strings to/from our entities
 * This is needed for form updates from the UI where we have bound
 * an ID to an object
 * It also provides general purpose conversion from known upload types
 * like byte[] to their useful equivalent objects - this supersedes the
 * rather clumsy property editor arrangements in previous versions of Spring
 */
public class CustomConverter extends DefaultConversionService implements GenericConverter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CustomConverter.class);

    /**
     * Default constructor used during the auto-wiring
     */
    public CustomConverter() {
        super();
        addConverter(this);
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        Set<ConvertiblePair> returnValue = new HashSet<>();

        try {
            // Add all of our entity beans

            for (Class pubClass : ClassUtils.getClasses(LogEntity.class.getPackage().getName())) {
                if (pubClass.getSimpleName().matches(".+Entity")) {
                    returnValue.add(new ConvertiblePair(String.class, pubClass));
                    logger.debug("Added converter for [{}]", pubClass.getSimpleName());
                }
            }

            // Add a multipart converter for the byte stream and for a full upload

            returnValue.add(new ConvertiblePair(MultipartFile.class, byte[].class));
            returnValue.add(new ConvertiblePair(MultipartFile.class, MediaEntity.class));

            // Add some useful (essential) converters

            returnValue.add(new ConvertiblePair(String.class, Timestamp.class));
            returnValue.add(new ConvertiblePair(String.class, Boolean.class));
            returnValue.add(new ConvertiblePair(String.class, boolean.class));
            returnValue.add(new ConvertiblePair(String.class, BigDecimal.class));
            returnValue.add(new ConvertiblePair(Double.class, String.class));
        }
        catch (Exception e) {
            logger.error("Problem registering Spring Bean type converters - {}", PivotalException.getErrorMessage(e));
        }

        return returnValue;
    }

    /** {@inheritDoc} */
    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

        // If the source is an empty string of any sort then return null

        if (source==null || (source instanceof String && Common.isBlank((String)source)))
            return null;

        // Parse the booleans

        else if (source instanceof String && (Boolean.class.isAssignableFrom(targetType.getType()) ||
                                              boolean.class.isAssignableFrom(targetType.getType())))
            return Common.isYes((String) source);

        // If this is a BigDecimal

        else if (source instanceof String && BigDecimal.class.isAssignableFrom(targetType.getType()))
            return new BigDecimal(((String)source).replaceAll(":","."));

        // If this is a Double

        else if (source instanceof Double && String.class.isAssignableFrom(targetType.getType()))
            return Common.formatNumber((Number)source, "0.0########");

        // Parse the multipart stuff - if a filepart is sent and the destination is a byte array
        // then just copy it in, otherwise it must be a MediaEntity object so create that
        // on the fly

        else if (source instanceof MultipartFile)
            try {
                if (byte[].class.isAssignableFrom(targetType.getType()))
                    return ((MultipartFile) source).getBytes();
                else if (((MultipartFile) source).isEmpty())
                    return null;
                else {
                    MediaEntity media = new MediaEntity();
//                    media.setFile(((MultipartFile) source).getBytes());
                    media.setName(Common.getFilename(((MultipartFile) source).getOriginalFilename()));
                    media.setExtension(Common.getFilenameExtension(media.getName()));
                    //TODO check this is used, as we are now storing the files in OID
                    return media;
                }
            }
            catch (Exception e) {
                logger.error("Problem converting multipart to byte array - {}", PivotalException.getErrorMessage(e));
                return null;
            }

        // Parse the timestamps

        else if (source instanceof String && Timestamp.class.isAssignableFrom(targetType.getType())) {
            Date tmp = Common.parseDateTime((String) source);
            return tmp==null?null:new Timestamp(tmp.getTime());
        }

        // If this is a number then it must be the ID of one of our entities (because that's all that's left)

        else if (Common.isBlank((String)source) || !((String)source).matches("[0-9]+") || Common.parseInt((String)source)==0)
            return null;

        else {
            return HibernateUtils.getEntity(targetType.getType(), Common.parseInt((String)source));
        }
    }
}
