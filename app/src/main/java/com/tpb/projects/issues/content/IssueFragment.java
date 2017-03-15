package com.tpb.projects.issues.content;

import android.support.v4.app.Fragment;

import com.tpb.projects.data.models.Issue;

/**
 * Created by theo on 14/03/17.
 */

public abstract class IssueFragment extends Fragment {

    public abstract void issueLoaded(Issue issue);

}
