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
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;

/**
 * Created by theo on 03/01/17.
 */

public class NewCommentDialog extends KeyboardDismissingDialogFragment {

    private NewCommentDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_comment, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setView(view);
        builder.setTitle(R.string.title_new_comment);

        final EditText body = (EditText) view.findViewById(R.id.comment_body_edit);
        final MarkedView md = (MarkedView) view.findViewById(R.id.comment_body_markdown);


        body.addTextChangedListener(new TextWatcher() {
            final Handler updateHandler = new Handler();
            long lastUpdated;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                lastUpdated = System.currentTimeMillis();
                updateHandler.postDelayed(() -> {
                    if(System.currentTimeMillis() - lastUpdated >= 190) {
                        md.setMDText(body.getText().toString());
                        md.reload();
                    }
                }, 200);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            if(mListener != null) mListener.commentCreated(body.getText().toString());
        });

        builder.setNeutralButton(R.string.action_no, (dialogInterface, i) -> {
            if(mListener != null) mListener.commentNotCreated();
        });

        builder.setNegativeButton(R.string.action_cancel, (d, i) -> {});

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(body, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }

    public void setListener(NewCommentDialogListener listener) {
        mListener = listener;
    }

    public interface NewCommentDialogListener {

        void commentCreated(String body);

        void commentNotCreated();

    }

}
