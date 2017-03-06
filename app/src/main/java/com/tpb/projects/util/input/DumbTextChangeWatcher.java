package com.tpb.projects.util.input;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by theo on 24/02/17.
 */

public abstract class DumbTextChangeWatcher implements TextWatcher {

    @Override
    public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public final void afterTextChanged(Editable s) {
    }

    @Override
    public final void onTextChanged(CharSequence s, int start, int before, int count) {
        textChanged();
    }

    public abstract void textChanged();
}
