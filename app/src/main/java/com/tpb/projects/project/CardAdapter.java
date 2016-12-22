package com.tpb.projects.project;

import android.content.ClipData;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 20/12/16.
 */

class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private ArrayList<Card> mCards = new ArrayList<>();
    private AndDown md = new AndDown();
    private ColumnFragment mParent;

    CardAdapter(ColumnFragment parent) {
        mParent = parent;
    }

    void setCards(ArrayList<Card> cards) {
        mCards = cards;
        notifyDataSetChanged();
    }

    void addCard(Card card) {
        mCards.add(card);
        notifyItemInserted(mCards.size());
    }

    void addCard(int pos, Card card) {
        Log.i(TAG, "addCard: Card being added to " + pos);
        mCards.add(pos, card);
        notifyItemInserted(pos);
    }

    void removeCard(Card card) {
        mCards.remove(card);
        notifyDataSetChanged();
    }

    void moveCard(int oldPos, int newPos) {
        final Card card = mCards.get(oldPos);
        mCards.remove(oldPos);
        mCards.add(newPos, card);
        notifyItemMoved(oldPos, newPos);
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

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CardHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_card, parent, false));
    }

    @Override
    public void onBindViewHolder(CardHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        holder.mCardView.setTag(mCards.get(pos).getId());
        holder.mCardView.setOnLongClickListener(view -> {
            final ClipData data = ClipData.newPlainText("", "");
            final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(data, shadowBuilder, view, 0);
            } else {
                view.startDrag(data, shadowBuilder, view, 0);
            }
            view.setVisibility(View.INVISIBLE);
            return true;
        });
        holder.mCardView.setOnDragListener(new CardDragListener(mParent.getContext()));

        if(mCards.get(pos).requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.IssueLoader() {
                @Override
                public void issueLoaded(Issue issue) {
                    mCards.get(pos).setRequiresLoadingFromIssue(false);
                    mCards.get(pos).setNote(issue.getTitle());
                    holder.mSpinner.setVisibility(View.INVISIBLE);
                    notifyItemChanged(pos);
                }

                @Override
                public void loadError() {

                }
            }, mCards.get(pos).getIssueId());
        } else {
            holder.mMarkDown.setHtml(
                    md.markdownToHtml(
                            mCards.get(pos).getNote()
                    ),
                    new HtmlHttpImageGetter(holder.mMarkDown)
            );
        }
        holder.mCardView.setAlpha(1.0f);
        holder.itemView.setAlpha(1.0f);
    }

    @Override
    public void onViewRecycled(CardHolder holder) {
        super.onViewRecycled(holder);
        holder.mCardView.setAlpha(1.0f);
        holder.itemView.setAlpha(1.0f);
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_markdown) HtmlTextView mMarkDown;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;
        @BindView(R.id.viewholder_card) CardView mCardView;

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }

}
