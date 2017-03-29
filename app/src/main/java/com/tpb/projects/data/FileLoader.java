package com.tpb.projects.data;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.projects.data.models.content.Node;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class FileLoader extends APIHandler {

    private static final String SEGMENT_CONTENTS = "/contents";

    public FileLoader(Context context) {
        super(context);
    }

    public void loadDirectory(DirectoryLoader loader, String repo, @Nullable String path, @Nullable Node parent) {
        String PATH = GIT_BASE + SEGMENT_REPOS + "/" + repo + SEGMENT_CONTENTS;
        if(path != null) PATH += "/" + path;
        AndroidNetworking.get(PATH)
                .addHeaders(API_AUTH_HEADERS)
                .getResponseOnlyFromNetwork()
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final List<Node> nodes = new ArrayList<>(response.length());
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                nodes.add(new Node(response.getJSONObject(i)));
                                nodes.get(i).setParent(parent);
                            }
                        } catch(JSONException jse) {
                            Log.e(TAG, "onResponse: ", jse);
                            if(loader != null) loader.directoryLoadError(APIError.UNPROCESSABLE);
                        }
                        Collections.sort(nodes, sorter);
                        if(loader != null) loader.directoryLoaded(nodes);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(loader != null) loader.directoryLoadError(parseError(anError));
                    }
                });
    }

    private static final Comparator<Node> sorter = (n1, n2) -> {
        if(n1.getType() == Node.NodeType.FILE && n2.getType() != Node.NodeType.FILE) return 1;
        if(n1.getType() != Node.NodeType.FILE && n2.getType() == Node.NodeType.FILE) return -1;
        return n1.getName().compareToIgnoreCase(n1.getName());
    };

    public void loadRawFile(StringRequestListener listener, String path) {
        AndroidNetworking.get(path)
                .addHeaders(API_AUTH_HEADERS)
                .setPriority(Priority.IMMEDIATE)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        if(listener != null) listener.onResponse(response);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if(listener != null) listener.onError(anError);
                    }
                });
    }

    public interface DirectoryLoader {

        void directoryLoaded(List<Node> directory);

        void directoryLoadError(APIError error);

    }

}
