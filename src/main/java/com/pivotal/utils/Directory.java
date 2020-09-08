/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import javax.naming.*;
import javax.naming.directory.*;
import java.text.Collator;
import java.util.*;

/**
 * Provides a convenient general purpose interface to a directory services
 * provider e.g. LDAP or AD
 */
public class Directory {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Directory.class);

    // Local values

    static final int LDAP_TIMEOUT = 5000;
    static final String DEFAULT_LDAP_PROVIDER = "com.sun.jndi.ldap.LdapCtxFactory";
    static final String LDAP_READ_TIMEOUT_SETTING = "com.sun.jndi.ldap.read.timeout";
    static final String LDAP_CONNECT_TIMEOUT_SETTING = "com.sun.jndi.ldap.connect.timeout";

    static final org.slf4j.Logger mobjLogger = org.slf4j.LoggerFactory.getLogger(Directory.class);
    static Hashtable<String,String> mobjEnv=new Hashtable<>();

    public int LastLdapErroCode;
    public boolean LastErrorIsPermanent;

    /**
     *
     * Creates an Directory object to use for querying
     *
     * @param sServer List of comma separated server addresses
     * @param sUID Username to open server with
     * @param sUPW Password to open server with
     * @param bUseSSL True if a SSL channel should be used
     *
     */
    public Directory(String sServer, String sUID, String sUPW, boolean bUseSSL) {
        this(sServer, sUID, sUPW, bUseSSL, DEFAULT_LDAP_PROVIDER);
    }

    /**
     *
     * Creates an Directory object to use for querying
     *
     * @param sServer List of comma separated server addresses
     * @param sUID Username to open server with
     * @param sUPW Password to open server with
     * @param bUseSSL True if a SSL channel should be used
     * @param sProvider Provider class to use
     *
     */
    public Directory(String sServer, String sUID, String sUPW, boolean bUseSSL, String sProvider) {

        if (!Common.isBlank(sServer)) {
            if (!sServer.toLowerCase().startsWith("ldap://")) sServer = "ldap://" + sServer;
            bUseSSL=bUseSSL || sServer.toLowerCase().startsWith("ldaps://");
            mobjEnv.put(Context.INITIAL_CONTEXT_FACTORY, sProvider);
            mobjEnv.put(Context.PROVIDER_URL, sServer);
            mobjEnv.put(Context.SECURITY_AUTHENTICATION, bUseSSL?"strong":Common.isBlank(sUID)?"none":"simple");
            mobjEnv.put(Context.SECURITY_PRINCIPAL, sUID);
            mobjEnv.put(Context.SECURITY_CREDENTIALS, sUPW);
            mobjEnv.put(LDAP_READ_TIMEOUT_SETTING, LDAP_TIMEOUT + "");
            mobjEnv.put(LDAP_CONNECT_TIMEOUT_SETTING, LDAP_TIMEOUT + "");
        }
    }

    /**
     * Creates a Directory object to authenticate users and get attributes
     *
     * @param sServer Server address in the form of ldap:// or ldaps://
     * @param sUID Username to open server with
     * @param sUPW Password to open server with
     *
     * @return Returns a Directory object to use
     */
    public static Directory getDirectory(String sServer, String sUID, String sUPW) {
        return new Directory(sServer, sUID, sUPW, false);
    }

    /**
     * Creates a Directory object to authenticate users and get attributes
     *
     * @param sServer List of comma separated server addresses
     * @param sUID Username to open server with
     * @param sUPW Password to open server with
     * @param bUseSSL True if a SSL channel should be used for all servers
     *
     * @return Returns a Directory object to use
     */
    public static Directory getDirectory(String sServer, String sUID, String sUPW, boolean bUseSSL) {
        return new Directory(sServer, sUID, sUPW, bUseSSL);
    }

    /**
     *
     * Searches the Directory server using the specified URL and returns an array of
     * DNs up to the number specified in iLimit
     *
     * @param sURL \n separated list of URLs to use to search the Directory server
     * @param iLimit Maximum number of DNs to return (0 means all)
     *
     * @return Array of DNs
     *
     * @throws Exception Throws exception if LDAP objects cannot be found
     *
     */
    public List<String> findObjects(String sURL, int iLimit) throws Exception {

        int iResultsCnt=0;
        List<String> objReturn=null;
        DirContext objCon=null;
        String[] asDummy={};

        // Setup the directory services

        try {

            // Attempt to open the Directory server

            objCon = new InitialDirContext(mobjEnv);
            SearchControls objPref = new SearchControls();
            objPref.setSearchScope(SearchControls.SUBTREE_SCOPE);
            objPref.setTimeLimit(LDAP_TIMEOUT);
            objPref.setDerefLinkFlag(true);
            objPref.setCountLimit(iLimit+1);
            objPref.setReturningObjFlag(false);
            objPref.setReturningAttributes(asDummy);

            // Now loop through the search URLs getting the results

            if (sURL==null) sURL = mobjEnv.get(Context.SECURITY_PRINCIPAL);
            if (Common.isBlank(sURL)) throw new PivotalException("No search URL has been specified");
            String[] asURL = sURL.replaceAll("\r","").split("\n");
            for (int iCnt=0; iCnt<asURL.length && iResultsCnt<iLimit; iCnt++) {
                asURL[iCnt] = asURL[iCnt].trim();
                if (!Common.isBlank(asURL[iCnt])) {

                    // If this is not a search type URL, then simply check the object exists

                    String[] asTmp = asURL[iCnt].split("\\?");
                    if (asTmp.length==1) {
                        Object objResults = objCon.lookup(asTmp[0]);
                        if (objResults!=null) {
                            if (objReturn==null) objReturn=new ArrayList<>();
                            objReturn.add(asURL[iCnt]);
                            iResultsCnt++;
                        }
                    }

                    // Search directory

                    else if (asTmp.length>3) {
                        if ("one".equalsIgnoreCase(asTmp[2]))
                            objPref.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                        else if ("base".equalsIgnoreCase(asTmp[2]))
                            objPref.setSearchScope(SearchControls.OBJECT_SCOPE);
                        else
                            objPref.setSearchScope(SearchControls.SUBTREE_SCOPE);
                        NamingEnumeration objResults = objCon.search(asTmp[0], asTmp[3], objPref);

                        // Get the results

                        while (objResults.hasMore() && iResultsCnt<iLimit) {
                            if (Common.isBlank(objReturn)) objReturn=new ArrayList<>();
                            SearchResult objDN = (SearchResult)objResults.next();
                            if (objReturn!=null)
                                objReturn.add((Common.isBlank(objDN.getName())?"":objDN.getName() + ',') + asTmp[0]);
                            iResultsCnt++;
                        }
                    }
                }
            }
        }
        catch (NamingException e){
            convertLdapErrorCode(e);
            throw new PivotalException("Problem with Directory - %s", e.getRootCause()!=null?e.getMessage():e.getRootCause().toString());
        }
        catch (Exception e){
            convertLdapErrorCode(e);
            throw new PivotalException("Problem with Directory - " + PivotalException.getErrorMessage(e));
        }
        finally {
            // Disconnect if we are connected
            Common.close(objCon);
        }

        return objReturn;
    }

    /**
     *
     * Returns an array of names of all of the attributes in the schema for a given
     * object
     *
     * @param sDN Returns all the attributes for the given object
     *
     * @return Array of names
     *
     * @throws Exception Throws exception if LDAP attributes cannot be read
     * *
     */
    public String[] getAttributeNames(String sDN) throws Exception {

        String[] asReturn=null;
        Set<String> objReturn=new TreeSet<>();
        DirContext objCon=null;

        // Attempt to open the Directory server

        try {
            objCon = new InitialDirContext(mobjEnv);

            // Get all the attributes for this object

            DirContext objResults = objCon.getSchemaClassDefinition(sDN);
            if (objResults != null) {
                SearchControls objScope = new SearchControls();
                objScope.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                NamingEnumeration objNames = objResults.search("", "(|(MUST=*)(MAY=*))", objScope);
                while (objNames.hasMore()) {

                    // Get all the Mandatory and Optional attributes

                    Attributes objAttributes = ((SearchResult)objNames.next()).getAttributes();
                    String[] asAttrs ={"MUST", "MAY"};
                    for (String asAttr : asAttrs) {
                        Attribute objAttr = objAttributes.get(asAttr);
                        if (objAttr != null) {
                            for (int iAttr = 0; iAttr < objAttr.size(); iAttr++) {
                                if (!objReturn.contains(objAttr.get(iAttr).toString()))
                                    objReturn.add(objAttr.get(iAttr).toString());
                            }
                        }
                    }
                }
            }

            // If there is anything to return

            if (!Common.isBlank(objReturn)) asReturn = objReturn.toArray(new String[1]);
        }
        catch (Exception e){
            convertLdapErrorCode(e);
            mobjLogger.error("Problem with Directory - " + PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            // Disconnect if we are connected
            Common.close(objCon);
        }

        return asReturn;
    }


    /**
     *
     * Returns a map of values for all the possible attributes for the given name
     *
     * @param sDN Returns all the attributes for the given object
     *
     * @return Array of names
     *
     * @throws Exception Throws exception if LDAP attributes cannot be read
     *
     */
    public Map<String,AttributeValue> getAttributeValues(String sDN) throws Exception{
        return getAttributeValues(sDN, null);
    }

    /**
     * Returns a map of attribute values for the given list of names
     *
     * @param sDN Returns all the attributes for the given object
     * @param asAttributes array of attribute names
     *
     * @return Array of names
     *
     * @throws Exception Throws exception if LDAP attributes cannot be read
     */
    public Map<String,AttributeValue> getAttributeValues(String sDN, String[] asAttributes) throws Exception {

        // Create a Map that is not case sensitive

        Collator objCollator = Collator.getInstance();
        objCollator.setStrength(Collator.PRIMARY);
        Map<String,AttributeValue> objReturn=new TreeMap<>(objCollator);
        DirContext objCon=null;

        // Attempt to open the Directory server

        try {
            objCon = new InitialDirContext(mobjEnv);

            // Get all the attributes for this object

            Attributes objAttributes;
            if (asAttributes==null)
                objAttributes = objCon.getAttributes(sDN);
            else
                objAttributes = objCon.getAttributes(sDN, asAttributes);
            if (objAttributes!=null) {
                NamingEnumeration objNames = objAttributes.getAll();
                while (objNames.hasMore()) {
                    Attribute objAttr = (Attribute)objNames.next();
                    objReturn.put(objAttr.getID(), new AttributeValue(objAttr));
                }
            }
        }
        catch (Exception e){
            convertLdapErrorCode(e);
            mobjLogger.error("Problem with Directory - " + PivotalException.getErrorMessage(e));
            throw e;
        }
        finally {
            // Disconnect if we are connected
            Common.close(objCon);
        }

        return objReturn;
    }

    /**
     * Authenticate the user credentials
     *
     * @throws Exception Throws exception if LDAP cannot be initialised or authenticated
     */
    public void authenticateUser() throws Exception {

        DirContext objCon=null;

        // Attempt to open the Directory server

        try {
            mobjLogger.debug("Opening Directory server");
            objCon = new InitialDirContext(mobjEnv);
        }
        catch (NamingException e){
            convertLdapErrorCode(e);
            mobjLogger.error("Problem with Directory - %s", e.getRootCause()!=null?e.getMessage():e.getRootCause().toString());
            LastLdapErroCode=0;
            throw e;
        }
        catch (Exception e){
            convertLdapErrorCode(e);
            mobjLogger.debug("Problem with Directory server - " + PivotalException.getErrorMessage(e));
            LastLdapErroCode=0;
            throw e;
        }
        finally {
            Common.close(objCon);
        }
    }

    /**
     *
     * Looks at the class of the exception and returns the JNDI related LDAP
     * error code.
     *
     * @param e Exception to check
     *
     */
    private void convertLdapErrorCode(Exception e) {

        LastErrorIsPermanent=true;

        if (e.getClass().equals(NamingException.class))
            LastLdapErroCode=1;
        else if (e.getClass().equals(CommunicationException.class))
            LastLdapErroCode=2;
        else if (e.getClass().equals(TimeLimitExceededException.class)) {
            LastLdapErroCode=3;
            LastErrorIsPermanent=false;
        }
        else if (e.getClass().equals(SizeLimitExceededException.class))
            LastLdapErroCode=4;
        else if (e.getClass().equals(AuthenticationNotSupportedException.class))
            LastLdapErroCode=7;
        else if (e.getClass().equals(PartialResultException.class))
            LastLdapErroCode=9;
        else if (e.getClass().equals(ReferralException.class))
            LastLdapErroCode=10;
        else if (e.getClass().equals(LimitExceededException.class))
            LastLdapErroCode=11;
        else if (e.getClass().equals(NoSuchAttributeException.class))
            LastLdapErroCode=16;
        else if (e.getClass().equals(InvalidAttributeIdentifierException.class))
            LastLdapErroCode=17;
        else if (e.getClass().equals(InvalidSearchFilterException.class))
            LastLdapErroCode=18;
        else if (e.getClass().equals(InvalidAttributeValueException.class))
            LastLdapErroCode=19;
        else if (e.getClass().equals(AttributeInUseException.class))
            LastLdapErroCode=20;
        else if (e.getClass().equals(NameNotFoundException.class))
            LastLdapErroCode=32;
        else if (e.getClass().equals(AuthenticationException.class))
            LastLdapErroCode=49;
        else if (e.getClass().equals(NoPermissionException.class))
            LastLdapErroCode=50;
        else if (e.getClass().equals(ServiceUnavailableException.class)) {
            LastLdapErroCode=51;
            LastErrorIsPermanent=false;
        }
        else if (e.getClass().equals(OperationNotSupportedException.class))
            LastLdapErroCode=53;
        else if (e.getClass().equals(InvalidNameException.class))
            LastLdapErroCode=64;
        else if (e.getClass().equals(SchemaViolationException.class))
            LastLdapErroCode=65;
        else if (e.getClass().equals(ContextNotEmptyException.class))
            LastLdapErroCode=66;
        else if (e.getClass().equals(NameAlreadyBoundException.class))
            LastLdapErroCode=68;
        else
            LastLdapErroCode=-1;
    }

    /**
     * Returns true if the last operation failed permanently
     *
     * @return True if permanent
     */
    public boolean isLastErrorIsPermanent() {
        return LastErrorIsPermanent;
    }

    /**
     * Returns the error code from th last operation
     *
     * @return LDAP specific error code
     */
    public int getLastErroCode() {
        return LastLdapErroCode;
    }

    /**
     * A class that describes an attribute value
     */
    public static class AttributeValue {

        public String Name;
        public String Value;

        /**
         * Create an LDAP attribute value object
         * @param objValue LDAP attribute
         */
        public AttributeValue(Attribute objValue) {
            Name = objValue.getID();
            try {

                // Store the value based on wether it has multiple values

                mobjLogger.debug("Reading value for attribute " + Name);
                if (objValue.size()==1)
                    Value = (String)objValue.get();
                else {
                    for (int iCnt=0; iCnt<objValue.size(); iCnt++) {
                        Value = (iCnt==0?"":Value + ';') + objValue.get(iCnt);
                    }
                }
            }
            catch (Exception e) {
                logger.debug("Cannot read attribute [%s] - %s", PivotalException.getErrorMessage(e));
            }
        }

        /**
         *
         * Convenient way to read values for Velocity
         *
         * @return Value of the attribute
         *
         */
        public String toString() {
            if (Value==null)
                return "";
            else
                return Value;
        }

        /**
         *
         * Convenient way to return the name for Velocity
         *
         * @return Name of the attribute
         *
         */
        public String getName() {
            return Name;
        }
    }
}
