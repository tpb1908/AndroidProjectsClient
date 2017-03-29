package com.tpb.projects.data.models.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by theo on 17/02/17.
 */

public class Node implements Parcelable {

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
            if(obj.has(SUBMODULE_GIT_URL_KEY)) {
                submoduleGitUrl = obj.getString(SUBMODULE_GIT_URL_KEY);
                type = NodeType.SUBMODULE;
            }
            if(isSubmodule(url, gitUrl)) type = NodeType.SUBMODULE;
        } catch(JSONException jse) {
            Log.e("Node", "Node: Exception: ", jse);
        }
    }

    private boolean isSubmodule(@NonNull String url, @NonNull String gitUrl) {
        try {
            int start = url.indexOf("com/") + 4;
            int repoStart = url.indexOf('/', url.indexOf('/', url.indexOf('/', start + 1) + 1));
            int repoEnd = url.indexOf('/', repoStart + 1) + 1;
            final String repo = url.substring(repoStart, repoEnd);
            start = gitUrl.indexOf("com/") + 4;
            repoStart = gitUrl.indexOf('/', gitUrl.indexOf('/', gitUrl.indexOf('/', start + 1) + 1));
            repoEnd = gitUrl.indexOf('/', repoStart + 1) + 1;
            return !repo.equals(gitUrl.substring(repoStart, repoEnd));
        } catch(IndexOutOfBoundsException iob) {
            return false;
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

        private final String type;

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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeInt(this.size);
        dest.writeString(this.encoding);
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeString(this.content);
        dest.writeString(this.sha);
        dest.writeString(this.url);
        dest.writeString(this.gitUrl);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.downloadUrl);
        dest.writeString(this.submoduleGitUrl);
        dest.writeParcelable(this.parent, flags);
        dest.writeTypedList(this.children);
    }

    protected Node(Parcel in) {
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : NodeType.values()[tmpType];
        this.size = in.readInt();
        this.encoding = in.readString();
        this.name = in.readString();
        this.path = in.readString();
        this.content = in.readString();
        this.sha = in.readString();
        this.url = in.readString();
        this.gitUrl = in.readString();
        this.htmlUrl = in.readString();
        this.downloadUrl = in.readString();
        this.submoduleGitUrl = in.readString();
        this.parent = in.readParcelable(Node.class.getClassLoader());
        this.children = in.createTypedArrayList(Node.CREATOR);
    }

    public static final Creator<Node> CREATOR = new Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel source) {
            return new Node(source);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };
}
