package com.tpb.projects.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.tpb.projects.data.models.files.Node;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class FileLoader extends APIHandler {

    private static final String SEGMENT_CONTENTS = "/contents";

    public FileLoader(Context context) {
        super(context);
    }

    public void loadDirectory(DirectoryLoader loader, String repo, @Nullable String path) {
        String PATH = GIT_BASE + SEGMENT_REPOS + "/" + repo + SEGMENT_CONTENTS;
        if(path != null) PATH += "/" + path;
        AndroidNetworking.get(PATH)
                .addHeaders(API_AUTH_HEADERS)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Node> nodes = new ArrayList<>(response.length());
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                nodes.add(new Node(response.getJSONObject(i)));
                            }
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                            if(loader != null) loader.directoryLoadError(APIError.UNPROCESSABLE);
                        }
                        if(loader != null) loader.directoryLoaded(nodes);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.directoryLoadError(parseError(anError));
                    }
                });
    }

    public interface DirectoryLoader {

        void directoryLoaded(List<Node> directory);

        void directoryLoadError(APIError error);

    }

}
