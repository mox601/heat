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
package com.hotels.heat.core.handlers;

import java.util.ArrayList;
import java.util.Map;

import com.hotels.heat.core.utils.log.Log;


/**
 * This object manipulates the test cases data in order to solve all possible placeholders.
 * This class aim is not to manage placeholders, but only to delegate PlaceholderHandler if necessary
 */
public class TestCaseMapHandler {

    private final Map<String, Object> testCaseMap;
    private final PlaceholderHandler placeholderHandler;
    private final Log logger = new Log(TestCaseMapHandler.class);
    private TestCase tcObject;

/**
 * TestCaseMapHandler constructor.
 * @param testCaseMapInput is a map containing all data coming from json input file.
 * @param placeholderhandler is the PlaceholderHandler to use. It could contain some data coming from the parsing of
 * the first part of the json input file, so it is useful to pass it in the constructor
 */
    public TestCaseMapHandler(TestCase tcObject, Map testCaseMapInput, PlaceholderHandler placeholderhandler) {
        this.placeholderHandler = placeholderhandler;
        this.testCaseMap = testCaseMapInput;
        this.tcObject = tcObject;
    }

    /**
     * This method is called from the runner, as first thing in test running and is useful to manipulate data
     * coming from the json input file.
     * @return the same structure of the json input file, but with placeholders resolved
     */
    public Map<String, Object> retriveProcessedMap() {
        logger.trace(this.tcObject, "input: '{}'", testCaseMap.toString());
        Map<String, Object> output = (Map<String, Object>) process(testCaseMap);
        logger.trace(this.tcObject, "output: '{}'", output.toString());
        return output;
    }


    private Object processString(Object input) {
        Object output = input;
        logger.trace(this.tcObject, "OLD input:'{}'", input.toString());
        if (input.toString().contains(PlaceholderHandler.PLACEHOLDER_SYMBOL_BEGIN)) {
            output = placeholderHandler.placeholderProcessString((String) input);
        }
        logger.trace(this.tcObject, "NEW input:'{}'", output.toString());
        return output;
    }

    private Object processMap(Object input) {
        Object output = input;
        ((Map<String, Object>) input).forEach((key, valueObj) -> {
            logger.trace(this.tcObject, "key:'{}' / OLD value: '{}'", key, valueObj.toString());
            if (valueObj.getClass().equals(String.class)) {
                valueObj = processString(valueObj);
            } else {
                valueObj = process(valueObj);
            }
            ((Map<String, Object>) output).put(key, valueObj);
            logger.trace(this.tcObject, "key:'{}' / NEW value: '{}'", key, valueObj.toString());
        });

        return output;
    }

    private Object processArrayList(Object input) {
        Object output = input;
        ((ArrayList<Object>) input).forEach((valueObj) -> {
            logger.trace(this.tcObject, "OLD value: '{}'", valueObj.toString());
            int index = ((ArrayList<Object>) input).indexOf(valueObj);
            if (valueObj.getClass().equals(String.class)) {
                valueObj = processString(valueObj);
            } else {
                valueObj = process(valueObj);
            }
            ((ArrayList<Object>) output).set(index, valueObj);
            logger.trace(this.tcObject, "NEW value: '{}'", valueObj.toString());
        });


        return output;
    }

    private Object process(Object input) {
        Object outputObj = input;
        String inputObjClass = input.getClass().getSimpleName();
        logger.trace(this.tcObject, "Class of object to process: {}", inputObjClass);
        logger.trace(this.tcObject, "BEFORE '{}'", input.toString());
        switch (inputObjClass) {
        case "String":
            outputObj = processString(input.toString());
            break;
        case "HashMap":
            outputObj = processMap((Map) input);
            break;
        case "ArrayList":
            outputObj = processArrayList(input);
            break;
        default:
            logger.debug(this.tcObject, "the object '{}' is not yet supported", inputObjClass);
            break;
        }
        logger.trace(this.tcObject, "AFTER '{}'", outputObj.toString());
        return outputObj;
    }


}
