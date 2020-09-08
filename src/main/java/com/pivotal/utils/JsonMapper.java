/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The purpose of this class is to provide a very simple means by which JSON
 * text can be parsed into a generalised Java form
 * It is initially aimed at Velocity whereby the dynamic introspection means
 * we don't have to worry about strict type mapping
 */
public class JsonMapper {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonMapper.class);

    // The jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper();

    static {

        // We do not want to include null values in the serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Prevent instantiation
     */
    private JsonMapper() {
    }

    /**
     * Parse the text and return a Java object
     *
     * @param text JSON text to parse
     * @return Java object
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseJson(String text) {

        T returnValue = null;

        // Check we have something to do

        if (!Common.isBlank(text)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
                mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
                mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
                mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
                returnValue = (T) mapper.readValue(text, Object.class);
            }
            catch (JsonParseException e) {
                logger.error("Problem parsing JSON text (badly formed) - " + PivotalException.getErrorMessage(e));
            }
            catch (Exception e) {
                logger.error("Problem parsing JSON text - " + PivotalException.getErrorMessage(e));
            }
        }
        return returnValue;
    }


    /**
     * Will deserialize a list of entities
     *
     * @param json  The serialized objects
     * @param clazz The class to use as the implementation
     * @return The list of Dashboard instances
     */
    public static <T> List<T> deserializeItemList(String json, Class<? extends T> clazz) {
        List<T> list = null;
        try {
            list = mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        }
        catch (IOException e) {
            logger.error("Could not deserialize list object - [%s]", json);
        }
        return list;
    }

    /**
     * Will deserialize a single entity
     *
     * @param json  The serialized object
     * @param clazz The class to use as the implementation
     * @return The Dashboard instance
     */
    public static <T> T deserializeItem(String json, Class<T> clazz) {
        return deserializeItem(json, clazz, null);
    }

    /**
     * Will deserialize a single entity
     *
     * @param json          The serialized object
     * @param clazz         The class to use as the implementation
     * @param deserializers Any deserializers to use within the parsing
     * @return The Dashboard instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeItem(String json, Class<T> clazz, List<DeserializerPair> deserializers) {
        T type = null;
        try {

            // If there is a custom serializer to use for the parsing

            if (!Common.isBlank(deserializers)) {
                SimpleModule serializeModule = new SimpleModule("DeserializerModule");
                for (DeserializerPair deserializerPair : deserializers)
                    serializeModule.addDeserializer(deserializerPair.getClassToDeserialize(), deserializerPair.getDeserializerToUse());
                mapper.registerModule(serializeModule);
            }

            // Now parse the list

            type = mapper.readValue(json, clazz);
        }
        catch (Exception e) {
            logger.error("Could not deserialize object - %s [%s]", PivotalException.getErrorMessage(e), json);
        }
        return type;
    }

    /**
     * Will attempt to convert the json into a map of key and value types.
     *
     * @param json       The serialized object
     * @param keyClass   The class of the key
     * @param valueClass The class of the value
     * @param <K>        The key type
     * @param <V>        The value type
     * @return The json deserialized into a Map or null if it fails
     */
    public static <K, V> Map<K, V> deserializeIntoMap(String json, Class<K> keyClass, Class<V> valueClass) {
        Map<K, V> map = null;
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, keyClass, valueClass);
        try {
            map = mapper.readValue(json, mapType);
        }
        catch (IOException e) {
            logger.error("Could not deserialize into map - %s [%s]", json);
        }
        return map;
    }

    /**
     * Will serialize an object into JSON
     *
     * @param item The item to serialize
     * @return The serialized json or null if it could not be serialized
     */
    public static String serializeItem(Object item) {
        String serialized = null;
        try {
            serialized = mapper.writeValueAsString(item);
        }
        catch (IOException e) {
            logger.error("Could not serialize object - [%s]", item);
        }
        return serialized;
    }

    /**
     * Will serialize an object into JSON using the specified view class
     *
     * @param item              The item to serialize
     * @param serializationView The view class to apply
     * @return The serialized json or null if it could not be serialized
     */
    public static String serializeItemUsingView(Object item, Class<?> serializationView) {
        String serialized = null;
        try {
            final ObjectWriter objectWriter = mapper.writerWithView(serializationView);
            serialized = objectWriter.writeValueAsString(item);
        }
        catch (IOException e) {
            logger.error("Could not serialize object - [%s]", item);
        }
        return serialized;
    }
}
