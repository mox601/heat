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
package com.hotels.heat.core.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hotels.heat.core.checks.BasicMultipleChecks;
import com.hotels.heat.core.specificexceptions.HeatException;
import com.hotels.heat.core.testcasedetails.TestCase;
import com.hotels.heat.core.utils.DataExtractionSupport;
import com.hotels.heat.core.utils.TestCaseUtils;
import com.hotels.heat.core.log.Log;
import com.hotels.heat.core.validations.ArithmeticalValidator;
import com.hotels.heat.core.validations.StringValidator;

import com.jayway.restassured.response.Response;


/**
 * Base utility for operations inside test checks.
 */
public class OperationHandler {

    public static final String OPERATION_JSON_ELEMENT = "operation";
    public static final String FORMAT_OF_TYPE_CHECK_JSON_ELEMENT = "formatOfTypeCheck";

    public static final String JSON_ELEM_DESCRIPTION = "description";
    public static final String JSON_ELEM_EXPECTED_VALUE = "expectedValue";
    public static final String JSON_ELEM_ACTUAL_VALUE = "actualValue";
    public static final String JSON_ELEM_REFERRING_OBJECT = "referringObjectName";


    private ArithmeticalValidator aritmeticalValidator;
    private StringValidator stringValidator;

    private String checkDescription;
    private boolean isBlocking = true;
    private Object responses;
    private DataExtractionSupport dataExtractionSupport;
    private final AssertionHandler assertionHandler;
    private Map fieldsToCheck;

    private Map<Integer, Map<String, String>> retrievedParameters;

    private Log logger = new Log(OperationHandler.class);
    private TestCase tcObject;

    /**
     * Handler for all possible operations (single mode, compare mode, flow mode).
     * @param fieldsToCheck it is the map representing the single check block from the json input file.
     * @param response it is the response retrieved after the request to the service under test
     */
    public OperationHandler(Map fieldsToCheck,
            Object response,
            TestCase tcObject) {
        this.tcObject = tcObject;
        this.retrievedParameters = new HashMap<>();
        this.responses = response;
        this.dataExtractionSupport = new DataExtractionSupport(this.tcObject);
        this.fieldsToCheck = fieldsToCheck;
        this.assertionHandler = new AssertionHandler(this.tcObject);
    }

    /**
     * Executes an operation of a single Check.
     * @return the outcome of the exectution
     */
    public boolean execute() {
        boolean isExecutionOk = true;

        if (!fieldsToCheck.containsKey(JSON_ELEM_ACTUAL_VALUE) || !fieldsToCheck.containsKey(JSON_ELEM_EXPECTED_VALUE)) {
            throw new HeatException(this.getClass(), this.tcObject, "json input format not supported! '{}'", fieldsToCheck.toString());
        }

        if (isComparingBlock()) {
            isExecutionOk &= multipleModeOperationExecution();
        } else {
            isExecutionOk &= singleModeOperationExecution();
        }

        return isExecutionOk;
    }

    private boolean isComparingBlock() {
        return fieldsToCheck.get(JSON_ELEM_ACTUAL_VALUE) instanceof Map
                && ((Map) fieldsToCheck.get(JSON_ELEM_ACTUAL_VALUE)).containsKey(JSON_ELEM_ACTUAL_VALUE)
                && ((Map) fieldsToCheck.get(JSON_ELEM_EXPECTED_VALUE)).containsKey(JSON_ELEM_ACTUAL_VALUE);
    }

