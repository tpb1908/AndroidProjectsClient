package com.tpb.github.data.auth;

/**
 * Created by theo on 15/12/16.
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.models.User;

import org.json.JSONObject;

public class OAuthHandler extends APIHandler {
    private static final String TAG = OAuthHandler.class.getSimpleName();

    private final GitHubSession mSession;
    private OAuthAuthenticationListener mListener;
    private final String mAuthUrl;
    private final String mTokenUrl;
    private String mAccessToken;

    private static String mCallbackUrl = "";
    private static final String AUTH_URL = "https://gitHub.com/login/oauth/authorize?";
    private static final String TOKEN_URL = "https://gitHub.com/login/oauth/access_token?";
    private static final String SCOPE = "user public_repo repo gist";
    private static final String RATE_LIMIT = "/rate_limit";

    private static final String TOKEN_URL_FORMAT = TOKEN_URL + "client_id=%1$s&client_secret=%2$s&redirect_uri=%3$s";
    private static final String AUTH_URL_FORMAT = AUTH_URL + "client_id=%1$s&scope=%2$s&redirect_uri=%3$s";

    public OAuthHandler(Context context, String clientId, String clientSecret,
                        String callbackUrl) {
        super(context);
        mSession = GitHubSession.getSession(context);
        mAccessToken = mSession.getAccessToken();
        mCallbackUrl = callbackUrl;
        mTokenUrl = String.format(TOKEN_URL_FORMAT, clientId, clientSecret, mCallbackUrl);
        mAuthUrl = String.format(AUTH_URL_FORMAT, clientId, SCOPE, mCallbackUrl);
    }

    public interface OAuthLoginListener {

        void onCodeCollected(String code);

        void onError(String error);
    }

    public OAuthLoginListener getListener() {
        return new OAuthLoginListener() {
            @Override
            public void onCodeCollected(String code) {
                getAccessToken(code);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "onError: " + error);
            }
        };
    }

    private void getAccessToken(final String code) {
        AndroidNetworking.get(mTokenUrl + "&code=" + code)
                         .build()
                         .getAsString(new StringRequestListener() {
                             @Override
                             public void onResponse(String response) {
                                 mAccessToken = response.substring(
                                         response.indexOf("access_token=") + 13,
                                         response.indexOf("&scope")
                                 );
                                 mSession.storeAccessToken(mAccessToken);
                                 initHeaders();
                                 mListener.onSuccess();
                                 fetchUserName();
                             }

                             @Override
                             public void onError(ANError anError) {
                                 mListener.onFail(anError.getErrorDetail());
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
                                 mSession.storeUser(response);
                                 final User user = mSession.getUser();
                                 mSession.storeCredentials(mAccessToken, user.getId(),
                                         user.getLogin()
                                 );
                                 mListener.userLoaded(user);
                             }

                             @Override
                             public void onError(ANError anError) {
                                 mListener.onFail(anError.getErrorDetail());
                                 Log.e(TAG, "onError: " + anError.getErrorDetail());
                             }
                         });

    }

    public boolean hasAccessToken() {
        Log.i(TAG, "\n\n\n\n\nACcess token " + mAccessToken);
        return mAccessToken != null;
    }

    public void setListener(OAuthAuthenticationListener listener) {
        mListener = listener;
    }

    public String getAuthUrl() {
        return mAuthUrl;
    }

    public void resetAccessToken() {
        if(mAccessToken != null) {
            mSession.resetAccessToken();
            mAccessToken = null;
        }
    }

    //https://developer.github.com/v3/oauth_authorizations/#check-an-authorization
    public static void validateKey(@NonNull final OAuthValidationListener listener) {
        AndroidNetworking.get(GIT_BASE + RATE_LIMIT)
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 listener.keyValidated(true);
                             }

                             @Override
                             public void onError(ANError anError) {
                                 listener.keyValidated(false);
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