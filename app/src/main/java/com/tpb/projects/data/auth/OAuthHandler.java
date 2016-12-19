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
import com.tpb.projects.user.LoginActivity;
import com.tpb.projects.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;


/**
 *
 * @author Thiago Locatelli <thiago.locatelli@gmail.com>
 * @author Lorensius W. L T <lorenz@londatiga.net>
 *
 */
public class OAuthHandler extends APIHandler {
    private GitHubSession mSession;
    private OAuthAuthenticationListener mListener;
    private String mAuthUrl;
    private String mTokenUrl;
    private String mAccessToken;

    /**
     * Callback url, as set in 'Manage OAuth Costumers' page
     * (https://developer.gitHub.com/)
     */

    public static String mCallbackUrl = "";
    private static final String AUTH_URL = "https://gitHub.com/login/oauth/authorize?";
    private static final String TOKEN_URL = "https://gitHub.com/login/oauth/access_token?";
    private static final String SCOPE = "user repo";

    private static final String TAG = OAuthHandler.class.getSimpleName();


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
            public void onComplete(String accessToken) {
                getAccessToken(accessToken);
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
        AndroidNetworking.get(GIT_BASE + "user")
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i(TAG, "onResponse: " + response.toString());
                            final String username = response.getString(Constants.JSON_KEY_LOGIN);
                            final String avatar_url = response.getString(Constants.JSON_JEY_AVATAR);
                            final String name = response.getString(Constants.JSON_KEY_NAME);
                            final String details = //TODO Format string resource
                                    response.getString(Constants.JSON_KEY_PUBLIC_REPO_COUNT) +
                                    " public repos\n" +
                                     "Following: " + response.getString(Constants.JSON_KEY_FOLLOWING) +
                                     "\nFollowers: " + response.getString(Constants.JSON_KEY_FOLLOWERS) +
                                     "\nLocation: " + response.getString(Constants.JSON_KEY_LOCATION);
                            mListener.userLoaded(name, username, details, avatar_url);
                            mSession.storeAccessToken(mAccessToken, response.getInt("id"), username);
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {

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
        return mSession.getUsername();
    }

    public void resetAccessToken() {
        if (mAccessToken != null) {
            mSession.resetAccessToken();
            mAccessToken = null;
        }
    }

    //https://developer.github.com/v3/oauth_authorizations/#check-an-authorization
    public void validateKey(OAuthValidationListener listener) {
        AndroidNetworking.get(GIT_BASE + "rate_limit")
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

        void userLoaded(String name, String id, String details, String imagePath);
    }

    public interface OAuthValidationListener {

        void keyValidated(boolean isValid);

    }
}