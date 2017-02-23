package com.tpb.projects.editors;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.error.ANError;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.Uploader;
import com.tpb.projects.data.models.Card;
import com.tpb.projects.data.models.Issue;
import com.tpb.projects.data.models.Label;
import com.tpb.projects.util.Data;
import com.tpb.projects.util.KeyBoardVisibilityChecker;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 13/02/17.
 */

public class CardEditor extends ImageLoadingActivity {
    private static final String TAG = CardEditor.class.getSimpleName();

    public static final int REQUEST_CODE_NEW_CARD = 1606;
    public static final int REQUEST_CODE_EDIT_CARD = 7180;

    @BindView(R.id.card_note_edit) EditText mEditor;
    @BindView(R.id.card_from_issue_button) Button mIssueButton;
    @BindView(R.id.card_clear_issue_button) Button mClearButton;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;
    @BindView(R.id.card_note_wrapper) TextInputLayout mEditorWrapper;
    private KeyBoardVisibilityChecker mKeyBoardChecker;


    private Card mCard;

    private boolean mHasBeenEdited = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Transparent_Dark : R.style.AppTheme_Transparent);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_card_editor);
        stub.inflate();
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();

        if(launchIntent.hasExtra(getString(R.string.parcel_card))) { //We are editing
            mCard = launchIntent.getParcelableExtra(getString(R.string.parcel_card));
            mEditor.setText(mCard.getNote());
        } else {
            mCard = new Card();
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
                        if(isClosing()) return;;
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
                            mCard.setFromIssue(validIssues.get(selectedIssuePosition));
                            mEditor.setFilters(new InputFilter[] {});
                            bindIssue(mCard.getIssue());
                            mEditor.setEnabled(false);
                            mEditorWrapper.setCounterEnabled(false);
                            mClearButton.setVisibility(View.VISIBLE);
                        }));
                        scBuilder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> dialogInterface.dismiss());
                        scBuilder.create().show();
                    }

                    @Override
                    public void issuesLoadError(APIHandler.APIError error) {
                        if(isClosing()) return;
                        pd.dismiss();
                        Toast.makeText(CardEditor.this, error.resId, Toast.LENGTH_SHORT).show();
                    }
                }, fullRepoName);
            });

            mClearButton.setOnClickListener((v) -> {
                mEditor.setText("");
                mEditor.setFilters(new InputFilter[] {new InputFilter.LengthFilter(250)});
                mEditorWrapper.setCounterEnabled(true);
                mCard = new Card();
                mClearButton.setVisibility(View.GONE);
            });
        }

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
            @Override
            public void snippetEntered(String snippet, int relativePosition) {
                if(mEditor.hasFocus() && mEditor.isEnabled()) {
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

        final View content = findViewById(android.R.id.content);
        content.setVisibility(View.VISIBLE);

        mKeyBoardChecker = new KeyBoardVisibilityChecker(content, new KeyBoardVisibilityChecker.KeyBoardVisibilityListener() {
            @Override
            public void keyboardShown() {
                mIssueButton.setVisibility(View.GONE);
            }

            @Override
            public void keyboardHidden() {
                if(mIssueButton.hasOnClickListeners()) {
                    mIssueButton.postDelayed(() -> mIssueButton.setVisibility(View.VISIBLE), 100);
                }
            }
        });

        mEditor.addTextChangedListener(new TextWatcher() {
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
        });

    }

    private void bindIssue(Issue issue) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<h1>");
        builder.append(issue.getTitle().replace("\n", "</h1><h1>")); //h1 won't do multiple lines
        builder.append("</h1>");
        builder.append("\n");
        if(issue.getBody() != null && issue.getBody().trim().length() > 0) {
            builder.append(issue.getBody().replaceFirst("\\s++$", ""));
            builder.append("\n");
        }
        if(issue.getLabels() != null && issue.getLabels().length > 0) {
            Label.appendLabels(builder, issue.getLabels(), "   ");
        }
        builder.append("\n");
        builder.append(
                String.format(
                        getString(R.string.text_opened_this_issue),
                        String.format(getString(R.string.text_href),
                                "https://github.com/" + issue.getOpenedBy().getLogin(),
                                issue.getOpenedBy().getLogin()
                        ),
                        DateUtils.getRelativeTimeSpanString(issue.getCreatedAt())
                )
        );
        mEditor.setText(Html.fromHtml(Data.parseMD(builder.toString(), issue.getRepoPath())));
    }

    @Override
    void imageLoadComplete(String image64) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> mUploadDialog.show());
        new Uploader().uploadImage(new Uploader.ImgurUploadListener() {
            @Override
            public void imageUploaded(String link) {
                Log.i(TAG, "imageUploaded: Image uploaded " + link);
                mUploadDialog.cancel();
                final String snippet = String.format(getString(R.string.text_image_link), link);
                final int start = Math.max(mEditor.getSelectionStart(), 0);
                mEditor.getText().insert(start, snippet);
                mEditor.setSelection(start + snippet.indexOf("]"));
            }

            @Override
            public void uploadError(ANError error) {

            }
        }, image64, (bytesUploaded, totalBytes) -> mUploadDialog.setProgress(Math.round((100 * bytesUploaded) / totalBytes)));
    }

    @Override
    void imageLoadException(IOException ioe) {

    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent done = new Intent();

        mCard.setNote(mEditor.getText().toString());
        done.putExtra(getString(R.string.parcel_card), mCard);

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
        Log.i(TAG, "finish: Card finish called");
        if(mHasBeenEdited) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_discard_changes);
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
            if(mKeyBoardChecker.isKeyboardOpen()) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
                mDoneButton.postDelayed(super::finish, 150);
            } else {
                super.finish();
            }
        }
    }
}
