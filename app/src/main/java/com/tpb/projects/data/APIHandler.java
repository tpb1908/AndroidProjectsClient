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

    protected static HashMap<String, String> API_AUTH_HEADERS = new HashMap<>();
    static HashMap<String, String> PROJECTS_PREVIEW_API_AUTH_HEADERS = new HashMap<>();
    static HashMap<String, String> ORGANIZATIONS_PREVIEW_ACCEPT_HEADERS = new HashMap<>();

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

    public static final int HTTP_OK_200 = 200; //OK

    public static final int HTTP_BAD_REQUEST_400 = 400; //Bad request

    public static final int HTTP_UNAUTHORIZED_401 = 401; //Login required, account locked, permission error

    public static final int HTTP_FORBIDDEN_403 = 403; //Forbidden server locked or other reasons

    public static final int HTTP_NOT_ALLOWED_405 = 405; //Not allowed (managed server)

    public static final int HTTP_419 = 419; //This function can only be executed with an CL-account

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

    public enum APIError {

        UNAUTHORIZED, FORBIDDEN,  NOT_FOUND, UNKNOWN

    }

}
