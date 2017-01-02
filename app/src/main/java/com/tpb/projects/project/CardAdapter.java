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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.data.models.User;
import com.tpb.projects.util.Analytics;
import com.tpb.projects.util.Data;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 20/12/16.
 */

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private ArrayList<Card> mCards = new ArrayList<>();
    private AndDown md = new AndDown();
    private ColumnFragment mParent;
    private Editor mEditor;
    private Repository.AccessLevel mAccessLevel;

    CardAdapter(ColumnFragment parent, Repository.AccessLevel accessLevel) {
        mParent = parent;
        mEditor = new Editor(mParent.getContext());
        mAccessLevel = accessLevel;
    }

    void setAccessLevel(Repository.AccessLevel accessLevel) {
        mAccessLevel = accessLevel;
        notifyDataSetChanged();
    }

    void setCards(ArrayList<Card> cards) {
        mCards = cards;
        notifyDataSetChanged();
    }

    void addCard(Card card) {
        mCards.add(0, card);
        notifyItemInserted(0);
    }

    void addCardFromDrag(Card card) {
        mCards.add(card);
        notifyItemInserted(mCards.size());
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), -1);
    }

    void addCardFromDrag(int pos, Card card) {
        Log.i(TAG, "createCard: Card being added to " + pos);
        mCards.add(pos, card);
        notifyItemInserted(pos);
        final int id = pos == 0 ? -1 : mCards.get(pos - 1).getId();
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), id);
    }

    void updateCard(Card card) {
        final int index = indexOf(card.getId());
        if(index != -1) {
            mCards.set(index, card);
            notifyItemChanged(index);
        }
    }

    void updateCard(Card card, int oldId) {
        final int index = indexOf(oldId);
        if(index != -1) {
            mCards.set(index, card);
            notifyItemChanged(index);
        }
    }

    void moveCardFromDrag(int oldPos, int newPos) {
        final Card card = mCards.get(oldPos);
        mCards.remove(oldPos);
        mCards.add(newPos, card);
        notifyItemMoved(oldPos, newPos);
        final int id = newPos == 0 ? -1 : mCards.get(newPos - 1).getId();
        mEditor.moveCard(null, mParent.mColumn.getId(), card.getId(), id);
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
            if(mCards.get(i).getId() == cardId) return i;
        }
        return -1;
    }

    ArrayList<Card> getCards() {
        return mCards;
    }

    private void openMenu(View view, int position) {
        mParent.openMenu(view, mCards.get(position));
    }

    private void cardClick(int position) {
        mParent.cardClick(mCards.get(position));
    }

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CardHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_card, parent, false));
    }

    @Override
    public void onBindViewHolder(CardHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        final Card card = mCards.get(pos);
        holder.mCardView.setAlpha(1.0f);
        holder.mSpinner.setVisibility(View.INVISIBLE);
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
            holder.mCardView.setOnDragListener(new CardDragListener(mParent.getContext()));
        } else {
            holder.mMenuButton.setVisibility(View.GONE);
        }
        if(card.hasIssue()) {
            holder.mIssueIcon.setVisibility(View.VISIBLE);
            holder.mIssueIcon.setImageResource(card.getIssue().isClosed() ? R.drawable.ic_issue_closed : R.drawable.ic_issue_open);
        } else {
            holder.mIssueIcon.setVisibility(View.GONE);
        }

        if(card.requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.IssueLoader() {
                @Override
                public void issueLoaded(Issue issue) {
                    card.setFromIssue(issue);
                    new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(pos));

                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_SUCCESS);
                    mParent.mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                }

                @Override
                public void issueLoadError() {
                    final Bundle bundle = new Bundle();
                    bundle.putString(Analytics.KEY_LOAD_STATUS, Analytics.VALUE_FAILURE);
                    mParent.mAnalytics.logEvent(Analytics.TAG_ISSUE_LOADED, bundle);
                }
            }, card.getIssueId());
        } else if(card.hasIssue()){

            final StringBuilder builder = new StringBuilder();
            builder.append(formatMD(card.getIssue().getTitle()));
            builder.append("<br>");
            if(card.getIssue().getBody() != null && !card.getIssue().getBody().isEmpty()) {
                builder.append(formatMD(card.getIssue().getBody()));
                builder.append("<br>");
            }

            builder.append(String.format(mParent.getString(R.string.text_opened_by),
                    String.format(mParent.getString(R.string.text_md_link),
                            "#" + Integer.toString(card.getIssue().getNumber()),
                            "https://github.com/" + mParent.mParent.mProject.getRepoFullName() + "/issues/" + Integer.toString(card.getIssue().getNumber())),
                    String.format(mParent.getString(R.string.text_md_link),
                            card.getIssue().getOpenedBy().getLogin(),
                            card.getIssue().getOpenedBy().getHtmlUrl()))
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

            if(card.getIssue().getClosedBy() != null) {
                builder.append("<br>");
                builder.append(String.format(mParent.getString(R.string.text_closed_by_link),
                        card.getIssue().getClosedBy().getLogin(),
                        card.getIssue().getClosedBy().getHtmlUrl(),
                        Data.timeAgo(card.getIssue().getClosedAt())));
            }

            if(card.getIssue().getLabels() != null && card.getIssue().getLabels().length > 0) {
                builder.append("<br>");
                Label.appendLabels(builder, card.getIssue().getLabels(), "   ");
            }
            if(card.getIssue().getComments() > 0) {
                builder.append("<br>");
                builder.append(mParent.getResources().getQuantityString(R.plurals.text_issue_comment_count, card.getIssue().getComments(), card.getIssue().getComments()));
            }
            holder.mText.setHtml(md.markdownToHtml(builder.toString()), new HtmlHttpImageGetter(holder.mText));
        } else {
            holder.mText.setHtml(
                    md.markdownToHtml(
                            formatMD(card.getNote())
                    ),
                    new HtmlHttpImageGetter(holder.mText)
            );
        }
    }

    private String formatMD(String s) {
        final StringBuilder builder = new StringBuilder();
        char p = ' ';
        char pp = ' ';
        final char[] cs = s.toCharArray();
        for(int i = 0; i < s.length(); i++) {
            if(pp != '\n' && cs[i] == '\n') {
                builder.append("\n");
            }
            if(cs[i] == '@' && (p == ' '  || p == '\n')) {
                //Max username length is 39 characters
                //Usernames can be alphanumeric with single hyphens
                i = parseUsername(builder, cs, i);
            } else if(cs[i] == '#'  && (p == ' '  || p == '\n')) {
                i = parseIssue(builder, cs, i);
            } else {
                builder.append(cs[i]);
            }
            pp = p;
            p = cs[i];
        }
        Log.i(TAG, "formatMD: " + builder.toString());
        return builder.toString();
    }

    private static int parseUsername(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder nameBuilder = new StringBuilder();
        char p = ' ';
        for(int i = ++pos; i < cs.length; i++) {
            if(((cs[i] >= 'A' && cs[i] <= 'Z') ||
                    (cs[i] >= '0' && cs[i] <= '9') ||
                    (cs[i] >= 'a' && cs[i] <= 'z') ||
                    (cs[i] == '-' && p != '-')) &&
                    i - pos < 38 &&
                    i != cs.length - 1) {
                nameBuilder.append(cs[i]);
                p = cs[i];
                //nameBuilder.length() > 0 stop us linking a single @
            } else if((cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1) && nameBuilder.length() > 0) {
                if(i == cs.length - 1) {
                    nameBuilder.append(cs[i]); //Otherwise we would miss the last char of the name
                }
                builder.append("[@");
                builder.append(nameBuilder.toString());
                builder.append(']');
                builder.append('(');
                builder.append("https://github.com/");
                builder.append(nameBuilder.toString());
                builder.append(')');
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("@");
                return --pos;
            }

        }
        builder.append("@");
        return --pos;
    }

    private int parseIssue(StringBuilder builder, char[] cs, int pos) {
        final StringBuilder numBuilder = new StringBuilder();
        for(int i = ++pos; i < cs.length; i++) {
            if(cs[i] >= '0' && cs[i] <= '9' && i != cs.length - 1) {
                numBuilder.append(cs[i]);
            } else if(cs[i] == ' ' || cs[i] == '\n' || i == cs.length - 1) {
                if(i == cs.length - 1) {
                    if(cs[i] >= '0' && cs[i] <= '9') {
                        numBuilder.append(cs[i]);
                    } else {
                        builder.append("#");
                        return --pos;
                    }
                }
                builder.append("[#");
                builder.append(numBuilder.toString());
                builder.append("]");
                builder.append("(");
                builder.append("https://github.com/");
                builder.append(mParent.mParent.mProject.getRepoFullName());
                builder.append("/issues/");
                builder.append(numBuilder.toString());
                builder.append(")");
                if(i != cs.length - 1) {
                    builder.append(cs[i]); // We still need to append the space or newline
                }
                return i;
            } else {
                builder.append("#");
                return --pos;
            }
        }
        builder.append("#");
        return --pos;
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

        @OnClick(R.id.card_menu_button)
        void onMenuClick(View v) {
            openMenu(v, getAdapterPosition());
        }

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(v -> cardClick(getAdapterPosition()));
            mText.setShowUnderLines(false);
        }

    }

}
