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

import static com.jayway.restassured.path.json.JsonPath.with;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hotels.heat.core.testcasedetails.TestCase;
import org.testng.SkipException;

import com.hotels.heat.core.environment.EnvironmentHandler;
import com.hotels.heat.core.handlers.PlaceholderHandler;
import com.hotels.heat.core.handlers.TestSuiteHandler;
import com.hotels.heat.core.runner.TestBaseRunner;
import com.hotels.heat.core.specificexception.HeatException;
import com.hotels.heat.core.log.Log;

import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.path.json.JsonPath;

/**
 * Class which reads out the test details from the JSON input files.
 *
 */
public class TestCaseUtils {

    public static final String NO_MATCH = "NO MATCH";

    public static final String JSON_FIELD_STEP_NUMBER = "stepNumber";
    public static final String JSON_FIELD_URL = "url";
    public static final String JSON_FIELD_HTTP_METHOD = "httpMethod";
    public static final String JSON_FIELD_POST_BODY = "postBody";
    public static final String JSON_FIELD_MULTIPART_BODY = "parts";
    public static final String JSON_FIELD_MULTIPART_FILE = "file";
    public static final String JSON_FIELD_MULTIPART_NAME = "name";
    public static final String JSON_FIELD_MULTIPART_CONTENT_TYPE = "contentType";
    public static final String JSON_FIELD_MULTIPART_VALUE = "value";
    public static final String JSON_FIELD_COOKIES = "cookies";
    public static final String JSON_FIELD_QUERY_PARAMETERS = "queryParameters";
    public static final String JSON_FIELD_HEADERS = "headers";

    private static final String JSONPATH_GENERAL_SETTINGS = "testSuite.generalSettings";
    private static final String JSONPATH_PRELOAD_SECTION = "testSuite.preloadVariables";
    private static final String JSONPATH_JSONSCHEMAS = "testSuite.jsonSchemas";
    private static final String JSONPATH_TEST_CASES = "testSuite.testCases";
    private static final String SUITE_DESCRIPTION_DEFAULT = "TEST SUITE";
    private static final String SUITE_DESCRIPTION_PATH = "suiteDesc";

    private Map<String, Object> tcParams;
    private Method httpMethod;
    private String webappPath;
    private String suiteDescription;
    private Map<String, String> jsonSchemas;
    private Iterator<Object[]> tcArrayIterator;
    private PlaceholderHandler placeholderHandler;
    private Map<String, Object> preloadVariables;

    private TestCase tcObject;
    private Log logger = new Log(TestCaseUtils.class);

    /**
     * Constructor for TestCaseUtils object.
     * It is used to handle all utilities for the specific request, in terms of collecting general settings and
     * preload variables coming from the json input file driving the test suite. Moreover it handles the running of any test cases
     * inside the suite.
     */
    public TestCaseUtils(TestCase tcObject) {
        this.webappPath = "";
        this.httpMethod = Method.GET;
        this.suiteDescription = SUITE_DESCRIPTION_DEFAULT;
        this.tcObject = tcObject;
    }

    private void loadGeneralSettings(JsonPath testSuiteJsonPath) {
        Map<String, String> generalSettings = testSuiteJsonPath.get(JSONPATH_GENERAL_SETTINGS);
        if (generalSettings.containsKey(JSON_FIELD_HTTP_METHOD)) {
            try {
                httpMethod = Method.valueOf(generalSettings.get(JSON_FIELD_HTTP_METHOD));
            } catch (IllegalArgumentException oEx) {
                throw new HeatException(TestCaseUtils.class, this.tcObject, "HTTP method '{}' not supported", generalSettings.get(JSON_FIELD_HTTP_METHOD));
            }
        }
        if (generalSettings.containsKey(SUITE_DESCRIPTION_PATH)) {
            suiteDescription = generalSettings.get(SUITE_DESCRIPTION_PATH);
        }

    }

    private void loadPreloadedSection(JsonPath testSuiteJsonPath) {
        preloadVariables = testSuiteJsonPath.get(JSONPATH_PRELOAD_SECTION);
        if (preloadVariables != null && !preloadVariables.isEmpty()) {
            logger.debug(this.tcObject, "PRELOAD VARIABLES PRESENT");
            placeholderHandler = new PlaceholderHandler(this.tcObject);
            for (Map.Entry<String, Object> entry : preloadVariables.entrySet()) {
                preloadVariables.put(entry.getKey(), placeholderHandler.placeholderProcessString(entry.getValue().toString()));
                logger.debug(this.tcObject, "PRELOADED VARIABLE: '{}' = '{}'", entry.getKey(), entry.getValue());
            }
        }
    }

