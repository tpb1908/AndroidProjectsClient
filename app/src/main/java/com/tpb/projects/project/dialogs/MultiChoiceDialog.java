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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.util.Log;
import android.widget.ListView;

import com.tpb.projects.R;

/**
 * Created by theo on 29/12/16.
 */

public class MultiChoiceDialog extends DialogFragment {

    private MultiChoiceDialogListener listener;
    private String[] choices;
    private boolean[] checked;
    private ListView listView;
    private int[] colors;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final Bundle arguments = getArguments();
        final int titleRes = arguments.getInt(getContext().getString(R.string.intent_title_res));
        builder.setTitle(titleRes);
        builder.setMultiChoiceItems(choices, checked, (dialogInterface, i, b) -> checked[i] = b);
        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            if(listener != null) listener.ChoicesComplete(choices, checked);
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(listener != null) listener.ChoicesCancelled();
        });
        final AlertDialog dialog = builder.create();
        listView = dialog.getListView();

        Log.i("Test", "onCreateDialog: Does the listview work? " + dialog.getListView());
        dialog.setOnShowListener(dialogInterface -> {
            if(colors != null) {
                for(int i = 0; i < listView.getChildCount() && i < colors.length; i++) {
                    try {
                        ((AppCompatCheckedTextView) listView.getChildAt(i)).setTextColor(colors[i]);
                    } catch(ClassCastException ignored) {}
                }
            }
        });
        return dialog;
    }

    public void setTextColors(int[] colors) {
        this.colors = colors;
    }

    public void setChoices(String[] choices, boolean[] checked) {
        this.choices = choices;
        this.checked = checked;
    }

    public void setListener(MultiChoiceDialogListener listener) {
        this.listener = listener;
    }

    public interface MultiChoiceDialogListener {

        void ChoicesComplete(String[] choices, boolean[] checked);

        void ChoicesCancelled();

    }
}
