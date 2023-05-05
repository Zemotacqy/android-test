package com.example.android.testing.espresso.IdlingResourceSample;

import android.util.Log;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class CustomListener extends RunListener {

    String TAG = "CustomListenerLogs";

    @Override
    public void testFailure(Failure failure) throws Exception {
        super.testFailure(failure);
        Log.d(TAG, "Execution of test failed : "+ failure.getMessage());
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        super.testAssumptionFailure(failure);
        Log.d(TAG, "Execution of test failed due to assumption : "+ failure.getMessage());
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        Log.d(TAG, "Execution of test finished : "+ description.toString());
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        super.testIgnored(description);
        Log.d(TAG, "Execution of test ignored : "+ description.toString());
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        super.testRunFinished(result);
        Log.d(TAG, "Execution of test run finished : "+ result.toString());
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        super.testRunStarted(description);
        Log.d(TAG, "Execution of test run started : "+ description.toString());
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
        Log.d(TAG, "Execution of test started : "+ description.toString());
    }

    @Override
    public void testSuiteFinished(Description description) throws Exception {
        super.testSuiteFinished(description);
        Log.d(TAG, "Execution of test suite finished : "+ description.toString());
    }

    @Override
    public void testSuiteStarted(Description description) throws Exception {
        super.testSuiteStarted(description);
        Log.d(TAG, "Execution of test suite started : "+ description.toString());
    }

    public String getTAG() {
        return "CustomListener";
    }
}
