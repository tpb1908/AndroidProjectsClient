package com.tpb.projects.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;

import org.json.JSONObject;

/**
 * Created by theo on 15/02/17.
 */

public class Uploader {

    private static final String IMGUR_AUTH_KEY = "Authorization";
    private static final String IMGUR_AUTH_FORMAT = "Client-ID %1$s";

    public Uploader() {

    }

    public void uploadImage(ImgurUploadListener listener, String image64, @Nullable UploadProgressListener uploadListener) {
        AndroidNetworking.upload("https://api.imgur.com/3/image")
                .addHeaders(IMGUR_AUTH_KEY, String.format(IMGUR_AUTH_FORMAT, com.tpb.projects.BuildConfig.IMGUR_CLIENT_ID))
                .addMultipartParameter("image", image64)
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(uploadListener)
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("Upload", "onResponse: " + response.toString());
                        try {
                            final String link = response.getJSONObject("data").getString("link");
                            if(listener != null) listener.imageUploaded(link);
                        } catch(Exception e) {
                            Log.e("Uploader", "onResponse: ", e);
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i("Upload error", "onError: " + anError.getErrorBody());
                        Log.i("Upload error", "onError: " + anError.getErrorDetail());
                        if(listener != null) listener.uploadError(anError);
                    }
                });
    }

    public interface ImgurUploadListener {

        void imageUploaded(String url);

        void uploadError(ANError error);

    }

}
