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

import java.util.Map;

import com.hotels.heat.core.testcasedetails.TestCase;
import com.hotels.heat.core.log.Log;
import org.testng.annotations.Test;

import com.hotels.heat.core.checks.BasicFlowChecks;
import com.hotels.heat.core.handlers.TestCaseMapHandler;
import com.hotels.heat.core.handlers.TestSuiteHandler;
import com.hotels.heat.core.utils.RestAssuredRequestMaker;

import com.jayway.restassured.response.Response;


/**
 * It is the runner of the flow mode.
 */
public class FlowMode extends TestBaseRunner {

    private Log logger = new Log(FlowMode.class);
    private TestCase tcObject;

    /**
     * Method that manages the execution of a single test case.
     * @param testCaseParams Map containing test case parameters coming from the json input file
     */
    @Test(dataProvider = "provider")
    public void runningTest(Map testCaseParams) {

        this.tcObject = super.getCurrentTestCase();
        TestCase.populateTestCaseObjAtomicTc(testCaseParams, this.tcObject);
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();


        if (!super.isTestCaseSkippable(this.tcObject, "", "")) { //TODO why are there webappName and webappPath in the check on skippability?????

            logger.debug(this.tcObject, "Preliminary parsing of placeholders in this test case - start");
            Map  testCaseParamsElaborated = super.resolvePlaceholdersInTcParams(this.tcObject, testCaseParams);
            logger.debug(this.tcObject, "Preliminary parsing of placeholders in this test case - end");

            logger.debug(this.tcObject, "This test case is not skippable");


            RestAssuredRequestMaker restAssuredRequestMaker = new RestAssuredRequestMaker(this.tcObject);

            BasicFlowChecks flowChecks = new BasicFlowChecks(this.tcObject, testSuiteHandler.getTestCaseUtils());
            flowChecks.setRestAssuredRequestMaker(restAssuredRequestMaker);

            TestCaseMapHandler tcMapHandler = new TestCaseMapHandler(this.tcObject, testCaseParamsElaborated, getPlaceholderHandler());
            Map<String, Object> elaboratedTestCaseParams = (Map) tcMapHandler.retriveProcessedMap();
            Map<String, Response> rspRetrieved = flowChecks.retrieveInfo(elaboratedTestCaseParams);

            super.specificChecks(this.tcObject, testCaseParamsElaborated, rspRetrieved, testSuiteHandler.getEnvironmentHandler().getEnvironmentUnderTest());

        } else {
            this.tcObject.setSkippable();
            logger.trace(this.tcObject, "This test case is skippable");
        }
        this.tcObject.resetTestCaseId();
    }
}
