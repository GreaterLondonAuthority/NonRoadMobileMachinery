/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.nrmm.annotation.security;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation allows users to define authorisation access control at both the controller class
 * and the method level.
 * <p>
 * When applied to the Controller class, it dictates what actions different users have for the whole
 * class.  When applied to a method in a Controller, it dictates who is allowed to call this method.
 * </p>
 * <p>
 * Unlike the rather obtuse and complicated Spring Security system, this annotation is simply an
 * advisory to NRMM and it is up to the dispatcher to check that users have the correct access.
 * <br>
 * See isUserAuthorised() for more details.
 * </p>
 * <p>
 * An empty use of this annotation indicates that all logged in users read access.
 * </p>
 */
@Documented
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface Authorise {

    /**
     * (Optional) If true, then this resource is available to anyone without having to be logged in
     * @return flag
     */
    boolean notLoggedIn() default false;

    /**
     * (Optional) If true, then this resource is available to anyone without having to be logged in
     * as long as they are using the IDE
     * @return flag
     */
    boolean inIde() default false;

    /**
     * (Optional) Array of privileges that are allowed access to this resource
     * @return array
     */
    Privileges[] view() default {};

    /**
     * (Optional) Array of privileges that have edit (add,delete,update) access
     * This is only really used for the Admin controllers
     * @return array
     */
    Privileges[] edit() default {};

    /**
     * (Optional) if true then this resource forms part of the NRMM RESTful API.
     * This means that requests are authenticated inline and the request is either
     * processed or rejected and no login page is displayed
     *
     * @return flag
     */
    boolean isRestfulApi() default false;

}
