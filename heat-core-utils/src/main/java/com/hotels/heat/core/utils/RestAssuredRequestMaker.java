/**
 * Copyright (C) 2015-2018 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.heat.core.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import com.hotels.heat.core.testcasedetails.TestCase;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.conn.ConnectTimeoutException;

import com.google.common.base.Optional;

import com.hotels.heat.core.specificexception.HeatException;
import com.hotels.heat.core.log.Log;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.internal.print.RequestPrinter;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.MultiPartSpecification;
import com.jayway.restassured.specification.RequestSpecification;


/**
 * Utility class to make requests and retrieve responses with rest assured.
 */
public class RestAssuredRequestMaker {

    private static final String LOG_LEVEL_PROPERTY      = "setLogLevel";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private RequestSpecification requestSpecification;
    private String defUrl;
    private String webappPath;

    private Log logger = new Log(RestAssuredRequestMaker.class);
    private TestCase tcObject;

    /**
     * Constructor of the class RestAssuredRequestMaker. This is the class that really executes the request against the service under test.
     */
    public RestAssuredRequestMaker(TestCase tcObject) {
        this.tcObject = tcObject;
        this.defUrl = "";
        this.webappPath = "";
    }

    public void setBasePath(String webappPath) {
        this.webappPath = webappPath;
    }

    /**
     * Builds a test request starting from test case parameters.
     *
     * @param httpMethod http request method
     * @param singleRequestParamsMap map containing json attribute related to a single test. It contains exactly the same data as the ones contained
     *                               in the json input file
     * @return TestRequest object with all the info related to the test to run
     */
    public TestRequest buildRequestByParams(Method httpMethod, Map singleRequestParamsMap) {

        Map<String, String> cookiesParam = setRequestParameters(singleRequestParamsMap, TestCaseUtils.JSON_FIELD_COOKIES);
        Map<String, Object> queryParams = setRequestParameters(singleRequestParamsMap, TestCaseUtils.JSON_FIELD_QUERY_PARAMETERS);
        Map<String, String> headersParam = setRequestParameters(singleRequestParamsMap, TestCaseUtils.JSON_FIELD_HEADERS);
        if (!headersParam.containsKey(HEADER_CONTENT_TYPE)) {
            headersParam.put(HEADER_CONTENT_TYPE, "application/json"); // set default Content-Type
        }

        String url = this.webappPath;
        if (singleRequestParamsMap.containsKey(TestCaseUtils.JSON_FIELD_URL)) {
            url += (String) singleRequestParamsMap.get(TestCaseUtils.JSON_FIELD_URL);
        }

        setRequestSpecification(cookiesParam, headersParam, url);

        logger.trace(this.tcObject, "HTTP-METHOD >> {}", httpMethod.name());

        headersParam.forEach((k, v) -> logger.trace(this.tcObject, "HEADER-PARAM >> {}:{}", k, v));

        cookiesParam.forEach((k, v) -> logger.trace(this.tcObject, "COOKIE-PARAM >> {}:{}", k, v));

        logger.trace(this.tcObject, "URL >> '{}'", url);

        TestRequest tcRequest = new TestRequest(url);
        tcRequest.setHeadersParams(headersParam);
        tcRequest.setCookieParams(cookiesParam);
        tcRequest.setHttpMethod(httpMethod);
        tcRequest.setQueryParams(queryParams);

        return tcRequest;
    }

    /**
     * Execute a request and return the related response. If the test is in DEBUG log level modality, it shows also the curl so that it can be
     * more useful to replicate a single request in a separate shell.
     *
     * @param testRequest TestRequest object with all the info related to the test to run
     * @return the Response retrieved from the service under test
     */
    public Response executeTestRequest(TestRequest testRequest) {
        HttpRequestFormatter reqFormatter = new HttpRequestFormatter(testRequest, this.tcObject);
        logger.debug(this.tcObject, "CURL-HINT >> \n" + reqFormatter.toCURL());

        return executeHttpRequest(testRequest.getHttpMethod(), testRequest.getUrl(), testRequest.getQueryParams());

    }

    private String getPostBodyFromQueryParams(Method httpMethod, Map<String, Object> queryParams) {
        String postBody = (String) queryParams.get(TestCaseUtils.JSON_FIELD_POST_BODY);
        try {
            postBody = HttpRequestFormatter.readPostBodyFromFile(postBody);
        } catch (FileNotFoundException fileException) {
            throw new HeatException(this.getClass(), this.tcObject, "RestAssuredMessages::executeHttpRequest --> post body file '{}' not found!", postBody);
        } catch (IOException ioException) {
            throw new HeatException(this.getClass(), this.tcObject, "RestAssuredMessages::executeHttpRequest --> IOException message: {}", ioException.getLocalizedMessage());
        } catch (Exception ex) {
            logger.error(this.tcObject, "({}) -- Exception in buildRequestByParams: {}", httpMethod.name(), ex.getLocalizedMessage());
        }
        return postBody;
    }

    private <K, V> Map<K, V> setRequestParameters(Map singleRequestParamsMap, String paramName) {
        Map<K, V> extractedParams;
        if (singleRequestParamsMap.containsKey(paramName)) {
            extractedParams = (Map<K, V>) singleRequestParamsMap.get(paramName);
        } else {
            extractedParams = new HashMap<>();
        }
        return extractedParams;
    }

