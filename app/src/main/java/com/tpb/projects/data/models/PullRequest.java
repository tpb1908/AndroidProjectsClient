package com.tpb.projects.data.models;

/**
 * Created by theo on 08/01/17.
 */

public class PullRequest extends DataModel {

    private int id;

    private String htmlURl;

    private String pathUrl;

    private String diffUrl;

    private String issueUrl;

    private String commentsUrl;

    private int number;

    private boolean isClosed;

    private String title;

    private String body;

    private User assignee;

    private boolean isLocked;

    private long createdAt;

    private long updatedAt;

    private long closedAt;

    private long mergedAt;

    private User creator;

    @Override
    public long getCreatedAt() {
        return 0;
    }
}
