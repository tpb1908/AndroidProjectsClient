package com.tpb.projects.data.models.files;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class Node {

    private NodeType type;
    private int size;
    private String encoding;
    private String name;
    private String path;
    private String content;
    private String sha;
    private String url;
    private String gitUrl;
    private String htmlUrl;
    private String downloadUrl;
    private String submoduleGitUrl;

    private Node parent;
    private List<Node> children;

    private static final String TYPE_KEY = "type";
    private static final String SIZE_KEY = "size";
    private static final String ENCODING_KEY = "encoding";
    private static final String NAME_KEY = "name";
    private static final String PATH_KEY = "path";
    private static final String CONTENT_KEY = "content";
    private static final String SHA_KEY = "sha";
    private static final String URL_KEY = "url";
    private static final String GIT_URL_KEY = "git_url";
    private static final String HTML_URL_KEY = "html_url";
    private static final String DOWNLOAD_URL_KEY = "download_url";
    private static final String SUBMODULE_GIT_URL_KEY = "submodule_git_url";


    public Node(JSONObject obj) {
        try {
            type = NodeType.fromString(obj.getString(TYPE_KEY));
            size = obj.getInt(SIZE_KEY);
            if(obj.has(ENCODING_KEY)) encoding = obj.getString(ENCODING_KEY);
            name = obj.getString(NAME_KEY);
            path = obj.getString(PATH_KEY);
            if(obj.has(CONTENT_KEY)) content = obj.getString(CONTENT_KEY);
            sha = obj.getString(SHA_KEY);
            url = obj.getString(URL_KEY);
            gitUrl = obj.getString(GIT_URL_KEY);
            htmlUrl = obj.getString(HTML_URL_KEY);
            downloadUrl = obj.getString(DOWNLOAD_URL_KEY);
            if(obj.has(SUBMODULE_GIT_URL_KEY))
                submoduleGitUrl = obj.getString(SUBMODULE_GIT_URL_KEY);
        } catch(JSONException jse) {
            Log.e("Node", "Node: Exception: ", jse);
        }
    }

    public NodeType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getContent() {
        return content;
    }

    public String getSha() {
        return sha;
    }

    public String getUrl() {
        return url;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getSubmoduleGitUrl() {
        return submoduleGitUrl;
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Node && sha.equals(((Node) obj).getSha());
    }

    @Override
    public String toString() {
        return "Node{" +
                "type=" + type +
                ", size=" + size +
                ", encoding='" + encoding + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", content='" + content + '\'' +
                ", sha='" + sha + '\'' +
                ", url='" + url + '\'' +
                ", gitUrl='" + gitUrl + '\'' +
                ", htmlUrl='" + htmlUrl + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", submoduleGitUrl='" + submoduleGitUrl + '\'' +
                '}';
    }

    public enum NodeType {

        FILE("file"),
        DIRECTORY("dir"),
        SYMLINK("symlink"),
        SUBMODULE("submodule");

        private String type;

        NodeType(String type) {
            this.type = type;
        }

        public static NodeType fromString(String type) {
            for(NodeType nt : NodeType.values()) {
                if(nt.type.equals(type)) return nt;
            }
            throw new IllegalArgumentException("No NodeType with String value " + type);
        }
    }

}
