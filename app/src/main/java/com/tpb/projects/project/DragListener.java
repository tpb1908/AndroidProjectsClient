package com.tpb.projects.project;

import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;
import android.view.View;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Card;

import java.util.ArrayList;

/**
 * Created by theo on 21/12/16.
 */

class DragListener implements View.OnDragListener {

    private boolean isDropped = false;

    DragListener() {
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        final int action = event.getAction();
        switch(action) {
            case DragEvent.ACTION_DRAG_STARTED:
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                break;
            case DragEvent.ACTION_DROP:
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
            case DragEvent.ACTION_DRAG_ENDED:
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
