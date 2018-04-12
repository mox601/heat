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
package com.hotels.heat.core.specificexception;

import com.hotels.heat.core.testcasedetails.TestCase;
import com.hotels.heat.core.log.Log;

/**
 * Specific exception for heat test framework.
 */
public class HeatException extends Error {

    private Log logger;

    public HeatException(Class InputClass, TestCase tcObj, Exception oEx) {
        logger = new Log(InputClass);
        logger.logException(oEx.getClass(), tcObj, oEx);
    }

    public HeatException(Class InputClass, TestCase tcObj, String message, Object... params) {
        logger = new Log(InputClass);
        logger.error(tcObj, message, params);
    }

    public HeatException(Class InputClass, TestCase tcObj, Exception oEx, String message, Object... params) {
        logger = new Log(InputClass);
        logger.error(tcObj, message, params);
        logger.logException(oEx.getClass(), tcObj, oEx);
    }
}
