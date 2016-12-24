package com.tpb.projects.project;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Card;

/**
 * Created by theo on 24/12/16.
 */

public class FullScreenDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MarkedView view = new MarkedView(getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if(getArguments() != null &&  getArguments().getParcelable(getContext().getString(R.string.parcel_card)) != null) {
            view.setMDText( ((Card)getArguments().getParcelable(getContext().getString(R.string.parcel_card))).getNote());
        }
        return new AlertDialog.Builder(getActivity()).setView(view).create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
