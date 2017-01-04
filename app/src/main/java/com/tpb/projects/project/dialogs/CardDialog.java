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
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

import java.util.ArrayList;

/**
 * Created by theo on 23/12/16.
 */

public class CardDialog extends KeyboardDismissingDialogFragment {
    private static final String TAG = CardDialog.class.getSimpleName();

    private CardDialogListener mListener;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_card, null);
        final EditText noteEdit = (EditText) view.findViewById(R.id.card_note_edit);
        final MarkedView md = (MarkedView) view.findViewById(R.id.card_note_markdown);
        final Card card;
        final boolean isNewCard;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);

        if(getArguments().getParcelable(getContext().getString(R.string.parcel_card)) != null) { //We are editing
            card = getArguments().getParcelable(getContext().getString(R.string.parcel_card));
            builder.setTitle(R.string.title_edit_card);
            noteEdit.setText(card.getNote());
            md.setMDText(card.getNote());
            isNewCard = false;
        } else {
            card = new Card();
            builder.setTitle(R.string.title_new_card);
            isNewCard = true;

            final String fullRepoName = getArguments().getString(getContext().getString(R.string.intent_repo));
            final ArrayList<Integer> invalidIds = getArguments().getIntegerArrayList(getContext().getString(R.string.intent_int_arraylist));

            final View issueButton = view.findViewById(R.id.card_from_issue_button);
            issueButton.setVisibility(View.VISIBLE);
            issueButton.setOnClickListener(view1 -> {
                final ProgressDialog pd = new ProgressDialog(getContext());
                pd.setTitle(R.string.text_loading_issues);
                pd.setCancelable(false);
                pd.show();
                new Loader(getContext()).loadOpenIssues(new Loader.IssuesLoader() {
                    private int selectedIssuePosition = 0;
                    @Override
                    public void issuesLoaded(Issue[] loadedIssues) {
                        pd.dismiss();
                        final ArrayList<Issue> validIssues = new ArrayList<>();
                        for(Issue i : loadedIssues) {
                            if(invalidIds.indexOf(i.getId()) == -1) validIssues.add(i);
                        }
                        if(validIssues.isEmpty()) {
                            Toast.makeText(getContext(), R.string.error_no_issues, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String[] texts = new String[validIssues.size()];
                        for(int i = 0; i < validIssues.size(); i++) {
                            texts[i] = String.format(getContext().getString(R.string.text_issue_single_line),
                                    validIssues.get(i).getNumber(), validIssues.get(i).getTitle());
                        }

                        final AlertDialog.Builder scBuilder = new AlertDialog.Builder(getContext());
                        scBuilder.setTitle(R.string.title_choose_issue);
                        scBuilder.setSingleChoiceItems(texts, 0, (dialogInterface, i) -> {
                            selectedIssuePosition = i;
                        });
                        scBuilder.setPositiveButton(R.string.action_ok, ((dialogInterface, i) -> {
                            if(mListener != null) mListener.issueCardCreated(validIssues.get(selectedIssuePosition));
                            dialogInterface.dismiss();
                            CardDialog.this.dismiss();
                        }));
                        scBuilder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                        scBuilder.create().show();
                    }

                    @Override
                    public void issuesLoadError(APIHandler.APIError error) {
                        pd.dismiss();
                        Toast.makeText(getContext(), error.resId, Toast.LENGTH_SHORT).show();
                    }
                }, fullRepoName);
            });
        }

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            card.setNote(noteEdit.getText().toString());
            if(mListener != null) {
                mListener.cardEditDone(card, isNewCard);
            }
            dismiss();
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.cardEditCancelled();
            dismiss();
        });

        noteEdit.addTextChangedListener(new TextWatcher() {
            final Handler updateHandler = new Handler();
            long lastUpdated;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                lastUpdated = System.currentTimeMillis();
                updateHandler.postDelayed(() -> {
                    if(System.currentTimeMillis() - lastUpdated >= 190) {
                        md.setMDText(noteEdit.getText().toString());
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

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(noteEdit, InputMethodManager.SHOW_IMPLICIT);
        });
        return dialog;
    }

    public void setListener(CardDialogListener listener) {
        mListener = listener;
    }

    public interface CardDialogListener {

        void cardEditDone(Card card, boolean isNewCard);

        void issueCardCreated(Issue issue);

        void cardEditCancelled();

    }

}
