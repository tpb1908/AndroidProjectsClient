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
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;

import java.util.ArrayList;

/**
 * Created by theo on 28/12/16.
 */

public class NewIssueDialog extends DialogFragment {
    private static final String TAG = NewIssueDialog.class.getSimpleName();

    private EditText title;
    private EditText body;
    private String repoFullName;
    private TextView assigneesText;
    private TextView labelsText;
    private ArrayList<String> assignees = new ArrayList<>();
    private ArrayList<String> selectedLabels = new ArrayList<>();

    private IssueDialogListener mListener;

    public void setListener(IssueDialogListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_issue, null);

        final MarkedView md = (MarkedView) view.findViewById(R.id.issue_body_markdown);
        title = (EditText) view.findViewById(R.id.issue_title_edit);
        body = (EditText) view.findViewById(R.id.issue_body_edit);

        repoFullName = getArguments().getString(getContext().getString(R.string.intent_repo));

        if(getArguments().containsKey(getString(R.string.parcel_card))) {
            final Card card = getArguments().getParcelable(getContext().getString(R.string.parcel_card));

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

        final Button setAssigneesButton = (Button) view.findViewById(R.id.issue_add_assignees_button);
        assigneesText = (TextView) view.findViewById(R.id.issue_assignees_text);
        setAssigneesButton.setOnClickListener((v) -> showAssigneesDialog());

        final Button setLabelsButton = (Button) view.findViewById(R.id.issue_add_labels_button);
        labelsText = (TextView) view.findViewById(R.id.issue_labels_text);
        setLabelsButton.setOnClickListener((v) -> showLabelsDialog());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_new_issue);

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {});
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.issueCreationCancelled();
            dismiss();
        });

        return builder.setView(view).create();
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new Editor(getContext()).createIssue(
                    new Editor.IssueCreationListener() {
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
                    assignees.toArray(new String[0]),
                    selectedLabels.toArray(new String[0])
            );
        }
    };

    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        new Handler().postDelayed(() -> {
            ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(onClickListener);
        }, 100);
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
                        final StringBuilder builder = new StringBuilder();
                        assignees.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                assignees.add(choices[i]);
                                builder.append(choices[i]);
                                builder.append(' ');
                            }
                        }

                        assigneesText.setText(builder.toString());
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                mcd.show(getFragmentManager(), TAG);
                mcd.setTextColors(new int[]{getContext().getResources().getColor(R.color.github_issue_open)});
            }

            @Override
            public void collaboratorsLoadError() {

            }
        }, repoFullName);
    }

    private void showLabelsDialog() {
        Toast.makeText(getContext(), R.string.text_loading_labels, Toast.LENGTH_SHORT).show();
        new Loader(getContext()).loadLabels(new Loader.LabelsLoader() {
            @Override
            public void labelsLoaded(Label[] labels) {
                final MultiChoiceDialog mcd = new MultiChoiceDialog();

                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_labels);
                mcd.setArguments(b);

                final String[] labelTexts = new String[labels.length];
                final int[] colors = new int[labels.length];
                final boolean[] choices = new boolean[labels.length];
                for(int i = 0; i < labels.length; i++) {
                    labelTexts[i] = labels[i].getName();
                    colors[i] = labels[i].getColor();
                    choices[i] = selectedLabels.indexOf(labels[i].getName()) != -1;
                }


                mcd.setChoices(labelTexts, choices);
                mcd.setTextColors(colors);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        final SpannableStringBuilder builder = new SpannableStringBuilder();
                        selectedLabels.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                selectedLabels.add(choices[i]);
                                final SpannableString s = new SpannableString(choices[i]);
                                s.setSpan(new ForegroundColorSpan(labels[i].getColor()), 0, choices[i].length(), 0);
                                builder.append(s);
                                builder.append(' ');
                            }
                        }
                        labelsText.setText(builder, TextView.BufferType.SPANNABLE);
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                mcd.show(getFragmentManager(), TAG);
            }

            @Override
            public void labelLoadError() {

            }
        }, repoFullName);
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
