package com.tpb.projects.user.fragments;

import android.support.v4.app.Fragment;

import com.tpb.projects.data.models.User;

/**
 * Created by theo on 10/03/17.
 */

public abstract class UserFragment extends Fragment {

    public abstract void userLoaded(User user);

}
