package com.tpb.projects.data.auth;

/**
 * Created by theo on 15/12/16.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.tpb.projects.user.LoginActivity;

import org.json.JSONObject;
import org.json.JSONTokener;

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

        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Fetching user info");
                int what = 0;

                try {
                    final URL url = new URL(API_URL + "/user?access_token="
                            + mAccessToken);

                    Log.d(TAG, "Opening URL " + url.toString());
                    final HttpURLConnection urlConnection = (HttpURLConnection) url
                            .openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.connect();
                    String response = streamToString(urlConnection
                            .getInputStream());

                    final JSONObject jsonObj = (JSONObject) new JSONTokener(response)
                            .nextValue();
                    final String id = jsonObj.getString("id");
                    final String login = jsonObj.getString("login");
                    Log.i(TAG, "Got user name: " + login);
                    mSession.storeAccessToken(mAccessToken, id, login);
                } catch (Exception ex) {
                    what = 1;
                    ex.printStackTrace();
                }

                mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
            }
        }.start();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 1) {
                if (msg.what == 0) {
                    fetchUserName();
                } else {
                    mListener.onFail("Failed to get access token");
                }
            } else {
                mListener.onSuccess();
            }
        }
    };

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
        public abstract void onSuccess();

        public abstract void onFail(String error);
    }
}