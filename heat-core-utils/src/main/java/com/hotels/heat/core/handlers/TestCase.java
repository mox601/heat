package com.hotels.heat.core.handlers;

public final class TestCase {

    private static TestCase TestCase;
    private String testSuiteName = null;
    private String testCaseIdNumber = null;
    private String testCaseDescription = null;
    private String testStepId = null;
    private boolean isSkippable = false;

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
}
