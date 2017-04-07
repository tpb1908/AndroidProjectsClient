package com.tpb.projects.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.github.data.auth.OAuthHandler;
import com.tpb.github.data.models.User;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.common.BaseActivity;
import com.tpb.projects.markdown.Spanner;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Logger;
import com.tpb.projects.util.UI;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.tpb.projects.flow.ProjectsApplication.mAnalytics;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends BaseActivity implements OAuthHandler.OAuthAuthenticationListener {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private boolean mLoginShown = false;

    @BindView(R.id.login_webview) WebView mWebView;
    @BindView(R.id.login_form) CardView mLogin;
    @BindView(R.id.progress_spinner) ProgressBar mSpinner;
    @BindView(R.id.user_details) LinearLayout mUserDetails;

    private Intent mLaunchIntent;

    private static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT
    );

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        final OAuthHandler OAuthHandler = new OAuthHandler(this,
                BuildConfig.GITHUB_CLIENT_ID,
                BuildConfig.GITHUB_CLIENT_SECRET,
                BuildConfig.GITHUB_REDIRECT_URL,
                this
        );

        if(getIntent().hasExtra(Intent.EXTRA_INTENT)) {
            mLaunchIntent = getIntent().getParcelableExtra(Intent.EXTRA_INTENT);
        } else {
            mLaunchIntent = new Intent(LoginActivity.this, UserActivity.class);
        }

        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new OAuthWebViewClient(OAuthHandler));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(OAuthHandler.getAuthUrl());
        mWebView.setLayoutParams(FILL);

        mUserDetails.setVisibility(View.GONE);
        UI.expand(mLogin);
    }

    @Override
    public void onSuccess() {
        mWebView.setVisibility(View.GONE);
        mSpinner.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFail(String error) {

    }

    @Override
    public void userLoaded(User user) {
        mSpinner.setVisibility(View.GONE);
        Spanner.displayUser(mUserDetails, user);
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.TAG_LOGIN, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
        new Handler().postDelayed(() -> {
            CookieSyncManager.createInstance(this);
            final CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            Logger.i(TAG, "userLoaded: Launching: " + mLaunchIntent);
            startActivity(mLaunchIntent);
            overridePendingTransition(R.anim.slide_up, R.anim.none);
            finish();
        }, 1500);
    }

    private void ensureWebViewVisible() {
        if(!mLoginShown) {
            new Handler().postDelayed(() -> {
                mWebView.setVisibility(View.VISIBLE);
                mSpinner.setVisibility(View.GONE);
                mLoginShown = true;
            }, 150);
        }

    }


    private class OAuthWebViewClient extends WebViewClient {
        private final OAuthHandler mAuthHandler;

        OAuthWebViewClient(OAuthHandler handler) {
            mAuthHandler = handler;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !(url.startsWith("https://github.com/login/oauth/authorize") ||
                    url.startsWith("https://github.com/login?") ||
                    url.startsWith("https://github.com/session") ||
                    url.startsWith(BuildConfig.GITHUB_REDIRECT_URL));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if(url.contains("?code=")) {
                final String[] parts = url.split("=");
                mAuthHandler.getAccessToken(parts[1]);
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            ensureWebViewVisible();
            super.onPageFinished(view, url);
        }

    }

}

