package com.tpb.projects.user.fragments;

import android.support.v4.app.Fragment;

import com.tpb.projects.data.models.User;
import com.tpb.projects.user.UserActivity;

/**
 * Created by theo on 10/03/17.
 */

public abstract class UserFragment extends Fragment {

    protected boolean mAreViewsValid;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAreViewsValid = false;
    }

    protected UserActivity getParent() {
        return (UserActivity) getActivity();
    }

    public abstract void userLoaded(User user);

}
