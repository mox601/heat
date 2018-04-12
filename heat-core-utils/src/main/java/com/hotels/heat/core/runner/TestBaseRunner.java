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
package com.hotels.heat.core.runner;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.hotels.heat.core.testcasedetails.TestCase;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.hotels.heat.core.handlers.PlaceholderHandler;
import com.hotels.heat.core.handlers.TestCaseMapHandler;
import com.hotels.heat.core.handlers.TestSuiteHandler;
import com.hotels.heat.core.heatspecificchecks.SpecificChecks;
import com.hotels.heat.core.utils.RunnerInterface;
import com.hotels.heat.core.log.Log;

import com.jayway.restassured.response.Response;


/**
 * Generic Test Runner class.
 */
public class TestBaseRunner implements RunnerInterface {

    static {
        (new Log(TestBaseRunner.class)).info(TestCase.getInstance(), "\n\n"
            + "      ___           ___           ___                 \n"
            + "     /__/\\         /  /\\         /  /\\          ___   \n"
            + "     \\  \\:\\       /  /:/_       /  /::\\        /  /\\  \n"
            + "      \\__\\:\\     /  /:/ /\\     /  /:/\\:\\      /  /:/  \n"
            + "  ___ /  /::\\   /  /:/ /:/_   /  /:/~/::\\    /  /:/   \n"
            + " /__/\\  /:/\\:\\ /__/:/ /:/ /\\ /__/:/ /:/\\:\\  /  /::\\   \n"
            + " \\  \\:\\/:/__\\/ \\  \\:\\/:/ /:/ \\  \\:\\/:/__\\/ /__/:/\\:\\  \n"
            + "  \\  \\::/       \\  \\::/ /:/   \\  \\::/      \\__\\/  \\:\\ \n"
            + "   \\  \\:\\        \\  \\:\\/:/     \\  \\:\\           \\  \\:\\\n"
            + "    \\  \\:\\        \\  \\::/       \\  \\:\\           \\__\\/\n"
            + "     \\__\\/         \\__\\/         \\__\\/                \n");

    }



    public static final String STATUS_SKIPPED = "SKIPPED";

    public static final String TESTCASE_ID_SEPARATOR = ".";
    public static final String TESTCASE_ID_SEPARATOR_ESCAPED = "\\.";

    public static final String SUITE_DESCRIPTION_CTX_ATTR = "suiteDescription";
    public static final String TC_DESCRIPTION_CTX_ATTR = "tcDescription";

    public static final String NO_INPUT_WEBAPP_NAME = "noInputWebappName";
    public static final String WEBAPP_NAME = "webappName";
    public static final String ENABLED_ENVIRONMENTS = "enabledEnvironments";
    public static final String ENV_PROP_FILE_PATH = "envPropFilePath";
    public static final String INPUT_JSON_PATH = "inputJsonPath";

    private PlaceholderHandler placeholderHandler;
    private String inputJsonPath;
    private TestCase tcObject = TestCase.getInstance();
    private Log logger = new Log(TestBaseRunner.class);

    /**
     * Method that takes test suites parameters and sets some environment properties.
     * @param propFilePath path of the property file data
     * @param inputWebappName name of the service to test (optional parameter)
     * @param context testNG context
     */
    @BeforeSuite
    @Override
    @Parameters(value = {ENV_PROP_FILE_PATH, WEBAPP_NAME})
    public void beforeTestSuite(String propFilePath,
                                @Optional(NO_INPUT_WEBAPP_NAME) String inputWebappName,
                                ITestContext context) {
        //logger.debug(this.tcObject, ">> Executing beforeTestSuite method");
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();
        testSuiteHandler.setPropertyFilePath(propFilePath);
        testSuiteHandler.populateEnvironmentHandler();
        populateTestCaseObject(context);
        testSuiteHandler.setTestCaseObject(this.tcObject);
        //logger.debug(this.tcObject, ">> Ended beforeTestSuite method");
    }

    private void populateTestCaseObject(ITestContext context) {
        tcObject.setTestSuiteName(context.getName());
    }

    /**
     * Method that takes tests parameters and sets some environment properties.
     * @param inputJsonParamPath path of the json input file with input data for tests
     * @param enabledEnvironments environments enabled for the specific suite
     * @param context testNG context
     */
    @BeforeTest
    @Override
    @Parameters(value = {INPUT_JSON_PATH, ENABLED_ENVIRONMENTS})
    public void beforeTestCase(String inputJsonParamPath,
        String enabledEnvironments,
        ITestContext context) {
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();
        testSuiteHandler.getEnvironmentHandler().setEnabledEnvironments(enabledEnvironments);
        inputJsonPath = inputJsonParamPath;
    }


    @Override
    @DataProvider(name = "provider")
    public Iterator<Object[]> providerJson() {
        TestSuiteHandler tcHandler = TestSuiteHandler.getInstance();
        tcHandler.setTestCaseObj(this.tcObject);
        Iterator<Object[]> it = tcHandler.getTestCaseUtils().jsonReader(inputJsonPath);
        return it;
    }