    private boolean multipleModeOperationExecution() {
        boolean isExecutionOk = false;

        String operation = loadOperationToExecuteOrDefault(ArithmeticalValidator.MATH_OPERATOR_EQUAL_TO);
        String fieldCheckFormat = loadFieldCheckFormatOrDefault(BasicMultipleChecks.DEFAULT_FORMAT_OF_TYPE_CHECK);
        String firstObj = retrieveObj(JSON_ELEM_ACTUAL_VALUE, fieldsToCheck, (Map<String, Response>) responses);
        String secondObj = retrieveObj(JSON_ELEM_EXPECTED_VALUE, fieldsToCheck, (Map<String, Response>) responses);
        checkDescription = fieldsToCheck.get(JSON_ELEM_DESCRIPTION).toString();
        isExecutionOk = mathOrStringChecks(operation, firstObj, secondObj, fieldCheckFormat);

        return isExecutionOk;
    }

    private String retrieveObj(String objName, Map<String, Object> singleCheckMap, Map<String, Response> mapServiceIdResponse) {
        Map<String, Object> objMapRetrieved = (Map<String, Object>) singleCheckMap.get(objName);
        String strRetrieved = "";
        try {
            if (objMapRetrieved.containsKey(JSON_ELEM_ACTUAL_VALUE)) {
                if (objMapRetrieved.containsKey(JSON_ELEM_REFERRING_OBJECT)) {
                    Response rsp = mapServiceIdResponse.get(objMapRetrieved.get(JSON_ELEM_REFERRING_OBJECT).toString());
                    DataExtractionSupport dataSupport = new DataExtractionSupport(this.tcObject);
                    strRetrieved = dataSupport.process(objMapRetrieved.get(JSON_ELEM_ACTUAL_VALUE), rsp, retrievedParameters);
                } else {
                    if (!objMapRetrieved.get(JSON_ELEM_ACTUAL_VALUE).toString().contains(PlaceholderHandler.PLACEHOLDER_SYMBOL_BEGIN)) {
                        strRetrieved = objMapRetrieved.get(JSON_ELEM_ACTUAL_VALUE).toString();
                    } else {
                        throw new HeatException(this.getClass(), this.tcObject, "Input Json does not contain 'referringObjectName' field");
                    }
                }
            } else {
                throw new HeatException(this.getClass(), this.tcObject, "Input Json does not contain 'actualValue' field");
            }
        } catch (Exception oEx) {
            throw new HeatException(this.getClass(), this.tcObject, oEx);
        }

        return strRetrieved;
    }


    private boolean singleModeOperationExecution() {
        boolean isExecutionOk = false;
        Class<?> aClass = fieldsToCheck.get(JSON_ELEM_EXPECTED_VALUE).getClass();
        if (aClass.equals(String.class) || aClass.equals(HashMap.class)) {
            String expectedValue = dataExtractionSupport.process(fieldsToCheck.get(JSON_ELEM_EXPECTED_VALUE),
                    (Response) responses, retrievedParameters);
            if (PlaceholderHandler.PLACEHOLDER_PRESENT.equals(expectedValue) || PlaceholderHandler.PLACEHOLDER_NOT_PRESENT.equals(expectedValue)) {
                isExecutionOk = checkJsonPathPresence(expectedValue);
            } else {
                Object actualValue = fieldsToCheck.get(JSON_ELEM_ACTUAL_VALUE);
                isExecutionOk = checkGenericFields(actualValue, expectedValue);
            }
        } else if (aClass.equals(ArrayList.class)) {
            isExecutionOk = containsCheck();
        }
        return isExecutionOk;
    }

    private boolean checkGenericFields(Object actualValue, String expectedValue) {
        boolean isExecutionOk;
        String processedActualValue = dataExtractionSupport.process(actualValue, (Response) responses, retrievedParameters);
        String operationToExecute = loadOperationToExecuteOrDefault(StringValidator.STRING_OPERATOR_EQUALS_TO);
        loadCheckDescription();
        String operationDescription = checkDescription + " --> '" + processedActualValue + "' '" + operationToExecute + "' '" + expectedValue + "'";
        logger.debug(this.tcObject, "{}: actualValue '{}' (processed '{}')  / operation '{}' / expectedValue '{}'",
                        checkDescription, actualValue, processedActualValue, operationToExecute, expectedValue);
        try {
            String fieldCheckFormat = loadFieldCheckFormatOrDefault("int");
            isExecutionOk = mathOrStringChecks(operationToExecute, processedActualValue, expectedValue, fieldCheckFormat);
        }  catch (Exception oEx) {
            logger.error(this.tcObject, "Exception: class {}, cause {}, message {}",
                    oEx.getClass(), oEx.getCause(), oEx.getLocalizedMessage());
            operationDescription = "<" + operationDescription + ">";
            throw new HeatException(this.getClass(), this.tcObject, "It is not possible to execute the check {}", operationDescription);
        }
        return isExecutionOk;
    }

