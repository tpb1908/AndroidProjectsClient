package com.tpb.projects.data.auth;

/**
 * Created by theo on 15/12/16.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.tpb.projects.user.LoginActivity;
import com.tpb.projects.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 *
 * @author Thiago Locatelli <thiago.locatelli@gmail.com>
 * @author Lorensius W. L T <lorenz@londatiga.net>
 *
 */
public class GitHubApp {
    private GitHubSession mSession;
    private OAuthAuthenticationListener mListener;
    private String mAuthUrl;
    private String mTokenUrl;
    private String mAccessToken;
    private Handler mHandler;


    /**
     * Callback url, as set in 'Manage OAuth Costumers' page
     * (https://developer.gitHub.com/)
     */

    public static String mCallbackUrl = "";
    private static final String AUTH_URL = "https://gitHub.com/login/oauth/authorize?";
    private static final String TOKEN_URL = "https://gitHub.com/login/oauth/access_token?";
    private static final String API_URL = "https://api.gitHub.com";
    private static final String SCOPE = "user repo";

    private static final String TAG = "GitHubAPI";


    public GitHubApp(Context context, String clientId, String clientSecret,
                     String callbackUrl) {
        mSession = new GitHubSession(context);
        mAccessToken = mSession.getAccessToken();
        mCallbackUrl = callbackUrl;
        mTokenUrl = TOKEN_URL + "client_id=" + clientId + "&client_secret="
                + clientSecret + "&redirect_uri=" + mCallbackUrl;
        mAuthUrl = AUTH_URL + "client_id=" + clientId + "&scope=" + SCOPE
                + "&redirect_uri=" + mCallbackUrl;
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.arg1 == 1) {
                    if(msg.what == 0) {
                        fetchUserName();
                    } else {
                        mListener.onFail("Failed to get access token");
                    }
                } else {
                    mListener.onSuccess();
                }
            }
        };
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
        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Getting access token");
                int what = 0;

                try {
                    final URL url = new URL(mTokenUrl + "&code=" + code);
                    Log.i(TAG, "Opening URL " + url.toString());
                    final HttpURLConnection urlConnection = (HttpURLConnection) url
                            .openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.connect();
                    final String response = streamToString(urlConnection
                            .getInputStream());
                    Log.i(TAG, "response " + response);
                    mAccessToken = response.substring(
                            response.indexOf("access_token=") + 13,
                            response.indexOf("&token_type"));
                    mSession.storeAccessToken(mAccessToken);
                    Log.i(TAG, "Got access token: " + mAccessToken);
                } catch (Exception ex) {
                    what = 1;
                    ex.printStackTrace();
                }

                mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
            }
        }.start();
    }

    private void fetchUserName() {
        AsyncTask.execute(() -> {
            Log.i(TAG, "Fetching user info");
            int what = 0;

            AndroidNetworking.get(API_URL + "/user?access_token=" + mAccessToken)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
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
                            } catch(JSONException jse) {
                                Log.e(TAG, "onResponse: ", jse);
                            }
                        }

                        @Override
                        public void onError(ANError anError) {

                        }
                    });

            mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
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


    private String streamToString(InputStream is) throws IOException {
        String str = "";

        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is));

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();
            } finally {
                is.close();
            }

            str = sb.toString();
        }

        return str;
    }

    public void resetAccessToken() {
        if (mAccessToken != null) {
            mSession.resetAccessToken();
            mAccessToken = null;
        }
    }

    public interface OAuthAuthenticationListener {
        void onSuccess();

        void onFail(String error);

        void userLoaded(String name, String id, String details, String imagePath);
    }
}