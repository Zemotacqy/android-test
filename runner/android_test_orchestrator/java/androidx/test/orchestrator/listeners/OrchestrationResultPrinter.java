/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.test.orchestrator.listeners;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.orchestrator.junit.ParcelableDescription;
import androidx.test.orchestrator.junit.ParcelableFailure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A line by line reimplementation of {@link
 * androidx.test.internal.runner.listener.InstrumentationResultPrinter}
 *
 * <p>{@link androidx.test.orchestrator.AndroidTestOrchestrator} needs to mirror the output
 * of a non-orchestrated AndroidJUnitRunner multi-test run, and thus requires a result printer.
 * InstrumentationResultPrinter cannot be reused because it extends from the JUnit RunListener,
 * which passes in JUnit specific objects which {@link
 * androidx.test.orchestrator.AndroidTestOrchestrator} cannot access.
 *
 * <p>TODO(b/35394729): Refactor expertise from this and InstrumentationResultPrinter to one place.
 */
public class OrchestrationResultPrinter extends OrchestrationRunListener {

  private static final String LOG_TAG = "OdoInstrResultPrinter";
  private static final String DTAG = "OrchestrationResultPrinter";

  /**
   * This value, if stored with key {@link android.app.Instrumentation#REPORT_KEY_IDENTIFIER},
   * identifies AndroidJUnitRunner as the source of the report. This is sent with all status
   * messages.
   */
  public static final String REPORT_VALUE_ID = "AndroidJUnitRunner";
  /**
   * If included in the status or final bundle sent to an IInstrumentationWatcher, this key
   * identifies the total number of tests that are being run. This is sent with all status messages.
   */
  public static final String REPORT_KEY_NUM_TOTAL = "numtests";
  /**
   * If included in the status or final bundle sent to an IInstrumentationWatcher, this key
   * identifies the sequence number of the current test. This is sent with any status message
   * describing a specific test being started or completed.
   */
  public static final String REPORT_KEY_NUM_CURRENT = "current";
  /**
   * If included in the status or final bundle sent to an IInstrumentationWatcher, this key
   * identifies the name of the current test class. This is sent with any status message describing
   * a specific test being started or completed.
   */
  public static final String REPORT_KEY_NAME_CLASS = "class";
  /**
   * If included in the status or final bundle sent to an IInstrumentationWatcher, this key
   * identifies the name of the current test. This is sent with any status message describing a
   * specific test being started or completed.
   */
  public static final String REPORT_KEY_NAME_TEST = "test";

  /** The test is starting. */
  public static final int REPORT_VALUE_RESULT_START = 1;
  /** The test completed successfully. */
  public static final int REPORT_VALUE_RESULT_OK = 0;
  /**
   * The test completed with an error.
   *
   * @deprecated not supported in JUnit4, use REPORT_VALUE_RESULT_FAILURE instead
   */
  @Deprecated public static final int REPORT_VALUE_RESULT_ERROR = -1;
  /** The test completed with a failure. */
  public static final int REPORT_VALUE_RESULT_FAILURE = -2;
  /** The test was ignored. */
  public static final int REPORT_VALUE_RESULT_IGNORED = -3;
  /** The test completed with an assumption failure. */
  public static final int REPORT_VALUE_RESULT_ASSUMPTION_FAILURE = -4;

  /**
   * If included in the status bundle sent to an IInstrumentationWatcher, this key identifies a
   * stack trace describing an error or failure. This is sent with any status message describing a
   * specific test being completed.
   */
  public static final String REPORT_KEY_STACK = "stack";

  private final Bundle resultTemplate;
  private Bundle testResult;
  int testNum = 0;
  int testResultCode = -999;
  String testClass = null;
  private ParcelableDescription description;

  public long getThreadTime() {
    return SystemClock.currentThreadTimeMillis();
  }

  public long getSysTime() {
    return System.currentTimeMillis();
  }

  public OrchestrationResultPrinter() {
    resultTemplate = new Bundle();
    testResult = new Bundle(resultTemplate);
  }

