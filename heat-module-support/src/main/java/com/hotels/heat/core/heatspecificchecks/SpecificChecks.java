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
package com.hotels.heat.core.heatspecificchecks;

import java.util.Map;
import java.util.Set;

import com.hotels.heat.core.testcasedetails.TestCase;
import com.jayway.restassured.response.Response;

/**
 * Abstract class to implement to create a specific check class in test modules.
 */
public abstract class SpecificChecks {

    public void process(Map testCaseParamenter, Map<String, Response> responsesRetrieved, TestCase testCaseDetails, String environment) {
        if (this.handledSuites().contains(testCaseDetails.getTestSuiteName())) {
            this.process(testCaseParamenter, responsesRetrieved, testCaseDetails, environment);
        }
    }

    protected abstract void process(Map testCaseParameter, Map<String, Response> responsesRetrieved, TestCase testCaseDetails, String environment);

    protected abstract Set<String> handledSuites();

}
