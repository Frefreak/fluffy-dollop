package io.cliper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

//ljt
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.*;

import java.util.Scanner;
/**
 * A login screen that offers login via email/password.
 *
 */





public class LoginActivity extends AppCompatActivity/* implements LoaderCallbacks<Cursor> */{

    /**
     * Id to identity READ_CONTACTS permission request.
     */
   // private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    //private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    //ljt
    private final WebSocketConnection mConnection = new WebSocketConnection();
    static String globaltoken ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.cliper.R.layout.activity_login);
        setupActionBar();
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(io.cliper.R.id.username);
        //populateAutoComplete();

        mPasswordView = (EditText) findViewById(io.cliper.R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == io.cliper.R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(io.cliper.R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(io.cliper.R.id.login_form);
        mProgressView = findViewById(io.cliper.R.id.login_progress);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private void writefile(String fileinput){
        FileWriter fw =null;
        try{
            File f = new File (Constant.tokenFileAbsPath);
            fw = new FileWriter(f,true);
        }catch(IOException e){
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw);
        pw.println(fileinput);
        pw.flush();;
        try{
            fw.flush();
            pw.close();
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    /**
     * Attempts to sign in or register a account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        mEmailView.setError(null);
        mPasswordView.setError(null);


        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();
        FileWriter fw = null;
        boolean cancel = false;
        View focusView = null;





        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(io.cliper.R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(io.cliper.R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isUsernameValid(email)) {
            mEmailView.setError(getString(io.cliper.R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        try {
            mConnection.connect(Constant.loginUrl, new WebSocketHandler() {
                @Override
                public void onOpen() {
                    JSONObject js = new JSONObject();
                    try {
                        js.put("username", email);
                        js.put("password", password);
                        js.put("deviceName", "Meizu");
                    } catch (JSONException e) {
                        Toast.makeText(getApplication(), "Errors in sending loginmsg.", Toast.LENGTH_LONG).show();
                    }
                    mConnection.sendTextMessage(js.toString());
                }

                @Override
                public void onTextMessage(String payload) {
                    JSONObject b = new JSONObject();

                    try {
                        JSONObject a = new JSONObject(payload);
                        String msg = a.getString("msg");

                        String temptoken = a.getString("token");
                        int code = a.getInt("code");
                        //These code are for protecting original token form covering by mistake login.
                        //And if login success, a new sync service basic on new token will begin.
                        if (temptoken  == null || temptoken.equals("")) {
                            Toast.makeText(getApplication(), "login failed" + " " + msg + " " + code, Toast.LENGTH_LONG).show();
                            showProgress(false);
                        }else {
                            try {
                                globaltoken = temptoken;
                                b.put("token", globaltoken);
                                //writefile(globaltoken);
                                PrintWriter writer = new PrintWriter(Constant.tokenFileAbsPath);
                                writer.println(globaltoken+"\n");
                                writer.close();
                                Toast.makeText(getApplication(), "login successed" + " " + msg + " " + code, Toast.LENGTH_LONG).show();
                                showProgress(false);
                                Intent returnhome = new Intent(getApplicationContext(), MainActivity.class);
                                returnhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(returnhome);
                            } catch (FileNotFoundException e) {
                                Toast.makeText(getApplication(), "Errors in receiving msg", Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getApplication(), "Errors in building receiving connnection", Toast.LENGTH_LONG).show();}
                    mConnection.sendTextMessage(b.toString());
                }

                @Override
                public void onClose(int code, String reason) {
                    // Toast.makeText(getApplication(), "Connection lost", Toast.LENGTH_LONG).show();
                }
            });
        }
        catch (WebSocketException e) {
            Toast.makeText(getApplication(), "Errors in building connnection", Toast.LENGTH_LONG).show();
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            //mAuthTask = new UserLoginTask(email, password);
            //mAuthTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return username.length() >= 3;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() >= 3;
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


}

