package com.tpb.projects.project;

import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Editor;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.Card;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Repository;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.common.NetworkImageView;
import com.tpb.projects.flow.IntentHandler;
import com.tpb.projects.markdown.Formatter;
import com.tpb.projects.util.Analytics;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.tpb.projects.flow.ProjectsApplication.mAnalytics;

/**
 * Created by theo on 20/12/16.
 */

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> implements Loader.ListLoader<Card> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private final ArrayList<Pair<Card, SpannableString>> mCards = new ArrayList<>();

    private final ColumnFragment mParent;
    private int mColumn;
    private final Editor mEditor;
    private static final HandlerThread parseThread = new HandlerThread("card_parser");

    private int mPage = 1;
    private boolean mIsLoading = false;
    private boolean mMaxPageReached = false;

    private SwipeRefreshLayout mRefresher;
    private Loader mLoader;

    static {
        parseThread.start();
    }

    private static final Handler mParseHandler = new Handler(parseThread.getLooper());
    private Repository.AccessLevel mAccessLevel;
    private final ProjectActivity.NavigationDragListener mNavListener;

    CardAdapter(ColumnFragment parent,
                ProjectActivity.NavigationDragListener navListener,
                Repository.AccessLevel accessLevel,
                SwipeRefreshLayout refresher) {
        mParent = parent;
        mEditor = Editor.getEditor(mParent.getContext());
        mLoader = Loader.getLoader(parent.getContext());
        mAccessLevel = accessLevel;
        mNavListener = navListener;
        mRefresher = refresher;
        mRefresher.setRefreshing(true);
    }

    void setColumn(int columnId) {
        mColumn = columnId;
        mCards.clear();
        notifyDataSetChanged();
        loadCards(true);
    }

    public void notifyBottomReached() {
        if(!mIsLoading && !mMaxPageReached) {
            mPage++;
            loadCards(false);
        }
    }

    private void loadCards(boolean resetPage) {
        mIsLoading = true;
        mRefresher.setRefreshing(true);
        if(resetPage) {
            mPage = 1;
            mMaxPageReached = false;
            final int oldSize = mCards.size();
            mCards.clear();
            notifyItemRangeRemoved(0, oldSize);
        }
        mLoader.loadCards(this, mParent.mColumn.getId(), mPage);
    }

    @Override
    public void listLoadComplete(List<Card> cards) {
        if(!mParent.isAdded()) return;
        mRefresher.setRefreshing(false);
        mIsLoading = false;
        if(cards.size() > 0) {
            int oldLength = mCards.size();
            if(mPage == 1) {
                mParent.mParent.notifyFragmentLoaded();
            }
            for(Card c : cards) {
                mCards.add(Pair.create(c, null));
            }
            mParent.mCardCount.setText(String.valueOf(mCards.size()));
            notifyItemRangeInserted(oldLength, mCards.size());
        } else {
            mMaxPageReached = true;
        }
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

    }

    void setAccessLevel(Repository.AccessLevel accessLevel) {
        mAccessLevel = accessLevel;
        notifyDataSetChanged();
    }

    void addCard(Card card) {
        mCards.add(0, Pair.create(card, null));
        notifyItemInserted(0);
    }

    void addCardFromDrag(Card card) {
        mCards.add(Pair.create(card, null));
        notifyItemInserted(mCards.size());
        mEditor.moveCard(null, mColumn, card.getId(), -1);
    }

    void addCardFromDrag(int pos, Card card) {
        mCards.add(pos, Pair.create(card, null));
        notifyItemInserted(pos);
        final int id = pos == 0 ? -1 : mCards.get(pos - 1).first.getId();
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), id);
    }

    void updateCard(Card card) {
        final int index = indexOf(card.getId());
        if(index != -1) {
            mCards.set(index, Pair.create(card, null));
            notifyItemChanged(index);
        }
    }

    void updateCard(Card card, int oldId) {
        final int index = indexOf(oldId);
        if(index != -1) {
            mCards.set(index, Pair.create(card, null));
            notifyItemChanged(index);
        }
    }

    void moveCardFromDrag(int oldPos, int newPos) {
        final Pair<Card, SpannableString> card = mCards.get(oldPos);
        mCards.remove(oldPos);
        mCards.add(newPos, card);
        notifyItemMoved(oldPos, newPos);
        final int id = newPos == 0 ? -1 : mCards.get(newPos - 1).first.getId();
        mEditor.moveCard(null, mParent.mColumn.getId(), card.first.getId(), id);
    }

    void removeCard(Card card) {
        final int index = indexOf(card.getId());
        if(index != -1) {
            mCards.remove(index);
            notifyItemRemoved(index);
        }
        //API call is handled in adapter to which card is added
    }

    int indexOf(int cardId) {
        for(int i = 0; i < mCards.size(); i++) {
            if(mCards.get(i).first.getId() == cardId) return i;
        }
        return -1;
    }

    List<Card> getCards() {
        final List<Card> cards = new ArrayList<>();
        for(Pair<Card, SpannableString> p : mCards) cards.add(p.first);
        return cards;
    }

    private void openMenu(View view, int position) {
        mParent.openMenu(view, mCards.get(position).first);
    }

    private void cardClick(CardHolder holder) {
        mParent.cardClick(holder.mText, mCards.get(holder.getAdapterPosition()).first);
    }

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CardHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.viewholder_card, parent, false));
    }

    @Override
    public void onBindViewHolder(CardHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Card card = mCards.get(pos).first;
        if(mAccessLevel == Repository.AccessLevel.ADMIN || mAccessLevel == Repository.AccessLevel.WRITE) {
            holder.mCardView.setTag(card.getId());
            holder.mCardView.setOnLongClickListener(view -> {
                final ClipData data = ClipData.newPlainText("", "");
                final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view.startDragAndDrop(data, shadowBuilder, view, 0);
                } else {
                    view.startDrag(data, shadowBuilder, view, 0);
                }
                view.setVisibility(View.INVISIBLE);
                return true;
            });
            holder.mCardView.setOnDragListener(
                    new CardDragListener(mParent.getContext(), mNavListener)
            );
        } else {
            holder.mMenuButton.setVisibility(View.GONE);
        }

        if(card.requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.ItemLoader<Issue>() {
                int loadCount = 0;

                @Override
                public void loadComplete(Issue data) {
                    if(mParent.isAdded() && !mParent.isRemoving()) {
                        mCards.get(pos).first.setFromIssue(data);
                        notifyItemChanged(pos);
                    }

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
                    mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    if(error != APIHandler.APIError.NO_CONNECTION) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
                        mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                        loadCount++;
                        if(loadCount < 5) {
                            mParent.loadIssue(this, card.getIssueId());
                        }
                    }
                }
            }, card.getIssueId());
        } else if(card.hasIssue()) {
            bindIssueCard(holder, pos);
        } else {
            bindStandardCard(holder, pos);
        }

    }

    private void bindStandardCard(CardHolder holder, int pos) {
        holder.mTitleLayout.setVisibility(View.GONE);
        if(mCards.get(pos).second == null) {
            final Card card = mCards.get(pos).first;
            holder.mText.setMarkdown(
                    Markdown.formatMD(
                            card.getNote(),
                            mParent.mParent.mProject.getRepoPath()
                    ),
                    new HttpImageGetter(holder.mText),
                    text -> mCards.set(pos, Pair.create(card, text))
            );
        } else {
            holder.mText.setText(mCards.get(pos).second);
        }
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mText);
    }

    private void bindIssueCard(CardHolder holder, int pos) {
        holder.mIssueIcon.setVisibility(View.VISIBLE);
        holder.mUserAvatar.setVisibility(View.VISIBLE);
        final Card card = mCards.get(pos).first;
        holder.mIssueIcon.setImageResource(
                card.getIssue().isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(card.getIssue().getOpenedBy().getAvatarUrl());
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mUserAvatar,
                card.getIssue().getOpenedBy().getLogin()
        );
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mText, holder.mUserAvatar,
                holder.mCardView, card.getIssue()
        );
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mIssueIcon, card.getIssue()
        );
        IntentHandler.addOnClickHandler(mParent.getActivity(), holder.mTitle, card.getIssue());
        holder.mTitleLayout.setVisibility(View.VISIBLE);

        holder.mTitle.setMarkdown(Formatter.bold(card.getIssue().getTitle()));
        if(mCards.get(pos).second == null) {
            holder.mText.setMarkdown(
                    Formatter.buildIssueSpan(
                            holder.itemView.getContext(),
                            card.getIssue(),
                            false,
                            true,
                            true,
                            true,
                            true
                    ).toString(),
                    new HttpImageGetter(holder.mText),
                    text -> mCards.set(pos, Pair.create(card, text))
            );
        } else {
            holder.mText.setText(mCards.get(pos).second);
        }
        holder.mSpinner.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.card_markdown) MarkdownTextView mText;
        @BindView(R.id.card_title) MarkdownTextView mTitle;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;
        @BindView(R.id.viewholder_card) CardView mCardView;
        @BindView(R.id.card_menu_button) View mMenuButton;
        @BindView(R.id.card_drawable_wrapper) View mTitleLayout;
        @BindView(R.id.card_issue_drawable) ImageView mIssueIcon;
        @BindView(R.id.card_user_avatar) NetworkImageView mUserAvatar;

        @OnClick(R.id.card_menu_button)
        void onMenuClick(View v) {
            openMenu(v, getAdapterPosition());
        }

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(v -> cardClick(this));
            mText.setOnClickListener(v -> cardClick(this));
            //mText.setParseHandler(mParseHandler);
        }

    }

}
