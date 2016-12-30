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
    private int accent;
    private View.OnDragListener mParent;

    CardDragListener(Context context) {
        accent = context.getResources().getColor(R.color.colorAccent);
    }

    CardDragListener(Context context, View.OnDragListener parent) {
        accent = context.getResources().getColor(R.color.colorAccent);
        mParent = parent;
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        if(mParent != null) mParent.onDrag(view, event);
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

                    Log.i(TAG, "onDrag: Dropping onto view of " + view.getHeight() + " with view at " + event.getY());
                    boolean below = event.getY() < view.getHeight() / 2;
                    if(below) {
                        targetPosition = Math.max(0, targetPosition - 1);
                    }
                    Log.i(TAG, "onDrag: Should drop below " + below);
                    if(source != target) {
                        if(targetPosition >= 0) {
                            Log.i(TAG, "onDrag: Adding to position " + targetPosition);
                            targetAdapter.addCardFromDrag(targetPosition, card);
                        } else {
                            targetAdapter.addCardFromDrag(card);
                        }
                        sourceAdapter.removeCard(card);
                    } else if(sourcePosition != targetPosition) { //We are moving a card
                        sourceAdapter.moveCardFromDrag(sourcePosition, targetPosition);
                    }

                } else if(view.getId() == R.id.column_recycler && ((RecyclerView) view).getAdapter().getItemCount() == 0) {
                    Log.i(TAG, "onDrag: Drop on the recycler");
                    sourceAdapter.removeCard(card);
                    targetAdapter.addCardFromDrag(card);
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

        if(!isDropped) {
            View vw = (View) event.getLocalState();
            vw.setVisibility(View.VISIBLE);
        }

        return true;
    }
}
