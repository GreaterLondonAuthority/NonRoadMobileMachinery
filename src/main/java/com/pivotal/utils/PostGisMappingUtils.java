/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

/**
 *
 */
public class PostGisMappingUtils {

    private static final Logger logger = LoggerFactory.getLogger(PostGisMappingUtils.class);

    private PostGisMappingUtils() {
    }

    /**
     * Needs to convert the PostGIS Polygon() format into one accepted by the Mapping Tool.
     * Currently this is a simple point list separated by commas.
     *
     * Currently want: ["532856.9737,181675.78949999996,532857.6316,181682.1052000001,532857.6316,..."]
     *           from: POLYGON((541401.097034593 180055.417435174,541403.364644659 180038.310895393,541411.932087783...,541401.097034593 180055.417435174))
     *
     * or        want: ["524974.002446135 179800.491899797,524974.002446135 179798.642429203,524950.314385592,...","525085.582858016 179928.915139028,525055.579080669 179929.964838554,524908.849973314,..."]
     *           from: MULTIPOLYGON(((524974.002446135 179800.491899797,524974.002446135 179798.642429203,524950.314385592 179791.804386572,524940.313126476 179809.169415884,524926.634570109 179830.75323758,524912.947768681 179859.704950238,524909.34467698 179871.411599245,524910.053752235 179870.221939781,524964.001187019 179890.226213617,524999.792997259 179900.223351966,525031.371581276 179817.06715518,524974.002446135 179800.491899797)),
     *                              ((525085.582858016 179928.915139028,525055.579080669 179929.964838554,524908.849973314 179873.01114138,524908.734542459 179873.381035499,524873.470416129 179979.70060184,524869.265434968 179986.018793277,524855.050949629 180001.274426397,524891.634285735 180013.650883673,524897.686160583 180016.020205462,524897.686160583 180017.599753321,524929.388420517 180027.466928871,524929.792428511 180027.59689167,524968.519480503 180037.454070082,524991.514955914 180043.88223004,525022.532875776 180057.74826093,525041.892279241 180066.545742677,525046.633189375 180054.959059331,525043.475330973 180054.959059331,525085.582858016 179928.915139028)))
     * @param rawPolygon polygon to be converted
     *
     * @return polygon as points
     */
    public static String convertFromPolygon(String rawPolygon) {

        logger.debug("Converting from polygon, before: " + rawPolygon);
        if (rawPolygon == null || "".equals(rawPolygon)) return null;
        StringBuilder builder = new StringBuilder();
        builder.append("[\"");

        // Clean up the string, looking for common differences
        if (rawPolygon.startsWith("POLYGON ("))
            rawPolygon = rawPolygon.replaceAll("POLYGON \\(", "POLYGON(");
        else if (rawPolygon.startsWith("MULTIPOLYGON ("))
            rawPolygon = rawPolygon.replaceAll("MULTIPOLYGON \\(", "MULTIPOLYGON(");

        rawPolygon = rawPolygon.replaceAll(", ",",").replaceAll("\\(\\(+","(").replaceAll("\\)\\)+",")");

        // Need to pull out inner lists of points and then process them
        if (rawPolygon.startsWith("POLYGON(")) {
           String innerRawPolygon = rawPolygon.substring("POLYGON".length(), rawPolygon.length());
           convertRawPolygonPoints(builder, innerRawPolygon);

           // Decision was not to try and handle 'donut' sites.
           // convertRawPolygonPoints(builder, innerRawPolygon.replace("),(", "\",\""));
        }
        else if (rawPolygon.startsWith("MULTIPOLYGON(")) {
           String innerRawPolygon = rawPolygon.substring("MULTIPOLYGON".length(), rawPolygon.length());
           while (innerRawPolygon.length() > 0) {
               int index = innerRawPolygon.indexOf("),(");
               if (index > -1) {
                   convertRawPolygonPoints(builder, innerRawPolygon.substring(0, index + 1));
                   builder.append("\",\"");
                   innerRawPolygon = innerRawPolygon.substring(index + 2, innerRawPolygon.length());
               }
               else {
                   convertRawPolygonPoints(builder, innerRawPolygon);
                   innerRawPolygon = "";
               }
           }
        }

        builder.append("\"]");
        logger.debug("Converted from polygon, after: " + builder.toString());
        return builder.toString();
    }

    private static void convertRawPolygonPoints(StringBuilder builder, String innerRawPolygon) {

       while (innerRawPolygon.length() > 0) {
           String section = innerRawPolygon.substring(1, innerRawPolygon.length() - 1);
           StringTokenizer tokens = new StringTokenizer(section, ",");
           while (tokens.hasMoreTokens()) {
               builder.append(tokens.nextToken().replace(" ", ","));
               if (tokens.hasMoreTokens()) {
                   builder.append(',');
               }
           }
           innerRawPolygon = "";
       }
    }

