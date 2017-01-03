/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.text.format.DateUtils;
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
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.project.dialogs.CardDialog;
import com.tpb.projects.project.dialogs.EditIssueDialog;
import com.tpb.projects.project.dialogs.FullScreenDialog;
import com.tpb.projects.project.dialogs.NewCommentDialog;
import com.tpb.projects.project.dialogs.NewIssueDialog;
import com.tpb.projects.util.Analytics;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.tpb.projects.data.SettingsActivity.Preferences.CardAction.COPY;

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

    ProjectActivity mParent;
    private ProjectActivity.NavigationDragListener mNavListener;
    private Editor mEditor;
    private Repository.AccessLevel mAccessLevel;
    private boolean mShouldAnimate;

    private CardAdapter mAdapter;


    public static ColumnFragment getInstance(Column column, ProjectActivity.NavigationDragListener navListener, Repository.AccessLevel accessLevel, boolean shouldAnimate) {
        final ColumnFragment cf = new ColumnFragment();
        cf.mColumn = column;
        cf.mNavListener = navListener;
        cf.mAccessLevel = accessLevel;
        cf.mShouldAnimate = shouldAnimate;
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

        mViewsValid = true;
        mAdapter = new CardAdapter(this, mAccessLevel);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setAnimationType(AnimatingRecycler.AnimationType.HORIZONTAL);
        if(!mShouldAnimate) mRecycler.disableAnimation();
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
            enableAccess(view);
        } else {
            disableAccess(view);
        }
        mName.clearFocus();


        displayLastUpdate();
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

    void setAccessLevel(Repository.AccessLevel accessLevel) {
        mAccessLevel = accessLevel;
        if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
            enableAccess(getView());
        } else {
            disableAccess(getView());
        }
        mAdapter.setAccessLevel(accessLevel);
    }

    private void enableAccess(View view) {
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
        ((NestedScrollView) view.findViewById(R.id.column_scrollview)).setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(scrollY - oldScrollY > 10) {
                    mParent.hideFab();
                } else if(scrollY - oldScrollY < -10) {
                    mParent.showFab();
                }
            }
        });
    }

    private void disableAccess(View view) {
        mName.setEnabled(false);
        view.findViewById(R.id.column_delete).setVisibility(View.GONE);
    }

    private void resetLastUpdate() {
        mColumn.setUpdatedAt(System.currentTimeMillis());
        displayLastUpdate();
    }

    private void displayLastUpdate() {
        mLastUpdate.setText(
                String.format(
                        getContext().getString(R.string.text_last_updated),
                        DateUtils.getRelativeTimeSpanString(mColumn.getUpdatedAt())
                )
        );
        mCardCount.setText(Integer.toString(mAdapter.getItemCount()));
    }

    @OnClick(R.id.column_delete)
    void deleteColumn() {
        //TODO Move this to an options menu for the column
        mParent.deleteColumn(mColumn);
    }

    void loadIssue(Loader.IssueLoader loader, int issueId) {
        mParent.loadIssue(loader, issueId);
    }

    @Override
    public void cardsLoaded(Card[] cards) {
        if(mViewsValid) {
            mCardCount.setText(Integer.toString(cards.length));
            if(mShouldAnimate) {
                mRecycler.enableAnimation();
            }
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

    void removeCard(Card card) {
        mAdapter.removeCard(card);
        resetLastUpdate();
    }

    void recreateCard(Card card) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.createCard(new Editor.CardCreationListener() {
            @Override
            public void cardCreated(int columnId, Card card) {
                addCard(card);
                mParent.mRefresher.setRefreshing(false);
            }

            @Override
            public void cardCreationError() {
                mParent.mRefresher.setRefreshing(false);
            }
        }, mColumn.getId(), card.getNote());
    }

    ArrayList<Card> getCards() {
        return mAdapter.getCards();
    }

    void showCardDialog(CardDialog dialog) {
        dialog.setListener(new CardDialog.CardDialogListener() {
            @Override
            public void cardEditDone(Card card, boolean isNewCard) {
                mParent.mRefresher.setRefreshing(true);
                if(isNewCard) {
                    mEditor.createCard(new Editor.CardCreationListener() {
                        @Override
                        public void cardCreated(int columnId, Card card) {
                            addCard(card);
                            mParent.mRefresher.setRefreshing(false);
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                            mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
                        }

                        @Override
                        public void cardCreationError() {
                            mParent.mRefresher.setRefreshing(false);
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                            mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
                        }
                    }, mColumn.getId(), card.getNote());
                } else {
                    mEditor.updateCard(new Editor.CardUpdateListener() {
                        @Override
                        public void cardUpdated(Card card) {
                            mAdapter.updateCard(card);
                            resetLastUpdate();
                            mParent.mRefresher.setRefreshing(false);
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                            mAnalytics.logEvent(Analytics.TAG_CARD_EDIT, bundle);
                        }

                        @Override
                        public void cardUpdateError() {
                            mParent.mRefresher.setRefreshing(false);
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                            mAnalytics.logEvent(Analytics.TAG_CARD_EDIT, bundle);
                        }
                    }, card.getId(), card.getNote());
                }
            }

            @Override
            public void issueCardCreated(Issue issue) {
                createIssueCard(issue);
            }

            @Override
            public void cardEditCancelled() {

            }
        });
        dialog.show(getFragmentManager(), "TAG");
    }

    void openMenu(View view, Card card) {
        //We use the non AppCompat popup as the AppCompat version has a bug which scrolls the RecyclerView up
        final android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), view);
        popup.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_edit_note:
                    final CardDialog dialog = new CardDialog();
                    final Bundle b = new Bundle();
                    b.putParcelable(getString(R.string.parcel_card), card);
                    dialog.setArguments(b);
                    showCardDialog(dialog);
                    break;
                case R.id.menu_delete_note:
                    mParent.deleteCard(card, true);
                    break;
                case R.id.menu_copy_card_note:
                    copyToClipboard(card.getNote());
                    break;
                case R.id.menu_copy_card_url:
                    copyToClipboard(String.format(mParent.getString(R.string.text_card_url),
                            mParent.mProject.getRepoFullName(),
                            mParent.mProject.getNumber(),
                            mCard.getId()));
                    break;
                case R.id.menu_copy_issue_url:
                    copyToClipboard(String.format(mParent.getString(R.string.text_issue_url),
                            mParent.mProject.getRepoFullName(),
                            card.getIssue().getNumber()));
                    break;
                case R.id.menu_card_fullscreen:
                    showFullscreen(card);
                    break;
                case R.id.menu_convert_to_issue:
                    final NewIssueDialog newDialog = new NewIssueDialog();
                    newDialog.setListener(new NewIssueDialog.IssueDialogListener() {
                        @Override
                        public void issueCreated(Issue issue) {
                            convertCardToIssue(card, issue);
                            final Bundle bundle = new Bundle();
                            bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                            mAnalytics.logEvent(Analytics.TAG_ISSUE_CREATED, bundle);
                        }

                        @Override
                        public void issueCreationCancelled() {
                        }
                    });
                    final Bundle c = new Bundle();
                    c.putParcelable(getString(R.string.parcel_card), card);
                    c.putString(getString(R.string.intent_repo), mParent.mProject.getRepoFullName());
                    newDialog.setArguments(c);
                    newDialog.show(getFragmentManager(), TAG);
                    break;
                case R.id.menu_edit_issue:
                    editIssue(card);
                    break;
                case R.id.menu_delete_issue_card:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.title_close_issue);
                    builder.setMessage(R.string.text_close_issue_on_delete);
                    builder.setPositiveButton(R.string.action_yes, (dialogInterface, i) -> {
                        mEditor.closeIssue(null, mParent.mProject.getRepoFullName(), card.getIssue().getNumber());
                        mParent.deleteCard(card, false);
                    });
                    builder.setNeutralButton(R.string.action_cancel, null);
                    builder.setNegativeButton(R.string.action_no, (dialogInterface, i) -> mParent.deleteCard(card, false));
                    builder.show();

                    break;
                case 1:
                    toggleIssueState(card);
                    break;
            }

            return true;
        });
        if(card.hasIssue()) {
            popup.inflate(R.menu.menu_issue);
            popup.getMenu().add(0, 1, 0, card.getIssue().isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue);
        } else {
            popup.inflate(R.menu.menu_card);
        }

        popup.show();
    }

    private void toggleIssueState(Card card) {
        final Editor.IssueStateChangeListener listener = new Editor.IssueStateChangeListener() {
            @Override
            public void issueStateChanged(Issue issue) {
                card.setFromIssue(issue);
                mAdapter.updateCard(card);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(issue.isClosed() ? Analytics.TAG_ISSUE_CLOSED : Analytics.TAG_ISSUE_OPENED, bundle);
            }

            @Override
            public void issueStateChangeError() {
                mAdapter.updateCard(card);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        };

        final NewCommentDialog dialog = new NewCommentDialog();
        dialog.setListener(new NewCommentDialog.NewCommentDialogListener() {
            @Override
            public void commentCreated(String body) {
                mEditor.createComment(new Editor.CommentCreationListener() {
                    @Override
                    public void commentCreated(Comment comment) {
                        if(card.getIssue().isClosed()) {
                            Log.i(TAG, "openMenu: Closing issue");
                            mEditor.openIssue(listener, mParent.mProject.getRepoFullName(), card.getIssueId());
                            resetLastUpdate();
                        } else {
                            Log.i(TAG, "openMenu: Opening issue");
                            mEditor.closeIssue(listener, mParent.mProject.getRepoFullName(), card.getIssueId());
                            resetLastUpdate();
                        }
                    }

                    @Override
                    public void commentCreationError() {

                    }
                }, mParent.mProject.getRepoFullName(), card.getIssue().getNumber(), body);

            }

            @Override
            public void commentNotCreated() {
                if(card.getIssue().isClosed()) {
                    Log.i(TAG, "openMenu: Closing issue");
                    mEditor.openIssue(listener, mParent.mProject.getRepoFullName(), card.getIssueId());
                    resetLastUpdate();
                } else {
                    Log.i(TAG, "openMenu: Opening issue");
                    mEditor.closeIssue(listener, mParent.mProject.getRepoFullName(), card.getIssueId());
                    resetLastUpdate();
                }
            }
        });
        dialog.show(getFragmentManager(), TAG);
    }

    private void editIssue(Card card) {
        final EditIssueDialog editDialog = new EditIssueDialog();
        editDialog.setListener(new EditIssueDialog.EditIssueDialogListener() {
            @Override
            public void issueEdited(Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
                mParent.mRefresher.setRefreshing(true);
                mEditor.editIssue(new Editor.IssueEditListener() {
                    @Override
                    public void issueEdited(Issue issue) {
                        card.setFromIssue(issue);
                        mAdapter.updateCard(card);
                        mParent.mRefresher.setRefreshing(false);
                        resetLastUpdate();
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }

                    @Override
                    public void issueEditError() {
                        mParent.mRefresher.setRefreshing(false);
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
                    }
                }, mParent.mProject.getRepoFullName(), issue, assignees, labels);
            }

            @Override
            public void issueEditCancelled() {

            }
        });
        final Bundle c = new Bundle();
        c.putParcelable(getString(R.string.parcel_issue), card.getIssue());
        c.putString(getString(R.string.intent_repo), mParent.mProject.getRepoFullName());
        editDialog.setArguments(c);
        editDialog.show(getFragmentManager(), TAG);
    }

    private void convertCardToIssue(Card oldCard, Issue issue) {
        mEditor.deleteCard(new Editor.CardDeletionListener() {
            @Override
            public void cardDeleted(Card card) {
                createIssueCard(issue, oldCard.getId());
                resetLastUpdate();
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(Analytics.TAG_CARD_DELETION, bundle);
            }

            @Override
            public void cardDeletionError() {
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_DELETION, bundle);
            }
        }, oldCard);
    }

    void createIssueCard(Issue issue) {
        createIssueCard(issue, -1);
    }

    void createIssueCard(Issue issue, int oldCardId) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.createCard(new Editor.CardCreationListener() {
            @Override
            public void cardCreated(int columnId, Card card) {
                Log.i(TAG, "cardCreated: Issue card created");
                mParent.mRefresher.setRefreshing(false);
                if(oldCardId == -1) {
                    mAdapter.addCard(card);
                } else {
                    mAdapter.updateCard(card, oldCardId);
                }
                resetLastUpdate();
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(Analytics.TAG_CARD_TO_ISSUE, bundle);

            }

            @Override
            public void cardCreationError() {
                mParent.mRefresher.setRefreshing(false);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_TO_ISSUE, bundle);
            }
        }, mColumn.getId(), issue.getId());
    }

    void copyToClipboard(String text) {
        final ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Card", text));
        Toast.makeText(mParent, getString(R.string.text_copied_to_board), Toast.LENGTH_SHORT).show();
    }

    void showFullscreen(Card card) {
        final FullScreenDialog dialog = new FullScreenDialog();
        final Bundle b = new Bundle();
        b.putParcelable(getString(R.string.parcel_card), card);
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), TAG);
    }

    void cardClick(Card card) {
        final SettingsActivity.Preferences.CardAction action;
        if(mAccessLevel == Repository.AccessLevel.NONE || mAccessLevel == Repository.AccessLevel.READ) {
            action = COPY;
        } else {
            action = SettingsActivity.Preferences.getPreferences(getContext()).getCardAction();
        }
        switch(action) {
            case EDIT:
                if(card.hasIssue()) {
                    editIssue(card);
                } else {
                    final CardDialog dialog = new CardDialog();
                    final Bundle b = new Bundle();
                    b.putParcelable(getString(R.string.parcel_card), card);
                    dialog.setArguments(b);
                    showCardDialog(dialog);
                }
                break;
            case FULLSCREEN:
                showFullscreen(card);
                break;
            case COPY:
                copyToClipboard(card.getNote());
                break;
        }
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
                Log.i(TAG, "onDrop: Column drop " + sourceTag + ", " + targetTag + ", direction " + (event.getX() < view.getWidth() / 2));
                if(sourceTag != targetTag && sourceView.getId() == R.id.column_card) {
                    mParent.moveColumn(sourceTag, targetTag, event.getX() < view.getWidth() / 2);
                }
            }
            return true;
        }
    }
}
