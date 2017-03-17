package com.tpb.projects.issues.content;

import android.support.v4.app.Fragment;

import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;

/**
 * Created by theo on 14/03/17.
 */

public abstract class IssueFragment extends Fragment {

    public abstract void issueLoaded(Issue issue);

    public abstract void setAccessLevel(Repository.AccessLevel level);

}
