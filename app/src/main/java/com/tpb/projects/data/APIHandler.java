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

package com.tpb.projects.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.StringRes;

import com.androidnetworking.error.ANError;
import com.tpb.projects.R;
import com.tpb.projects.data.auth.GitHubSession;

import java.util.HashMap;

/**
 * Created by theo on 18/12/16.
 */

public abstract class APIHandler {
    public static final String TAG = APIHandler.class.getSimpleName();

    protected static final String GIT_BASE = "https://api.github.com";
    private static final String ACCEPT_HEADER_KEY = "Accept";
    private static final String ACCEPT_HEADER = "application/vnd.github.v3+json";
    private static final String ORGANIZATIONS_PREVIEW_ACCEPT_HEADER = "application/vnd.github.korra-preview";
    private static final String PROJECTS_PREVIEW_ACCEPT_HEADER = "application/vnd.github.inertia-preview+json";
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String AUTHORIZATION_TOKEN_FORMAT = "token %1$s";
    private static GitHubSession mSession;

    protected static final HashMap<String, String> API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> PROJECTS_PREVIEW_API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS = new HashMap<>();

    static final String SEGMENT_USER = "/user";
    static final String SEGMENT_USERS = "/users";
    static final String SEGMENT_REPOS = "/repos";
    static final String SEGMENT_README = "/readme";
    static final String SEGMENT_COLLABORATORS = "/collaborators";
    static final String SEGMENT_LABELS = "/labels";
    static final String SEGMENT_PROJECTS = "/projects";
    static final String SEGMENT_COLUMNS = "/columns";
    static final String SEGMENT_ISSUES = "/issues";
    static final String SEGMENT_PERMISSION = "/permission";
    static final String SEGMENT_CARDS = "/cards";
    static final String SEGMENT_MOVES = "/moves";
    static final String SEGMENT_COMMENTS = "/comments";
    static final String SEGMENT_EVENTS = "/events";
    static final String SEGMENT_STARRED = "/starred";
    static final String SEGMENT_SUBSCRIPTION = "/subscription";


    protected APIHandler(Context context) {
        if(mSession == null) {
            mSession = GitHubSession.getSession(context);
            initHeaders();
        }
    }

