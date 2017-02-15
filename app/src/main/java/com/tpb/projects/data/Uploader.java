package com.tpb.projects.data;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONObject;

/**
 * Created by theo on 15/02/17.
 */

public class Uploader {

    private static final String IMGUR_AUTH_KEY = "Authorization:";
    private static final String IMGUR_AUTH_FORMAT = "Client-ID %1$s";

    public Uploader() {

    }

    public void uploadImage(String image64) {
        AndroidNetworking.upload("https://api.imgur.com/3/image")
                .addHeaders(IMGUR_AUTH_KEY, String.format(IMGUR_AUTH_FORMAT, com.tpb.projects.BuildConfig.IMGUR_CLIENT_ID))
                .addMultipartParameter("image", image64)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {

                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

}
