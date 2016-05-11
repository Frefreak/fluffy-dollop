package io.cliper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


/**
 * A regist screen that offers login via email/password.
 * Doing registe activity. Some codes are generated automatly and their function may be confused.
 * All msg transported in Json format and the returned token will be decoded here and save in to file.
 *--ljt
 */
public class RegisterActivity extends AppCompatActivity /*implements LoaderCallbacks<Cursor>*/ {



    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    //ljt
    final String loginid = "ws://104.207.144.233:4564/register";
    private final WebSocketConnection mConnection = new WebSocketConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.cliper.R.layout.activity_register);
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
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     * In developing state, msg will be sent out using simple Toast.
     */

    private void attemptLogin() {


        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

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
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(io.cliper.R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;

        }
        if (cancel) {
        /*
        * Establishing connection and get msg form server.
        * To see transport formation, click:https://github.com/Frefreak/fluffy-dollop/blob/master/server/spec.md
        * The "deciceName" may be used in further development,but it is useless now.
        * */
            focusView.requestFocus();
        }
        else {
            try {
                mConnection.connect(loginid, new WebSocketHandler() {
                    @Override
                    public void onOpen() {
                        JSONObject js = new JSONObject();
                        try {
                            js.put("username", email);
                            js.put("password", password);
                            js.put("deviceName", "Meizu");
                        } catch (JSONException e) {
                            Toast.makeText(getApplication(), "onOpenError", Toast.LENGTH_LONG).show();
                        }
                        mConnection.sendTextMessage(js.toString());
                    }

                    @Override
                    public void onTextMessage(String payload) {
                        try {
                            JSONObject a = new JSONObject(payload);
                            int registcode = a.getInt("code");
                            String registmsg = a.getString("msg");

                            if (registcode != 200) {
                                Toast.makeText(getApplication(), "registe failed" + " " + registmsg + " " + registcode, Toast.LENGTH_LONG).show();
                                showProgress(false);
                            } else {
                                Toast.makeText(getApplication(), "registe successed, please login" + " " + registcode, Toast.LENGTH_LONG).show();
                                showProgress(false);
                                Intent returnhome = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(returnhome);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplication(), "onTextMessageError", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onClose(int code, String reason) {;}
                });
            } catch (WebSocketException e) {
                Toast.makeText(getApplication(), "WebSocketError", Toast.LENGTH_LONG).show();
            }
        }
    }



    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.length() >= 3;
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

