package com.tpb.github.data;

import android.content.Context;
import android.support.annotation.NonNull;
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

public class Uploader extends APIHandler {

    private static final String IMGUR_AUTH_KEY = "Authorization";
    private static final String IMGUR_AUTH_FORMAT = "Client-ID %1$s";


    protected Uploader(Context context) {
        super(context);
    }

    public static void uploadImage(@NonNull final ImgurUploadListener listener, String image64, @Nullable UploadProgressListener uploadListener, @NonNull String clientId) {
        AndroidNetworking.upload("https://api.imgur.com/3/image")
                         .addHeaders(IMGUR_AUTH_KEY, String.format(IMGUR_AUTH_FORMAT,
                                 clientId
                         ))
                         .addMultipartParameter("image", image64)
                         .setPriority(Priority.HIGH)
                         .build()
                         .setUploadProgressListener(uploadListener)
                         .getAsJSONObject(new JSONObjectRequestListener() {
                             @Override
                             public void onResponse(JSONObject response) {
                                 try {
                                     final String link = response.getJSONObject("data").getString("link");
                                     listener.imageUploaded(link);
                                 } catch(Exception e) {
                                     Log.e("Uploader", "onResponse: ", e);
                                 }
                             }

                             @Override
                             public void onError(ANError anError) {
                                 listener.uploadError(parseError(anError));
                             }
                         });
    }

    public interface ImgurUploadListener {

        void imageUploaded(String url);

        void uploadError(APIError error);

    }

}
