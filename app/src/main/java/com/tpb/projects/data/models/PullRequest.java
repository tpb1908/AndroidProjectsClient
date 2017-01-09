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
