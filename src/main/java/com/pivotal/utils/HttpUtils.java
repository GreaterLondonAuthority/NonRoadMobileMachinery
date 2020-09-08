/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HttpUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpUtils.class);

    public static final int READ_TIMEOUT = 50000;
    public static final int CONNECT_TIMEOUT = 5000;
    public static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT; DigExt)";
    public static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
    public static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain; charset=utf-8";

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     * Uses a 5 second timeout
     * You can pass a request object - this allows the call to use the cookies,
     * particularly the session cookie, with the new HTTP GET i.e. share the session
     * from the caller in the new call
     *
     * @param url     Fully specified URL of the page
     * @param request The request to use for session
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, HttpServletRequest request) {
        return getUrl(url, READ_TIMEOUT, null, null, new CookieManager(request));
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url     Fully specified URL of the page
     * @param timeout Timeout in milliseconds
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, int timeout) {
        return getUrl(url, timeout, null, null);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url            Fully specified URL of the page
     * @param timeout        Timeout in milliseconds
     * @param authentication Username:Password
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, int timeout, String authentication) {
        return executeUrl(url, timeout, null, "GET", null, null, authentication, null);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url         Fully specified URL of the page
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, int timeout, String contentType, String proxy) {
        return getUrl(url, timeout, contentType, proxy, null);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url         Fully specified URL of the page
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @param cookies     Cookies to add to the request
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, int timeout, String contentType, String proxy, CookieManager cookies) {
        return executeUrl(url, timeout, contentType, "GET", null, proxy, "", cookies);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url         Fully specified URL of the page
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @param cookies     Cookies to add to the request
     * @param headers     Map of extra HTTP headers to set
     * @return An HttpContent object
     */
    public static HttpContent getUrl(String url, int timeout, String contentType, String proxy, CookieManager cookies, Map<String, String> headers) {
        return executeUrl(url, timeout, contentType, "GET", null, proxy, "", cookies, headers);
    }

    /**
     * Deletes the resource giben by the specified URL
     * Uses a 5 second timeout
     *
     * @param url Fully specified URL of the page
     * @return An HttpContent object
     */
    public static HttpContent deleteUrl(String url) {
        return deleteUrl(url, READ_TIMEOUT);
    }

    /**
     * Deletes the resource given by the specified URL and waits timeout for read
     *
     * @param url     Fully specified URL of the page
     * @param timeout Timeout in milliseconds
     * @return An HttpContent object
     */
    public static HttpContent deleteUrl(String url, int timeout) {
        return deleteUrl(url, timeout, null, null);
    }

    /**
     * Deletes the resource given by the specified URL and waits timeout for read
     * using the specified content type
     *
     * @param url         Fully specified URL of the page
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @return An HttpContent object
     */
    public static HttpContent deleteUrl(String url, int timeout, String contentType, String proxy) {
        return executeUrl(url, timeout, contentType, "DELETE", null, proxy, "", null);
    }

    /**
     * Writes the content to the server given by the URL
     * Uses a 5 second timeout
     *
     * @param url     Fully specified URL of the page
     * @param content Content to put on the server
     * @return An HttpContent object
     */
    public static HttpContent putUrl(String url, String content) {
        return putUrl(url, content, READ_TIMEOUT);
    }

    /**
     * Writes the content to the server given by the URL using the specified read timeout
     *
     * @param url     Fully specified URL of the page
     * @param content Content to put on the server
     * @param timeout Timeout in milliseconds
     * @return An HttpContent object
     */
    public static HttpContent putUrl(String url, String content, int timeout) {
        return putUrl(url, content, timeout, null, null);
    }

    /**
     * Writes the content to the server given by the URL using the specified read timeout
     * and content type
     *
     * @param url         Fully specified URL of the page
     * @param content     Content to put on the server
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @return An HttpContent object
     */
    public static HttpContent putUrl(String url, String content, int timeout, String contentType, String proxy) {
        return executeUrl(url, timeout, contentType, "PUT", content, proxy, "", null);
    }

    /**
     * Posts the content to the server given by the URL
     * Uses a 5 second timeout
     *
     * @param url            Fully specified URL of the page
     * @param postParameters Map of parameters to send
     * @return An HttpContent object
     */
    public static HttpContent postUrl(String url, Map<String, String> postParameters) {
        return postUrl(url, postParameters, READ_TIMEOUT);
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout
     *
     * @param url            Fully specified URL of the page
     * @param postParameters Map of parameters to send
     * @param timeout        Timeout in milliseconds
     * @return An HttpContent object
     */
    public static HttpContent postUrl(String url, Map<String, String> postParameters, int timeout) {
        return postUrl(url, postParameters, timeout, null, null);
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout
     * and content type
     *
     * @param url            Fully specified URL of the page
     * @param postParameters Map of parameters to send
     * @param timeout        Timeout in milliseconds
     * @param contentType    Content type to specify
     * @param proxy          URL of proxy server to use e.g. 10.0.0.1:8080
     * @return An HttpContent object
     */
    public static HttpContent postUrl(String url, Map<String, String> postParameters, int timeout, String contentType, String proxy) {
        return executeUrl(url, timeout, contentType, "POST", postParameters, proxy);
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout
     * and content type
     *
     * @param url            Fully specified URL of the page
     * @param postParameters Map of parameters to send
     * @param timeout        Timeout in milliseconds
     * @param contentType    Content type to specify
     * @param proxy          URL of proxy server to use e.g. 10.0.0.1:8080
     * @param headers        Map of extra HTTP headers to set
     * @return An HttpContent object
     */
    public static HttpContent postUrl(String url, Map<String, String> postParameters, int timeout, String contentType, String proxy, Map<String, String> headers) {
        return executeUrl(url, timeout, contentType, "POST", postParameters, proxy, headers);
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout etc.
     * Creates the payload as a multipart object expecting that the map of parameters will
     * contain File objects that will be streamed
     *
     * @param url            Fully specified URL of the page
     * @param postParameters Map of parameters to send
     * @param timeout        Timeout in milliseconds
     * @return An HttpContent object
     */
    public static HttpContent postUrlMultipart(String url, Map<String, Object> postParameters, int timeout) {
        return postUrlMultipart(url, postParameters, timeout, null, null, null);
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout etc.
     * Creates the payload as a multipart object expecting that the map of parameters will
     * contain File objects that will be streamed
     *
     * @param url                  Fully specified URL of the page
     * @param postParameters       Map of parameters to send
     * @param timeout              Timeout in milliseconds
     * @param proxy                URL of proxy server to use e.g. 10.0.0.1:8080 (can be null)
     * @param authenticationString Username:Password (can be null)
     * @param cookies              Cookies to add to the request (can be null)
     * @return An HttpContent object
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static HttpContent postUrlMultipart(String url, Map<String, Object> postParameters, int timeout, String proxy, String authenticationString, CookieManager cookies) {
        HttpContent returnValue = null;
        InputStream input;
        ByteArrayOutputStream returnedOutput = null;
        HttpURLConnection connection = null;
        OutputStream output = null;
        String error = null;
        File tmpFile = null;
        try {

            // Get an appropriate connection

            String separator = Common.generateGUID();
            String contentType = String.format("multipart/form-data, boundary=%s", separator);
            connection = getUrlConnection(url, "POST", timeout, contentType, proxy, authenticationString, cookies);
            tmpFile = Common.getTemporaryFile();

            // If we have some parameters, then we need to create a temporary file to
            // output them to so that we can calculate the length

            if (!Common.isBlank(postParameters)) {
                output = new BufferedOutputStream(new FileOutputStream(tmpFile));
                for (Map.Entry entry : postParameters.entrySet()) {
                    if (!Common.isBlank(entry.getKey())) {
                        output.write(String.format("--%s\r\n", separator).getBytes("UTF-8"));

                        // Don't send content if there is no value

                        if (!Common.isBlank(entry.getValue())) {
                            Object value = entry.getValue();
                            String name = (String) entry.getKey();

                            // Check what type of object this actually is

                            if (value instanceof File) {
                                File tmp = (File) value;
                                output.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n", name, tmp.getName()).getBytes("UTF-8"));
                                output.write(String.format("Content-Type: %s\r\n\r\n", Common.getMimeType(tmp.getName())).getBytes("UTF-8"));
                                Common.pipeInputToOutputStream((File) value, output, false, false);
                            }
                            else if (value instanceof InputStream) {
                                output.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n", name, name).getBytes("UTF-8"));
                                output.write(String.format("Content-Type: %s\r\n\r\n", Common.getMimeType("tmp.bin")).getBytes("UTF-8"));
                                Common.pipeInputToOutputStream((InputStream) value, output, false, false);
                            }
                            else {
                                output.write(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s", name, value).getBytes("UTF-8"));
                            }
                        }
                        output.write("\r\n".getBytes("UTF-8"));
                    }
                }
                output.write(String.format("--%s--\r\n", separator).getBytes("UTF-8"));
                output.flush();
                output.close();

                // Now we can set the content length and pipe it all out to the
                // actual socket stream

                connection.setRequestProperty("Content-Length", tmpFile.length() + "");
                Common.pipeInputToOutputStream(tmpFile, connection.getOutputStream());
            }

            // Read the data from the server

            input = connection.getInputStream();
            returnedOutput = new ByteArrayOutputStream();
            Common.pipeInputToOutputStream(input, returnedOutput);

            // Create the content

            String encoding = connection.getContentEncoding();
            if (Common.isBlank(encoding)) {
                returnValue = new HttpContent(connection, returnedOutput.toString());
            }
            else {
                returnValue = new HttpContent(connection, returnedOutput.toString(encoding));
            }
        }
        catch (MalformedURLException e) {
            error = "MalformedURLException: " + e + " [" + url + ']';
            logger.debug(error);
        }
        catch (IOException e) {
            error = "IOException: " + e + " [" + url + ']';
            logger.debug(error);
        }
        catch (Exception e) {
            error = "Exception: " + e + " [" + url + ']';
            logger.debug(error);
        }

        // Cleanup after ourselves

        finally {
            Common.close(returnedOutput, output);
            if (error != null) returnValue = new HttpContent(connection, null, error);
            if (tmpFile != null) tmpFile.delete();
            if (connection != null) connection.disconnect();
        }

        return returnValue;
    }

    /**
     * Posts the content to the server given by the URL using the specified read timeout
     * and content type
     *
     * @param url                  Fully specified URL of the page
     * @param payload              Parameters or resource to upload
     * @param timeout              Timeout in milliseconds
     * @param contentType          Content type to specify
     * @param proxy                URL of proxy server to use e.g. 10.0.0.1:8080
     * @param authenticationString Authentication string to use
     * @return An HttpContent object
     */
    public static HttpContent postUrl(String url, String payload, int timeout, String contentType, String proxy, String authenticationString) {
        return executeUrl(url, timeout, contentType, "POST", payload, proxy, authenticationString, null);
    }


    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url            Fully specified URL of the page
     * @param timeout        Timeout in milliseconds
     * @param contentType    Content type to specify
     * @param methodType     Type of request GET, POST, PUT etc
     * @param postParameters Map of parameters to be posted
     * @param proxy          URL of proxy server to use e.g. 10.0.0.1:8080
     * @return An HttpContent object
     */
    private static HttpContent executeUrl(String url, int timeout, String contentType, String methodType, Map<String, String> postParameters, String proxy) {
        return executeUrl(url, timeout, contentType, methodType, postParameters, proxy, null);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url            Fully specified URL of the page
     * @param timeout        Timeout in milliseconds
     * @param contentType    Content type to specify
     * @param methodType     Type of request GET, POST, PUT etc
     * @param postParameters Map of parameters to be posted
     * @param proxy          URL of proxy server to use e.g. 10.0.0.1:8080
     * @param headers        Map of extra HTTP headers to set
     * @return An HttpContent object
     */
    private static HttpContent executeUrl(String url, int timeout, String contentType, String methodType, Map<String, String> postParameters, String proxy, Map<String, String> headers) {
        String payload = "";
        if (!Common.isBlank(postParameters)) {
            for (Map.Entry entry : postParameters.entrySet()) {
                if (!Common.isBlank(payload)) payload += "&";
                payload += (String) entry.getKey() + '=' + Common.encodeURL((String) entry.getValue(), "UTF-8");
            }
        }
        return executeUrl(url, timeout, contentType, methodType, payload, proxy, "", null, headers);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url         Fully specified URL of the page
     * @param timeout     Timeout in milliseconds
     * @param contentType Content type to specify
     * @param methodType  Type of request GET, POST, PUT etc
     * @param payload     Parameters or resource to upload
     * @param proxy       URL of proxy server to use e.g. 10.0.0.1:8080
     * @param cookies     Cookies to add to the request
     * @return An HttpContent object
     */
    private static HttpContent executeUrl(String url, int timeout, String contentType, String methodType, String payload, String proxy, CookieManager cookies) {
        return executeUrl(url, timeout, contentType, methodType, payload, proxy, "", cookies);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url                  Fully specified URL of the page
     * @param timeout              Timeout in milliseconds
     * @param contentType          Content type to specify
     * @param methodType           Type of request GET, POST, PUT etc
     * @param payload              Parameters or resource to upload
     * @param proxy                URL of proxy server to use e.g. 10.0.0.1:8080
     * @param authenticationString Authentication string to use
     * @param cookies              Cookies to add to the request
     * @return An HttpContent object
     */
    private static HttpContent executeUrl(String url, int timeout, String contentType, String methodType, String payload, String proxy, String authenticationString, CookieManager cookies) {
        return executeUrl(url, timeout, contentType, methodType, payload, proxy, authenticationString, cookies, null);
    }

    /**
     * Reads the page given by the URL and waits for the timeout for the response
     *
     * @param url                  Fully specified URL of the page
     * @param timeout              Timeout in milliseconds
     * @param contentType          Content type to specify
     * @param methodType           Type of request GET, POST, PUT etc
     * @param payload              Parameters or resource to upload
     * @param proxy                URL of proxy server to use e.g. 10.0.0.1:8080
     * @param authenticationString Authentication string to use
     * @param cookies              Cookies to add to the request
     * @param headers              Map of extra HTTP headers to set
     * @return An HttpContent object
     */
    public static HttpContent executeUrl(String url, int timeout, String contentType, String methodType, String payload, String proxy, String authenticationString, CookieManager cookies, Map<String, String> headers) {

        HttpContent returnValue = null;
        InputStream input;
        DataOutputStream output = null;
        ByteArrayOutputStream returnedOutput = null;
        HttpURLConnection connection = null;
        String error = null;
        try {

            // Get an appropriate connection

            connection = getUrlConnection(url, methodType, timeout, contentType, proxy, authenticationString, cookies);

            // If we have some parameters, then send them encoded to the output

            if (!Common.isBlank(payload)) {
                connection.setRequestProperty("Content-Length", Integer.toString(payload.getBytes().length) + "");

                if (headers != null) {
                    for (String header : headers.keySet()) {
                        connection.setRequestProperty(header, headers.get(header));
                    }
                }

                output = new DataOutputStream(connection.getOutputStream());
                output.writeBytes(payload);
                output.close();
                output = null;
            }

            // Read the data from the server

            input = connection.getInputStream();
            returnedOutput = new ByteArrayOutputStream();
            Common.pipeInputToOutputStream(input, returnedOutput);

            // Create the content

            String encoding = connection.getContentEncoding();
            if (Common.isBlank(encoding)) {
                returnValue = new HttpContent(connection, returnedOutput.toString(), null, returnedOutput.toByteArray());
            }
            else {
                returnValue = new HttpContent(connection, returnedOutput.toString(encoding), null, returnedOutput.toByteArray());
            }
        }
        catch (MalformedURLException e) {
            error = e + " [" + url + ']';
            logger.debug(error);
        }
        catch (SocketTimeoutException e) {
            error = e + " [" + url + ']';
            logger.debug(error);
        }
        catch (IOException e) {
            error = e + " [" + url + ']';
            logger.debug(error);
        }

        // Cleanup after ourselves

        finally {
            Common.close(returnedOutput, output);
            if (error != null) {
                returnValue = new HttpContent(connection, null, error);
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return returnValue;
    }

    /**
     * Creates a new HTTP type connection to the specified URL using the method
     * type and content type specified
     *
     * @param url                  Fully specified URL to use
     * @param methodType           GET, PUT, POST etc
     * @param timeout              Read timeout in milliseconds
     * @param contentType          Type of content of request - set to null to allow automatic choice
     * @param proxy                URL of proxy server to use e.g. 10.0.0.1:8080
     * @param authenticationString Authentication string to use
     * @param cookies              Cookies to add to the request
     * @return An HTTP(S) connection object
     *
     * @throws java.net.MalformedURLException If there is a problem with the URL
     * @throws java.io.IOException            If there is a problem with establishing the connection
     */
    private static HttpURLConnection getUrlConnection(String url, String methodType, int timeout, String contentType, String proxy, String authenticationString, CookieManager cookies) throws IOException {

        HttpURLConnection connection;
        URL objUrl = new URL(url);

        // If it is an HTTPS then engage the trust all manager

        if ("https".equalsIgnoreCase(objUrl.getProtocol())) {
            TrustAllManager.installTrustAllManager();
        }

        // Open the connection, including the proxy if there is one

        if (!Common.isBlank(proxy)) {
            String host = proxy.trim();
            int port = 0;
            if (proxy.matches("[^: ]+[: ] *[0-9]+")) {
                host = proxy.split(" *[: ] *", 2)[0];
                port = Common.parseInt(proxy.split(" *[: ] *", 2)[1]);
            }
            Proxy proxyObj = new Proxy(Common.doStringsMatch(objUrl.getProtocol(), "https") ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(host, port));
            connection = (HttpURLConnection) objUrl.openConnection(proxyObj);
        }
        else {
            connection = (HttpURLConnection) objUrl.openConnection();
        }

        // Apply the Basic authentication string if present (username:password)

        if (!Common.isBlank(authenticationString)) {
            try {
                connection.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary(authenticationString.getBytes()));
            }
            catch (Exception e) {
                logger.warn("Setting of authorisation failed " + e.getMessage());
            }
        }

        // Add the cookies if they exist

        if (cookies != null) {
            CookieManager.setCookies(connection, cookies);
        }

        // Setup the rest of the options - no cache etc

        connection.setAllowUserInteraction(false);
        connection.setDefaultUseCaches(false);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setDoInput(true);
        connection.setDoOutput(Common.doStringsMatch(methodType, "post", "put"));

        // Set the HTTP(S) connection settings

        connection.setRequestMethod(methodType.toUpperCase());
        connection.setFollowRedirects(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Host", objUrl.getHost());
        connection.setRequestProperty("User-Agent", USER_AGENT);

        // Set the character encoding

        if (Common.isBlank(contentType)) {
            if (Common.doStringsMatch(methodType, "post", "put"))
                connection.setRequestProperty("Content-Type", FORM_CONTENT_TYPE);
        }
        else
            connection.setRequestProperty("Content-Type", contentType);

        return connection;
    }

    /**
     * Returns the IP address of the remote host taking care of where it might
     * be held in a load balancer X header
     *
     * @param request Servlet request
     * @return IP address of the client
     */
    public static String getAddressFromRequest(HttpServletRequest request) {
        if (request != null) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && (forwardedFor = Common.findNonPrivateIpAddress(forwardedFor)) != null)
                return forwardedFor;
            return request.getRemoteAddr();
        }
        else return null;
    }

    /**
     * Retruns the hostname of the client or IP address if the hostname is absent
     *
     * @param request Servlet request
     * @return Name or IP of the host
     */
    public static String getHostnameFromRequest(HttpServletRequest request) {
        String addr = getAddressFromRequest(request);
        try {
            return Inet4Address.getByName(addr).getHostName();
        }
        catch (Exception e) {
            logger.debug(PivotalException.getErrorMessage(e));
        }
        return addr;
    }

    /**
     * Returns the INET address from the request
     *
     * @param request Servlet request
     * @return INET address
     *
     * @throws java.net.UnknownHostException
     */
    public static InetAddress getInet4AddressFromRequest(HttpServletRequest request) throws UnknownHostException {
        return Inet4Address.getByName(getAddressFromRequest(request));
    }

    /**
     * Extracts the base URL from the request. This is useful when you wish to create a URL back to the
     * application. The address they used will get back here, but it will not work if the request information is not
     * send to the application by a reverse proxy (hint: use the AJP protocol).
     *
     * @param request The request to extract the base url from
     * @return The base url from this request
     */
    public static String getBaseUrl(HttpServletRequest request) {

        // This is cheating as it can only be called during a request. If this method is called outside of a request
        // then it will return an empty string.

        try {

            // The request is available for this thread through the ServletHelper

            URL currentRequestURL = new URL(request.getRequestURL().toString());

            // Create the base URL

            return String.format("%s://%s%s", currentRequestURL.getProtocol(), currentRequestURL.getAuthority(), request.getContextPath());
        }
        catch (Exception e) {
            logger.error("Could not extract URL from request", e);
        }

        // This should not be null, but we have to something if it is

        return Common.emptyString;
    }

    /**
     * A class that encapsulates an HTTP/HTTPS page
     */
    public static class HttpContent {

        String contentType;
        String contentEncoding;
        byte[] byteContent;
        String content;
        Date date;
        Date lastModified;
        Map<String, List<String>> headers;
        int responseCode;
        String responseMessage;
        CookieManager cookies;
        String error;

        /**
         * Creates a content object based on the URL connection
         *
         * @param connection URL Connection object
         * @param content    String content of the page
         */
        private HttpContent(HttpURLConnection connection, String content) {
            this(connection, content, null, null);
        }

        /**
         * Creates a content object based on the URL connection
         *
         * @param connection URL Connection object
         * @param content    String content of the page
         * @param error      Any errors with the page
         */
        private HttpContent(HttpURLConnection connection, String content, String error) {
            this(connection, content, error, null);
        }

        /**
         * Creates a content object based on the URL connection
         *
         * @param connection  URL Connection object
         * @param content     String content of the page
         * @param error       Any errors with the page
         * @param byteContent Contents of the response as a byte[]. Useful for images or other binary content
         */
        private HttpContent(HttpURLConnection connection, String content, String error, byte[] byteContent) {
            this.byteContent = byteContent;
            this.content = content;
            this.error = error;
            responseCode = HttpURLConnection.HTTP_NOT_FOUND;
            responseMessage = "NOT FOUND";
            try {
                if (connection != null) {
                    contentType = connection.getContentType();
                    contentEncoding = connection.getContentEncoding();
                    date = new Date(connection.getDate());
                    lastModified = new Date(connection.getLastModified());
                    headers = connection.getHeaderFields();
                    responseCode = connection.getResponseCode();
                    responseMessage = connection.getResponseMessage();
                    cookies = new CookieManager(connection);
                }
            }
            catch (Exception e) {
                logger.debug(PivotalException.getErrorMessage(e));
            }
        }

        /**
         * Returns the content type of the URL
         *
         * @return Content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Returns the character encoding of the page
         *
         * @return Character encoding
         */
        public String getContentEncoding() {
            return contentEncoding;
        }

        /**
         * Returns the byte[] page content
         *
         * @return byteContent
         */
        public byte[] getByteContent() {
            return byteContent;
        }

        /**
         * Returns the string page content
         *
         * @return Content
         */
        public String getContent() {
            return content;
        }

        /**
         * Returns the string page content as a JSON object
         *
         * @return JSON object
         */
        public Object getContentFromJSON() {
            return Common.readJSON(content);
        }

        /**
         * Returns the XML page content as a DomHelper object
         *
         * @return DomHelper object
         */
        public DomHelper getContentFromXML() {
            return Common.readXML(content);
        }

        /**
         * Returns the string page content
         *
         * @return Content
         */
        public String toString() {
            return content;
        }

        /**
         * Returns the date as the resource is known by on the server
         *
         * @return Resource date
         */
        public Date getDate() {
            return date;
        }

        /**
         * Returns the date that the resource was last modified
         *
         * @return Date of last modification
         */
        public Date getLastModified() {
            return lastModified;
        }

        /**
         * Returns the map of header values as returned from the server
         *
         * @return Map of lists keyed on string
         */
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        /**
         * Returns the response code from the request
         *
         * @return HTTP Response code
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * Returns true if the response is OK
         *
         * @return True if OK
         */
        public boolean isResponseOK() {
            return responseCode == HttpServletResponse.SC_OK;
        }

        /**
         * Returns the response message from the request
         *
         * @return HTTP Status message
         */
        public String getResponseMessage() {
            return responseMessage;
        }

        /**
         * Returns the cookie manager for these cookies
         *
         * @return Cookie manager
         */
        public CookieManager getCookies() {
            return cookies;
        }

        /**
         * Returns any error message from the recent transaction
         *
         * @return Error string
         */
        public String getError() {
            return error;
        }
    }

}
