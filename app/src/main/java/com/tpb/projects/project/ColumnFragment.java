package com.tpb.projects.project;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.util.Data;
import com.tpb.projects.views.AnimatingRecycler;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by theo on 19/12/16.
 */

public class ColumnFragment extends Fragment {
    private static final String TAG = ColumnFragment.class.getSimpleName();

    private Unbinder unbinder;
    private boolean mViewsValid = false;

    Column mColumn;

    @BindView(R.id.column_card) CardView mCard;
    @BindView(R.id.column_name) EditText mName;
    @BindView(R.id.column_last_updated) TextView mLastUpdate;
    @BindView(R.id.column_card_count) TextView mCardCount;
    @BindView(R.id.column_recycler) AnimatingRecycler mRecycler;

    private ProjectActivity mParent;
    private Editor mEditor;

    private CardAdapter mAdapter;

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
        mLastUpdate.setText(
                String.format(
                        getContext().getString(R.string.text_last_updated),
                        Data.timeAgo(mColumn.getUpdatedAt())
                )
        );
        mViewsValid = true;
        mAdapter = new CardAdapter(this);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mEditor = new Editor(getContext());
        mName.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE) {
                mEditor.updateColumn(new Editor.ColumnChangeListener() {
                    @Override
                    public void columnChanged(Column column) {

                    }

                    @Override
                    public void changeError() {

                    }
                }, mColumn.getId(), mName.getText().toString());
                if(mViewsValid) {
                    mColumn.setName(mName.getText().toString());
                    mColumn.setUpdatedAt(System.currentTimeMillis() / 1000);
                    mLastUpdate.setText(
                            String.format(
                                    getContext().getString(R.string.text_last_updated),
                                    Data.timeAgo(mColumn.getUpdatedAt())
                            )
                    );
                }
                return false;
            }
            return false;
        });
        new Loader(getContext()).loadCards(new Loader.CardsLoader() {
            @Override
            public void cardsLoaded(Card[] cards) {
                if(mViewsValid) {
                    mCardCount.setText(Integer.toString(cards.length));
                    mRecycler.enableAnimation();
                    mAdapter.setCards(new ArrayList<>(Arrays.asList(cards)));
                }
                Log.i(TAG, "cardsLoaded: " + Arrays.toString(cards));
            }

            @Override
            public void loadError() {

            }
        }, mColumn.getId());
    }

    @OnClick(R.id.column_delete)
    void deleteColumn() {
        mParent.deleteColumn(mColumn);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mParent = (ProjectActivity) context;
        } catch(ClassCastException cce) {
            throw new IllegalArgumentException("Parent of ColumnFragment must be ProjectActivity");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        mViewsValid = false;
    }
}