    /**
     * Elaboration of test case parameters before any request (method executed as first step in the runner).
     * @param testCaseParams Map containing test case parameters coming from the json input file
     * @return the same structure as the input parameters but with placeholders resolved
     */
    @Override
    public Map resolvePlaceholdersInTcParams(TestCase testCaseObj, Map<String, Object> testCaseParams) {
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();
        // now we start elaborating the parameters.
        placeholderHandler = new PlaceholderHandler(testCaseObj);
        placeholderHandler.setPreloadedVariables(testSuiteHandler.getTestCaseUtils().getPreloadedVariables());

        TestCaseMapHandler tcMapHandler = new TestCaseMapHandler(testCaseObj, testCaseParams, placeholderHandler);

        Map<String, Object> elaboratedTestCaseParams = (Map) tcMapHandler.retriveProcessedMap();
        testSuiteHandler.getTestCaseUtils().setTcParams(elaboratedTestCaseParams);
        return elaboratedTestCaseParams;
    }

    /**
     * Checks if the test case is skippable or not, basing on the name of the current test suite (if the system parameter 'heatTest' is set in the
     * test running command) and on other suppositions.
     * @param testCaseObj TestCase object
     * @param webappName name of the service under test
     * @param webappPath path of the service under test (basing on the environment)
     * @return a boolean that indicates if this test case will be skipped
     */
    public boolean isTestCaseSkippable(TestCase testCaseObj, String webappName, String webappPath) {

        String currentTestSuiteName = testCaseObj.getTestSuiteName();
        String currentTestCaseId = testCaseObj.getTestCaseIdNumber();

        boolean thisTestIsSkippable = false;
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();
        testSuiteHandler.setTestCaseObject(testCaseObj);

        boolean isParamsValid = testSuiteHandler.getTestCaseUtils().isCommonParametersValid(webappName, webappPath, getInputJsonPath(),
                testSuiteHandler.getEnvironmentHandler());
        if (!isParamsValid) {
            thisTestIsSkippable = true; //Skip current test if shared parameters are missing
        } else {

            boolean isCurrentInList = false;
            List<String> heatTestPropertyList = testSuiteHandler.getEnvironmentHandler().getHeatTestPropertyList();
            for (String heatTestProperty : heatTestPropertyList) {

                String[] heatTestPropertySplitted = heatTestPropertySplit(heatTestProperty);
                String suiteNameToRun = heatTestPropertySplitted[0];
                String testCaseIdToRun = heatTestPropertySplitted[1];

                if (suiteNameToRun != null && testCaseIdToRun == null && suiteNameToRun.equalsIgnoreCase(currentTestSuiteName)) {
                    isCurrentInList = true; //Only current suite is specified
                }

                if (suiteNameToRun != null && testCaseIdToRun != null
                    && suiteNameToRun.equalsIgnoreCase(currentTestSuiteName)
                    && testCaseIdToRun.equalsIgnoreCase(currentTestCaseId)) {
                    isCurrentInList = true; //Both suite and test id are specified
                }
            }

            //Skipping current test if there is a list of tests to run (sys property 'heatTest') and the current one is not in that list
            thisTestIsSkippable =  heatTestPropertyList.size() > 0 && !isCurrentInList;
        }

        return thisTestIsSkippable;
    }


    /**
     * Splits the heatTest system property.
     * @param heatTestProperty value of the 'heatTest' system property
     * @return an array of strings containing all the single properties specified in the 'heatTest' system property
     */
    public static String[] heatTestPropertySplit(String heatTestProperty) {
        String[] safeSplit = null;
        if (heatTestProperty != null) {
            //if an element of that list does not contain ".", it means that it is not a specific test case and the output will be a String[] of two elements
            // and the second one is null (it is a test SUITE)
            if (!heatTestProperty.contains(TESTCASE_ID_SEPARATOR)) {
                safeSplit = new String[]{heatTestProperty, null};
            } else {
                //if the element contains the separator ".", it means that it is a specific test case and the output will be a String[] of two elements not null
                safeSplit = heatTestProperty.split(TESTCASE_ID_SEPARATOR_ESCAPED);
            }
        } else {
            safeSplit = new String[]{null, null};
        }
        return safeSplit;
    }


    /**
     * Executes, if necessary, the specific checks related to the test case that is currently running.
     * @param testCaseParams Map containing test case parameters coming from the json input file
     * @param rspRetrieved Map of the responses retrieved from the just executed requests. If the current modality is 'SingleMode', this map will
     *                     contain only one element, otherwise it will contain as much elements as the single test case requires
     * @param environment environment used for this test
     */
    @Override
    public void specificChecks(TestCase tcObject, Map testCaseParams, Map<String, Response> rspRetrieved, String environment) {
        ServiceLoader.load(SpecificChecks.class).forEach((checks) -> {
            checks.process(testCaseParams, rspRetrieved, this.tcObject,
                    TestSuiteHandler.getInstance().getEnvironmentHandler().getEnvironmentUnderTest());
        });
    }


    public PlaceholderHandler getPlaceholderHandler() {
        return placeholderHandler;
    }

    public String getInputJsonPath() {
        return inputJsonPath;
    }

    public void setInputJsonPath(String inputJsonPath) {
        this.inputJsonPath = inputJsonPath;
    }

    public TestCase getCurrentTestCase() {
        return this.tcObject;
    }

//    public ITestContext getTestContext() {
//        return testContext;
//    }

//    public TestCase retrieveTc(Map testCaseParams) {
//        String testSuiteName = testContext.getName();
//        String testCaseId = testCaseParams.get(TestBaseRunner.ATTR_TESTCASE_ID).toString();
//        return TestCase.getInstance(testSuiteName, testCaseId);
//    }



}
