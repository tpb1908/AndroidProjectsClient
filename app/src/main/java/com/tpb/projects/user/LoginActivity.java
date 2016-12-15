package com.tpb.projects.user;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import com.androidnetworking.AndroidNetworking;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.util.Lists;
import com.tpb.projects.R;
import com.tpb.projects.data.api.AsyncResourceLoader;
import com.tpb.projects.data.api.GitHub;
import com.tpb.projects.data.api.GitRequestInitializer;
import com.tpb.projects.data.api.OAuth;
import com.tpb.projects.data.models.Error;
import com.tpb.projects.data.models.Repos;
import com.tpb.projects.util.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    @BindView(R.id.email) TextInputEditText mEmailView;
    @BindView(R.id.password) TextInputEditText mPasswordView;
    @BindView(R.id.login_progress) View mProgressView;
    @BindView(R.id.email_login_form) View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        AndroidNetworking.initialize(this);
        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if(id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        final Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(view -> attemptLogin());
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if(mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String login = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if(!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid login .
        if(TextUtils.isEmpty(login)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if(cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(login, password);
            mAuthTask.execute((Void) null);
        }
    }


    private boolean isPasswordValid(String password) {
        return password.length() >= 7; //Min password length
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {

        final int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

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

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch(InterruptedException e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if(success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public static class GitHubLoader extends AsyncResourceLoader<Repos> {

        private final OAuth oauth;
        private GitHub.User.ListReposRequest request;
        private final int page;

        public GitHubLoader(FragmentActivity activity, int since) {
            super(activity);
            this.page = since;
            this.oauth = OAuth.newInstance(activity.getApplicationContext(),
                    activity.getSupportFragmentManager(),
                    new ClientParametersAuthentication(Constants.CLIENT_ID,
                            Constants.CLIENT_SECRET),
                    Constants.AUTH_SERVER_URL,
                    Constants.TOKEN_SERVER_URL,
                    Constants.REDIRECT_URL,
                    Lists.newArrayList());
        }

        public boolean isLoadMoreRequest() {
            return page != 0;
        }

        @Override
        public Repos loadResourceInBackground() throws Exception {
            Credential credential = oauth.authorizeExplicitly("github").getResult();

            GitHub github =
                    new GitHub.Builder(OAuth.HTTP_TRANSPORT, OAuth.JSON_FACTORY, credential)
                            .setApplicationName(getContext().getString(R.string.app_name))
                            .setGitHubRequestInitializer(new GitRequestInitializer(credential))
                            .build();
            request = github.user().repos();
            if (isLoadMoreRequest()) {
                request.setPage(page);
            }
            return request.execute();
        }

        
        @Override
        public void updateErrorStateIfApplicable(AsyncResourceLoader.Result<Repos> result) {
            Repos data = result.data;
            result.success = request.getLastStatusCode() == HttpStatusCodes.STATUS_CODE_OK;
            if (result.success) {
                result.errorMessage = null;
            } else {
                result.errorMessage = data.getMessage();
                if (data.getErrors() != null && data.getErrors().size() > 0) {
                    Error error = data.getErrors().get(0);
                    result.errorMessage += (error.getCode());
                }
            }
        }

    }



}

