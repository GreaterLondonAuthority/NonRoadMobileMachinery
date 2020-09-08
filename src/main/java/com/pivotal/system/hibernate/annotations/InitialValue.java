/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that this field is a special initialisation instruction
 * It can also optionally indicate which entities must be created before this one
 * and whether the instance should only be created when the database is new
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface InitialValue {

    /**
     * (Optional) Array of entity classes that must be created before this one
     * @return array or classes
     */
    Class[] depends() default {};

    /**
     * (Optional) True if the entity should only be applied to a new database
     * @return True if the entity should only be applied to a new database
     */
    boolean onlyNewDatabase() default false;

    /**
     * (Optional) JSON expression containing values to add to every command
     * @return Additional JSON or null
     */
    String defaults() default "";

    /**
     * (Optional) list of JSON values to prepend to values of the same name
     * @return JSON string of values
     */
    String prepends() default "";

    /**
     * (Optional) list of JSON values to append to values of the same name
     * @return JSON string of values
     */
    String appends() default "";

    /**
     * (Optional) list of JSON values to substitute into values
     * @return JSON string of values
     */
    String constants() default "";
}
