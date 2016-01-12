package org.worldbank.transport.driver.ActivityTests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.robotium.solo.Solo;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.worldbank.transport.driver.MockLoginUrlBuilder;
import org.worldbank.transport.driver.R;
import org.worldbank.transport.driver.activities.LoginActivity;
import org.worldbank.transport.driver.activities.RecordListActivity;
import org.worldbank.transport.driver.staticmodels.DriverApp;
import org.worldbank.transport.driver.staticmodels.DriverAppContext;
import org.worldbank.transport.driver.staticmodels.DriverUserInfo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Functional tests for the login activity.
 *
 * Created by kathrynkillebrew on 12/17/15.
 */
public class LoginActivityFunctionalTests extends ActivityInstrumentationTestCase2<LoginActivity> {

    private LoginActivity activity;
    private DriverApp app;
    private Solo solo;
    private Instrumentation instrumentation;

    // views
    AutoCompleteTextView usernameField;
    EditText passwordField;
    Button loginButton;
    View progress;
    TextView errorMessage;

    public LoginActivityFunctionalTests() {
        super(LoginActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        instrumentation = getInstrumentation();

        // clear any saved user info on device from shared preferences
        clearSharedPreferences();

        activity = getActivity();

        DriverAppContext driverAppContext = new DriverAppContext((DriverApp) instrumentation
                .getTargetContext().getApplicationContext());
        app = driverAppContext.getDriverApp();

        usernameField = (AutoCompleteTextView) activity.findViewById(R.id.email);
        passwordField = (EditText) activity.findViewById(R.id.password);
        loginButton = (Button) activity.findViewById(R.id.email_sign_in_button);
        progress = activity.findViewById(R.id.login_progress);
        errorMessage = (TextView) activity.findViewById(R.id.error_message);
    }

    private void clearSharedPreferences() {
        Context targetContext = instrumentation.getTargetContext();
        SharedPreferences preferences = targetContext.getSharedPreferences(
                targetContext.getString(R.string.shared_preferences_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    @SmallTest
    public void testIsThisThingOn() {
        assertEquals("Dummy test", 4, 2 + 2);
    }

    @SmallTest
    public void testActivityExists() {
        assertNotNull("Login Activity is null", activity);
    }

    @SmallTest
    public void testViewPlacement() {
        View rootView = activity.findViewById(R.id.login_form);

        ViewAsserts.assertOnScreen(rootView, usernameField);
        ViewAsserts.assertOnScreen(rootView, passwordField);
        ViewAsserts.assertOnScreen(rootView, loginButton);

        ViewAsserts.assertLeftAligned(usernameField, passwordField);
        ViewAsserts.assertRightAligned(usernameField, passwordField);
    }

    @MediumTest
    public void testEmptyUsername() {
        solo = new Solo(instrumentation, activity);
        solo.clearEditText(usernameField);
        solo.typeText(passwordField, "SOMEJUNKHERE");
        solo.clickOnView(loginButton);

        solo.waitForView(progress);

        solo.assertCurrentActivity("User allowed to log in without username", LoginActivity.class);
        solo.waitForView(passwordField);

        assertNotNull("Username error message did not appear", usernameField.getError());
        assertNull("Password error appeared when field is OK", passwordField.getError());

        assertEquals("Username does not have focus on error", true, usernameField.hasFocus());
        assertEquals("Progress indicator showing when login form has error",
                View.GONE, progress.getVisibility());

        solo.finishOpenedActivities();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @MediumTest
    public void testEmptyPassword() {
        solo = new Solo(instrumentation, activity);

        solo.waitForView(passwordField);

        solo.clickOnView(usernameField);
        solo.typeText(usernameField, "SOMEJUNKHERE");
        solo.clickOnView(passwordField);
        solo.clearEditText(passwordField);
        solo.clickOnView(loginButton);

        solo.waitForView(progress);

        solo.assertCurrentActivity("User allowed to log in without password", LoginActivity.class);
        solo.waitForView(passwordField);

        assertNotNull("Password error message did not appear", passwordField.getError());
        assertNull("Username error appeared when field is OK", usernameField.getError());
        assertEquals("Password does not have focus on error", true, passwordField.hasFocus());
        assertEquals("Progress indicator showing when login form has error",
                View.GONE, progress.getVisibility());

        solo.finishOpenedActivities();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @MediumTest
    public void testMockServer() {
        MockWebServer server = new MockWebServer();
        try {
            server.enqueue(new MockResponse().setBody("yo"));
            server.start();

            HttpUrl httpUrl = server.url("/");
            URL url = httpUrl.url();

            URLConnection urlConnection = url.openConnection();
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader ir = new BufferedReader(new InputStreamReader(in));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = ir.readLine()) != null) {
                stringBuilder.append(line);
            }
            ir.close();
            in.close();
            String responseStr = stringBuilder.toString();

            server.shutdown();

            assertEquals("did not get mock server response, yo", "yo", responseStr);

        } catch (IOException e) {
            e.printStackTrace();
            fail("no or bad mock server response");
        }

    }

    @MediumTest
    public void testSuccessfulLogin() {

        // mock server responses
        MockWebServer server = new MockWebServer();

        try {
            // mock server responses by setting the URLs that will be used by LoginTask
            activity.mLoginUrlBuilder = new MockLoginUrlBuilder(server);

            // prepare mock server responses
            MockResponse tokenResponse = new MockResponse()
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody("{\"token\":\"15903f0d0dd44d79b6507f59470b5005\",\"user\":999}");
            server.enqueue(tokenResponse);

            MockResponse userInfoResponse = new MockResponse()
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody("{\"id\":999,\"url\":\"http://driver.example.com/api/users/999/\",\"username\":\"superfoo\",\"email\":\"superfoo@example.com\",\"groups\":[\"admin\"],\"date_joined\":\"2015-12-01T22:56:04.039208Z\",\"is_staff\":false,\"is_superuser\":false}");
            server.enqueue(userInfoResponse);
            server.start();

            // fill out login form
            solo = new Solo(instrumentation, activity);
            solo.typeText(usernameField, "superfoo");
            solo.typeText(passwordField, "somepassword");

            solo.clickOnView(loginButton);

            assertTrue("Main activity did not get launched after login", solo.waitForActivity(RecordListActivity.class));

            //////////////////////////////////
            // check expected requests made
            assertEquals("Expected login task to request user token and info", 2, server.getRequestCount());
            RecordedRequest firstRequest = server.takeRequest();
            RecordedRequest secondRequest = server.takeRequest();

            // paths here are the mocked ones in MockLoginUrlBuilder
            assertEquals("Expected login task to request user token first", "/token", firstRequest.getPath());
            assertEquals("Expected login task to request user info second", "/user", secondRequest.getPath());
            server.shutdown();
            ////////////////////////////////

            // check user info was set
            DriverUserInfo userInfo = app.getUserInfo();

            assertEquals("Username not set correctly", "superfoo", userInfo.username);
            assertEquals("User email not set correctly", "superfoo@example.com", userInfo.email);
            assertEquals("User ID incorrect", 999, userInfo.id);
            assertEquals("User groups not set correctly", 1, userInfo.groups.size());
            assertEquals("User should have write permission", true, userInfo.hasWritePermission());
            assertEquals("User token not set correctly", "15903f0d0dd44d79b6507f59470b5005", userInfo.getUserToken());

            solo.finishOpenedActivities();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // unset user info again for test state
            app.setUserInfo(null);

        } catch (IOException e) {
            e.printStackTrace();
            fail("Login activity test encountered server error");
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Login activity test encountered error checking server requests");
        }
    }

    @MediumTest
    public void testAttemptLoginInsufficientPrivileges() {

        // mock server responses
        MockWebServer server = new MockWebServer();

        try {
            // mock server responses by setting the URLs that will be used by LoginTask
            activity.mLoginUrlBuilder = new MockLoginUrlBuilder(server);

            // prepare mock server responses
            MockResponse tokenResponse = new MockResponse()
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody("{\"token\":\"15903f0d0dd44d79b6507f59470b5005\",\"user\":42}");
            server.enqueue(tokenResponse);

            MockResponse userInfoResponse = new MockResponse()
                    .setHeader("Content-Type", "application/json; charset=UTF-8")
                    .setBody("{\"id\":42,\"url\":\"http://driver.example.com/api/users/42/\",\"username\":\"publicuser\",\"email\":\"publicuser@example.com\",\"groups\":[\"public\"],\"date_joined\":\"2015-12-01T22:56:04.039208Z\",\"is_staff\":false,\"is_superuser\":false}");
            server.enqueue(userInfoResponse);
            server.start();

            // fill out login form
            solo = new Solo(instrumentation, activity);
            solo.typeText(usernameField, "publicuser");
            solo.typeText(passwordField, "somepassword");

            // go!
            solo.clickOnView(loginButton);

            // wait for progress bar to be dismissed
            assertTrue(solo.waitForView(progress));

            solo.assertCurrentActivity("User without write access allowed to log in", LoginActivity.class);
            assertTrue(solo.waitForView(usernameField));

            server.shutdown();

            // check appropriate error message displayed
            assertNotSame("Progress indicator still showing when login form has error",
                    View.VISIBLE, progress.getVisibility());

            Context targetContext = instrumentation.getTargetContext();
            assertEquals("Message about insufficient privileges not displayed",
                    targetContext.getString(R.string.error_user_cannot_write_records), errorMessage.getText());

            // check user info was ~not~ saved
            assertEquals("Username should be empty after failed login", 0, app.getUserInfo().username.length());
            solo.finishOpenedActivities();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail("Login activity test encountered server error");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (activity != null) {
            activity.finish();
        }
    }
}
