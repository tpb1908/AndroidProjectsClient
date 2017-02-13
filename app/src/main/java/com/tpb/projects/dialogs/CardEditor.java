package com.tpb.projects.dialogs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Issue;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 13/02/17.
 */

public class CardEditor extends AppCompatActivity {
    private static final String TAG = CardEditor.class.getSimpleName();

    @BindView(R.id.card_note_edit) EditText mEditor;
    @BindView(R.id.card_from_issue_button) Button mIssueButton;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_card_editor);
        stub.inflate();

        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();

        if(launchIntent.hasExtra(getString(R.string.parcel_card))) { //We are editing

        } else {
            final String fullRepoName = launchIntent.getStringExtra(getString(R.string.intent_repo));
            final ArrayList<Integer> invalidIds = launchIntent.getIntegerArrayListExtra(getString(R.string.intent_int_arraylist));

            mIssueButton.setVisibility(View.VISIBLE);
            mIssueButton.setOnClickListener(view1 -> {
                final ProgressDialog pd = new ProgressDialog(CardEditor.this);
                pd.setTitle(R.string.text_loading_issues);
                pd.setCancelable(false);
                pd.show();
                new Loader(CardEditor.this).loadOpenIssues(new Loader.IssuesLoader() {
                    private int selectedIssuePosition = 0;
                    @Override
                    public void issuesLoaded(Issue[] loadedIssues) {
                        pd.dismiss();
                        final ArrayList<Issue> validIssues = new ArrayList<>();
                        for(Issue i : loadedIssues) {
                            if(invalidIds.indexOf(i.getId()) == -1) validIssues.add(i);
                        }
                        if(validIssues.isEmpty()) {
                            Toast.makeText(CardEditor.this, R.string.error_no_issues, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String[] texts = new String[validIssues.size()];
                        for(int i = 0; i < validIssues.size(); i++) {
                            texts[i] = String.format(getString(R.string.text_issue_single_line),
                                    validIssues.get(i).getNumber(), validIssues.get(i).getTitle());
                        }

                        final AlertDialog.Builder scBuilder = new AlertDialog.Builder(CardEditor.this);
                        scBuilder.setTitle(R.string.title_choose_issue);
                        scBuilder.setSingleChoiceItems(texts, 0, (dialogInterface, i) -> {
                            selectedIssuePosition = i;
                        });
                        scBuilder.setPositiveButton(R.string.action_ok, ((dialogInterface, i) -> {
                            Log.i(TAG, "issuesLoaded: Issue selected: " + validIssues.get(selectedIssuePosition));
                        }));
                        scBuilder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                        scBuilder.create().show();
                    }

                    @Override
                    public void issuesLoadError(APIHandler.APIError error) {
                        pd.dismiss();
                        Toast.makeText(CardEditor.this, error.resId, Toast.LENGTH_SHORT).show();
                    }
                }, fullRepoName);
            });
        }

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
            @Override
            public void snippetEntered(String snippet, int relativePosition) {
                if(mEditor.hasFocus()) {
                    final int start = Math.max(mEditor.getSelectionStart(), 0);
                    mEditor.getText().insert(start, snippet);
                    mEditor.setSelection(start + relativePosition);
                }
            }

            @Override
            public String getText() {
                return mEditor.getText().toString();
            }
        });



    }
}
