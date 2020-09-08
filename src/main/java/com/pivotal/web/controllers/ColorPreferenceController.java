/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.web.controllers.utils.Authorise;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>This is a simple controller that will just collect the recent colors used by a user and store them so that
 * they can be presented to the user the next time they use the app</p>
 *
 */
@Authorise
@Controller
@RequestMapping("/color")
public class ColorPreferenceController extends AbstractController {
    // This only uses the preferences part of the abstract controller
}
