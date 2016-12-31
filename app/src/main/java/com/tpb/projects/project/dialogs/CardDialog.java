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
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;

import java.util.ArrayList;

/**
 * Created by theo on 23/12/16.
 */

public class CardDialog extends DialogFragment {
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

        if(getArguments().getParcelable(getContext().getString(R.string.parcel_card)) != null) {
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
            issueButton.setOnClickListener(view1 -> new Loader(getContext()).loadOpenIssues(new Loader.IssuesLoader() {
                private int selectedIssuePosition = 0;
                @Override
                public void issuesLoaded(Issue[] loadedIssues) {
                    if(loadedIssues.length == 0) {
                        Toast.makeText(getContext(), R.string.error_no_issues, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final ArrayList<Issue> validIssues = new ArrayList<>();
                    for(Issue i : loadedIssues) {
                        if(invalidIds.indexOf(i.getId()) == -1) validIssues.add(i);
                    }
                    final String[] texts = new String[validIssues.size()];
                    for(int i = 0; i < validIssues.size(); i++) {
                        texts[i] = String.format(getContext().getString(R.string.text_issue_single_line),
                                validIssues.get(i).getNumber(), validIssues.get(i).getTitle());
                    }

                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                    builder1.setTitle(R.string.title_choose_issue);
                    builder1.setSingleChoiceItems(texts, 0, (dialogInterface, i) -> {
                        selectedIssuePosition = i;
                    });
                    builder1.setPositiveButton(R.string.action_ok, ((dialogInterface, i) -> {
                       if(mListener != null) mListener.issueCardCreated(validIssues.get(selectedIssuePosition));
                        dialogInterface.dismiss();
                        CardDialog.this.dismiss();
                    }));
                    builder1.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                    builder1.create().show();
                }

                @Override
                public void issuesLoadError() {

                }
            }, fullRepoName));
            //TODO Allow choosing an open issue
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

        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
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