    private void loadCheckDescription() {
        if (fieldsToCheck.containsKey(JSON_ELEM_DESCRIPTION)) {
            checkDescription = "{" + (String) fieldsToCheck.get(JSON_ELEM_DESCRIPTION) + "}";
        }
    }

    private boolean mathOrStringChecks(String operationToExecute,
            String processedActualValue,
            String expectedValue,
            String fieldCheckFormat) {
        boolean isExecutionOk;
        if (isItMathematicalCheck(operationToExecute)) {
            aritmeticalValidator = new ArithmeticalValidator(this.tcObject);
            isExecutionOk = aritmeticalValidator.mathematicalChecks(isBlocking, operationToExecute, processedActualValue, expectedValue, checkDescription, fieldCheckFormat);
        } else {
            stringValidator = new StringValidator(this.tcObject);
            isExecutionOk = stringValidator.stringEqualChecks(isBlocking, operationToExecute, processedActualValue, expectedValue,
                    checkDescription + " -->");
        }
        return isExecutionOk;
    }


    private boolean containsCheck() {
        boolean isExecutionOk;
        // TODO: verify if the cast to Response is correct. Maybe it is unuseful to pass it with the cast... maybe it can be simply not passed
        String actualValue = dataExtractionSupport.process(fieldsToCheck.get(JSON_ELEM_ACTUAL_VALUE),
                (Response) responses, retrievedParameters);
        String operationToExecute = loadOperationToExecuteOrDefault(StringValidator.STRING_OPERATOR_CONTAINS);
        // get each element of the array and placeholderProcessString it with the placeholderProcessString handler
        List<String> expectedElementList = (List<String>) fieldsToCheck.get(JSON_ELEM_EXPECTED_VALUE);
        List<String> processedList = new ArrayList<>();
        PlaceholderHandler placeholderHandler = new PlaceholderHandler(this.tcObject);
        placeholderHandler.setResponse((Response) responses);
        // TODO: bisogna passare al placeholder la response!!!!!
        expectedElementList.forEach(listElement -> {
            processedList.add((String) placeholderHandler.placeholderProcessString(listElement));
        });
        loadCheckDescription();
        String assertionString = checkDescription + "-->";
        boolean isContainsCheckOk = true;
        switch (operationToExecute) {
        case StringValidator.STRING_OPERATOR_CONTAINS:
            for (String element : processedList) {
                logger.debug(this.tcObject, "{} actualValue ('{}') has to contain '{}'", assertionString, actualValue, element);
                isContainsCheckOk = isContainsCheckOk && assertionHandler.assertion(false, "assertTrue",
                        "actualValue ('" + actualValue + "') has to contain '" + element + "'", actualValue.contains(element));
            }
            isExecutionOk = assertionHandler.assertion(isBlocking, "assertTrue", assertionString, isContainsCheckOk);
            break;
        case StringValidator.STRING_OPERATOR_NOT_CONTAINS:
            for (String element : processedList) {
                logger.debug(this.tcObject, "{} actualValue ('{}') has not to contain '{}'", assertionString, actualValue, element);
                isContainsCheckOk = isContainsCheckOk && assertionHandler.assertion(false, "assertFalse",
                        "actualValue ('" + actualValue + "') has not to contain '" + element + "'", actualValue.contains(element));
            }
            isExecutionOk = assertionHandler.assertion(isBlocking, "assertTrue", assertionString, isContainsCheckOk);
            break;
        default:
            isExecutionOk = false;
            logger.error(this.tcObject, "Unsupported operation in case of expected values as array");
            throw new HeatException(this.getClass(), this.tcObject, "Unsupported operation in case of expected values as array");
        }

        return isExecutionOk;
    }


