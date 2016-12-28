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
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.auth.GitHubSession;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

/**
 * Created by theo on 28/12/16.
 */

public class NewIssueDialog extends DialogFragment {
    private static final String TAG = NewIssueDialog.class.getSimpleName();

    private EditText title;
    private EditText body;
    private String repoFullName;

    private IssueDialogListener mListener;

    public void setListener(IssueDialogListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_issue, null);

        final MarkedView md = (MarkedView) view.findViewById(R.id.issue_body_markdown);
        title = (EditText) view.findViewById(R.id.issue_title_edit);
        body = (EditText) view.findViewById(R.id.issue_body_edit);


        final Card card = getArguments().getParcelable(getContext().getString(R.string.parcel_card));
        repoFullName = getArguments().getString(getContext().getString(R.string.intent_repo));

        final String[] text = card.getNote().split("\n", 2);
        if(text[0].length() > 140) {
            text[1] = text[0].substring(140) + text[1];
            text[0] = text[0].substring(0, 140);
        }
        title.setText(text[0]);
        if(text.length > 1) {
            md.setMDText(text[1]);
            body.setText(text[1]);
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

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_new_issue);

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {});
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.issueCreationCancelled();
            dismiss();
        });

        return builder.setView(view).create();
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        new Handler().postDelayed(() -> {
            ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view ->
                    new Editor(getContext()).createIssue(new Editor.IssueCreationListener() {
                             @Override
                             public void issueCreated(Issue issue) {
                                 Toast.makeText(getContext(), R.string.text_issue_created, Toast.LENGTH_SHORT).show();
                                 if(mListener != null) mListener.issueCreated(issue);
                                 Log.i(TAG, "issueCreated: " + issue.toString());
                                 dismiss();
                             }

                             @Override
                             public void issueCreationError() {

                             }
                         },
                            repoFullName,
                            title.getText().toString(),
                            body.getText().toString(),
                            new String[] {GitHubSession.getSession(getContext()).getUsername()}
                    )
            );
        }, 100);
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    public interface IssueDialogListener {

        void issueCreated(Issue issue);

        void issueCreationCancelled();
    }

}