    /**
     * executeHttpRequest is the method that, according to the specific http method (GET, POST, PUT, DELETE) executes the request.
     *
     * @param httpMethod is the HTTP method of the request
     * @param url is the url of the service to call
     * @param queryParams is a Map<String, String> with the query parameters
     * @return the response of the request done.
     */
    private Response executeHttpRequest(Method httpMethod, String url, Map<String, Object> queryParams) {
        Response serviceResponse = null;

        try {
            requestSpecification.redirects().follow(false);

            if (queryParams.containsKey(TestCaseUtils.JSON_FIELD_POST_BODY)) {
                requestSpecification.body(getPostBodyFromQueryParams(httpMethod, queryParams));
            } else if (queryParams.containsKey(TestCaseUtils.JSON_FIELD_MULTIPART_BODY)) {
                final Object parts = queryParams.get(TestCaseUtils.JSON_FIELD_MULTIPART_BODY);
                if (!(parts instanceof List)) {
                    throw new HeatException(this.getClass(), this.tcObject, "'parts' definition should be an array");
                }

                final List<MultiPartSpecification> multipart = MultipartUtils.convertToMultipart((List<Map<String, String>>) parts);

                multipart.forEach(requestSpecification::multiPart);

            } else if (!queryParams.isEmpty()) {
                addQueryParameters(queryParams);
            }

            logger.debug(this.tcObject, "Detailed Request: \n{}", getRequestDetails(httpMethod, url));
            switch (httpMethod) {
            case GET:
                serviceResponse = requestSpecification.when().get(url);
                break;
            case PUT:
                serviceResponse = requestSpecification.when().put(url);
                break;
            case DELETE:
                serviceResponse = requestSpecification.when().delete(url);
                break;
            case POST:
                serviceResponse = requestSpecification.when().post(url);
                break;
            default:
                logger.warn(this.tcObject, "HTTP METHOD '{}' not recognized. GET METHOD used as default", httpMethod.toString());
                serviceResponse = requestSpecification.when().get(url);
                break;
            }
            logger.debug(this.tcObject, "The response is: {}", serviceResponse.asString());
        } catch (Exception oEx) {
            logger.logException(this.getClass(), this.tcObject, oEx);
            if (oEx.getClass().equals(ConnectTimeoutException.class)) {
                throw new HeatException(this.getClass(), this.tcObject, oEx, "RestAssuredMessages::executeHttpRequest --> Connect Timeout Exception");
            }
            logger.error(this.tcObject, "The response is null");
        }
        return serviceResponse;

    }


    private void addQueryParameters(Map<String, ?> queryParams) {
        queryParams.forEach((key, value) -> {
            if (value instanceof String) {
                requestSpecification.parameter(key, value);
            } else {
                ((ArrayList<String>) value).forEach(singleValue -> {
                    requestSpecification.parameter(key, singleValue);
                });
            }
        });
    }

    /**
     * Method useful to set data to the RequestSpecification (url, cookies and headers).
     *
     * @param url is the url of the service to call
     * @param cookies is a key-value Map with the cookies
     * @param headers is a key-value Map with the headers
     */
    public void setRequestSpecification(Map<String, String> cookies, Map<String, String> headers, String url) {
        requestSpecification = protocolSetting(url);
        if (cookies != null) {
            requestSpecification = requestSpecification.cookies(cookies);
        }
        if (headers != null) {
            requestSpecification = requestSpecification.headers(headers);
        }
    }

    /**
     * This method returns a RequestSpecification object based on a specific
     * protocol.
     *
     * @param url the environment url.
     * @return RequestSpecification RequestSpecification
     */
    public RequestSpecification protocolSetting(String url) {
        RequestSpecification testReqSpecification;
        if (url.startsWith("https://")) {
            testReqSpecification = givenSsl();
        } else {
            testReqSpecification = given();
        }
        return testReqSpecification;
    }

    /**
     * This method returns a RequestSpecification object specific for the http
     * method.
     *
     * @return the request specification
     */
    private RequestSpecification given() {
        RequestSpecification restSpec;
        String logLevelProperty = System.getProperty(LOG_LEVEL_PROPERTY, Level.INFO.toString()).toUpperCase();
        if (Level.DEBUG.toString().equals(logLevelProperty)) {
            restSpec = RestAssured.given().baseUri(defUrl);
        } else {
            restSpec = RestAssured.given().log().ifValidationFails().baseUri(defUrl);
        }
        return restSpec;
    }

    /**
     * This method returns a RequestSpecification object specific for the https
     * method.
     *
     * @return the request specification for SSL endpoint
     */
    private RequestSpecification givenSsl() {
        RequestSpecification restSpec;
        String logLevelProperty = System.getProperty(LOG_LEVEL_PROPERTY, Level.INFO.toString()).toUpperCase();
        if (Level.DEBUG.toString().equals(logLevelProperty)) {
            restSpec = RestAssured.given().urlEncodingEnabled(true).relaxedHTTPSValidation().baseUri(defUrl);
        } else {
            restSpec = RestAssured.given().urlEncodingEnabled(true).log().ifValidationFails().relaxedHTTPSValidation().baseUri(defUrl);
        }
        return restSpec;
    }

    private String getRequestDetails(Method httpMethod, String url) {
        Optional<String> requestDetails = Optional.absent();
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            requestDetails = Optional.fromNullable(RequestPrinter.print((FilterableRequestSpecification) requestSpecification, httpMethod.name(), url, LogDetail.ALL,
                    new PrintStream(os), true));
        } catch (IOException e) {
            logger.error(this.tcObject, "Unable to log 'Request Details', error occured during retrieving the information");
        }
        return requestDetails.or("");
    }

}