    private void loadJsonSchemaForOutputValidation(JsonPath testSuiteJsonPath) {
        jsonSchemas = testSuiteJsonPath.get(JSONPATH_JSONSCHEMAS);
    }

    private Iterator<Object[]> getTestCaseIterator(JsonPath testSuiteJsonPath) {
        List<Object> testCases = testSuiteJsonPath.get(JSONPATH_TEST_CASES);
        List<Object[]> listOfArray = new ArrayList<>();
        for (Iterator<Object> testCasesIterator = testCases.iterator(); testCasesIterator.hasNext();) {
            Object[] array = new Object[1];
            array[0] = testCasesIterator.next();
            listOfArray.add(array);
        }
        tcArrayIterator = listOfArray.iterator();
        return tcArrayIterator;
    }

    /**
     * It is the method that handles the reading of the json input file that is driving the test suite.
     * @param testSuiteFilePath the path of the json input file
     * @return the iterator of the test cases described in the json input file
     */
    public Iterator<Object[]> jsonReader(String testSuiteFilePath) {
        Iterator<Object[]> iterator = null;
        if (logger == null) {
            throw new HeatException(TestCaseUtils.class, this.tcObject, "logUtils null");
        }
        if (this.tcObject == null) {
            throw new HeatException(TestCaseUtils.class, this.tcObject, "context null");
        }

        //check if the test suite is runnable (in terms of enabled environments or test suite explicitly declared in the 'heatTest' system property)
        if (isTestSuiteRunnable(this.tcObject.getTestSuiteName())) { //TODO handle this check with the tcObject
            File testSuiteJsonFile;

            try {
                testSuiteJsonFile = new File(getClass().getResource(testSuiteFilePath).getPath());
            } catch (NullPointerException oEx) {
                logger.error(this.tcObject,"the file '{}' does not exist", testSuiteFilePath);
                throw new HeatException(TestCaseUtils.class, this.tcObject, "the file '{}' does not exist", testSuiteFilePath);
            }

            try {
                JsonPath testSuiteJsonPath = with(testSuiteJsonFile);

                loadGeneralSettings(testSuiteJsonPath);
                loadPreloadedSection(testSuiteJsonPath);
                loadJsonSchemaForOutputValidation(testSuiteJsonPath);
                iterator = getTestCaseIterator(testSuiteJsonPath);
            } catch (Exception oEx) {
                logger.error(this.tcObject,"catched exception message: '{}' \n cause: '{}'",
                        oEx.getLocalizedMessage(), oEx.getCause());
                throw new HeatException(TestCaseUtils.class, this.tcObject, "catched exception '{}'", oEx.getLocalizedMessage());
            }
        } else {
            logger.debug(this.tcObject,"SKIPPED test suite");
            throw new SkipException("[" + this.tcObject.getTestCaseName() + "] Skip test: this suite is not requested");
        }
        return iterator;
    }

    public Map<String, Object> getPreloadedVariables() {
        return preloadVariables;
    }

    /**
     * Method that checks if the test suite is runnable or it is skippable.
     * Checks are made basing on the environments enabled for the specific test (specified in the testng.xml and in the 'environment' system property
     * set in the test running execution command that specifies the environment against with we want to run the test)
     * and on the name of a test suite (explicitly requested by 'heatTest' system property set during the test running execution command).
     * @param currentTestSuite the name of the test suite currently in execution
     * @return a boolean value: 'true' if the test is runnable, 'false' if it is not.
     */
    public boolean isTestSuiteRunnable(String currentTestSuite) {
        boolean isTSrunnable = false;
        EnvironmentHandler eh = TestSuiteHandler.getInstance().getEnvironmentHandler();

        String enabledEnvironments = eh.getEnabledEnvironments();
        String envUnderTest = eh.getEnvironmentUnderTest();
        if (enabledEnvironments.contains(envUnderTest)) {

            List<String> heatTestPropertyList = eh.getHeatTestPropertyList();
            if (heatTestPropertyList.isEmpty()) {
                isTSrunnable = true;
            } else {
                for (String heatTestProperty : heatTestPropertyList) {

                    String[] sysPropTestIdSplitted = TestBaseRunner.heatTestPropertySplit(heatTestProperty);
                    String suiteNameToRun = sysPropTestIdSplitted[0];

                    if (suiteNameToRun == null || (suiteNameToRun != null && currentTestSuite.equalsIgnoreCase(suiteNameToRun))) {
                        isTSrunnable = true;
                        break;
                    }
                }
            }
        }

        return isTSrunnable;
    }

