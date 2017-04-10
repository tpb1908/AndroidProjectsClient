package com.tpb.github.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.androidnetworking.error.ANError;
import com.tpb.github.R;
import com.tpb.github.data.auth.GitHubSession;

import java.util.HashMap;

/**
 * Created by theo on 18/12/16.
 */

public abstract class APIHandler {
    static final String TAG = APIHandler.class.getSimpleName();

    protected static final String GIT_BASE = "https://api.github.com";
    private static final String ACCEPT_HEADER_KEY = "Accept";
    private static final String ACCEPT_HEADER = "application/vnd.github.v3+json";
    private static final String ORGANIZATIONS_PREVIEW_ACCEPT_HEADER = "application/vnd.github.korra-preview";
    private static final String PROJECTS_PREVIEW_ACCEPT_HEADER = "application/vnd.github.inertia-preview+json";
    private static final String REPO_LICENSE_PREVIEW_ACCEPT_HEADER = "application/vnd.github.drax-preview+json";
    private static final String PAGES_PREVIEW_ACCEPT_HEADER = "application/vnd.github.mister-fantastic-preview+json";
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String AUTHORIZATION_TOKEN_FORMAT = "token %1$s";
    private static GitHubSession mSession;

    protected static final HashMap<String, String> API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> PROJECTS_API_API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> ORGANIZATIONS_API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> LICENSES_API_API_AUTH_HEADERS = new HashMap<>();
    static final HashMap<String, String> PAGES_API_API_AUTH_HEADERS = new HashMap<>();

    protected static final String SEGMENT_USER = "/user";
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
    static final String SEGMENT_MILESTONES = "/milestones";
    static final String SEGMENT_GISTS = "/gists";
    static final String SEGMENT_FOLLOWING = "/following";
    static final String SEGMENT_FOLLOWERS = "/followers";
    static final String SEGMENT_COMMITS = "/commits";
    static final String SEGMENT_NOTIFICATIONS = "/notifications";

    protected APIHandler(Context context) {
        if(mSession == null) {
            mSession = GitHubSession.getSession(context);
            initHeaders();
        }
    }

