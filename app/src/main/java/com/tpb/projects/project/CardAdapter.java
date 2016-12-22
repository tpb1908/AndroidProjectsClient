package com.tpb.projects.project;

import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
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

    private void addCard(Card card) {
        mCards.add(card);
        notifyItemInserted(mCards.size());
    }

    private void addCard(int pos, Card card) {
        Log.i(TAG, "addCard: Card being added to " + pos);
        mCards.add(pos, card);
        notifyItemInserted(pos);
    }

    private void removeCard(Card card) {
        mCards.remove(card);
        notifyDataSetChanged();
    }

    private void moveCard(int oldPos, int newPos) {
        final Card card = mCards.get(oldPos);
        mCards.remove(oldPos);
        mCards.add(newPos, card);
        notifyItemMoved(oldPos, newPos);
    }

    private int indexOf(int cardId) {
        for(int i = 0; i < mCards.size(); i++) {
            if(mCards.get(i).getId() == cardId) return i;
        }
        return -1;
    }

    private ArrayList<Card> getCards() {
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
            view.startDrag(data, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);
            return true;
        });
        holder.mCardView.setOnDragListener(new DragListener());

        if(mCards.get(pos).requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
            mParent.loadIssue(new Loader.IssueLoader() {
                @Override
                public void issueLoaded(Issue issue) {
                    Log.i(TAG, "issueLoaded: " + issue.getTitle());
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

    class DragListener implements View.OnDragListener {

        boolean isDropped = false;
        private DisplayMetrics metrics;
        private Drawable selectedBG;
        private int accent;

        DragListener() {
            metrics = new DisplayMetrics();
            accent = mParent.getContext().getResources().getColor(R.color.colorAccent);
        }

        @Override
        public boolean onDrag(View view, DragEvent event) {
            final int action = event.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    mParent.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    if(event.getX() / metrics.widthPixels > 0.85f) {
                        ((ProjectActivity) mParent.getActivity()).dragRight();
                    } else if(event.getX() / metrics.widthPixels < 0.15f) {
                        ((ProjectActivity) mParent.getActivity()).dragLeft();
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    isDropped = true;
                    int sourcePosition, targetPosition = -1;
                    final View sourceView = (View) event.getLocalState();
                    view.setVisibility(View.VISIBLE);
                    final RecyclerView target;
                    final RecyclerView source = (RecyclerView) sourceView.getParent();

                    final CardAdapter sourceAdapter = (CardAdapter) source.getAdapter();
                    sourcePosition = sourceAdapter.indexOf((int) sourceView.getTag());

                    final Card card = sourceAdapter.getCards().get(sourcePosition);
                    if(view.getId() == R.id.viewholder_card) {
                        target = (RecyclerView) view.getParent();
                    } else {
                        target = (RecyclerView) view;
                    }
                    final CardAdapter targetAdapter = (CardAdapter) target.getAdapter();
                    Log.i(TAG, "onDrag: Dropping onto view at " + view.getY() + " with view at " + sourceView.getY());
                    if(view.getId() == R.id.viewholder_card) {

                        targetPosition = targetAdapter.indexOf((int) view.getTag());

                        //TODO get y positions of each view and decide on which side to add the card
                        if(source != target) {
                            if(targetPosition >= 0) {
                                Log.i(TAG, "onDrag: Adding to position " + targetPosition);
                                targetAdapter.addCard(targetPosition, card);
                            } else {
                                targetAdapter.addCard(card);
                            }
                            sourceAdapter.removeCard(card);
                        } else { //We are moving a card
                            sourceAdapter.moveCard(sourcePosition, targetPosition);
                        }

                    } else if(view.getId() == R.id.column_recycler && ((RecyclerView) view).getAdapter().getItemCount() == 0) {
                        Log.i(TAG, "onDrag: Drop on the recycler");
                        sourceAdapter.removeCard(card);
                        targetAdapter.addCard(card);
                    }
                    view.setBackground(selectedBG);
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                  //  Log.i(TAG, "onDrag: Drag entered");
                    if(view.getId() == R.id.viewholder_card
                            || (view.getId() == R.id.column_recycler && ((RecyclerView) view).getAdapter().getItemCount() == 0)) {
                        selectedBG = view.getBackground();
                        view.setBackgroundColor(accent);
                    }
                    //This is when we have entered another view

                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.i(TAG, "onDrag: Drag exited");
                    view.setBackground(selectedBG);
                    //This is when we have exited another view
                    break;
                default:
                    break;
            }

            if (!isDropped) {
                View vw = (View) event.getLocalState();
                vw.setVisibility(View.VISIBLE);
            }

            return true;
        }
    }


}