    /**
     * Needs to convert the Mapping Tool point format into one accepted by PostGIS.
     *  Currently we are creating a Polygon object from the points without any analysis.
     *
     * Currently want: POLYGON((541401.097034593 180055.417435174,541403.364644659 180038.310895393,541411.932087783...,541401.097034593 180055.417435174))
     *           from: 532856.9737,181675.78949999996,532857.6316,181682.1052000001,532857.6316,...
     *
     * or        want: MULTIPOLYGON(((524974.002446135 179800.491899797,524974.002446135 179798.642429203,524950.314385592 179791.804386572,524940.313126476 179809.169415884,524926.634570109 179830.75323758,524912.947768681 179859.704950238,524909.34467698 179871.411599245,524910.053752235 179870.221939781,524964.001187019 179890.226213617,524999.792997259 179900.223351966,525031.371581276 179817.06715518,524974.002446135 179800.491899797)),
     *                              ((525085.582858016 179928.915139028,525055.579080669 179929.964838554,524908.849973314 179873.01114138,524908.734542459 179873.381035499,524873.470416129 179979.70060184,524869.265434968 179986.018793277,524855.050949629 180001.274426397,524891.634285735 180013.650883673,524897.686160583 180016.020205462,524897.686160583 180017.599753321,524929.388420517 180027.466928871,524929.792428511 180027.59689167,524968.519480503 180037.454070082,524991.514955914 180043.88223004,525022.532875776 180057.74826093,525041.892279241 180066.545742677,525046.633189375 180054.959059331,525043.475330973 180054.959059331,525085.582858016 179928.915139028)))
     *           from: ["524974.002446135,179800.491899797,524974.002446135,179798.642429203,524950.314385592,179791.804386572,524940.313126476,179809.169415884,524926.634570109,179830.75323758,524912.947768681,179859.704950238,524909.34467698,179871.411599245,524910.053752235,179870.221939781,524964.001187019,179890.226213617,524999.792997259,179900.223351966,525031.371581276,179817.06715518,524974.002446135,179800.491899797","525085.582858016,179928.915139028,525055.579080669,179929.964838554,524908.849973314,179873.01114138,524908.734542459,179873.381035499,524873.470416129,179979.70060184,524869.265434968,179986.018793277,524855.050949629,180001.274426397,524891.634285735,180013.650883673,524897.686160583,180016.020205462,524897.686160583,180017.599753321,524929.388420517,180027.466928871,524929.792428511,180027.59689167,524968.519480503,180037.454070082,524991.514955914,180043.88223004,525022.532875776,180057.74826093,525041.892279241,180066.545742677,525046.633189375,180054.959059331,525043.475330973,180054.959059331,525085.582858016,179928.915139028"]
     *
     * @param points points to be converted
     *
     * @return points as a polygon
     */
    public static String convertToPolygon(String points) {

        logger.debug("Converting to polygon, before: " + points);

        if (points == null || "".equals(points)) return null;
        StringBuilder builder = new StringBuilder();

       points = points.replaceAll("\\(","").replaceAll("\\)","");
        if (points.contains("\",\"")) {
           builder.append("MULTIPOLYGON(");
           StringTokenizer outsideTokens = new StringTokenizer(points, "\"");
           while (outsideTokens.hasMoreTokens()) {
               String outsideToken = outsideTokens.nextToken();
               if (outsideToken.length() > 1) {
                   if (builder.length() > "MULTIPOLYGON(".length()) {
                       builder.append(',');
                   }

                   builder.append("((");
                   StringTokenizer tokens = new StringTokenizer(outsideToken, ",");
                   while (tokens.hasMoreTokens()) {
                       builder.append(tokens.nextToken()).append(' ').append(tokens.nextToken());
                       if (tokens.hasMoreTokens()) {
                           builder.append(',');
                       }
                   }
                   builder.append("))");
               }
           }
           builder.append(")");
        }
        else {
           if (points.startsWith("[\"")) {
               points = points.substring(2, points.length() - 2);
           }
           builder.append("POLYGON((");
           StringTokenizer tokens = new StringTokenizer(points, ",");
           while (tokens.hasMoreTokens()) {
               builder.append(tokens.nextToken()).append(' ').append(tokens.nextToken());
               if (tokens.hasMoreTokens()) {
                   builder.append(',');
               }
           }
           builder.append("))");
        }

        logger.debug("Converted to polygon, after: " + builder.toString());
        return builder.toString();
    }
}
