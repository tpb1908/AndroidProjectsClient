package com.tpb.projects.issues.fragments;

import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Repository;
import com.tpb.projects.common.ViewSafeFragment;

/**
 * Created by theo on 14/03/17.
 */

public abstract class IssueFragment extends ViewSafeFragment {

    public abstract void issueLoaded(Issue issue);

    public abstract void setAccessLevel(Repository.AccessLevel level);

}
