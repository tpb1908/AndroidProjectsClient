package com.tpb.projects.util;

import com.tpb.projects.BuildConfig;

/**
 * Created by theo on 14/12/16.
 */

public class Constants {
    public static final String CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID;
    public static final String CLIENT_SECRET = BuildConfig.GITHUB_CLIENT_SECRET;
    public static final String GIT_API_ROOT_URL = "https://api.github.com/";
    public static final String AUTH_SERVER_URL = "https://github.com/login/oauth/authorize";
    public static final String TOKEN_SERVER_URL = "https://github.com/login/oauth/access_token";
    public static final String REDIRECT_URL = "https://github.com/tpb1908/AndroidProjectsClient";
    public static final  String CREDENTIALS_STORE_PREF_FILE = "oauth";

    public static final String JSON_KEY_LOGIN = "login";
    public static final String JSON_KEY_ID = "id";
    public static final String JSON_JEY_AVATAR = "avatar_url";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_PUBLIC_REPO_COUNT = "public_repos";
    public static final String JSON_KEY_LOCATION = "location";
    public static final String JSON_KEY_FOLLOWERS ="followers";
    public static final String JSON_KEY_FOLLOWING = "following";

    private Constants() {}
}