    public void setWebappPath(String path) {
        this.webappPath = path;
    }

    public void setTcParams(Map<String, Object> params) {
        tcParams = params;
    }

    public Method getHttpMethod() {
        return httpMethod;
    }

    public String getSuiteDescription() {
        return suiteDescription;
    }

    /**
     * This method returns the path of the json schema to check.
     *
     * @param jsonSchemaToCheck the json schema to check. It could be set to
     * 'correctResponse' or 'errorResponse'
     * @return the json schema path
     */
    public String getRspJsonSchemaPath(String jsonSchemaToCheck) {
        String jsonSchemaChoosen = PlaceholderHandler.PLACEHOLDER_JSON_SCHEMA_NO_CHECK;
        if (jsonSchemas != null) {
            jsonSchemaChoosen = jsonSchemas.get(jsonSchemaToCheck);
        }
        return jsonSchemaChoosen;
    }

    /**
     * Method usefult to extract a regex from a specific string. If the regex does not produce any result, in output there will be the original string.
     * @param stringToProcess it is the string to parse
     * @param patternForFormat is the pattern (regex) to use
     * @param group it is the group to retrieve from the regular expression extraction
     * @return the extracted string.
     */
    public String regexpExtractor(String stringToProcess, String patternForFormat, int group) {
        String outputStr = stringToProcess;
        try {
            Pattern formatPattern = Pattern.compile(patternForFormat);
            Matcher formatMatcher = formatPattern.matcher(stringToProcess);
            if (formatMatcher.find()) {
                outputStr = formatMatcher.group(group);
            }
        } catch (Exception oEx) {
            logger.warn(this.tcObject,"regexpExtractor warning: stringToProcess = '{}'", stringToProcess);
            logger.warn(this.tcObject,"regexpExtractor warning: patternForFormat = '{}'", patternForFormat);
            logger.warn(this.tcObject,"regexpExtractor warning: group = '{}'", group);
            logger.warn(this.tcObject,"regexpExtractor warning: cause '{}'", oEx.getCause());
        }
        return outputStr;
    }

    /**
     * Method usefult to extract a regex from a specific string. If the regex does not produce any result, in output there will be 'NO MATCH'.
     * @param stringToProcess it is the string to parse
     * @param patternForFormat is the pattern (regex) to use
     * @param group it is the group to retrieve from the regular expression extraction
     * @return the extracted string.
     */
    public String getRegexpMatch(String stringToProcess, String patternForFormat, int group) {
        String outputStr = NO_MATCH;
        try {
            outputStr = regexpExtractor(stringToProcess, patternForFormat, group);
            if (outputStr.equals(stringToProcess)) {
                outputStr = NO_MATCH;
            }
        } catch (Exception oEx) {
            logger.warn(this.tcObject,"Exception cause '{}'", oEx.getCause());
        }
        return outputStr;
    }

    public boolean getSystemParamOnBlocking() {
        return "true".equals(System.getProperty("blockingAssert", "true"));
    }

    /**
     * Method useful to check if all the test parameters are valid (used to check if the suite is runnable or not).
     * @param webappName name of the service under test
     * @param webappPath path of the service under test, referring to the specific environment
     * @param inputJsonPath path of the json input file
     * @param eh environment handler, useful to manage environment variables
     * @return boolean value. 'true' if all the parameters are valid, 'false' otherwise.
     */
    public boolean isCommonParametersValid(String webappName,
        String webappPath,
        String inputJsonPath,
        EnvironmentHandler eh) {
        boolean isValid = true;
        if (webappPath == null) {
            logger.debug(this.tcObject,"webApp path (webapp = {}) is null", webappName);
            isValid = false;
        }
        if (inputJsonPath == null) {
            logger.debug(this.tcObject,"json input file not specified");
            isValid = false;
        }
        String environmentUnderTest = eh.getEnvironmentUnderTest();
        if (!environmentUnderTest.startsWith("http")) {   // customized environments are always enabled
            String enabledEnvironments = eh.getEnabledEnvironments();
            if (!enabledEnvironments.contains(environmentUnderTest)) {
                isValid = false;
            }
        }
        return isValid;
    }

    public void setTcObject(TestCase tcObject) {
        this.tcObject = tcObject;
    }
}
