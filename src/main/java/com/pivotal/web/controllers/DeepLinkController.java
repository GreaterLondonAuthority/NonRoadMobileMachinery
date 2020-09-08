/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers;

import com.pivotal.utils.Common;
import com.pivotal.utils.JsonMapper;
import com.pivotal.web.controllers.utils.Authorise;
import com.pivotal.web.servlet.ServletHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages deeplinks into the application
 *
 */
@Authorise
@Controller
@RequestMapping("/deeplink")
public class DeepLinkController extends AbstractController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DeepLinkController.class);

    /**
     * {@inheritDoc}
     */
    @RequestMapping(value = "/**", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String loadDeepLink(HttpSession session, HttpServletRequest request) {


        Map<String, Object> deepLink = new HashMap<>();

        String pathInfo = ServletHelper.getPathInfo(request).toLowerCase();
        if (pathInfo.startsWith("/deeplink"))
            pathInfo = pathInfo.substring(9);

        if (pathInfo.startsWith("/"))
            pathInfo = pathInfo.substring(1);

        deepLink.put("Path", Common.split(pathInfo, "/"));
        deepLink.put("QueryString", request.getParameterMap());

        session.setAttribute("DeepLink", JsonMapper.serializeItem(deepLink));

        return "redirect:/dashboard";
    }
}
