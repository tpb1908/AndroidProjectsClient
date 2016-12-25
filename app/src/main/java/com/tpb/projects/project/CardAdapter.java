package com.tpb.projects.project;

import android.content.ClipData;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

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
    private boolean mCanEdit;

    CardAdapter(ColumnFragment parent, boolean canEdit) {
        mParent = parent;
        mEditor = new Editor(mParent.getContext());
        mCanEdit = canEdit;
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
        if(mCanEdit) {
            holder.mCardView.setTag(mCards.get(pos).getId());
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
                public void issueLoadError() {

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
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_markdown) HtmlTextView mMarkDown;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;
        @BindView(R.id.viewholder_card) CardView mCardView;
        @BindView(R.id.card_menu_button) ImageButton mMenuButton;

        @OnClick(R.id.card_menu_button)
        void onMenuClick(View v) {
            openMenu(v, getAdapterPosition());
        }

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(v -> cardClick(getAdapterPosition()));
        }

    }

}
