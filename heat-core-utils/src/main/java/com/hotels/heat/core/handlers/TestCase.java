package com.hotels.heat.core.handlers;

public final class TestCase {

    private static TestCase TestCase;
    private String testSuiteName = null;
    private String testIdNumber = null;
    private boolean isSkippable = false;

    private TestCase(String suiteName,
                     String id) {
        this.testSuiteName = suiteName;
        this.testIdNumber = id;
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
            TestCase.setTestIdNumber(id);
        }
        return TestCase;
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }

    public void setTestSuiteName(String suiteName) {
        this.testSuiteName = suiteName;
    }

    public String getTestIdNumber() {
        return testIdNumber;
    }

    public void setTestIdNumber(String id) {
        this.testIdNumber = id;
    }

    public String getTestCaseName() {
        String testCaseName = "";
        if (this.testSuiteName != null) {
            testCaseName += this.testSuiteName;
            if (this.testIdNumber != null) {
                testCaseName += "." + this.testIdNumber;
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
}
