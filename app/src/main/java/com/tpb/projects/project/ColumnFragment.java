package com.tpb.projects.project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Data;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by theo on 19/12/16.
 */

public class ColumnFragment extends Fragment implements Loader.CardsLoader {
    private static final String TAG = ColumnFragment.class.getSimpleName();

    FirebaseAnalytics mAnalytics;

    private Unbinder unbinder;
    private boolean mViewsValid = false;

    Column mColumn;

    @BindView(R.id.column_card) CardView mCard;
    @BindView(R.id.column_name) EditText mName;
    @BindView(R.id.column_last_updated) TextView mLastUpdate;
    @BindView(R.id.column_card_count) TextView mCardCount;
    @BindView(R.id.column_recycler) AnimatingRecycler mRecycler;

    private ProjectActivity mParent;
    private ProjectActivity.NavigationDragListener mNavListener;
    private Editor mEditor;
    private boolean mCanEdit;

    private CardAdapter mAdapter;


    public static ColumnFragment getInstance(Column column, ProjectActivity.NavigationDragListener navListener, boolean canEdit) {
        final ColumnFragment cf = new ColumnFragment();
        cf.mColumn = column;
        cf.mNavListener = navListener;
        cf.mCanEdit = canEdit;
        return cf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_column, container, false);
        unbinder = ButterKnife.bind(this, view);
        if(mColumn == null && savedInstanceState != null) {
            mColumn = savedInstanceState.getParcelable(getString(R.string.parcel_column));
        }

        mName.setText(mColumn.getName());
        displayLastUpdate();

        mViewsValid = true;
        mAdapter = new CardAdapter(this, mCanEdit);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        if(mCanEdit) {
            mRecycler.setOnDragListener(new CardDragListener(getContext(), mNavListener));
            mCard.setTag(mColumn.getId());
            mCard.setOnLongClickListener(v -> {
                final ClipData data = ClipData.newPlainText("", "");
                final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    v.startDragAndDrop(data, shadowBuilder, v, 0);
                } else {
                    v.startDrag(data, shadowBuilder, v, 0);
                }
                // v.setVisibility(View.INVISIBLE);
                return true;
            });
            final ColumnDragListener listener = new ColumnDragListener(mCard);
            mCard.setOnDragListener(new ColumnDragListener());
            mName.setOnDragListener(listener);
            mLastUpdate.setOnDragListener(listener);
            mCard.setOnDragListener(listener);
        } else {
            mName.setEnabled(false);
            view.findViewById(R.id.column_delete).setVisibility(View.GONE);
        }
        mName.clearFocus();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAnalytics = FirebaseAnalytics.getInstance(getContext());

        mEditor = new Editor(getContext());
        mName.setOnEditorActionListener((textView, i, keyEvent) -> {
            if(i == EditorInfo.IME_ACTION_DONE) {
                if(mName.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), R.string.error_no_column_title, Toast.LENGTH_SHORT).show();
                    mName.setText(mColumn.getName());
                } else {
                    mEditor.updateColumnName(new Editor.ColumnNameChangeListener() {
                        @Override
                        public void columnNameChanged(Column column) {
                            if(mViewsValid) {
                                mColumn.setName(mName.getText().toString());
                                resetLastUpdate();
                            }
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.TAG_PROJECT_EDIT, Analytics.VALUE_SUCCESS);
                            mAnalytics.logEvent(Analytics.TAG_COLUMN_NAME_CHANGE, bundle);
                        }

                        @Override
                        public void columnNameChangeError() {
                            Toast.makeText(getContext(), R.string.error_title_change_failed, Toast.LENGTH_SHORT).show();
                            mName.setText(mColumn.getName());
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.TAG_PROJECT_EDIT, Analytics.VALUE_FAILURE);
                            mAnalytics.logEvent(Analytics.TAG_COLUMN_NAME_CHANGE, bundle);
                        }
                    }, mColumn.getId(), mName.getText().toString());

                }
                return false;
            }
            return false;
        });
        new Loader(getContext()).loadCards(this, mColumn.getId());
    }

    private void resetLastUpdate() {
        mColumn.setUpdatedAt(System.currentTimeMillis() / 1000);
        displayLastUpdate();
    }

    private void displayLastUpdate() {
        mLastUpdate.setText(
                String.format(
                        getContext().getString(R.string.text_last_updated),
                        Data.timeAgo(mColumn.getUpdatedAt())
                )
        );
    }

    @OnClick(R.id.column_delete)
    void deleteColumn() {
        mParent.deleteColumn(mColumn);
    }

    void loadIssue(Loader.IssueLoader loader, int issueId) {
        mParent.loadIssue(loader, issueId);
    }

    void hideRecycler() {
        if(mRecycler != null) mRecycler.setVisibility(View.INVISIBLE);
    }

    @Override
    public void cardsLoaded(Card[] cards) {
        if(mViewsValid) {
            mCardCount.setText(Integer.toString(cards.length));
            mRecycler.enableAnimation();
            mAdapter.setCards(new ArrayList<>(Arrays.asList(cards)));
        }
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_CARDS_LOADED, bundle);
    }

    @Override
    public void cardsLoadError() {
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
        mAnalytics.logEvent(Analytics.TAG_CARDS_LOADED, bundle);
    }

    void addCard(Card card) {
        mAdapter.addCard(card);
        resetLastUpdate();
    }

    void updateCard(Card card) {
        mAdapter.updateCard(card);
        resetLastUpdate();
    }

    void removeCard(Card card) {
        mAdapter.removeCard(card);
        resetLastUpdate();
    }

    void openMenu(View view, Card card) {
        //We use the non AppCompat popup as the AppCompat version has a bug which scrolls the RecyclerView up
        final android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), view);
        popup.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_edit_note:
                    mParent.editCard(card);
                    break;
                case R.id.menu_delete_note:
                    mParent.deleteCard(card);
                    break;
                case R.id.menu_copy_card_note:
                    final ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("Card text", card.getNote()));
                    break;
                case R.id.menu_card_fullscreen:
                    final FullScreenDialog dialog = new FullScreenDialog();
                    final Bundle b = new Bundle();
                    b.putParcelable(getString(R.string.parcel_card), card);
                    dialog.setArguments(b);
                    dialog.show(getFragmentManager(), TAG);
            }

            return true;
        });
        popup.inflate(R.menu.menu_card);
        popup.show();
    }

    void cardClick(Card card) {
        //TODO other options
        mParent.editCard(card);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getString(R.string.parcel_column), mColumn);
    }

    private class ColumnDragListener implements View.OnDragListener {
        private View mActualTarget;

        ColumnDragListener() {
        }

        ColumnDragListener(View actualTarget) {
            mActualTarget = actualTarget;
            /*
            The problem with this listener is that the Card has numerous children.
            This means that when we drop another card onto the card we are actually
            dropping the view onto a child.
            In order to have a drag listener which covers the entire layout, we
            add modified drag listeners to each of the children, with a reference
            to their parent.
             */
        }

        @Override
        public boolean onDrag(View view, DragEvent event) {
            if(event.getAction() == DragEvent.ACTION_DROP) {
                final View sourceView = (View) event.getLocalState();
                view.setVisibility(View.VISIBLE);

                final int sourceTag = (int) sourceView.getTag();
                final int targetTag = (int) (mActualTarget == null ? view.getTag() : mActualTarget.getTag());
                Log.i(TAG, "onDrop: Column drop " + sourceTag + ", " + targetTag);
                if(sourceTag != targetTag) {
                    mParent.moveColumn(sourceTag, targetTag);
                }
            }
            return true;
        }
    }
}