    protected final void initHeaders() {
        final String accessToken = mSession.getAccessToken();
        API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY,
                String.format(AUTHORIZATION_TOKEN_FORMAT, accessToken)
        );
        API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, ACCEPT_HEADER);

        ORGANIZATIONS_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY,
                String.format(AUTHORIZATION_TOKEN_FORMAT, accessToken)
        );
        ORGANIZATIONS_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, ORGANIZATIONS_PREVIEW_ACCEPT_HEADER);

        PROJECTS_API_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY,
                String.format(AUTHORIZATION_TOKEN_FORMAT, accessToken)
        );
        PROJECTS_API_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, PROJECTS_PREVIEW_ACCEPT_HEADER);

        LICENSES_API_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY,
                String.format(AUTHORIZATION_TOKEN_FORMAT, accessToken)
        );
        LICENSES_API_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, REPO_LICENSE_PREVIEW_ACCEPT_HEADER);

        PAGES_API_API_AUTH_HEADERS.put(AUTHORIZATION_HEADER_KEY,
                String.format(AUTHORIZATION_TOKEN_FORMAT, accessToken)
        );
        PAGES_API_API_AUTH_HEADERS.put(ACCEPT_HEADER_KEY, PAGES_PREVIEW_ACCEPT_HEADER);
    }

    private static final String CONNECTION_ERROR = "connectionError";

    public static final int HTTP_OK_200 = 200; //OK

    public static final String HTTP_REDIRECT_NEW_LOCATION = "Location";
    public static final int HTTP_301_REDIRECTED = 301; //Should redirect through the value in location

    public static final int HTTP_302_TEMPORARY_REDIRECT = 302; //Redirect for this request only
    public static final int HTTP_307_TEMPORARY_REDIRECT = 307; //Same as above

    private static final int HTTP_BAD_REQUEST_400 = 400; //Bad request problems parsing JSON

    public static final String KEY_MESSAGE = "message";
    private static final String MESSAGE_BAD_CREDENTIALS = "Bad credentials";
    private static final int HTTP_UNAUTHORIZED_401 = 401; //Login required, account locked, permission error

    private static final String MESSAGE_MAX_LOGIN_ATTEMPTS = "Maximum number of login attempts exceeded.";

    public static final String KEY_HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private static final String MESSAGE_RATE_LIMIT_START = "API rate limit exceeded";
    private static final String MESSAGE_ABUSE_LIMIT = "You have triggered an abuse detection mechanism";
    private static final int HTTP_FORBIDDEN_403 = 403; //Forbidden server locked or other reasons

    private static final int HTTP_NOT_FOUND_404 = 404;

    private static final int HTTP_NOT_ALLOWED_405 = 405; //Not allowed (managed server)

    private static final int HTTP_409 = 409; //Returned when loading commits for empty repo

    private static final int HTTP_419 = 419; //This function can only be executed with an CL-account

    public static final String ERROR_MESSAGE_UNPROCESSABLE = "Validation Failed";
    public static final String ERROR_MESSAGE_VALIDATION_MISSING = "missing";
    public static final String ERROR_MESSAGE_VALIDATION_MISSING_FIELD = "missing_field";
    public static final String ERROR_MESSAGE_VALIDATION_INVALID = "invalid";
    public static final String ERROR_MESSAGE_VALIDATION_ALREADY_EXISTS = "already_exists";
    private static final String ERROR_MESSAGE_EMPTY_REPOSITORY = "Git Repository is empty.";
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

    public static final int GIT_LOGIN_FAILED_1601 = 1601; //Login failed

    public static final int GIT_LOGIN_FAILED_1602 = 1602; //Login failed unknown

    public static final int GIT_GOOGLE_AUTHENTICATOR_OTP_REQUIRED_1603 = 1603; //Google auth error

    public static final int GIT_YUBIKEY_1604 = 1604; //Yubikey OTP required

    public static final int GIT_NOT_LOGGED_IN_1605 = 1605; //Not logged in as user

    //1700 invite https://github.com/GleSYS/API/wiki/API-Error-codes#17xx---invite

    //1800 test account https://github.com/GleSYS/API/wiki/API-Error-codes#18xx---test-account

    //1900 network https://github.com/GleSYS/API/wiki/API-Error-codes#19xx---network

    static APIError parseError(ANError error) {
        APIError apiError;
        if(CONNECTION_ERROR.equals(error.getErrorDetail())) {
            apiError = APIError.NO_CONNECTION;
        } else {
            switch(error.getErrorCode()) {
                case HTTP_BAD_REQUEST_400:
                    apiError = APIError.BAD_REQUEST;
                    break;
                case HTTP_UNAUTHORIZED_401:
                    apiError = APIError.UNAUTHORIZED;
                    if(error.getErrorBody() != null) {
                        if(error.getErrorBody().contains(MESSAGE_BAD_CREDENTIALS)) {
                            apiError = APIError.BAD_CREDENTIALS;
                        } else if(error.getErrorBody().contains(MESSAGE_MAX_LOGIN_ATTEMPTS)) {
                            apiError = APIError.MAX_LOGIN_ATTEMPTS;
                        }
                    }
                    break;
                case HTTP_FORBIDDEN_403:
                    apiError = APIError.FORBIDDEN;
                    if(error.getErrorBody() != null) {
                        if(error.getErrorBody().contains(MESSAGE_RATE_LIMIT_START)) {
                            apiError = APIError.RATE_LIMIT;
                        } else if(error.getErrorBody().contains(MESSAGE_ABUSE_LIMIT)) {
                            apiError = APIError.ABUSE_LIMIT;
                        }
                    }
                    break;
                case HTTP_NOT_ALLOWED_405:
                    apiError = APIError.NOT_ALLOWED;
                    break;
                case HTTP_NOT_FOUND_404:
                    apiError = APIError.NOT_FOUND;
                    break;
                case HTTP_UNPROCESSABLE_422:
                    apiError = APIError.UNPROCESSABLE;
                    break;
                case HTTP_409:
                    if(error.getErrorBody() != null && error.getErrorBody().contains(
                            ERROR_MESSAGE_EMPTY_REPOSITORY)) {
                        apiError = APIError.EMPTY_REPOSITORY;
                        break;
                    }
                default:
                    apiError = APIError.UNKNOWN;
            }
        }
        apiError.error = error;
        return apiError;
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
        BAD_REQUEST(R.string.error_bad_request),
        EMPTY_REPOSITORY(R.string.error_empty_repository);

        @StringRes
        public final int resId;

        @Nullable ANError error;

        APIError(@StringRes int resId) {
            this.resId = resId;
        }
    }


}
