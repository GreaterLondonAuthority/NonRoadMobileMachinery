/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/* *
 * Provides a default trust manager that trusts any certificate or host name
 *
 ***************************************************************************/
public class TrustAllManager implements X509TrustManager, HostnameVerifier {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TrustAllManager.class);
    static protected SSLContext SecureSocketContext;

    static {
        SecureSocketContext = null;
    }

    /*     * Given the partial or complete certificate chain provided by the peer,
     * build a certificate path to a trusted root and return if it can be validated
     * and is trusted for client SSL authentication based on the authentication type.
     *
     * The authentication type is determined by the actual certificate used. For
     * instance, if RSAPublicKey is used, the authType should be "RSA". Checking
     * is case-sensitive.
     *
     * @param objCerts - the peer certificate chain
     * @param sAuthType - the authentication type based on the client certificate
     *
     * @throws CertificateException - if the certificate chain is not trusted by this TrustManager.
     *
     ***************************************************************************/
    public void checkClientTrusted(X509Certificate[] objCerts, String sAuthType) throws CertificateException {
    }

    /*     * Given the partial or complete certificate chain provided by the peer,
     * build a certificate path to a trusted root and return if it can be validated
     * and is trusted for client SSL authentication based on the authentication type.
     *
     * The authentication type is the key exchange algorithm portion of the cipher
     * suites represented as a String, such as "RSA", "DHE_DSS". Note: for some
     * exportable cipher suites, the key exchange algorithm is determined at run
     * time during the handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5,
     * the authType should be RSA_EXPORT when an ephemeral RSA key is used for the
     * key exchange, and RSA when the key from the server certificate is used.
     * Checking is case-sensitive
     *
     * @param objCerts - the peer certificate chain
     * @param sAuthType - the authentication type based on the client certificate
     *
     * @throws CertificateException - if the certificate chain is not trusted by this TrustManager.
     *
     ***************************************************************************/
    public void checkServerTrusted(X509Certificate[] objCerts, String sAuthType) throws CertificateException {
        logger.debug("AuthType is " + sAuthType + " Cert issuers;");
        for (X509Certificate objCert : objCerts) {
            logger.debug("    Principle:" + objCert.getIssuerX500Principal().getName() + " Issuer:" + objCert.getIssuerDN().getName());
        }
    }

    /*     * Return an array of certificate authority certificates which are trusted
     * for authenticating peers.
     *
     * @return a non-null (possibly empty) array of acceptable CA issuer certificates
     *
     ***************************************************************************/
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    /*     *
     * Always returns true to accept any host name
     *
     * @param sHostName Host name to verify
     * @param objSession SSL Session in operation
     *
     * @return True if host name is accepted
     *
     ***************************************************************************/
    public boolean verify(String sHostName, SSLSession objSession) {
        logger.debug("Accepting host " + sHostName);
        return true;
    }

    /**
     * Installs the trust all manager into the JVM for the TLS protocol
     */
    public static void installTrustAllManager() {
        installTrustAllManager("TLS");
    }

    /**
     * Installs the trust all manager into the JVM for the given protocol
     *
     * @param protocol TLS/SSL etc
     */
    synchronized public static void installTrustAllManager(String protocol) {

        if (SecureSocketContext == null) {
            try {
                SecureSocketContext = SSLContext.getInstance(protocol);
                SecureSocketContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(SecureSocketContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new TrustAllManager());
            }
            catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to initialise " + protocol + " context", e);
            }
            catch (KeyManagementException e) {
                throw new RuntimeException("Unable to initialise " + protocol + " context", e);
            }
        }
    }
}