  @Override
  public void orchestrationRunStarted(int testCount) {
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"runStarted\", \"timestamp\": \""+ System.currentTimeMillis() + "\"}");
    resultTemplate.putString(Instrumentation.REPORT_KEY_IDENTIFIER, REPORT_VALUE_ID);
    resultTemplate.putInt(REPORT_KEY_NUM_TOTAL, testCount);
  }

  /** send a status for the start of a each test, so long tests can be seen as "running" */
  @Override
  public void testStarted(ParcelableDescription description) {
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"testStarted\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description.getClassName() + "\", \"methodName\": \"" + description.getMethodName() +"\"}");
    this.description = description; // cache ParcelableDescription in case of a crash
    String testClass = description.getClassName();
    String testName = description.getMethodName();
    testResult = new Bundle(resultTemplate);
    testResult.putString(REPORT_KEY_NAME_CLASS, testClass);
    testResult.putString(REPORT_KEY_NAME_TEST, testName);
    testResult.putInt(REPORT_KEY_NUM_CURRENT, ++testNum);
    // pretty printing
    if (testClass != null && !testClass.equals(this.testClass)) {
      testResult.putString(
          Instrumentation.REPORT_KEY_STREAMRESULT, String.format("\n%s:", testClass));
      this.testClass = testClass;
    } else {
      testResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "");
    }

    sendStatus(REPORT_VALUE_RESULT_START, testResult);
    testResultCode = REPORT_VALUE_RESULT_OK;
  }

  @Override
  public void testFinished(ParcelableDescription description) {
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"testFinished\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description.getClassName() + "\", \"methodName\": \"" + description.getMethodName() +"\"}");
    if (testResultCode == REPORT_VALUE_RESULT_OK) {
      testResult.putString(Instrumentation.REPORT_KEY_STREAMRESULT, ".");
    }
    sendStatus(testResultCode, testResult);
  }

  @Override
  public void testFailure(ParcelableFailure failure) {
    ParcelableDescription description1 = failure.getDescription();
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"testFailure\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description1.getClassName() + "\", \"methodName\": \"" + description1.getMethodName() +"\", \"trace\": \"" + failure.getTrace() + "\"}");
    testResultCode = REPORT_VALUE_RESULT_FAILURE;
    reportFailure(failure);
  }

  @Override
  public void testAssumptionFailure(ParcelableFailure failure) {
    ParcelableDescription description1 = failure.getDescription();
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"testAssumptionFailure\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description1.getClassName() + "\", \"methodName\": \"" + description1.getMethodName() +"\", \"trace\": \"" + failure.getTrace() + "\"}");
    testResultCode = REPORT_VALUE_RESULT_ASSUMPTION_FAILURE;
    testResult.putString(REPORT_KEY_STACK, failure.getTrace());
  }

  private void reportFailure(ParcelableFailure failure) {
    ParcelableDescription description1 = failure.getDescription();
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"reportFailure\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description1.getClassName() + "\", \"methodName\": \"" + description1.getMethodName() +"\", \"trace\": \"" + failure.getTrace() + "\"}");
    testResult.putString(REPORT_KEY_STACK, failure.getTrace());
    // pretty printing
    testResult.putString(
        Instrumentation.REPORT_KEY_STREAMRESULT,
        String.format(
            "\nError in %s:\n%s", failure.getDescription().getDisplayName(), failure.getTrace()));
  }

  @Override
  public void testIgnored(ParcelableDescription description) {
    HTTPTask httpTask = new HTTPTask();
    makeRequest("{\"event\": \"testIgnored\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"className\": \"" + description.getClassName() + "\", \"methodName\": \"" + description.getMethodName() +"\"}");
    testStarted(description);
    testResultCode = REPORT_VALUE_RESULT_IGNORED;
    testFinished(description);
  }

  /**
   * Produce a more meaningful crash report including stack trace and report it back to
   * Instrumentation results.
   */
  public void reportProcessCrash(Throwable t) {
    Log.i(DTAG, "reportProcessCrash");
    try {
      testResultCode = REPORT_VALUE_RESULT_FAILURE;
      ParcelableFailure failure = new ParcelableFailure(description, t);
      testResult.putString(REPORT_KEY_STACK, failure.getTrace());
      // pretty printing
      testResult.putString(
          Instrumentation.REPORT_KEY_STREAMRESULT,
          String.format(
              "\nProcess crashed while executing %s:\n%s",
              description.getDisplayName(), failure.getTrace()));
      testFinished(description);
    } catch (Exception e) {
      if (null == description) {
        Log.e(LOG_TAG, "Failed to initialize test before process crash");
      } else {
        Log.e(
            LOG_TAG,
            "Failed to mark test "
                + description.getDisplayName()
                + " as finished after process crash");
      }
    }
  }

  /** Convenience method for {@link #getInstrumentation()#sendStatus()} */
  public void sendStatus(int code, Bundle bundle) {
    getInstrumentation().sendStatus(code, bundle);
  }

  public void orchestrationRunFinished(
      PrintStream streamResult, OrchestrationResult orchestrationResults) {
    makeRequest("{\"event\": \"orchestrationRunFinished\", \"timestamp\": \""+ System.currentTimeMillis() + "\", \"runTime\": \"" + orchestrationResults.getRunTime() + "\", \"runCount\": \"" + orchestrationResults.getRunCount() +"\", \"expectedCount\": \"" + orchestrationResults.getExpectedCount() + "\", \"FailureCount\": \"" + orchestrationResults.getFailureCount() + "\"}");
    // reuse TextListener to display a summary of the run
    new TextListener(streamResult).testRunFinished(orchestrationResults);
  }

  public String makeRequest(String testEvent) {
    String response;
    Log.d("DEVICE_MACHINE_BRIDGE", "Starting HTTP Req");
    try {
      URL url = new URL("https://868d-110-226-182-90.ngrok-free.app");
      Thread.sleep(10000);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setDoInput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      OutputStream os = conn.getOutputStream();
      os.write(testEvent.getBytes("UTF-8"));
      os.flush();
      os.close();

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;x
        StringBuilder responseBuilder = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
          responseBuilder.append(inputLine);
        }
        in.close();
        response = responseBuilder.toString();
      } else {
        response = "Error: " + responseCode;
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = "Error: " + e.getMessage();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    Log.d("DEVICE_MACHINE_BRIDGE", "Ending HTTP Req");
    return response;
  }
}
