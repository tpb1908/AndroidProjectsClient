package com.tpb.projects.project;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tpb.projects.R;

/**
 * Created by theo on 19/12/16.
 */

public class ColumnFragment extends Fragment {


    public static ColumnFragment getInstance() {
        return new ColumnFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_column, container, false);
    }
}
