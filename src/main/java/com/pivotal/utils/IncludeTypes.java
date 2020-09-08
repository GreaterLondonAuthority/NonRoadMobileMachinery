/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * <p>This is included as a class within the the templates and allows you to choose which libraries you wish to include
 * when parsing both JS and CSS include files</p>
 *
*/
public enum IncludeTypes {

    /**
     * The blockly libraries will be imported
     */
    BLOCKLY,

    /**
     * The botstrap JS framework will be imported
     */
    BOOTSTRAP,

    /**
     * The code mirror library will be imported
     */
    CODEMIRROR,

    /**
     * The JQuery colorbox library will be imported
     */
    COLORBOX,

    /**
     * The easy pie chart JS library will be imported
     */
    EASYPIECHART,

    /**
     * The Froala editor will be imported
     */
    FROALA,

    /**
     * The guage library will be imported
     */
    GAUGE,

    /**
     * If you want the gridster styles added
     */
    GRIDSTER,

    /**
     * If you want the gridstack styles added
     */
    GRIDSTACK,

    /**
     * The jquery UI framework will be imported
     */
    JQUERY_UI,

    /**
     * The kendo JS framework will be imported
     */
    KENDO,

    /**
     * Kendo labs styles
     */
    KENDO_LABS,

    /**
     * The kendo chart styles
     */
    KENDO_CHARTS,

    /**
     * The kendo maps styles
     */
    KENDO_MAPS,

    /**
     * The kendo excluded styles
     */
    KENDO_EXCLUDE,

    /**
     * The layout planner will be imported
     */
    LAYOUT_PLANNER,

    /**
     * The menu libraries will be imported
     */
    MENU,

    /**
     * The Pivot Grid library will be imported
     */
    PIVOT,

    /**
     * The raphael library will be imported
     */
    RAPHAEL,

    /**
     * The 3.js library will be imported
     */
    THREEJS,

    /**
     * The velocity js library will be imported
     */
    VELOCITY,

    /**
     * The weather icons will be imported
     */
    WEATHER;

    /**
     * @return The JS include types as a map allowing it to be injected into the velocity context
     */
    public static Map<String, IncludeTypes> getIncludesAsMap() {
        Map<String, IncludeTypes> map = Maps.newHashMap();
        for(IncludeTypes js : IncludeTypes.values()) {
            map.put(js.toString(), js);
        }
        return map;
    }
}
