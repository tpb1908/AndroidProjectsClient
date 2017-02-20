/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.data.auth;

/**
 * Created by theo on 15/12/16.
 */

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.models.User;
import com.tpb.projects.login.LoginActivity;

import org.json.JSONObject;


/**
 * @author Thiago Locatelli <thiago.locatelli@gmail.com>
 * @author Lorensius W. L T <lorenz@londatiga.net>
 */
public class OAuthHandler extends APIHandler {
    private static final String TAG = OAuthHandler.class.getSimpleName();

    private final GitHubSession mSession;
    private OAuthAuthenticationListener mListener;
    private final String mAuthUrl;
    private final String mTokenUrl;
    private String mAccessToken;

    /**
     * Callback url, as set in 'Manage OAuth Costumers' page
     * (https://developer.gitHub.com/)
     */

    public static String mCallbackUrl = "";
    private static final String AUTH_URL = "https://gitHub.com/login/oauth/authorize?";
    private static final String TOKEN_URL = "https://gitHub.com/login/oauth/access_token?";
    private static final String SCOPE = "user repo";
    private static final String RATE_LIMIT = "/rate_limit";


    public OAuthHandler(Context context, String clientId, String clientSecret,
                        String callbackUrl) {
        super(context);
        mSession = GitHubSession.getSession(context);
        mAccessToken = mSession.getAccessToken();
        mCallbackUrl = callbackUrl;
        mTokenUrl = TOKEN_URL + "client_id=" + clientId + "&client_secret="
                + clientSecret + "&redirect_uri=" + mCallbackUrl;
        mAuthUrl = AUTH_URL + "client_id=" + clientId + "&scope=" + SCOPE
                + "&redirect_uri=" + mCallbackUrl;
    }

    public LoginActivity.OAuthLoginListener getListener() {
        return new LoginActivity.OAuthLoginListener() {
            @Override
            public void onCodeCollected(String code) {
                getAccessToken(code);
            }

            @Override
            public void onError(String error) {
                Log.i(TAG, "onError: " + error);
            }
        };
    }

    public String getAuthUrl() {
        return mAuthUrl;
    }

    private void getAccessToken(final String code) {
        AndroidNetworking.get(mTokenUrl + "&code=" + code)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "onResponse: AccessToken: " + response);
                        mAccessToken = response.substring(
                                response.indexOf("access_token=") + 13,
                                response.indexOf("&scope"));
                        mSession.storeAccessToken(mAccessToken);
                        initHeaders();
                        mListener.onSuccess();
                        fetchUserName();
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    private void fetchUserName() {
        AndroidNetworking.get(GIT_BASE + "/user")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse: " + response.toString());
                        final User user = User.parse(response);
                        mSession.storeCredentials(mAccessToken, user.getId(), user.getLogin());
                        mListener.userLoaded(user);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i(TAG, "onError: " + anError.getErrorDetail());
                    }
                });

    }

    public boolean hasAccessToken() {
        return mAccessToken != null;
    }

    public void setListener(OAuthAuthenticationListener listener) {
        mListener = listener;
    }

    public String getUserName() {
        return mSession.getUserLogin();
    }

    public void resetAccessToken() {
        if(mAccessToken != null) {
            mSession.resetAccessToken();
            mAccessToken = null;
        }
    }

    //https://developer.github.com/v3/oauth_authorizations/#check-an-authorization
    public void validateKey(OAuthValidationListener listener) {
        AndroidNetworking.get(GIT_BASE + RATE_LIMIT)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(listener != null) listener.keyValidated(true);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.keyValidated(false);
                        Log.i(TAG, "onError: " + anError.getErrorBody());
                    }
                });
    }

    public interface OAuthAuthenticationListener {
        void onSuccess();

        void onFail(String error);

        void userLoaded(User user);

    }

    public interface OAuthValidationListener {

        void keyValidated(boolean isValid);

    }
}