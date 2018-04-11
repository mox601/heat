/**
 * Copyright (C) 2015-2017 Expedia Inc.
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

import com.hotels.heat.core.handlers.TestCase;
import com.hotels.heat.core.utils.log.Log;
import org.testng.Reporter;
import org.testng.annotations.Test;

import com.hotels.heat.core.checks.BasicMultipleChecks;
import com.hotels.heat.core.handlers.TestSuiteHandler;
import com.hotels.heat.core.specificexception.HeatException;
import com.hotels.heat.core.utils.RestAssuredRequestMaker;

import com.jayway.restassured.response.Response;


/**
 * It is the runner of the compare mode.
 */
public class CompareMode extends TestBaseRunner {

    private Log logger = new Log(CompareMode.class);
    private TestCase tcObject;

    /**
     * Method that manages the execution of a single test case.
     * @param testCaseParams Map containing test case parameters coming from the json input file
     */
    @Test(dataProvider = "provider")
    public void runningTest(Map testCaseParams) {

        this.tcObject = super.getTcObject();
        this.tcObject = super.populateTestCaseObjAtomicTc(testCaseParams, this.tcObject);
        TestSuiteHandler testSuiteHandler = TestSuiteHandler.getInstance();

        if (!super.isTestCaseSkippable(this.tcObject, "", "")) {
            Map  testCaseParamsElaborated = super.resolvePlaceholdersInTcParams(this.tcObject, testCaseParams);
            try {
                RestAssuredRequestMaker restAssuredRequestMaker = new RestAssuredRequestMaker();
                BasicMultipleChecks compareChecks = new BasicMultipleChecks(this.tcObject);
                compareChecks.setRestAssuredRequestMaker(restAssuredRequestMaker);
                Map<String, Response> rspRetrieved = compareChecks.retrieveInfo(testCaseParamsElaborated);
                if (rspRetrieved.isEmpty()) {
                    logger.debug(this.tcObject, "not any retrieved response");
                } else {
                    rspRetrieved.entrySet().stream().forEach((entry) -> {
                        if (entry.getValue() == null) {
                            logger.debug(this.tcObject, "RSP retrieved by {} --> null", entry.getKey());
                        } else {
                            logger.debug(this.tcObject, "RSP retrieved by {} --> {}",
                                    entry.getKey(), entry.getValue().asString());
                            Reporter.log(entry.getValue().asString()); //TODO what is this REPORTER???????
                        }
                    });
                    compareChecks.expects(testSuiteHandler.getTestCaseUtils().getSystemParamOnBlocking(), testCaseParamsElaborated, rspRetrieved);

                    super.specificChecks(this.tcObject, testCaseParamsElaborated, rspRetrieved, testSuiteHandler.getEnvironmentHandler().getEnvironmentUnderTest());


                }
            } catch (Exception oEx) {
                throw new HeatException(this.getClass(), this.tcObject, oEx);
            }
        } else {
            this.tcObject.setSkippable();
            logger.debug(this.tcObject, "This test case is skippable");
        }
    }

}
