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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Comment;

/**
 * Created by theo on 03/01/17.
 */

public class CommentDialog extends KeyboardDismissingDialogFragment {

    private CommentDialogListener mListener;
    private boolean mShouldDisplayNeutralButton;
    private int titleRes = R.string.title_new_comment;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_comment, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setView(view);
        builder.setTitle(titleRes);

        final EditText body = (EditText) view.findViewById(R.id.comment_body_edit);
        final TextInputLayout wrapper = (TextInputLayout) view.findViewById(R.id.comment_body_wrapper);
        final MarkedView md = (MarkedView) view.findViewById(R.id.comment_body_markdown);

        if(getArguments() != null && getArguments().containsKey(getString(R.string.intent_comment))) {
            final Comment c = getArguments().getParcelable(getString(R.string.intent_comment));
            if(c != null) {
                body.setText(c.getBody());
                md.setMDText(c.getBody());
            }
        }

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
            if(mListener != null) mListener.onPositive(body.getText().toString());
        });

        if(mShouldDisplayNeutralButton) {
            builder.setNeutralButton(R.string.action_no, (dialogInterface, i) -> {
                if(mListener != null) mListener.onCancelled();
            });
            wrapper.setHint(getString(R.string.hint_comment_state_change));
        } else {
            wrapper.setHint(getString(R.string.hint_comment_new));
        }

        builder.setNegativeButton(R.string.action_cancel, (d, i) -> {});

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(body, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }

    public void setTitleResource(@StringRes int resId) {
        titleRes = resId;
    }

    public void enableNeutralButton() {
        mShouldDisplayNeutralButton = true;
    }

    public void setListener(CommentDialogListener listener) {
        mListener = listener;
    }

    public interface CommentDialogListener {

        void onPositive(String body);

        void onCancelled();

    }

}
