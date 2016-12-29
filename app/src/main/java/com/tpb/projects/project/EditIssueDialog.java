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
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.User;

import java.util.ArrayList;

/**
 * Created by theo on 28/12/16.
 */

public class EditIssueDialog extends DialogFragment {
    private static final String TAG = EditIssueDialog.class.getSimpleName();

    private EditText title;
    private EditText body;
    private EditIssueDialogListener mListener;

    private String repoFullName;
    private TextView assigneesText;
    private ArrayList<String> assignees = new ArrayList<>();
    private ArrayList<String> selectedLabels = new ArrayList<>();


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_issue, null);

        final MarkedView md = (MarkedView) view.findViewById(R.id.issue_body_markdown);
        title = (EditText) view.findViewById(R.id.issue_title_edit);
        body = (EditText) view.findViewById(R.id.issue_body_edit);

        final Issue issue = getArguments().getParcelable(getContext().getString(R.string.parcel_issue));
        repoFullName = getArguments().getString(getContext().getString(R.string.intent_repo));

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

        final Button setAssigneesButton = (Button) view.findViewById(R.id.issue_add_assignees_button);
        assigneesText = (TextView) view.findViewById(R.id.issue_assignees_text);
        new Loader(getContext()).loadCollaborators(new Loader.CollaboratorsLoader() {
            @Override
            public void collaboratorsLoaded(User[] collaborators) {
                assignees.clear();
                for(User u : collaborators) {
                    assignees.add(u.getLogin());
                }
                setAssigneesText();
            }

            @Override
            public void collaboratorsLoadError() {

            }
        }, repoFullName);

        setAssigneesButton.setOnClickListener((v) -> showAssigneesDialog());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_edit_issue);

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            issue.setTitle(title.getText().toString());
            issue.setBody(body.getText().toString());
            if(mListener != null) mListener.issueEdited(issue, assignees.toArray(new String[0]), selectedLabels.toArray(new String[0]));
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.issueEditCancelled();
            dismiss();
        });

        return builder.setView(view).create();
    }

    private void setAssigneesText() {
        final StringBuilder builder = new StringBuilder();
        for(String a : assignees) {
            builder.append(a);
            builder.append(' ');
        }

        assigneesText.setText(builder.toString());
    }

    private void showAssigneesDialog() {
        Toast.makeText(getContext(), R.string.text_loading_collaborators, Toast.LENGTH_SHORT).show();
        new Loader(getContext()).loadCollaborators(new Loader.CollaboratorsLoader() {
            @Override
            public void collaboratorsLoaded(User[] collaborators) {
                final MultiChoiceDialog mcd = new MultiChoiceDialog();

                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_assignees);
                mcd.setArguments(b);

                final String[] collabNames = new String[collaborators.length];
                final boolean[] checked = new boolean[collabNames.length];
                for(int i = 0; i < collabNames.length; i++) {
                    collabNames[i] = collaborators[i].getLogin();
                    if(assignees.indexOf(collabNames[i]) != -1) {
                        checked[i] = true;
                    }
                }
                mcd.setChoices(collabNames, checked);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        assignees.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) assignees.add(choices[i]);
                        }
                        setAssigneesText();
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                mcd.show(getFragmentManager(), TAG);
            }

            @Override
            public void collaboratorsLoadError() {

            }
        }, repoFullName);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    public void setListener(EditIssueDialogListener listener) {
        mListener = listener;
    }

    public interface EditIssueDialogListener {

        void issueEdited(Issue issue, @Nullable String[] assignees, @Nullable String[] labels);

        void issueEditCancelled();

    }

}
