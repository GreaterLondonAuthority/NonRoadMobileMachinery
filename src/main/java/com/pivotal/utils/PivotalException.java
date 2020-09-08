/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * PivotalUtils specific error handling
 */
public class PivotalException extends RuntimeException {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PivotalException.class);
    private static final long serialVersionUID = -2249475166160092779L;

    private String errorInfo;
    private String errorModule;
    private String errorMessage;
    private Throwable cause;

    /**
     * Creates the exception handler with the given error message
     *
     * @param objError Error message
     */
    public PivotalException(Throwable objError){
        this(null, objError);
    }

    /**
     * Creates the exception handler with the given error message
     *
     * @param message Message to precede actual error with
     * @param objError Error message
     */
    public PivotalException(String message, Throwable objError){
        super(objError);
        cause=objError.getCause();
        errorMessage=(message!=null?message + " - ":"") + (objError.getMessage()!=null?objError.getMessage():objError.getClass().getName());
        errorInfo=getStackTrace(this);
        if (objError.getStackTrace()!=null) {
            StackTraceElement objCaller = objError.getStackTrace()[0];
            errorModule = objCaller.getClassName();
            if (objCaller.getMethodName()!=null) errorInfo = errorInfo + " method:" + objCaller.getMethodName() + "()";
            errorInfo = errorInfo + " line:" + Integer.toString(objCaller.getLineNumber());
        }
    }

    /**
     * Creates the exception handler with the given error message
     *
     * @param errorMessage String error
     */
    public PivotalException(String errorMessage){
        this.errorMessage=errorMessage;
    }

    /**
     * Creates the exception handler with the given error message
     * and String parameters
     *
     * @param errorMessage String error
     * @param parameters String replacement parameters
     */
    public PivotalException(String errorMessage, Object... parameters){
        if (parameters!=null)
            try {
                this.errorMessage = String.format(errorMessage, parameters);
            }
            catch (Exception e) {
                this.errorMessage=errorMessage;
            }
        else
            this.errorMessage=errorMessage;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the stack trace for the given exception
     *
     * @param objException Exception to mine
     * @return String
     */
    public static String getStackTrace(Throwable objException) {
        PrintWriter objPrint=null;
        StringWriter objWriter = new StringWriter();
        try {
            objPrint=new PrintWriter(objWriter);
            objException.printStackTrace(objPrint);
        }
        catch (Exception e){
            logger.warn("Problem getting stack trace - " + e.getMessage());
        }
        finally {
            Common.close(objPrint);
        }
        return objWriter.toString();
    }

    /**
     * Returns the error message for the given
     *
     * @param exception Exception to interrogate
     *
     * @return Error message
     */
    public static String getErrorMessage(Throwable exception) {
        return getErrorMessage(exception, null);
    }

    /**
     * Returns the error message for the given
     *
     * @param exception Exception to interrogate
     * @param stackTrace Accompanying stack trace of error
     *
     * @return Error message
     */
    public static String getErrorMessage(Throwable exception, String stackTrace) {
        String sReturn=null;
        if (exception!=null) {
            sReturn=exception.getMessage()!=null?exception.getMessage():exception.getClass().getName();
            sReturn+=exception.getCause()!=null?" - Cause:" + exception.getCause().getMessage():"";
            if (exception instanceof PivotalException) {
                PivotalException tmp=(PivotalException)exception;
                if (tmp.errorInfo!=null) sReturn+='\n' + tmp.errorInfo;
            }
            else if (!Common.isBlank(stackTrace) && exception.getMessage()==null) {
                sReturn=stackTrace;
            }
        }
        return sReturn;
    }
}
