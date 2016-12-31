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

package com.tpb.projects.project.dialogs;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by theo on 31/12/16.
 */

public abstract class KeyboardDismissingDialogFragment extends DialogFragment {


    @Override
    public void dismiss() {
        if(getDialog().getCurrentFocus() != null) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        super.dismiss();
    }

    @Override
    public void dismissAllowingStateLoss() {
        if(getDialog().getCurrentFocus() != null) {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        super.dismissAllowingStateLoss();
    }


}
