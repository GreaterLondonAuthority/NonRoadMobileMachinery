/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.nrmm.annotation.mobile;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation allows you to specify if a controller accepts a mobile view. If not, the page will not be allowed
 * unless the user is not currently in the mobile view.
 *
 * <p>The controller should be marked if it has any mobile views available. Each @RequestMapping method should be marked
 * if using Spring and any of the HTTP methods if using a basic Servlet.</p>
 *
 * <p>One method may be used for both the desktop and mobile views. If doing this, you must check the request
 * parameters for a 'DeviceInfo' {@link DeviceInfo} or if using the template renderer the 'DeviceInfo' will
 * automatically be added to the context.</p>
 *
 */
@Documented
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface HasMobileData {

    /**
     * Allows the user to describe the use of the annotation if required - makes it nice to provide comments within the
     * annotation.
     *
     * @return The description of the annotation use
     */
    String value() default "";
}
