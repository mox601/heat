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
package com.hotels.heat.core.testcasedetails;


import java.util.Map;

public final class TestCase {

    private static TestCase TestCase;
    private String testSuiteName = null;
    private String testCaseIdNumber = null;
    private String testCaseDescription = null;
    private String testStepId = null;
    private boolean isSkippable = false;

    public static final String ATTR_TESTCASE_ID = "testId";
    public static final String ATTR_TESTCASE_DESCRIPTION = "testName";

    private TestCase(String suiteName,
                     String id) {
        this.testSuiteName = suiteName;
        this.testCaseIdNumber = id;
    }

    private TestCase() {}

    /**
     * Singleton implementation for the object.
     * @return the singleton instance of the object
     */
    public static synchronized TestCase getInstance() {
        if (TestCase == null) {
            TestCase = new TestCase();
        }
        return TestCase;
    }

    /**
     * Singleton implementation for the object.
     * @return the singleton instance of the object
     */
    public static synchronized TestCase getInstance(String suiteName,
                                                    String id) {
        if (TestCase == null) {
            TestCase = new TestCase(suiteName, id);
            TestCase.setTestSuiteName(suiteName);
            TestCase.setTestCaseIdNumber(id);
        }
        return TestCase;
    }

    /**
     * Method to set useful parameters in the context managed by testNG.
     * Parameters that will be set will be: 'testId', 'suiteDescription', 'tcDescription'
     * @param testCaseParams Map containing test case parameters coming from the json input file
     */
    /*public void setContextAttributes(Map<String, Object> testCaseParams) {
        String testCaseID = testCaseParams.get(ATTR_TESTCASE_ID).toString();
        testContext.setAttribute(ATTR_TESTCASE_ID, testCaseID);
        String suiteDescription = TestSuiteHandler.getInstance().getTestCaseUtils().getSuiteDescription();
        testContext.setAttribute(SUITE_DESCRIPTION_CTX_ATTR, suiteDescription);
        String testCaseDesc = testCaseParams.get(ATTR_TESTCASE_NAME).toString();
        testContext.setAttribute(TC_DESCRIPTION_CTX_ATTR, testCaseDesc);
    }*/

    public static void populateTestCaseObjAtomicTc(Map testCaseParams, TestCase tcObjectInput) {
        tcObjectInput.setTestCaseIdNumber(testCaseParams.get(ATTR_TESTCASE_ID).toString());
        tcObjectInput.setTestCaseDescription(testCaseParams.get(ATTR_TESTCASE_DESCRIPTION).toString());
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }

    public void setTestSuiteName(String suiteName) {
        this.testSuiteName = suiteName;
    }

    public void setTestCaseDescription(String testCaseDescription) {
        this.testCaseDescription = testCaseDescription;
    }

    public String getTestCaseIdNumber() {
        return testCaseIdNumber;
    }

    public void setTestCaseIdNumber(String id) {
        this.testCaseIdNumber = id;
    }

    public String getTestCaseName() {
        String testCaseName = "";
        if (this.testSuiteName != null) {
            testCaseName += this.testSuiteName;
            if (this.testCaseIdNumber != null) {
                testCaseName += "." + this.testCaseIdNumber;
                if (testStepId != null) {
                    testCaseName += " - step #" + this.testStepId;
                }
            }
        }
        return testCaseName;
    }

    public void setSkippable() {
        isSkippable = true;
    }

    public boolean isSkippable() {
        return isSkippable;
    }


    public void setStepId(int blockID) {
        this.testStepId = String.valueOf(blockID);
    }

    public void resetStepId() {
        this.testStepId = null;
    }

    public void resetTestCaseId() {
        this.testCaseIdNumber = null;
    }

    public String getTestCaseDescription() {
        return this.testCaseDescription;
    }
}
