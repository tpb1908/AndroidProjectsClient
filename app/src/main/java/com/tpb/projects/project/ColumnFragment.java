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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.widget.ANImageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Column;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.editors.CardEditor;
import com.tpb.projects.editors.CommentEditor;
import com.tpb.projects.editors.FullScreenDialog;
import com.tpb.projects.editors.IssueEditor;
import com.tpb.projects.issues.IssueActivity;
import com.tpb.projects.user.UserActivity;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.UI;

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
    @BindView(R.id.column_scrollview) NestedScrollView mNestedScroller;
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
        mAdapter = new CardAdapter(this, mNavListener, mAccessLevel);
        mRecycler.setAdapter(mAdapter);
        mRecycler.disableAnimation();
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
                        int loadCount = 0;

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
                        public void columnNameChangeError(APIHandler.APIError error) {
                            if(error != APIHandler.APIError.NO_CONNECTION) {
                                if(loadCount < 5) {
                                    loadCount++;
                                    mEditor.updateColumnName(this, mColumn.getId(), mName.getText().toString());
                                } else {
                                    Toast.makeText(getContext(), R.string.error_title_change_failed, Toast.LENGTH_SHORT).show();
                                    mName.setText(mColumn.getName());
                                    final Bundle bundle = new Bundle();
                                    bundle.putString(Analytics.TAG_PROJECT_EDIT, Analytics.VALUE_FAILURE);
                                    mAnalytics.logEvent(Analytics.TAG_COLUMN_NAME_CHANGE, bundle);
                                }
                            }
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
        mParent.loadIssue(loader, issueId, mColumn);
    }

    @Override
    public void cardsLoaded(Card[] cards) {
        if(mViewsValid) {
            mCardCount.setText(Integer.toString(cards.length));
            if(mShouldAnimate) {
                mRecycler.enableAnimation();
            }
            mRecycler.disableAnimation();
            mAdapter.setCards(new ArrayList<>(Arrays.asList(cards)));
        }
        mParent.notifyFragmentLoaded();
        final Bundle bundle = new Bundle();
        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
        mAnalytics.logEvent(Analytics.TAG_CARDS_LOADED, bundle);
    }

    @Override
    public void cardsLoadError(APIHandler.APIError error) {
        if(error != APIHandler.APIError.NO_CONNECTION) {
            final Bundle bundle = new Bundle();
            bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
            mAnalytics.logEvent(Analytics.TAG_CARDS_LOADED, bundle);
            //TODO check for auth errors
        } //TODO Add a listener to wait for network change and reload automatically

    }

    private void addCard(Card card) {
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
            int createAttempts = 0;

            @Override
            public void cardCreated(int columnId, Card card) {
                addCard(card);
                mParent.mRefresher.setRefreshing(false);
            }

            @Override
            public void cardCreationError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(createAttempts < 5) {
                        createAttempts++;
                        mEditor.createCard(this, mColumn.getId(), card.getNote());
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
            }
        }, mColumn.getId(), card.getNote());
    }

    boolean attemptMoveTo(int cardId) {
        final int index = mAdapter.indexOf(cardId);
        if(index == -1) return false;

        final View view = mRecycler.findViewHolderForAdapterPosition(index).itemView;
        final ObjectAnimator colorFade = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                new ArgbEvaluator(),
                getContext().getResources().getColor(R.color.md_grey_800),
                getContext().getResources().getColor(R.color.colorAccent));
        colorFade.setDuration(300);
        colorFade.setRepeatMode(ObjectAnimator.REVERSE);
        colorFade.setRepeatCount(1);
        colorFade.start();

        //Initial height is the top cardview
        int height = ((LinearLayout) mNestedScroller.getChildAt(0)).getChildAt(0).getHeight();
        for(int i = 0; i < mRecycler.getChildCount() && i < index; i++) {
            height += mRecycler.getChildAt(i).getHeight();
        }
        Log.i(TAG, "attemptMoveTo: Height of " + height);
        mNestedScroller.scrollTo(0, height);

        return true;
    }

    ArrayList<Card> getCards() {
        return mAdapter.getCards();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != IssueEditor.RESULT_OK) return;
        String[] assignees = null;
        String[] labels = null;
        if(data.hasExtra(getString(R.string.intent_issue_assignees))) {
            assignees = data.getStringArrayExtra(getString(R.string.intent_issue_assignees));
        }
        if(data.hasExtra(getString(R.string.intent_issue_labels))) {
            labels = data.getStringArrayExtra(getString(R.string.intent_issue_labels));
        }
        switch(requestCode) {
            case IssueEditor.REQUEST_CODE_EDIT_ISSUE:
                final Card card = data.getParcelableExtra(getString(R.string.parcel_card));
                final Issue edited = data.getParcelableExtra(getString(R.string.parcel_issue));
                editIssue(card, edited, assignees, labels);
                break;
            case IssueEditor.REQUEST_CODE_ISSUE_FROM_CARD:
                final Card oldCard = data.getParcelableExtra(getString(R.string.parcel_card));
                final Issue issue = data.getParcelableExtra(getString(R.string.parcel_issue));

                mEditor.createIssue(new Editor.IssueCreationListener() {
                    @Override
                    public void issueCreated(Issue issue) {
                        convertCardToIssue(oldCard, issue);
                    }

                    @Override
                    public void issueCreationError(APIHandler.APIError error) {

                    }
                }, mParent.mProject.getRepoPath(), issue.getTitle(), issue.getBody(), assignees, labels);
                break;
        }
    }

    void openMenu(View view, Card card) {
        //We use the non AppCompat popup as the AppCompat version has a bug which scrolls the RecyclerView up
        final android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), view);
        popup.setOnMenuItemClickListener(menuItem -> {
            switch(menuItem.getItemId()) {
                case R.id.menu_edit_note:
                    final Intent i = new Intent(getContext(), CardEditor.class);
                    i.putExtra(getString(R.string.parcel_card), card);
                    UI.getViewCenterOnScreen(i, view);
                    getActivity().startActivityForResult(i, CardEditor.REQUEST_CODE_EDIT_CARD);
                    break;
                case R.id.menu_delete_note:
                    mParent.deleteCard(card, true);
                    break;
                case R.id.menu_copy_card_note:
                    copyToClipboard(card.getNote());
                    break;
                case R.id.menu_copy_card_url:
                    copyToClipboard(String.format(mParent.getString(R.string.text_card_url),
                            mParent.mProject.getRepoPath(),
                            mParent.mProject.getNumber(),
                            mCard.getId()));
                    break;
                case R.id.menu_copy_issue_url:
                    copyToClipboard(String.format(mParent.getString(R.string.text_issue_url),
                            mParent.mProject.getRepoPath(),
                            card.getIssue().getNumber()));
                    break;
                case R.id.menu_card_fullscreen:
                    showFullscreen(card);
                    break;
                case R.id.menu_convert_to_issue:
                    final Intent intent = new Intent(getContext(), IssueEditor.class);
                    intent.putExtra(getString(R.string.intent_repo), mParent.mProject.getRepoPath());
                    intent.putExtra(getString(R.string.parcel_card), card);
                    UI.getViewCenterOnScreen(intent, view);
                    getActivity().startActivityForResult(intent, IssueEditor.REQUEST_CODE_ISSUE_FROM_CARD);
                    break;
                case R.id.menu_edit_issue:
                    showIssueEditor(view, card);
                    break;
                case R.id.menu_delete_issue_card:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.title_close_issue);
                    builder.setMessage(R.string.text_close_issue_on_delete);
                    builder.setPositiveButton(R.string.action_yes, (dialogInterface, j) -> {
                        mEditor.closeIssue(null, mParent.mProject.getRepoPath(), card.getIssue().getNumber());
                        mParent.deleteCard(card, false);
                    });
                    builder.setNeutralButton(R.string.action_cancel, null);
                    builder.setNegativeButton(R.string.action_no, (dialogInterface, j) -> mParent.deleteCard(card, false));
                    final Dialog deleteDialog = builder.create();
                    deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                    deleteDialog.show();
                    break;
                case 1:
                    toggleIssueState(card);
                    break;
            }

            return true;
        });
        if(card.hasIssue()) {
            popup.inflate(R.menu.menu_card_issue);
            popup.getMenu().add(0, 1, 0, card.getIssue().isClosed() ? R.string.menu_reopen_issue : R.string.menu_close_issue);
        } else {
            popup.inflate(R.menu.menu_card);
        }

        popup.show();
    }

    void newCard(Card card) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.createCard(new Editor.CardCreationListener() {
            int createAttempts = 0;

            @Override
            public void cardCreated(int columnId, Card card) {
                addCard(card);
                mParent.mRefresher.setRefreshing(false);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
            }

            @Override
            public void cardCreationError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(createAttempts < 5) {
                        createAttempts++;
                        mEditor.createCard(this, mColumn.getId(), card.getNote());
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_CREATION, bundle);
            }
        }, mColumn.getId(), card.getNote());
    }

    void editCard(Card card) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.updateCard(new Editor.CardUpdateListener() {
            int updateAttempts = 0;

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
            public void cardUpdateError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(updateAttempts < 5) {
                        updateAttempts++;
                        mEditor.updateCard(this, card.getId(), card.getNote());
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_EDIT, bundle);
            }
        }, card.getId(), card.getNote());
    }

    private void toggleIssueState(Card card) {
        final Editor.IssueStateChangeListener listener = new Editor.IssueStateChangeListener() {
            int stateChangeAttempts = 0;

            @Override
            public void issueStateChanged(Issue issue) {
                card.setFromIssue(issue);
                mAdapter.updateCard(card);
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(issue.isClosed() ? Analytics.TAG_ISSUE_CLOSED : Analytics.TAG_ISSUE_OPENED, bundle);
            }

            @Override
            public void issueStateChangeError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();

                } else {
                    if(stateChangeAttempts < 5) {
                        if(card.getIssue().isClosed()) {
                            stateChangeAttempts++;
                            mEditor.openIssue(this, mParent.mProject.getRepoPath(), card.getIssueId());
                        } else {
                            mEditor.closeIssue(this, mParent.mProject.getRepoPath(), card.getIssueId());
                        }
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        };


        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.title_state_change_comment);
        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(card.getIssue().isClosed()) {
                    mEditor.openIssue(listener, card.getIssue().getRepoPath(), card.getIssue().getNumber());
                } else {
                    mEditor.closeIssue(listener, card.getIssue().getRepoPath(), card.getIssue().getNumber());
                }
                final Intent i = new Intent(getContext(), CommentEditor.class);
                i.putExtra(getString(R.string.parcel_issue), card.getIssue());
                getActivity().startActivityForResult(i, CommentEditor.REQUEST_CODE_COMMENT_FOR_STATE);

            }
        });
        builder.setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(card.getIssue().isClosed()) {
                    mEditor.openIssue(listener, card.getIssue().getRepoPath(), card.getIssue().getNumber());
                } else {
                    mEditor.closeIssue(listener, card.getIssue().getRepoPath(), card.getIssue().getNumber());
                }
            }
        });
        builder.setNeutralButton(R.string.action_cancel, null);
        builder.create().show();
    }

    private void showIssueEditor(View view, Card card) {
        final Intent i = new Intent(getContext(), IssueEditor.class);
        i.putExtra(getString(R.string.intent_repo), mParent.mProject.getRepoPath());
        i.putExtra(getString(R.string.parcel_card), card);
        i.putExtra(getString(R.string.parcel_issue), card.getIssue());
        UI.getViewCenterOnScreen(i, view);
        getActivity().startActivityForResult(i, IssueEditor.REQUEST_CODE_EDIT_ISSUE);
    }

    private void editIssue(Card card, Issue issue, @Nullable String[] assignees, @Nullable String[] labels) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.editIssue(new Editor.IssueEditListener() {
            int issueCreationAttempts = 0;

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
            public void issueEditError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                } else {
                    if(issueCreationAttempts < 5) {
                        issueCreationAttempts++;
                        mEditor.editIssue(this, issue.getRepoPath(), issue, assignees, labels);
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_ISSUE_EDIT, bundle);
            }
        }, card.getIssue().getRepoPath(), issue, assignees, labels);
    }

    private void convertCardToIssue(Card oldCard, Issue issue) {
        mEditor.deleteCard(new Editor.CardDeletionListener() {
            int cardDeletionAttempts = 0;

            @Override
            public void cardDeleted(Card card) {
                createIssueCard(issue, oldCard.getId());
                resetLastUpdate();
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_SUCCESS);
                mAnalytics.logEvent(Analytics.TAG_CARD_DELETION, bundle);
            }

            @Override
            public void cardDeletionError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                } else {
                    if(cardDeletionAttempts < 5) {
                        cardDeletionAttempts++;
                        mEditor.deleteCard(this, oldCard);
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }

                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_DELETION, bundle);
            }
        }, oldCard);
    }

    void createIssueCard(Issue issue) {
        createIssueCard(issue, -1);
    }

    private void createIssueCard(Issue issue, int oldCardId) {
        mParent.mRefresher.setRefreshing(true);
        mEditor.createCard(new Editor.CardCreationListener() {
            int issueCardCreationAttempts = 0;

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
            public void cardCreationError(APIHandler.APIError error) {
                if(error == APIHandler.APIError.NO_CONNECTION) {
                    mParent.mRefresher.setRefreshing(false);
                    Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                } else {
                    if(issueCardCreationAttempts < 5) {
                        issueCardCreationAttempts++;
                        mEditor.createCard(this, mColumn.getId(), issue.getId());
                    } else {
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                        mParent.mRefresher.setRefreshing(false);
                    }
                }
                final Bundle bundle = new Bundle();
                bundle.putString(Analytics.KEY_EDIT_STATUS, Analytics.VALUE_FAILURE);
                mAnalytics.logEvent(Analytics.TAG_CARD_TO_ISSUE, bundle);
            }
        }, mColumn.getId(), issue.getId());
    }

    private void copyToClipboard(String text) {
        final ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Card", text));
        Toast.makeText(mParent, getString(R.string.text_copied_to_board), Toast.LENGTH_SHORT).show();
    }

    private void showFullscreen(Card card) {
        final FullScreenDialog dialog = new FullScreenDialog();
        final Bundle b = new Bundle();
        b.putString(getString(R.string.intent_markdown), card.getNote());
        b.putString(getString(R.string.intent_repo), mParent.mProject.getRepoPath());
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), TAG);
    }

    void cardClick(View view, Card card) {
        final SettingsActivity.Preferences.CardAction action;
        if(mAccessLevel == Repository.AccessLevel.NONE || mAccessLevel == Repository.AccessLevel.READ) {
            action = COPY;
        } else {
            action = SettingsActivity.Preferences.getPreferences(getContext()).getCardAction();
        }
        switch(action) {
            case EDIT:
                if(card.hasIssue()) {
                    showIssueEditor(view, card);
                } else {
                    final Intent i = new Intent(getContext(), CardEditor.class);
                    i.putExtra(getString(R.string.parcel_card), card);
                    UI.getViewCenterOnScreen(i, view);
                    getActivity().startActivityForResult(i, CardEditor.REQUEST_CODE_EDIT_CARD);
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

    void openUser(View view, String login) {
        final Intent i = new Intent(getContext(), UserActivity.class);
        i.putExtra(getString(R.string.intent_username), login);
        if(view instanceof ANImageView) {
            if(((ANImageView) view).getDrawable() != null) {
                Log.i(TAG, "openUser: Putting bitmap");
                i.putExtra(getString(R.string.intent_drawable), ((BitmapDrawable) ((ANImageView) view).getDrawable()).getBitmap());
            }
            startActivity(i, ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    view,
                    getString(R.string.transition_user_image)
                    ).toBundle()
            );
        } else {
            UI.getViewCenterOnScreen(i, view);
            startActivity(i);
        }
    }

    void openIssue(View view, String url) {
        final int number = Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
        final Intent i = new Intent(getContext(), IssueActivity.class);
        i.putExtra(getString(R.string.intent_repo), mParent.mProject.getRepoPath());
        i.putExtra(getString(R.string.intent_issue_number), number);
        UI.getViewCenterOnScreen(i, view);
        startActivity(i);
    }

    void scrollUp() {
        final LinearLayoutManager lm = (LinearLayoutManager) mRecycler.getLayoutManager();
        final int pos = lm.findFirstVisibleItemPosition();
        final int height = mRecycler.getChildAt(pos).getHeight();
        mNestedScroller.smoothScrollBy(0, -height);
        Log.i(TAG, "scrollUp: Scrolling up");
        //mNestedScroller.smoothScrollTo(0, Math.max(mNestedScroller.getScrollY() - height, 0));
    }

    void scrollDown() {
        final LinearLayoutManager lm = (LinearLayoutManager) mRecycler.getLayoutManager();
        final int pos = lm.findLastVisibleItemPosition();
        final int height = mRecycler.getChildAt(pos).getHeight();
        mNestedScroller.smoothScrollBy(0, height);
        Log.i(TAG, "scrollDown: Scrolling down");
        //mNestedScroller.smoothScrollTo(0, Math.min(mNestedScroller.getScrollY() + height, mNestedScroller.getChildAt(0).getHeight()));
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
