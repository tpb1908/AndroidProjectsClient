package com.tpb.projects.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.data.models.User;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 07/02/17.
 */

public class IssueEditor extends AppCompatActivity {
    private static final String TAG = IssueEditor.class.getSimpleName();

    public static final int REQUEST_CODE_NEW_ISSUE = 3025;
    public static final int REQUEST_CODE_EDIT_ISSUE = 1188;
    public static final int REQUEST_CODE_ISSUE_FROM_CARD = 9836;

    @BindView(R.id.issue_title_edit) EditText mTitleEdit;
    @BindView(R.id.issue_body_edit) EditText mBodyEdit;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;
    @BindView(R.id.issue_labels_text) TextView mLabelsText;
    @BindView(R.id.issue_assignees_text) HtmlTextView mAssigneesText;
    @BindView(R.id.issue_information_layout) View mInfoLayout;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;

    private final ArrayList<String> mAssignees = new ArrayList<>();
    private final ArrayList<String> mSelectedLabels = new ArrayList<>();

    private Card mLaunchCard;
    private Issue mLaunchIssue;

    private String repoFullName;

    private boolean mHasBeenEdited = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_issue_editor);
        stub.inflate();
        ButterKnife.bind(this);

        mAssigneesText.setShowUnderLines(false);

        final Intent launchIntent = getIntent();
        repoFullName = launchIntent.getStringExtra(getString(R.string.intent_repo));
        if(launchIntent.hasExtra(getString(R.string.parcel_issue))) {
            mLaunchIssue = launchIntent.getParcelableExtra(getString(R.string.parcel_issue));
            mLaunchCard = launchIntent.getParcelableExtra(getString(R.string.parcel_card));
            mTitleEdit.setText(mLaunchIssue.getTitle());
            mBodyEdit.setText(mLaunchIssue.getBody());

            if(mLaunchIssue.getAssignees() != null) {
                for(User u : mLaunchIssue.getAssignees()) mAssignees.add(u.getLogin());
                setAssigneesText();
            }

            if(mLaunchIssue.getLabels() != null && mLaunchIssue.getLabels().length > 0) {
                final ArrayList<String> labels = new ArrayList<>();
                final ArrayList<Integer> colours = new ArrayList<>();
                for(Label l : mLaunchIssue.getLabels())  {
                    labels.add(l.getName());
                    colours.add(l.getColor());
                }
                setLabelsText(labels, colours);
            }

        } else if(launchIntent.hasExtra(getString(R.string.parcel_card))) {
            mLaunchCard = launchIntent.getParcelableExtra(getString(R.string.parcel_card));

            final String[] text = mLaunchCard.getNote().split("\n", 2);
            if(text[0].length() > 140) {
                text[1] = text[0].substring(140) + text[1];
                text[0] = text[0].substring(0, 140);
            }
            mTitleEdit.setText(text[0]);
            if(text.length > 1) {
                mBodyEdit.setText(text[1]);
            }
        }

        final TextWatcher editWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mHasBeenEdited = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        mTitleEdit.addTextChangedListener(editWatcher);
        mBodyEdit.addTextChangedListener(editWatcher);

        final View content = findViewById(android.R.id.content);

        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            final Rect r = new Rect();
            content.getWindowVisibleDisplayFrame(r);
            final int screenHeight = content.getRootView().getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            final int keypadHeight = screenHeight - r.bottom;

            Log.d(TAG, "keypadHeight = " + keypadHeight);

            if(keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                mInfoLayout.setVisibility(View.GONE);
                // keyboard is opened
            }
            else {
                mInfoLayout.postDelayed(() -> mInfoLayout.setVisibility(View.VISIBLE), 100);
                // keyboard is closed
            }
        });

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
            @Override
            public void snippetEntered(String snippet, int relativePosition) {
                mHasBeenEdited = true;
                if(mTitleEdit.hasFocus()) {
                    final int start = Math.max(mTitleEdit.getSelectionStart(), 0);
                    mTitleEdit.getText().insert(start, snippet);
                    mTitleEdit.setSelection(start + relativePosition);
                } else if(mBodyEdit.hasFocus()) {
                    final int start = Math.max(mBodyEdit.getSelectionStart(), 0);
                    mBodyEdit.getText().insert(start, snippet);
                    mBodyEdit.setSelection(start + relativePosition);
                }
            }

            @Override
            public String getText() {
                return mBodyEdit.getText().toString();
            }
        });


    }

    @OnClick(R.id.issue_add_assignees_button)
    public void showAssigneesDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_collaborators);
        pd.setCancelable(false);
        pd.show();
        new Loader(this).loadCollaborators(new Loader.CollaboratorsLoader() {
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
                    if(mAssignees.indexOf(collabNames[i]) != -1) {
                        checked[i] = true;
                    }
                }
                mcd.setChoices(collabNames, checked);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        mAssignees.clear();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) mAssignees.add(choices[i]);
                        }
                        setAssigneesText();
                        mHasBeenEdited = true;
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                pd.dismiss();
                mcd.show(getSupportFragmentManager(), TAG);
            }

            @Override
            public void collaboratorsLoadError(APIHandler.APIError error) {
                pd.dismiss();
                Toast.makeText(IssueEditor.this, error.resId, Toast.LENGTH_SHORT).show();
            }
        }, repoFullName);
    }

    @OnClick(R.id.issue_add_labels_button)
    public void showLabelsDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_labels);
        pd.setCancelable(false);
        pd.show();
        new Loader(this).loadLabels(new Loader.LabelsLoader() {
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
                    choices[i] = mSelectedLabels.indexOf(labels[i].getName()) != -1;
                }

                mcd.setChoices(labelTexts, choices);
                mcd.setTextColors(colors);
                mcd.setListener(new MultiChoiceDialog.MultiChoiceDialogListener() {
                    @Override
                    public void ChoicesComplete(String[] choices, boolean[] checked) {
                        mSelectedLabels.clear();
                        final ArrayList<String> labels = new ArrayList<>();
                        final ArrayList<Integer> colours = new ArrayList<>();
                        for(int i = 0; i < choices.length; i++) {
                            if(checked[i]) {
                                mSelectedLabels.add(choices[i]);
                                labels.add(choices[i]);
                                colours.add(colors[i]);
                            }
                        }
                        setLabelsText(labels, colours);
                        mHasBeenEdited = true;
                    }

                    @Override
                    public void ChoicesCancelled() {

                    }
                });
                pd.dismiss();
                mcd.show(getSupportFragmentManager(), TAG);
            }

            @Override
            public void labelLoadError(APIHandler.APIError error) {
                Toast.makeText(IssueEditor.this, error.resId, Toast.LENGTH_SHORT).show();
                pd.cancel();
            }
        }, repoFullName);
    }

    private void setAssigneesText() {
        final StringBuilder builder = new StringBuilder();
        for(String a : mAssignees) {
            builder.append(String.format(getString(R.string.text_href), "https://github.com/" + a, a));
            builder.append("<br>");
        }
        if(builder.length() > 0) {
            mAssigneesText.setVisibility(View.VISIBLE);
            mAssigneesText.setHtml(builder.toString());
        } else {
            mAssigneesText.setVisibility(View.GONE);
        }
    }

    private void setLabelsText(ArrayList<String> names, ArrayList<Integer> colors) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        mSelectedLabels.clear();
        for(int i = 0; i < names.size(); i++) {
            mSelectedLabels.add(names.get(i));
            final SpannableString s = new SpannableString(names.get(i));
            s.setSpan(new ForegroundColorSpan(colors.get(i)), 0, names.get(i).length(), 0);
            builder.append(s);
            builder.append('\n');
        }
        if(builder.length() > 0) {
            mLabelsText.setVisibility(View.VISIBLE);
            mLabelsText.setText(builder, TextView.BufferType.SPANNABLE);
        } else {
            mLabelsText.setVisibility(View.GONE);
        }

    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent done = new Intent();
        if(mLaunchIssue == null) {
            mLaunchIssue = new Issue();
        }
        mLaunchIssue.setTitle(mTitleEdit.getText().toString());
        mLaunchIssue.setBody(mBodyEdit.getText().toString());
        done.putExtra(getString(R.string.parcel_issue), mLaunchIssue);
        if(mLaunchCard != null) done.putExtra(getString(R.string.parcel_card), mLaunchCard);
        if(mSelectedLabels.size() > 0) done.putExtra(getString(R.string.intent_issue_labels), mSelectedLabels.toArray(new String[0]));
        if(mAssignees.size() > 0) done.putExtra(getString(R.string.intent_issue_assignees), mAssignees.toArray(new String[0]));

        setResult(RESULT_OK, done);
        mHasBeenEdited = false;
        finish();
    }

    @OnClick(R.id.markdown_editor_discard)
    void onDiscard() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void finish() {
        if(mHasBeenEdited) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_discard_issue);
            builder.setPositiveButton(R.string.action_yes, (dialogInterface, i) -> {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
                mDoneButton.postDelayed(super::finish, 150);
            });
            builder.setNegativeButton(R.string.action_no, null);
            final Dialog deleteDialog = builder.create();
            deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            deleteDialog.show();
        } else {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
            mDoneButton.postDelayed(super::finish, 150);
        }
    }
}
