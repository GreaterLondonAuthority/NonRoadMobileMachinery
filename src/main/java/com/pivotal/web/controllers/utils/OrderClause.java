/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.web.controllers.utils;

import com.pivotal.utils.Common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Handy way of storing and getting an order clause
 */
public class OrderClause implements Serializable {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderClause.class);
    private static final long serialVersionUID = 2165609232490454124L;
    private String orderBy;
    private boolean orderDescending;

    /**
     * Creates an order clause object holder
     *
     * @param orderBy The column to sort by
     * @param orderDescending True if the order is descending
     */
    public OrderClause(String orderBy, boolean orderDescending) {
        this.orderBy = orderBy;
        this.orderDescending = orderDescending;
    }

    /**
     * Returns the column to order by
     *
     * @return column name
     */
    public String getOrderBy() {
        return orderBy;
    }

    /**
     * Returns true if the ordering is descending
     *
     * @return Boolean
     */
    public boolean isOrderDescending() {
        return orderDescending;
    }

    /**
     * Returns the order clause or an empty string
     *
     * @return Order clause
     */
    public String getClause() {
        return getClause(false);
    }

    /**
     * Returns the order clause or an empty string
     *
     * @param omitOrderBy True if the "ORDER BY" bit is ommitted
     * @return Order clause
     */
    public String getClause(boolean omitOrderBy) {
        String sReturn="";
        if (!Common.isBlank(orderBy)) {
            sReturn = (omitOrderBy?"":" order by ") + orderBy;
            sReturn+=orderDescending?" desc":" asc";
        }
        return sReturn;
    }

    /**
     * Creates a
     *
     * @param request Request object
     * @param session Session object
     * @param viewName View to get order for
     * @param defaultOrder Default order to use
     * @return Oder clause object
     */
    public static OrderClause getOrder(HttpServletRequest request, HttpSession session, String viewName, String defaultOrder) {

        // Create a clause from th request

        String orderBy=request.getParameter("orderBy");
        boolean orderDescending=Common.isYes(request.getParameter("orderDescending"));
        OrderClause order=new OrderClause(orderBy, orderDescending);

        // If there isn't anything, then try it for the session

        if (Common.isBlank(orderBy)) {
            if (session!=null) order=(OrderClause)session.getAttribute(viewName + "-order");
            if (order==null && defaultOrder!=null) order=new OrderClause(defaultOrder.split(" +")[0], defaultOrder.matches("(?ims).+ *desc.*"));
        }

        // Save the value back in the session for later

        if (session!=null) session.setAttribute(viewName + "-order", order);
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getClause();
    }

    /**
     * Return true if there is an order clause
     *
     * @return True if there is an order clause
     */
    public boolean hasOrderClause() {
        return !Common.isBlank(getClause());
    }

    /**
     * Returns an order parameter string suitable for inclusion in
     * a query string
     *
     * @return Query string clauses
     */
    public String getUrlParameters() {
        String returnValue="";
        if (hasOrderClause()) {
            returnValue += "orderBy=" + Common.encodeURL(orderBy);
            if (orderDescending) returnValue += "&orderDescending=true";
        }
        return returnValue;
    }
}
