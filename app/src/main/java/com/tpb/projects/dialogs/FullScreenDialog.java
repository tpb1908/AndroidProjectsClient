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

package com.tpb.projects.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.util.Data;

/**
 * Created by theo on 24/12/16.
 */

public class FullScreenDialog extends KeyboardDismissingDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MarkedView view = new MarkedView(getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if(getArguments() != null &&  getArguments().containsKey(getString(R.string.intent_markdown))) {
            String markdown = getArguments().getString(getString(R.string.intent_markdown));
            /*
            WebView does not respect minimum height, so we have to pad the markdown
             */
            if(Data.instancesOf(markdown, "\n") + Data.instancesOf(markdown, "<br>") < 4) {
                markdown = "<br>" + markdown + "<br><br>";
            }

            view.setMDText(markdown);
        }
        return new AlertDialog.Builder(getActivity()).setView(view).create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
