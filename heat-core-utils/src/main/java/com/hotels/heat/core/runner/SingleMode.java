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

import java.util.HashMap;
import java.util.Map;

import com.hotels.heat.core.testcasedetails.TestCase;
import com.hotels.heat.core.log.Log;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.hotels.heat.core.checks.BasicChecks;
import com.hotels.heat.core.handlers.TestSuiteHandler;
import com.hotels.heat.core.specificexceptions.HeatException;
import com.hotels.heat.core.utils.RestAssuredRequestMaker;
import com.hotels.heat.core.utils.TestCaseUtils;
import com.hotels.heat.core.utils.TestRequest;

import com.jayway.restassured.response.Response;



/**
 * It is the runner of the single mode.
 */
public class SingleMode extends TestBaseRunner {

    private String webappPath;
    private String webappName;
    private Log logger = new Log(SingleMode.class);
    private TestCase tcObject;

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
        super.beforeTestSuite(propFilePath, inputWebappName, context);
        TestSuiteHandler.getInstance().setWebappName(inputWebappName);
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
        super.beforeTestCase(inputJsonParamPath, enabledEnvironments, context);
        this.webappName = TestSuiteHandler.getInstance().getWebappName();
        this.webappPath = TestSuiteHandler.getInstance().getEnvironmentHandler().getEnvironmentUrl(webappName);
    }


    /**
     * Method that manages the execution of a single test case.
     * @param testCaseParams Map containing test case parameters coming from the json input file
     */
    @Test(dataProvider = "provider")
    public void runningTest(Map testCaseParams) {

        this.tcObject = super.getCurrentTestCase();
        TestCase.populateTestCaseObjAtomicTc(testCaseParams, this.tcObject);

        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance(); //TODO: is it really necessary????


        if (!super.isTestCaseSkippable(this.tcObject, webappName, webappPath)) {
            Map  testCaseParamsElaborated = super.resolvePlaceholdersInTcParams(this.tcObject, testCaseParams);
            logger.debug(this.tcObject, "test case not skippable");
            Response apiResponse = executeRequest(this.tcObject, testCaseParamsElaborated);

            testSuiteHandler.getTestCaseUtils().setWebappPath(webappPath);
            BasicChecks basicChecks = new BasicChecks(this.tcObject);
            basicChecks.setResponse(apiResponse);
            basicChecks.commonTestValidation(testCaseParamsElaborated);

            Map<String, Response> rspMap = new HashMap<>();
            rspMap.put(webappName, apiResponse);

            super.specificChecks(this.tcObject, testCaseParamsElaborated, rspMap, testSuiteHandler.getEnvironmentHandler().getEnvironmentUnderTest());

        } else {
            logger.trace(this.tcObject, "test case skippable");
            this.tcObject.setSkippable();
        }

        this.tcObject.resetTestCaseId();
    }

    private Response executeRequest(TestCase testCaseObj, Map testCaseParamsElaborated) {
        Response apiRsp;

        try {
            RestAssuredRequestMaker restAssuredRequestMaker = new RestAssuredRequestMaker(this.tcObject);
            TestCaseUtils testCaseUtils = TestSuiteHandler.getInstance().getTestCaseUtils();
            testCaseUtils.setTcParams(testCaseParamsElaborated);

            restAssuredRequestMaker.setBasePath(webappPath);
            TestRequest tr = restAssuredRequestMaker.buildRequestByParams(testCaseUtils.getHttpMethod(), testCaseParamsElaborated);
            tr.getHeadersParams().put("X-Heat-Test-Id", testCaseObj.getTestCaseName());
            apiRsp = restAssuredRequestMaker.executeTestRequest(tr);
            if (apiRsp == null) {
                throw new HeatException(SingleMode.class, testCaseObj, "Exception: the service has provided a response null");
            }

        } catch (Exception oEx) {
            throw new HeatException(SingleMode.class, testCaseObj, oEx);
        }

        return apiRsp;
    }

}