    private String loadOperationToExecuteOrDefault(String defaultValue) {
        String operationToExecute = defaultValue;
        if (fieldsToCheck.containsKey(OPERATION_JSON_ELEMENT)) {
            operationToExecute = (String) fieldsToCheck.get(OPERATION_JSON_ELEMENT);
        }
        return operationToExecute;
    }

    private String loadFieldCheckFormatOrDefault(String defaultValue) {
        String fieldFormat = defaultValue;
        if (fieldsToCheck.containsKey(FORMAT_OF_TYPE_CHECK_JSON_ELEMENT)) {
            fieldFormat = fieldsToCheck.get(FORMAT_OF_TYPE_CHECK_JSON_ELEMENT).toString();
        }
        return fieldFormat;
    }


    private boolean checkJsonPathPresence(String expectedValue) {
        boolean isExecutionOk = false;
        String actualValue = fieldsToCheck.get(JSON_ELEM_ACTUAL_VALUE).toString();
        // the check can be executed ONLY if the actual value is a "${path"-style placeholder
        if (actualValue.contains(PlaceholderHandler.PATH_PLACEHOLDER)) {
            TestCaseUtils testCaseUtils = TestSuiteHandler.getInstance().getTestCaseUtils();
            String jsonPathToCheck = testCaseUtils.regexpExtractor(actualValue, PlaceholderHandler.PATH_JSONPATH_REGEXP, 1);
            logger.debug(this.tcObject, "check if the path '{}' is {} in the response",
                        jsonPathToCheck, expectedValue);
            boolean isPathPresent = fieldPresent((Response) responses, jsonPathToCheck);
            if (PlaceholderHandler.PLACEHOLDER_NOT_PRESENT.equals(expectedValue)) {
                isExecutionOk = assertionHandler.assertion(isBlocking, "assertFalse",
                    "json path '" + jsonPathToCheck + "' has not to be present in the response --> ", isPathPresent);
            } else if (PlaceholderHandler.PLACEHOLDER_PRESENT.equals(expectedValue)) {
                isExecutionOk = assertionHandler.assertion(isBlocking, "assertTrue",
                    "json path '" + jsonPathToCheck + "' has to be present in the response -->", isPathPresent);
            }
        } else {
            isExecutionOk = false;
            logger.error(this.tcObject, "the check can be executed ONLY if the actual value is a \"{}path\"-style placeholder", PlaceholderHandler.PLACEHOLDER_SYMBOL_BEGIN);
        }
        return isExecutionOk;
    }


    /**
     * Method to check if a given 'json path' is present in a response.
     * @param response It is the response to parse
     * @param jsonPathToCheck it is the json path to check
     * @return true if the path is present in the response, otherwise false
     */
    private boolean fieldPresent(Response response, String jsonPathToCheck) {
        boolean isFieldPresent = true;
        try {
            response.jsonPath().get(jsonPathToCheck).toString();
            isFieldPresent = true;
        } catch (Exception oEx) {
            isFieldPresent = false;
        }
        return isFieldPresent;
    }

    private boolean isItMathematicalCheck(String operation) {
        return operation.matches("[=|<|>]") || ">=".equals(operation) || "<=".equals(operation);
    }

    public void setOperationBlocking(boolean isBlocking) {
        this.isBlocking = isBlocking;
    }

    public void setFlowOutputParameters(Map<Integer, Map<String, String>> retrievedParameters) {
        this.retrievedParameters = retrievedParameters;
    }

}
