package com.tpb.projects.project;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.commonsware.cwac.anddown.AndDown;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Card;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 20/12/16.
 */

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardHolder> {
    private static final String TAG = CardAdapter.class.getSimpleName();

    private ArrayList<Card> mCards = new ArrayList<>();
    private AndDown md = new AndDown();
    private ColumnFragment mParent;

    public CardAdapter(ColumnFragment parent) {
        mParent = parent;
    }

    void setCards(ArrayList<Card> cards) {
        mCards = cards;
        notifyDataSetChanged();
    }

    @Override
    public CardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CardHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_card, parent, false));
    }

    @Override
    public void onBindViewHolder(CardHolder holder, int position) {
        final int pos = holder.getAdapterPosition();
        if(mCards.get(pos).requiresLoadingFromIssue()) {
            holder.mSpinner.setVisibility(View.VISIBLE);
        } else {
            holder.mMarkDown.setText(Html.fromHtml(md.markdownToHtml(mCards.get(holder.getAdapterPosition()).getNote())));
        }
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    class CardHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.card_markdown) TextView mMarkDown;
        @BindView(R.id.card_issue_progress) ProgressBar mSpinner;

        CardHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

    }
}
