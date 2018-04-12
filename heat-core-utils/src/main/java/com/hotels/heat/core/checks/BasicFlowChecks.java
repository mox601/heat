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
package com.hotels.heat.core.checks;

import java.util.HashMap;
import java.util.Map;

import com.hotels.heat.core.testcasedetails.TestCase;

import com.hotels.heat.core.handlers.PlaceholderHandler;
import com.hotels.heat.core.specificexception.HeatException;
import com.hotels.heat.core.utils.TestCaseUtils;
import com.hotels.heat.core.log.Log;

import com.jayway.restassured.response.Response;


/**
 * Basic utility class in flow tests.
 */
public class BasicFlowChecks extends BasicMultipleChecks {

    public static final String OUTPUT_PARAMS_JSON_ELEMENT = "outputParams";
    private static final String FIELD_DELAY_BEFORE = "delayBefore";
    private static final String FIELD_DELAY_AFTER = "delayAfter";

    private final Map<Integer, Map<String, String>> retrievedParameters = new HashMap<>();

    private TestCase tcObject;
    private TestCaseUtils tcUtils;
    private Log logger = new Log(BasicFlowChecks.class);

    public BasicFlowChecks(TestCase tcObject, TestCaseUtils tcUtils) {
        super(tcObject);
        this.tcObject = tcObject;
        this.tcUtils = tcUtils;
    }


    /**
     * This method retrieves info from the json input objects.
     * @param testCaseParamsInput the input parameters to define a test case
     * @return Map webapp name, response from the specified webapp
     */
    @Override
    public Map<String, Response> retrieveInfo(Map testCaseParamsInput) {
        Map<String, Response> respRetrieved = new HashMap<>();
        try {
            compactInfoToCompare(testCaseParamsInput);
            if (getIsRunnable()) {
                int numberOfBlocks = getHttpMethods().size();
                logger.trace(this.tcObject, "number of blocks to load: {}", numberOfBlocks);
                Map<String, Object> singleObjecs = getInputJsonObjs();
                getSteps().forEach((blockID, singleBlockName) -> {
                    logger.debug(this.tcObject, "loading the block id {}: '{}'", blockID, singleBlockName);
                    this.tcObject.setStepId(blockID);
                    Map singleBlockObj = (Map) singleObjecs.get(singleBlockName);

                    if (!retrievedParameters.isEmpty()) {
                        singleBlockObj = processJsonBlockWithPreviousStepsParameters(singleBlockObj);
                    }

                    addDelayOnStep(singleBlockObj, FIELD_DELAY_BEFORE);

                    Response rspStep = retrieveSingleBlockRsp(singleBlockName, singleBlockObj);
                    respRetrieved.put(singleBlockName, rspStep);
                    Map<String, Object> inputJsonBlock = (Map<String, Object>) getInputJsonObjs().get(singleBlockName);
                    logger.debug(this.tcObject, "starting common validation on the block n. {}: '{}'", blockID, singleBlockName);
                    stepValidation(rspStep, blockID, inputJsonBlock);

                    logger.debug(this.tcObject, "Retrieving the output parameters");
                    if (inputJsonBlock.containsKey(OUTPUT_PARAMS_JSON_ELEMENT)) {
                        extractOutputDataFromResponse(blockID, inputJsonBlock, rspStep);
                    }

                    addDelayOnStep(singleBlockObj, FIELD_DELAY_AFTER);
                    this.tcObject.resetStepId();
                });

            }
        } catch (Exception oEx) {
            throw new HeatException(this.getClass(), this.tcObject, oEx);
        }
        return respRetrieved;
    }

    private Map processJsonBlockWithPreviousStepsParameters(Map singleBlockObj) {
        logger.debug(this.tcObject, "Processing the step before execution");
        PlaceholderHandler placeholderHandler = new PlaceholderHandler(this.tcObject);
        placeholderHandler.setFlowVariables(retrievedParameters);
        Map processedJsonBlock = placeholderHandler.placeholderProcessMap(singleBlockObj);
        return processedJsonBlock;
    }


    private void addDelayOnStep(Map stepObject, String fieldName) {
        if (stepObject.containsKey(fieldName)) {
            int delayMs = Integer.valueOf(stepObject.get(fieldName).toString());
            try {
                logger.info(this.tcObject, "Delay of {} ms", delayMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException ex) {
                logger.logException(this.getClass(), this.tcObject, ex, "Interrupted Exception during '{}' phase", fieldName);
            }
        }
    }

    private void stepValidation(Response stepResponse, Integer stepNumber, Map inputJsonBlock) {
        BasicChecks basicChecks = new BasicChecks(this.tcObject);
        basicChecks.setResponse(stepResponse);
        logger.debug(this.tcObject, "response: '{}'", stepResponse.asString());
        basicChecks.setFlowOutputParameters(retrievedParameters);
        basicChecks.commonTestValidation(inputJsonBlock);
    }

        /**
     * Updates the output parameters soon after the request has made.
     *
     * @param blockID the id of the current block
     * @param paramName the name of the parameter to update
     * @param valueToStore the value updated
     */
    private void updateParameters(Integer blockID, String paramName, String valueToStore) {
        if (!retrievedParameters.containsKey(blockID)) {
            retrievedParameters.put(blockID, new HashMap<>());
        }
        Map<String, String> tmp = retrievedParameters.get(blockID);
        logger.debug(this.tcObject, "storing Step[{}].{} = '{}'", blockID, paramName, valueToStore);
        tmp.put(paramName, valueToStore);
        retrievedParameters.put(blockID, tmp);
    }

    private void extractOutputDataFromResponse(Integer blockID, Map<String, Object> inputJsonBlock, Response rsp) {
        Map<String, String> singleBlockOutputParam = (Map<String, String>) inputJsonBlock.get(OUTPUT_PARAMS_JSON_ELEMENT);
        singleBlockOutputParam.forEach((paramName, paramValue)-> {
            logger.debug(this.tcObject, "storing '{}':'{}'", paramName, paramValue);

            PlaceholderHandler placeholderHandler = new PlaceholderHandler(this.tcObject);
            placeholderHandler.setResponse(rsp);
            updateParameters(blockID, paramName, (String) placeholderHandler.placeholderProcessString(paramValue));

        });
    }

}
