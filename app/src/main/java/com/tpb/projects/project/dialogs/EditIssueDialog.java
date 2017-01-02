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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

/**
 * Created by theo on 28/12/16.
 */

public class EditIssueDialog extends KeyboardDismissingDialogFragment {
    private static final String TAG = EditIssueDialog.class.getSimpleName();

    private EditText title;
    private EditText body;
    private EditIssueDialogListener mListener;

    private String repoFullName;
    private HtmlTextView assigneesText;
    private TextView labelsText;
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
        md.setMDText(issue.getBody());

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
        assigneesText = (HtmlTextView) view.findViewById(R.id.issue_assignees_text);
        assigneesText.setShowUnderLines(false);

        if(issue.getAssignees() != null) {
            for(User u : issue.getAssignees()) {
                assignees.add(u.getLogin());
            }
        }
        setAssigneesText();

        setAssigneesButton.setOnClickListener((v) -> showAssigneesDialog());

        final Button setLabelsButton = (Button) view.findViewById(R.id.issue_add_labels_button);
        labelsText = (TextView) view.findViewById(R.id.issue_labels_text);
        setLabelsButton.setOnClickListener((v) -> showLabelsDialog());

        if(issue.getLabels() != null) {
            final ArrayList<String> names = new ArrayList<>();
            final ArrayList<Integer> colours = new ArrayList<>();
            for(Label l : issue.getLabels()) {
                names.add(l.getName());
                colours.add(l.getColor());
            }
            setLabelsText(names, colours);
        }


        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);
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

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(title, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }

    private void setAssigneesText() {
        final StringBuilder builder = new StringBuilder();
        for(String a : assignees) {
            builder.append(String.format(getContext().getString(R.string.text_href), "https://github.com/" + a, a));
            builder.append("<br>");
        }
        assigneesText.setHtml(builder.toString());
    }

    private void setLabelsText(ArrayList<String> names, ArrayList<Integer> colors) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        selectedLabels.clear();
        for(int i = 0; i < names.size(); i++) {
            selectedLabels.add(names.get(i));
            final SpannableString s = new SpannableString(names.get(i));
            s.setSpan(new ForegroundColorSpan(colors.get(i)), 0, names.get(i).length(), 0);
            builder.append(s);
            builder.append('\n');
        }
        labelsText.setText(builder, TextView.BufferType.SPANNABLE);
    }

    private void showAssigneesDialog() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(R.string.text_loading_collaborators);
        pd.setCancelable(false);
        pd.show();
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
                pd.dismiss();
                mcd.show(getFragmentManager(), TAG);
            }

            @Override
            public void collaboratorsLoadError() {

            }
        }, repoFullName);
    }

    private void showLabelsDialog() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setTitle(R.string.text_loading_labels);
        pd.setCancelable(false);
        pd.show();
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
                        selectedLabels.clear();
                        final ArrayList<String> labels = new ArrayList<>();
                        final ArrayList<Integer> colours = new ArrayList<>();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                selectedLabels.add(choices[i]);
                                labels.add(choices[i]);
                                colours.add(colors[i]);
                            }
                        }
                        setLabelsText(labels, colours);
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                pd.dismiss();
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

    public void setListener(EditIssueDialogListener listener) {
        mListener = listener;
    }

    public interface EditIssueDialogListener {

        void issueEdited(Issue issue, @Nullable String[] assignees, @Nullable String[] labels);

        void issueEditCancelled();

    }

}
