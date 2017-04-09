package com.tpb.github.data;

import android.content.Context;
import android.support.annotation.Nullable;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.tpb.github.data.models.content.Node;

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

    public void loadDirectory(final Loader.ListLoader<Node> loader, String repo, @Nullable final String path, @Nullable final Node parent, @Nullable String ref) {
        String fullPath = GIT_BASE + SEGMENT_REPOS + "/" + repo + SEGMENT_CONTENTS;
        if(path != null) fullPath += "/" + path;
        if(ref != null) fullPath += "?ref=" + ref;
        AndroidNetworking.get(fullPath)
                         .addHeaders(API_AUTH_HEADERS)
                         .getResponseOnlyFromNetwork()
                         .build()
                         .getAsJSONArray(new JSONArrayRequestListener() {
                             @Override
                             public void onResponse(JSONArray response) {
                                 try {
                                     final List<Node> nodes = new ArrayList<>(response.length());
                                     for(int i = 0; i < response.length(); i++) {
                                         final Node n = new Node(response.getJSONObject(i));
                                         n.setParent(parent);
                                         nodes.add(n);
                                     }
                                     Collections.sort(nodes, sorter);
                                     loader.listLoadComplete(nodes);
                                 } catch(JSONException jse) {
                                     loader.listLoadError(APIError.UNPROCESSABLE);
                                 }

                             }

                             @Override
                             public void onError(ANError anError) {
                                 loader.listLoadError(parseError(anError));
                             }
                         });
    }

    private static final Comparator<Node> sorter = new Comparator<Node>() {
        @Override
        public int compare(Node n1, Node n2) {
            if(n1.getType() == Node.NodeType.FILE && n2.getType() != Node.NodeType.FILE) return 1;
            if(n1.getType() != Node.NodeType.FILE && n2.getType() == Node.NodeType.FILE) return -1;
            return n1.getName().compareToIgnoreCase(n1.getName());
        }
    };

    public void loadRawFile(final StringRequestListener listener, final String path) {
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

}
