package com.tpb.github.data.auth;

/**
 * Created by theo on 15/12/16.
 */

import android.content.Context;
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
    private final OAuthAuthenticationListener mListener;
    private final String mAuthUrl;
    private final String mTokenUrl;
    private String mAccessToken;

    private static final String AUTH_URL = "https://gitHub.com/login/oauth/authorize?";
    private static final String TOKEN_URL = "https://gitHub.com/login/oauth/access_token?";
    private static final String SCOPE = "user public_repo repo gist";

    private static final String TOKEN_URL_FORMAT = TOKEN_URL + "client_id=%1$s&client_secret=%2$s&redirect_uri=%3$s";
    private static final String AUTH_URL_FORMAT = AUTH_URL + "client_id=%1$s&scope=%2$s&redirect_uri=%3$s";

    public OAuthHandler(Context context, String clientId, String clientSecret,
                        String callbackUrl,
                        OAuthAuthenticationListener listener) {
        super(context);
        mSession = GitHubSession.getSession(context);
        mTokenUrl = String.format(TOKEN_URL_FORMAT, clientId, clientSecret, callbackUrl);
        mAuthUrl = String.format(AUTH_URL_FORMAT, clientId, SCOPE, callbackUrl);
        mListener = listener;
    }

    public void getAccessToken(final String code) {
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
                                 fetchUser();
                             }

                             @Override
                             public void onError(ANError anError) {
                                 mListener.onFail(anError.getErrorDetail());
                             }
                         });
    }

    private void fetchUser() {
        AndroidNetworking.get(GIT_BASE + "/user")
                         .addHeaders(API_AUTH_HEADERS)
                         .build()
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 mSession.storeUser(response);
                                 mListener.userLoaded(mSession.getUser());
                             }

                             @Override
                             public void onError(ANError anError) {
                                 mListener.onFail(anError.getErrorDetail());
                                 Log.e(TAG, "onError: " + anError.getErrorDetail());
                             }
                         });

    }

    public String getAuthUrl() {
        return mAuthUrl;
    }

    public interface OAuthAuthenticationListener {
        void onSuccess();

        void onFail(String error);

        void userLoaded(User user);

    }
}