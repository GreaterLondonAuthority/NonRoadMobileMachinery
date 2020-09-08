/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.servlet;

import com.pivotal.utils.ClassUtils;
import com.pivotal.utils.Common;
import org.springframework.web.servlet.support.BindStatus;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import java.beans.PropertyDescriptor;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Wraps the Spring BindStatus so that we can add some NRMM features
 */
public class SpringBindStatus extends BindStatus {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SpringBindStatus.class);
    private boolean required = false;
    private boolean unique = false;
    private int length = 0;
    private boolean numeric = false;
    private boolean realNumber = false;
    private boolean date = false;
    private boolean string = false;
    private boolean isBoolean = false;
    private String table;
    private int precision = 0;
    private int scale = 0;

    /**
     * Create a new BindStatus instance, representing a field or object status.
     *
     * @param requestContext the current RequestContext
     * @param path the bean and property path for which values and errors
     * will be resolved (e.g. "customer.address.street")
     * @param htmlEscape whether to HTML-escape error messages and string values
     * @throws java.lang.IllegalStateException if no corresponding Errors object found
     */
    public SpringBindStatus(SpringRequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
        super(requestContext, path, htmlEscape);

        // Get the general characteristics

        if (getValueType().equals(Integer.class) || getValueType().equals(Long.class) ||
            getValueType().equals(Double.class) || getValueType().equals(Short.class) ||
            getValueType().equals(int.class) || getValueType().equals(long.class) ||
            getValueType().equals(double.class) || getValueType().equals(short.class)) {
            numeric = true;
            if (getValueType().equals(Double.class) || getValueType().equals(Short.class) ||
                getValueType().equals(double.class) || getValueType().equals(short.class)) {
                realNumber = true;
            }
        }
        else if (getValueType().equals(Boolean.class) || getValueType().equals(boolean.class)) {
            isBoolean = true;
        }
        else if (getValueType().equals(Date.class) || getValueType().equals(Timestamp.class)) {
            date = true;
        }
        else {
            string = true;
        }

        // Get some annotation information

        String beanName = Common.getItem(getPath(), "\\.", 0);
        Object entity = requestContext.getModel().get(beanName);
        if (entity != null) {
            PropertyDescriptor desc = ClassUtils.getPropertyDescriptor(entity.getClass(), getExpression());
            if (desc!=null) {
                Column column = desc.getReadMethod().getAnnotation(Column.class);
                if (column!=null) {
                    required = !column.nullable() && !isBoolean;
                    unique  = column.unique();
                    length = column.length();
                    table = column.table();
                    precision = column.precision();
                    scale = column.scale();
                }
                else {
                    JoinColumn joinColumn = desc.getReadMethod().getAnnotation(JoinColumn.class);
                    if (joinColumn!=null) {
                        required = !joinColumn.nullable() && !isBoolean;
                        unique  = joinColumn.unique();
                        table = joinColumn.table();
                    }
                }
            }
        }
    }

    /**
     * True if the property is required
     *
     * @return True if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * True if the column value is meant to be unique
     *
     * @return True if unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Maximum size of the storage - only relevant for String types
     *
     * @return Length of the storage required
     */
    public int getLength() {
        return length;
    }

    /**
     * True of the column is numeric
     *
     * @return True if numeric
     */
    public boolean isNumeric() {
        return numeric;
    }

    /**
     * True of the column is a real number
     *
     * @return True if real
     */
    public boolean isRealNumber() {
        return realNumber;
    }

    /**
     * True if the column is a date type
     *
     * @return True if date
     */
    public boolean isDate() {
        return date;
    }

    /**
     * True if the column represents a string
     *
     * @return True if string
     */
    public boolean isString() {
        return string;
    }

    /**
     * True if the the type is string and the length is greater then 200 characters
     *
     * @return True if String and &gt; 200 characters
     */
    public boolean isLongString() {
        return length > 200;
    }

    /**
     * True if the column is a boolean type
     *
     * @return True if boolean
     */
    public boolean isBoolean() {
        return isBoolean;
    }

    /**
     * Returns the table name that this column belongs to
     *
     * @return Table name
     */
    public String getTable() {
        return table;
    }

    /**
     * If the column is numeric, this is the precision
     *
     * @return Precision of the column
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * If the column is numeric, this is the scale
     *
     * @return Scale of the column
     */
    public int getScale() {
        return scale;
    }
}
