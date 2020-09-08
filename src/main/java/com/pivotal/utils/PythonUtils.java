/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

import org.python.core.PyException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides functionality for handling Python execution from within NRMM.
 */
public class PythonUtils {

    /**
     * Creates a nicely formatted string of the line errors within the exception
     *
     * @param e Python exception
     * @param script Script that was executed
     * @return Formatted exception notice
     */
    public static String getPythonBackTrace(PyException e, String script) {

        // Cannot handle no exception,  no script or no exception message.
        if (e == null || Common.isBlank(script)) {
            return null;
        }

        // Try and find where the problem is actually occurring in the script
        String traceBack = e.toString();
        if (Common.isBlank(traceBack)) {
            return null;
        }

        String[] scriptLines = script.split("\n");
        String[] lines = traceBack.split("[\r\n]+");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Search for line numbers

            if (line.matches("(?mis).+line [0-9]+,.+")) {
                Matcher matcher = Pattern.compile("(?mis)line ([0-9]+),").matcher(line);
                if (matcher.find()) {

                    // Get the line from the script and add it to the backtrace
                    int lineNumber = Common.parseInt(matcher.group(1));
                    if (lineNumber > 0 && lineNumber <= scriptLines.length) {
                        lines[i] = line + "\n\t[" + scriptLines[lineNumber - 1].trim() + ']';
                    }
                }
            }
        }
        return Common.join(lines, "\n");
    }

}
