package com.tpb.projects.editors;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.inputmethod.InputMethodManager;

import com.tpb.projects.R;

/**
 * Created by theo on 31/12/16.
 */

public abstract class KeyboardDismissingDialogFragment extends DialogFragment {


    @Override
    public void dismiss() {
        //Check if a text input is focused
        if(getDialog().getCurrentFocus() != null) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            //Close the keyboard with the window token of the focused text input
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        super.dismiss();
    }

    //Does the same as dismiss
    @Override
    public void dismissAllowingStateLoss() {
        if(getDialog().getCurrentFocus() != null) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        super.dismissAllowingStateLoss();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Enable the slide in animation
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

}
