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

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Issue;

/**
 * Created by theo on 28/12/16.
 */

public class EditIssueDialog extends DialogFragment {
    private static final String TAG = EditIssueDialog.class.getSimpleName();

    private EditText title;
    private EditText body;
    private EditIssueDialogListener mListener;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_issue, null);

        final MarkedView md = (MarkedView) view.findViewById(R.id.issue_body_markdown);
        title = (EditText) view.findViewById(R.id.issue_title_edit);
        body = (EditText) view.findViewById(R.id.issue_body_edit);

        final Issue issue = getArguments().getParcelable(getContext().getString(R.string.parcel_issue));

        title.setText(issue.getTitle());
        body.setText(issue.getBody());

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

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_new_issue);

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            issue.setTitle(title.getText().toString());
            issue.setBody(body.getText().toString());
            if(mListener != null) mListener.issueEdited(issue);
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.issueEditCancelled();
            dismiss();
        });

        return builder.setView(view).create();
    }

    public void setListener(EditIssueDialogListener listener) {
        mListener = listener;
    }

    public interface EditIssueDialogListener {

        void issueEdited(Issue issue);

        void issueEditCancelled();

    }

}
