/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.reporting.reports;

import com.pivotal.monitoring.utils.Parameter;
import com.pivotal.monitoring.utils.ParameterValue;
import com.pivotal.reporting.publishing.Recipient;
import com.pivotal.system.data.dao.DataSourceUtils;
import com.pivotal.system.hibernate.entities.ScheduledTaskEntity;
import com.pivotal.system.hibernate.utils.HibernateUtils;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;
import com.pivotal.utils.VelocityUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Used to supply parameter values to a report at runtime
 */
public class RuntimeParameter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RuntimeParameter.class);

    private String name;
    private Object value;

    /**
     * Constructs a runtime parameter
     *
     * @param name Name of the parameter
     * @param value Value of the parameter
     */
    public RuntimeParameter(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructs a runtime parameter using the values taken from
     * the specific parameter entity
     *
     * @param parameter Entity to look up
     * @param recipients List of the recipients that the report is going to
     * @param task a {@link com.pivotal.system.hibernate.entities.ScheduledTaskEntity} object.
     */
    public RuntimeParameter(ScheduledTaskEntity task, ParameterValue parameter, List<Recipient> recipients) {
        if (parameter==null)
            throw new PivotalException("An empty parameter has been passed to the constructor");
        else if (Common.isBlank(recipients))
            throw new PivotalException("An empty recipients list was passed to the runtime parameter [" + parameter.getName() + ']');
        else {

            // Parse the values for system variables

            name = parameter.getName();
            String content= HibernateUtils.parseSystemVariables(parameter.getString(), task);

            // Now parse the content substituting any possible recipient values

            ParseParameterContent(task, parameter, HibernateUtils.parseRecipientVariables(content, recipients.get(0)));
            logger.debug("Using parameter value for [{}] - [{}]", name, value);
        }
    }

    /**
     * Constructs a runtime parameter using the values taken from
     * the specific parameter entity
     *
     * @param parameter Entity to look up
     * @param task a {@link com.pivotal.system.hibernate.entities.ScheduledTaskEntity} object.
     */
    public RuntimeParameter(ScheduledTaskEntity task, ParameterValue parameter) {
        if (parameter==null)
            throw new PivotalException("An empty parameter has been passed to the constructor");
        else {

            // Parse the values for system variables

            name = parameter.getName();
            String content=HibernateUtils.parseSystemVariables(parameter.getString().trim(), task);

            // Is it a select statement

            ParseParameterContent(task, parameter, content);
            logger.debug("Using parameter value for [{}] - [{}]", name, value);
        }
    }

    /**
     * Constructs a runtime parameter using the values taken from
     * the report parameter
     *
     * @param parameter Entity to look up
     */
    public RuntimeParameter(ParameterValue parameter) {
        if (parameter==null)
            throw new PivotalException("An empty parameter has been passed to the constructor");
        else {
            name = parameter.getName();
            value = parameter.getString();
            logger.debug("Using parameter value for [{}] - [{}]", name, value);
        }
    }

    /**
     * Returns the name of the parameter
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns te string representation of the parameter value
     *
     * @return String
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the runtime value of the parameter
     *
     * @param value Value to set the parameter to
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Parses the parameter content to look for select statements and similar so that it
     * will expand the value into perhaps something from a database etc
     *
     * @param paramValue Content of the parameter
     * @param parameter Parameter definition
     */
    private void ParseParameterContent(ScheduledTaskEntity task, Parameter parameter, String paramValue) {

        if (paramValue!=null) {

            // Is it a select statement

            if (paramValue.matches("(?is)^\\s*select\\s.+")) {
                logger.debug("Using select lookup to get parameter value for [{}] using select statement [{}]", parameter.getName(), paramValue);
                List<LinkedHashMap<String,String>> values= DataSourceUtils.getResults(task.getDatasource(), paramValue, 2);
                if (Common.isBlank(values))
                    throw new PivotalException("The parameter lookup for [" + parameter.getName() + "] is empty - using select statement [" + paramValue + ']');
                else if (values.size()>1)
                    throw new PivotalException("The parameter lookup for [" + parameter.getName() + "] returns more than one row - using select statement [" + paramValue + ']');
                else {

                    // Use the first the first row and the first value of that row

                    List<String> tmp=new ArrayList<>(values.get(0).values());
                    if (Common.isBlank(tmp))
                        throw new PivotalException("The parameter for [" + parameter.getName() + "] is empty");
                    else
                        value=tmp.get(0);
                }
            }
            // Allow for velocity declarations of lists and maps
            // These take the form of ["value","value"] for lists and {"key":"value","key":"value"} for maps

            else if (paramValue.matches("(?ims)\\s*[{\\[].+[}\\]}]\\s*")) {
                paramValue="#set($" + name + '=' + paramValue + ')';
                Writer output=new StringWriter();
                VelocityEngine engine;
                try {
                    engine= VelocityUtils.getEngine();
                    Context context= VelocityUtils.getVelocityContext();
                    engine.evaluate(context, output, HibernateUtils.class.getSimpleName(), paramValue);
                    value=context.get(name);
                }
                catch (Exception e) {
                    logger.error("Problem parsing map/list parameter {} - {}", paramValue, PivotalException.getErrorMessage(e));
                }
            }
            else {
                value=paramValue;
            }
        }
    }
}
