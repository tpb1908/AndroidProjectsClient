package com.tpb.projects.project;

import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.androidnetworking.widget.ANImageView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.IntentHandler;
import com.tpb.projects.util.MDParser;

import org.sufficientlysecure.htmltext.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltext.dialogs.CodeDialog;
import org.sufficientlysecure.htmltext.dialogs.ImageDialog;
import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 20/12/16.
 */

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private final ArrayList<Pair<Card, SpannableString>> mCards = new ArrayList<>();

    private final ColumnFragment mParent;
    private final Editor mEditor;
    private static final HandlerThread parseThread = new HandlerThread("card_parser");

    static {
        parseThread.start();
    }

    private static final Handler mParseHandler = new Handler(parseThread.getLooper());
    private Repository.AccessLevel mAccessLevel;
    private final ProjectActivity.NavigationDragListener mNavListener;

    CardAdapter(ColumnFragment parent, ProjectActivity.NavigationDragListener navListener, Repository.AccessLevel accessLevel) {
        mParent = parent;
        mEditor = new Editor(mParent.getContext());
        mAccessLevel = accessLevel;
        mNavListener = navListener;
    }

    void setAccessLevel(Repository.AccessLevel accessLevel) {
        mAccessLevel = accessLevel;
        notifyDataSetChanged();
    }

    void setCards(ArrayList<Card> cards) {
        mCards.clear();
        for(Card c : cards) {
            mCards.add(new Pair<>(c, null));
        }
        notifyDataSetChanged();
    }

    void addCard(Card card) {
        mCards.add(0, new Pair<>(card, null));
        notifyItemInserted(0);
    }

    void addCardFromDrag(Card card) {
        mCards.add(new Pair<>(card, null));
        notifyItemInserted(mCards.size());
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), -1);
    }

    void addCardFromDrag(int pos, Card card) {
        Log.i(TAG, "createCard: Card being added to " + pos);
        mCards.add(pos, new Pair<>(card, null));
        notifyItemInserted(pos);
        final int id = pos == 0 ? -1 : mCards.get(pos - 1).first.getId();
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), id);
    }

    void updateCard(Card card) {
        final int index = indexOf(card.getId());
        if(index != -1) {
            mCards.set(index, new Pair<>(card, null));
            notifyItemChanged(index);
        }
    }

    void updateCard(Card card, int oldId) {
        final int index = indexOf(oldId);
        if(index != -1) {
            mCards.set(index, new Pair<>(card, null));
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

    ArrayList<Card> getCards() {
        final ArrayList<Card> cards = new ArrayList<>();
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
        return new CardHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_card, parent, false));
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
            holder.mCardView.setOnDragListener(new CardDragListener(mParent.getContext(), mNavListener));
        } else {
            holder.mMenuButton.setVisibility(View.GONE);
        }

        if(card.requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.GITModelLoader<Issue>() {
                int loadCount = 0;

                @Override
                public void loadComplete(Issue data) {
                    if(mParent.isAdded() && !mParent.isRemoving()) {
                        card.setFromIssue(data);
                        bindIssueCard(holder, pos);
                    }
                    // notifyItemChanged(pos);

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
                    mParent.mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    if(error != APIHandler.APIError.NO_CONNECTION) {
                        final Bundle bundle = new Bundle();
                        bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
                        mParent.mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                        loadCount++;
                        if(loadCount < 5) {
                            mParent.loadIssue(this, card.getIssueId());
                        } //TODO make view tap to try again
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
        holder.mIssueIcon.setVisibility(View.GONE);
        holder.mUserAvatar.setVisibility(View.GONE);
        if(mCards.get(pos).second == null) {
            final Card card = mCards.get(pos).first;
            holder.mText.setHtml(
                    MDParser.parseMD(
                            card.getNote(),
                            mParent.mParent.mProject.getRepoPath()
                    ),
                    new HtmlHttpImageGetter(holder.mText, holder.mText),
                    text -> mCards.set(pos, new Pair<>(card, text))
            );
        } else {
            holder.mText.setText(mCards.get(pos).second);
        }
        IntentHandler.addGitHubIntentHandler(mParent.getActivity(), holder.mText);
    }

    private void bindIssueCard(CardHolder holder, int pos) {
        holder.mIssueIcon.setVisibility(View.VISIBLE);
        holder.mUserAvatar.setVisibility(View.VISIBLE);
        final Card card = mCards.get(pos).first;
        holder.mIssueIcon.setImageResource(card.getIssue().isClosed() ? R.drawable.ic_state_closed : R.drawable.ic_state_open);
        holder.mUserAvatar.setImageUrl(card.getIssue().getOpenedBy().getAvatarUrl());
        IntentHandler.addGitHubIntentHandler(mParent.getActivity(), holder.mUserAvatar, card.getIssue().getOpenedBy().getLogin());
        IntentHandler.addGitHubIntentHandler(mParent.getActivity(), holder.mText, holder.mUserAvatar, card.getIssue());
        if(mCards.get(pos).second == null) {

            final StringBuilder builder = new StringBuilder();
            builder.append("<b>");
            builder.append(MDParser.escape(card.getIssue().getTitle()));
            builder.append("</b><br><br>");
            if(card.getIssue().getBody() != null && !card.getIssue().getBody().isEmpty()) {
                builder.append(MDParser.formatMD(card.getIssue().getBody().replaceFirst("\\s++$", ""), card.getIssue().getRepoPath()));
                builder.append(" \n\n ");
            }

            builder.append(String.format(mParent.getString(R.string.text_issue_opened_by),
                    String.format(mParent.getString(R.string.text_md_link),
                            "#" + Integer.toString(card.getIssue().getNumber()),
                            "https://github.com/" + mParent.mParent.mProject.getRepoPath() + "/issues/" + Integer.toString(card.getIssue().getNumber())
                    ),
                    String.format(mParent.getString(R.string.text_md_link),
                            card.getIssue().getOpenedBy().getLogin(),
                            card.getIssue().getOpenedBy().getHtmlUrl()
                    ),
                    DateUtils.getRelativeTimeSpanString(card.getIssue().getCreatedAt()))
            );

            if(card.getIssue().getAssignees() != null) {
                builder.append("<br>");
                builder.append(mParent.getString(R.string.text_assigned_to));
                builder.append(' ');
                for(User u : card.getIssue().getAssignees()) {
                    builder.append(String.format(mParent.getString(R.string.text_md_link),
                            u.getLogin(),
                            u.getHtmlUrl()));
                    builder.append(' ');
                }
            }

            if(card.getIssue().isClosed() && card.getIssue().getClosedBy() != null) {
                builder.append("<br>");
                builder.append(String.format(mParent.getString(R.string.text_closed_by_link),
                        card.getIssue().getClosedBy().getLogin(),
                        card.getIssue().getClosedBy().getHtmlUrl(),
                        DateUtils.getRelativeTimeSpanString(card.getIssue().getClosedAt())));
            }

            if(card.getIssue().getLabels() != null && card.getIssue().getLabels().length > 0) {
                builder.append("<br>");
                Label.appendLabels(builder, card.getIssue().getLabels(), "   ");
            }
            if(card.getIssue().getComments() > 0) {
                builder.append("<br>");
                builder.append(mParent.getResources().getQuantityString(R.plurals.text_issue_comment_count, card.getIssue().getComments(), card.getIssue().getComments()));
            }
            holder.mText.setHtml(MDParser.parseMD(builder.toString(), card.getIssue().getRepoPath()),
                    new HtmlHttpImageGetter(holder.mText, holder.mText),
                    text -> mCards.set(pos, new Pair<>(card, text))
            );
        } else {
            holder.mText.setText(mCards.get(pos).second);
        }
        holder.mSpinner.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.card_markdown) HtmlTextView mText;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;
        @BindView(R.id.viewholder_card) CardView mCardView;
        @BindView(R.id.card_menu_button) ImageButton mMenuButton;
        @BindView(R.id.card_issue_drawable) ImageView mIssueIcon;
        @BindView(R.id.card_user_avatar) ANImageView mUserAvatar;

        @OnClick(R.id.card_menu_button)
        void onMenuClick(View v) {
            openMenu(v, getAdapterPosition());
        }

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(v -> cardClick(this));
            mText.setShowUnderLines(false);
            mText.setParseHandler(mParseHandler);
            mText.setImageHandler(new ImageDialog(mText.getContext()));
            mText.setCodeClickHandler(new CodeDialog(mText.getContext()));
        }

    }

}
