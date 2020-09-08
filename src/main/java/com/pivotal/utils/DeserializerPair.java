/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Allows you to pass the deserializers you wish to use for deserializing a json string if you are not able
 * to add the annotation to the class or do not wish to run into overflows within the deserializer.
 *
 * @param <T> The type of class to deserialize with the deserializer
*/
public class DeserializerPair<T> {

    // The class of the item you wish to
    private Class<T> classToDeserialize;

    // The deserializer to use for this class
    private JsonDeserializer<? extends T> deserializerToUse;

    /**
     * Will create a new deserialization pair to send along to the deserialize method
     *
     * @param classToDeserialize The class to use the deserializer for
     * @param deserializerToUse  The deserializer for the class type
     */
    public DeserializerPair(Class<T> classToDeserialize, JsonDeserializer<? extends T> deserializerToUse) {
        this.classToDeserialize = classToDeserialize;
        this.deserializerToUse = deserializerToUse;
    }

    /**
     * @return The class to deserialize
     */
    public Class<T> getClassToDeserialize() {
        return classToDeserialize;
    }

    /**
     * @return The deserializer to use
     */
    public JsonDeserializer<? extends T> getDeserializerToUse() {
        return deserializerToUse;
    }
}
