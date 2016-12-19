package com.tpb.projects.project;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Column;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by theo on 19/12/16.
 */

public class ColumnFragment extends Fragment {
    private static final String TAG = ColumnFragment.class.getSimpleName();

    private Unbinder unbinder;

    private Column mColumn;

    @BindView(R.id.column_name) TextView mName;
    @BindView(R.id.column_last_updated) TextView mLastUpdate;
    @BindView(R.id.column_card_count) TextView mCardCount;

    public static ColumnFragment getInstance(Column column) {
        final ColumnFragment cf = new ColumnFragment();
        cf.mColumn = column;
        return cf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_column, container, false);
        unbinder = ButterKnife.bind(this, view);
        mName.setText(mColumn.getName());
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
