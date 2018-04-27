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
package com.hotels.heat.core.listeners;

import com.hotels.heat.core.log.Log;
import com.hotels.heat.core.testcasedetails.TestCase;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * CustomTestNgListener is a listener that makes easier to log errors.
 */
public class CustomTestNgListener extends TestListenerAdapter {

    private Log logger = new Log(CustomTestNgListener.class);
    private TestCase tcObject = TestCase.getInstance();

    @Override
    public void onTestFailure(ITestResult tr) {
        logger.trace(this.tcObject, "onTestFailure - start");

        logger.trace(this.tcObject, "onTestFailure - end");
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        logger.trace(this.tcObject, "onTestSuccess - start");

        logger.trace(this.tcObject, "onTestSuccess - end");
    }

    @Override
    public void onFinish(ITestContext testContext) {
        logger.trace(this.tcObject, "onFinish of suite {} - start", testContext.getName());

        logger.trace(this.tcObject, "onFinish of suite {} - end", testContext.getName());
    }

    @Override
    public void onStart(ITestContext testContext) {
        logger.trace(this.tcObject, "onStart of suite {} - start", testContext.getName());

        logger.trace(this.tcObject, "onStart of suite {} - end", testContext.getName());
    }

}
