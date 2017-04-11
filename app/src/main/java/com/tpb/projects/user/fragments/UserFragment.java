package com.tpb.projects.user.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.tpb.github.data.models.User;
import com.tpb.projects.R;
import com.tpb.projects.common.ViewSafeFragment;
import com.tpb.projects.user.UserActivity;

/**
 * Created by theo on 10/03/17.
 */

public abstract class UserFragment extends ViewSafeFragment {

    protected User mUser;

    public abstract void userLoaded(User user);

    protected UserActivity getParent() {
        return (UserActivity) getActivity();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState
                .containsKey(getString(R.string.parcel_user))) {
            mUser = savedInstanceState.getParcelable(getString(R.string.parcel_user));
            userLoaded(mUser);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getString(R.string.parcel_user), mUser);
    }

}
