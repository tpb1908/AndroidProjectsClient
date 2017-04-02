package com.tpb.projects.project;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.tpb.projects.R;
import com.tpb.projects.data.models.Card;

/**
 * Created by theo on 22/12/16.
 */

class CardDragListener implements View.OnDragListener {
    private static final String TAG = CardDragListener.class.getSimpleName();

    private boolean isDropped = false;
    private Drawable selectedBG;
    private final int accent;
    private final View.OnDragListener mParent;

    CardDragListener(Context context, View.OnDragListener parent) {
        accent = context.getResources().getColor(R.color.colorAccent);
        mParent = parent;
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        if(mParent != null) {
            mParent.onDrag(view, event);
        }
        final int action = event.getAction();
        final View sourceView = (View) event.getLocalState();
        if(sourceView.getId() == R.id.column_card || view.getTag() == sourceView.getTag()) {
            return true;
        }

        switch(action) {
            case DragEvent.ACTION_DROP:
                isDropped = true;
                int sourcePosition, targetPosition;

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

                if(view.getId() == R.id.viewholder_card) {
                    targetPosition = targetAdapter.indexOf((int) view.getTag());
                    Log.i(TAG, "onDrag: Hovering over position " + targetPosition);
                    if(event.getY() < view.getHeight() / 2) {
                        targetPosition = Math.max(0, targetPosition - 1);
                    }
                    if(source != target) {
                        if(targetPosition >= 0) {
                            targetAdapter.addCardFromDrag(targetPosition, card);
                        } else {
                            targetAdapter.addCardFromDrag(card);
                        }
                        sourceAdapter.removeCard(card);
                    } else if(sourcePosition != targetPosition) { //We are moving a card
                        sourceAdapter.moveCardFromDrag(sourcePosition, targetPosition);
                    }

                } else if(view.getId() == R.id.column_recycler && ((RecyclerView) view).getAdapter()
                                                                                       .getItemCount() == 0) {
                    sourceAdapter.removeCard(card);
                    targetAdapter.addCardFromDrag(card);
                }
                view.setBackground(selectedBG);


                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                //  Log.i(TAG, "onDrag: Drag entered");
                if(view.getId() == R.id.viewholder_card
                        || (view.getId() == R.id.column_recycler && ((RecyclerView) view)
                        .getAdapter().getItemCount() == 0)) {
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

        if(!isDropped) {
            View vw = (View) event.getLocalState();
            vw.setVisibility(View.VISIBLE);
        }

        return true;
    }
}
