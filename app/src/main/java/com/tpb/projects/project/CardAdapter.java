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
        holder.mCardView.setTag(pos);
        holder.mCardView.setOnLongClickListener(view -> {
            final ClipData data = ClipData.newPlainText("", "");
            final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            view.startDrag(data, shadowBuilder, view, 0);
            view.setVisibility(View.INVISIBLE);
            return true;
        });
        holder.mCardView.setOnDragListener(new DragListener());
        if(mCards.size() == 0) {
            holder.mMarkDown.setText("\nNo cards\n");
            return;
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
                public void loadError() {

                }
            }, mCards.get(pos).getIssueId());
        } else {
            holder.mMarkDown.setHtml(
                    md.markdownToHtml(
                            mCards.get(holder.getAdapterPosition()).getNote()
                    ),
                    new HtmlHttpImageGetter(holder.mMarkDown)
            );
        }

    }

    @Override
    public int getItemCount() {
        return Math.max(1, mCards.size());
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

    private class DragListener implements View.OnDragListener {

        private boolean isDropped = false;
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
            Log.i(TAG, "onDrag: (" + event.getX() + ", " + event.getY() + ")");
            switch(action) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    mParent.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    if(event.getX() / metrics.widthPixels > 0.85f) {
                        Log.i(TAG, "onDrag: Right");
                        ((ProjectActivity) mParent.getActivity()).dragRight();
                    } else if(event.getX() / metrics.widthPixels < 0.15f) {
                        Log.i(TAG, "onDrag: Left");
                        ((ProjectActivity) mParent.getActivity()).dragLeft();
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    Log.i(TAG, "onDrag: Drag drop");
                    isDropped = true;
                    int positionSource, positionTarget = -1;
                    final View viewSource = (View) event.getLocalState();
                    if(view.getId() == R.id.viewholder_card) {
                        final RecyclerView target = (RecyclerView) view.getParent();

                        positionTarget = (int) view.getTag();

                        final RecyclerView source = (RecyclerView) viewSource.getParent();

                        final CardAdapter adapterSource = (CardAdapter) source.getAdapter();
                        positionSource = (int) viewSource.getTag();

                        final Card card = adapterSource.getCards().get(positionSource);
                        final ArrayList<Card> cardsSource = adapterSource.getCards();

                        cardsSource.remove(card);

                        adapterSource.setCards(cardsSource);
                        adapterSource.notifyDataSetChanged();

                        final CardAdapter targetAdapter = (CardAdapter) target.getAdapter();
                        ArrayList<Card> cardsTarget = targetAdapter.getCards();
                        if(positionTarget >= 0) {
                            cardsTarget.add(positionTarget, card);
                        } else {
                            cardsTarget.add(card);
                        }


                        targetAdapter.setCards(cardsTarget);
                        targetAdapter.notifyDataSetChanged();

                        view.setVisibility(View.VISIBLE);

                    }
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.i(TAG, "onDrag: Drag entered");
                    selectedBG = view.getBackground();
                    view.setBackgroundColor(accent);
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
            if(!isDropped) {
                ((View) event.getLocalState()).setVisibility(View.VISIBLE);
            }

            return true;
        }
    }


}
