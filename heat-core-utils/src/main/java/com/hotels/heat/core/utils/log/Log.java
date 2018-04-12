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
package com.hotels.heat.core.utils.log;

import com.hotels.heat.core.testcasedetails.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;


/**
 * This class contains utilities for logging.
 */
public class Log {

    //TODO substitute these strings with the proper logback enum
    private Class InputClass;

    private String logLevel;


    public Log(Class InputClass) {
        this.setLogLevel();
        this.InputClass = InputClass;
    }

    /**
     * This method sets the log level (logback).
     */
    public void setLogLevel() {
        logLevel = System.getProperty("logLevel", Level.INFO.toString());
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Level logLevelSetting;

        try {
            logLevelSetting = Level.valueOf(logLevel.toUpperCase());
        } catch (Exception oEx) {
            logLevelSetting = Level.INFO;
        }
        root.setLevel(logLevelSetting);

    }


    public void logException(Class ExceptionClass, TestCase tcObj, Exception oEx) {
        Logger logger = LoggerFactory.getLogger(ExceptionClass);
        logger.error("{} >> Exception: class {}, cause {}, message {}",
                tcObj.getTestCaseName(), oEx.getClass(), oEx.getCause(), oEx.getLocalizedMessage());
    }

    public void logException(Class ExceptionClass, TestCase tcObj, Exception oEx, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(ExceptionClass);
        logger.error("[{}] >> {}", tcObj.getTestCaseName(), message, params);
        logger.error("{} >> Exception: class {}, cause {}, message {}",
                tcObj.getTestCaseName(), oEx.getClass(), oEx.getCause(), oEx.getLocalizedMessage());
    }

    public void info(TestCase tcObj, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(this.InputClass);
        logger.info("[{}] >> {}", tcObj.getTestCaseName(), message, params);
    }

    public void debug(TestCase tcObj, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(this.InputClass);
        logger.debug("[{}] >> {}", tcObj.getTestCaseName(), message, params);
    }

    public void error(TestCase tcObj, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(this.InputClass);
        logger.error("[{}] >> {}", tcObj.getTestCaseName(), message, params);
    }

    public void warn(TestCase tcObj, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(this.InputClass);
        logger.warn("[{}] >> {}", tcObj.getTestCaseName(), message, params);
    }

    public void trace(TestCase tcObj, String message, Object... params) {
        Logger logger = LoggerFactory.getLogger(this.InputClass);
        logger.trace("[{}] >> {}", tcObj.getTestCaseName(), message, params);
    }

}