    protected void initHeaders() {
        API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, ACCEPT_HEADER);
        API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY, String.format(AUTHORIZATION_TOKEN_FORMAT, mSession.getAccessToken()));
        PROJECTS_PREVIEW_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, PROJECTS_PREVIEW_ACCEPT_HEADER);
        PROJECTS_PREVIEW_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY, String.format(AUTHORIZATION_TOKEN_FORMAT, mSession.getAccessToken()));
        ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS.put(ACCEPT_HEADER_KEY, ORGANIZATIONS_PREVIEW_ACCEPT_HEADER);
        ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS.put(AUTHORIZATION_HEADER_KEY, String.format(AUTHORIZATION_TOKEN_FORMAT, mSession.getAccessToken()));
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }

    private static final String CONNECTION_ERROR = "connectionError";

    public static final int HTTP_OK_200 = 200; //OK

    public static final String HTTP_REDIRECT_NEW_LOCATION = "Location";
    public static final int HTTP_301_REDIRECTED = 301; //Should redirect through the value in location

    public static final int HTTP_302_TEMPORARY_REDIRECT = 302; //Redirect for this request only
    public static final int HTTP_307_TEMPORARY_REDIRECT = 307; //Same as above

    private static final int HTTP_BAD_REQUEST_400 = 400; //Bad request problems passing JSON

    public static final String KEY_MESSAGE = "message";
    private static final String MESSAGE_BAD_CREDENTIALS = "Bad credentials";
    private static final int HTTP_UNAUTHORIZED_401 = 401; //Login required, account locked, permission error

    private static final String MESSAGE_MAX_LOGIN_ATTEMPTS = "Maximum number of login attempts exceeded. Please try again later.";

    /*
    Unauthenticated requests have a 60/h limit which is unusable for the app
    Authenticated requests have a 5000/h limit
     */
    public static final String KEY_HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String MESSAGE_RATE_LIMIT_START = "API rate limit exceeded";
    private static final String MESSAGE_ABUSE_LIMIT = "You have triggered an abuse detection mechanism and have been temporarily blocked from content creation. Please retry your request again later.";
    private static final int HTTP_FORBIDDEN_403 = 403; //Forbidden server locked or other reasons
    
    private static final int HTTP_NOT_FOUND_404 = 404;

    private static final int HTTP_NOT_ALLOWED_405 = 405; //Not allowed (managed server)

    public static final int HTTP_419 = 419; //This function can only be executed with an CL-account

    public static final String ERROR_MESSAGE_UNPROCESSABLE = "Validation Failed";
    public static final String ERROR_MESSAGE_VALIDATION_MISSING = "missing";
    public static final String ERROR_MESSAGE_VALIDATION_MISSING_FIELD = "missing_field";
    public static final String ERROR_MESSAGE_VALIDATION_INVALID = "invalid";
    public static final String ERROR_MESSAGE_VALIDATION_ALREADY_EXISTS = "already_exists";
    private static final int HTTP_UNPROCESSABLE_422 = 422; // Validation failed

    //600 codes are server codes https://github.com/GleSYS/API/wiki/API-Error-codes#6xx---server

    //700 codes are ip errors https://github.com/GleSYS/API/wiki/API-Error-codes#7xx---ip

    //800 codes are archive codes https://github.com/GleSYS/API/wiki/API-Error-codes#8xx---archive

    //900 domain https://github.com/GleSYS/API/wiki/API-Error-codes#9xx---domain

    //1000 email https://github.com/GleSYS/API/wiki/API-Error-codes#10xx---email

    //1100 livechat https://github.com/GleSYS/API/wiki/API-Error-codes#11xx---livechat

    //1200 invoice https://github.com/GleSYS/API/wiki/API-Error-codes#11xx---livechat

    //1300 glera https://github.com/GleSYS/API/wiki/API-Error-codes#13xx---glera

    //1400 transaction https://github.com/GleSYS/API/wiki/API-Error-codes#14xx---transaction

    //1500 vpn https://github.com/GleSYS/API/wiki/API-Error-codes#15xx---vpn

    public static final  int GIT_LOGIN_FAILED_1601 = 1601; //Login failed

    public static final int GIT_LOGIN_FAILED_1602 = 1602; //Login failed unknown

    public static final int GIT_GOOGLE_AUTHENTICATOR_OTP_REQUIRED_1603 = 1603; //Google auth error

    public static final int GIT_YUBIKEY_1604 = 1604; //Yubikey OTP required

    public static final int GIT_NOT_LOGGED_IN_1605 = 1605; //Not logged in as user

    //1700 invite https://github.com/GleSYS/API/wiki/API-Error-codes#17xx---invite

    //1800 test account https://github.com/GleSYS/API/wiki/API-Error-codes#18xx---test-account

    //1900 network https://github.com/GleSYS/API/wiki/API-Error-codes#19xx---network

    static APIError parseError(ANError error) {
        if(CONNECTION_ERROR.equals(error.getErrorDetail())) return APIError.NO_CONNECTION;
        switch(error.getErrorCode()) {
            case HTTP_BAD_REQUEST_400: return APIError.BAD_REQUEST;
            case HTTP_UNAUTHORIZED_401:
                if(error.getErrorBody() != null) {
                    if(error.getErrorBody().contains(MESSAGE_BAD_CREDENTIALS)) return APIError.BAD_CREDENTIALS;
                    if(error.getErrorBody().contains(MESSAGE_MAX_LOGIN_ATTEMPTS)) return APIError.MAX_LOGIN_ATTEMPTS;
                }
                return APIError.UNAUTHORIZED;
            case HTTP_FORBIDDEN_403:
                if(error.getErrorBody() != null) {
                    if(error.getErrorBody().contains(MESSAGE_RATE_LIMIT_START)) return APIError.RATE_LIMIT;
                    if(error.getErrorBody().contains(MESSAGE_ABUSE_LIMIT)) return APIError.ABUSE_LIMIT;
                }
                return APIError.FORBIDDEN;
            case HTTP_NOT_ALLOWED_405: return APIError.NOT_ALLOWED;
            case HTTP_NOT_FOUND_404: return APIError.NOT_FOUND;
            case HTTP_UNPROCESSABLE_422: return APIError.UNPROCESSABLE;
        }

        return APIError.UNKNOWN;
    }

    public enum APIError {

        NO_CONNECTION(R.string.error_no_connection),
        UNAUTHORIZED(R.string.error_unauthorized),
        FORBIDDEN(R.string.error_forbidden),
        NOT_FOUND(R.string.error_not_found),
        UNKNOWN(R.string.error_unknown),
        RATE_LIMIT(R.string.error_rate_limit),
        ABUSE_LIMIT(R.string.error_abuse_limit),
        MAX_LOGIN_ATTEMPTS(R.string.error_max_login_attempts),
        UNPROCESSABLE(R.string.error_unprocessable),
        BAD_CREDENTIALS(R.string.error_bad_credentials),
        NOT_ALLOWED(R.string.error_not_allowed),
        BAD_REQUEST(R.string.error_bad_request);

        @StringRes
        public final int resId;

        APIError(@StringRes int resId) {
            this.resId = resId;
        }


    }

}
