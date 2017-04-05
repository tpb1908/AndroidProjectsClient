package com.tpb.projects.editors;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidnetworking.error.ANError;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.Uploader;
import com.tpb.github.data.models.Card;
import com.tpb.github.data.models.Issue;
import com.tpb.github.data.models.Label;
import com.tpb.github.data.models.User;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.dialogs.CodeDialog;
import com.tpb.mdtext.dialogs.ImageDialog;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.mdtext.views.MarkdownTextView;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.projects.markdown.Spanner;
import com.tpb.projects.util.Logger;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.input.DumbTextChangeWatcher;
import com.tpb.projects.util.input.KeyBoardVisibilityChecker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 07/02/17.
 */

public class IssueEditor extends EditorActivity {
    private static final String TAG = IssueEditor.class.getSimpleName();

    public static final int REQUEST_CODE_NEW_ISSUE = 3025;
    public static final int REQUEST_CODE_EDIT_ISSUE = 1188;
    public static final int REQUEST_CODE_ISSUE_FROM_CARD = 9836;

    @BindView(R.id.issue_title_edit) EditText mTitleEdit;
    @BindView(R.id.issue_body_edit) MarkdownEditText mBodyEdit;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;
    @BindView(R.id.issue_labels_text) MarkdownTextView mLabelsText;
    @BindView(R.id.issue_assignees_text) MarkdownTextView mAssigneesText;
    @BindView(R.id.issue_information_layout) View mInfoLayout;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    private KeyBoardVisibilityChecker mKeyBoardChecker;

    private final ArrayList<String> mAssignees = new ArrayList<>();
    private final ArrayList<String> mSelectedLabels = new ArrayList<>();

    private Card mLaunchCard;
    private Issue mLaunchIssue;

    private String mRepo;

    private boolean mHasBeenEdited = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_issue_editor);
        stub.inflate();
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        mRepo = launchIntent.getStringExtra(getString(R.string.intent_repo));
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
                for(Label l : mLaunchIssue.getLabels()) {
                    labels.add(l.getName());
                    colours.add(l.getColor());
                }
                setLabelsText(labels, colours);
            }

        } else if(launchIntent.hasExtra(getString(R.string.parcel_card))) {
            mLaunchCard = launchIntent.getParcelableExtra(getString(R.string.parcel_card));
            //Split the first line of the card to use as a title
            final String[] text = mLaunchCard.getNote().split("\n", 2);
            //If the title is too long we ellipsize it
            if(text[0].length() > 140) {
                text[1] = "..." + text[0].substring(137) + text[1];
                text[0] = text[0].substring(0, 137) + "...";
            }
            mTitleEdit.setText(text[0]);
            if(text.length > 1) {
                mBodyEdit.setText(text[1]);
            }
        }

        final DumbTextChangeWatcher editWatcher = new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {

                mHasBeenEdited = mHasBeenEdited || mBodyEdit.isEditing();
            }
        };

        mTitleEdit.addTextChangedListener(editWatcher);
        mBodyEdit.addTextChangedListener(editWatcher);
        mBodyEdit.setCodeClickHandler(new CodeDialog(this));
        mBodyEdit.setImageHandler(new ImageDialog(this));

        final View content = findViewById(android.R.id.content);

        mKeyBoardChecker = new KeyBoardVisibilityChecker(content,
                new KeyBoardVisibilityChecker.KeyBoardVisibilityListener() {
                    @Override
                    public void keyboardShown() {
                        mInfoLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void keyboardHidden() {
                        if(mBodyEdit.isEditing()) {
                            mInfoLayout.postDelayed(() -> mInfoLayout.setVisibility(View.VISIBLE),
                                    100
                            );
                        }
                    }
                }
        );

        new MarkdownButtonAdapter(this, mEditButtons,
                new MarkdownButtonAdapter.MarkDownButtonListener() {
                    @Override
                    public void snippetEntered(String snippet, int relativePosition) {
                        mHasBeenEdited = true;
                        //Check which EditText has focus and insert into the correct one
                        if(mTitleEdit.hasFocus()) {
                            final int start = Math.max(mTitleEdit.getSelectionStart(), 0);
                            mTitleEdit.getText().insert(start, snippet);
                            mTitleEdit.setSelection(start + relativePosition);
                        } else if(mBodyEdit.hasFocus() && mBodyEdit.isEditing()) {
                            final int start = Math.max(mBodyEdit.getSelectionStart(), 0);
                            mBodyEdit.getText().insert(start, snippet);
                            Logger.i(TAG,
                                    "snippetEntered: Setting selection " + (start + relativePosition)
                            );
                            mBodyEdit.setSelection(start + relativePosition);
                        }
                    }

                    @Override
                    public String getText() {
                        return mBodyEdit.getInputText().toString();
                    }

                    @Override
                    public void previewCalled() {
                        if(mBodyEdit.isEditing()) {
                            mBodyEdit.saveText();
                            String repo = null;
                            if(mLaunchIssue != null) repo = mLaunchIssue.getRepoFullName();
                            mBodyEdit.disableEditing();
                            mBodyEdit.setMarkdown(
                                    Markdown.formatMD(mBodyEdit.getInputText().toString(), repo),
                                    new HttpImageGetter(mBodyEdit, mBodyEdit)
                            );
                            mInfoLayout.setVisibility(View.GONE);
                        } else {
                            mBodyEdit.restoreText();
                            mBodyEdit.enableEditing();
                            if(!mKeyBoardChecker.isKeyboardOpen())
                                mInfoLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

    }

    @OnClick(R.id.issue_add_assignees_button)
    public void showAssigneesDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_collaborators);
        pd.setCancelable(false);
        pd.show();
        new Loader(this).loadCollaborators(new Loader.ListLoader<User>() {
            @Override
            public void listLoadComplete(List<User> collaborators) {
                final MultiChoiceDialog mcd = new MultiChoiceDialog();
                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_assignees);
                mcd.setArguments(b);

                final String[] names = new String[collaborators.size()];
                final boolean[] checked = new boolean[names.length];
                for(int i = 0; i < names.length; i++) {
                    names[i] = collaborators.get(i).getLogin();
                    if(mAssignees.indexOf(names[i]) != -1) {
                        checked[i] = true;
                    }
                }
                mcd.setChoices(names, checked);
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
                if(isClosing()) return; //Activity has been closed
                pd.dismiss();
                mcd.show(getSupportFragmentManager(), TAG);
            }

            @Override
            public void listLoadError(APIHandler.APIError error) {
                if(isClosing()) return;
                pd.dismiss();
                Toast.makeText(IssueEditor.this, error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepo);
    }

    @OnClick(R.id.issue_add_labels_button)
    public void showLabelsDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(R.string.text_loading_labels);
        pd.setCancelable(false);
        pd.show();
        new Loader(this).loadLabels(new Loader.ListLoader<Label>() {
            @Override
            public void listLoadComplete(List<Label> labels) {
                Logger.i(TAG, "listLoadComplete: " + labels.toString());
                final MultiChoiceDialog mcd = new MultiChoiceDialog();

                final Bundle b = new Bundle();
                b.putInt(getString(R.string.intent_title_res), R.string.title_choose_labels);
                mcd.setArguments(b);

                final String[] labelTexts = new String[labels.size()];
                final int[] colors = new int[labels.size()];
                final boolean[] choices = new boolean[labels.size()];
                for(int i = 0; i < labels.size(); i++) {
                    labelTexts[i] = labels.get(i).getName();
                    colors[i] = labels.get(i).getColor();
                    choices[i] = mSelectedLabels.indexOf(labels.get(i).getName()) != -1;
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
                if(isClosing()) return;
                pd.dismiss();
                mcd.show(getSupportFragmentManager(), TAG);
            }

            @Override
            public void listLoadError(APIHandler.APIError error) {
                if(isClosing()) return;
                pd.dismiss();
                Toast.makeText(IssueEditor.this, error.resId, Toast.LENGTH_SHORT).show();
            }
        }, mRepo);
    }

    private void setAssigneesText() {
        final StringBuilder builder = new StringBuilder();
        for(String a : mAssignees) {
            builder.append(
                    String.format(getString(R.string.text_href), "https://github.com/" + a, a));
            builder.append("<br>");
        }
        if(builder.length() > 0) {
            mAssigneesText.setVisibility(View.VISIBLE);
            mAssigneesText.setMarkdown(builder.toString());
        } else {
            mAssigneesText.setVisibility(View.GONE);
        }
    }

    private void setLabelsText(ArrayList<String> names, ArrayList<Integer> colors) {
        final StringBuilder builder = new StringBuilder();
        mSelectedLabels.clear();
        builder.append("<ul bulleted=\"false\">");
        for(int i = 0; i < names.size(); i++) {
            mSelectedLabels.add(names.get(i));
            builder.append("<li>");
            builder.append(Spanner.getLabelString(names.get(i), colors.get(i)));
            builder.append("</li>");
        }
        builder.append("</ul>");
        if(builder.length() > 0) {
            mLabelsText.setVisibility(View.VISIBLE);
            mLabelsText.setMarkdown(builder.toString());
        } else {
            mLabelsText.setVisibility(View.GONE);
        }

    }

    @Override
    void imageLoadComplete(String image64) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> mUploadDialog.show());
        new Uploader().uploadImage(new Uploader.ImgurUploadListener() {
                                       @Override
                                       public void imageUploaded(String link) {
                                           mUploadDialog.cancel();
                                           final String snippet = String.format(getString(R.string.text_image_link), link);
                                           Util.insertString(mBodyEdit, snippet);
                                       }

                                       @Override
                                       public void uploadError(ANError error) {

                                       }
                                   }, image64, (bUp, bTotal) -> mUploadDialog.setProgress(Math.round((100 * bUp) / bTotal)),
                BuildConfig.IMGUR_CLIENT_ID
        );
    }

    @Override
    void imageLoadException(IOException ioe) {

    }

    @Override
    protected void emojiChosen(String emoji) {
        Util.insertString(mBodyEdit, String.format(":%1$s:", emoji));
    }

    @Override
    protected void insertString(String c) {
        Util.insertString(mBodyEdit, c);
    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent done = new Intent();
        if(mLaunchIssue == null) {
            mLaunchIssue = new Issue();
        }
        mLaunchIssue.setTitle(mTitleEdit.getText().toString());
        mLaunchIssue.setBody(mBodyEdit.getInputText().toString());
        done.putExtra(getString(R.string.parcel_issue), mLaunchIssue);
        if(mLaunchCard != null) done.putExtra(getString(R.string.parcel_card), mLaunchCard);
        if(mSelectedLabels.size() > 0)
            done.putExtra(getString(R.string.intent_issue_labels),
                    mSelectedLabels.toArray(new String[0])
            );
        if(mAssignees.size() > 0)
            done.putExtra(getString(R.string.intent_issue_assignees),
                    mAssignees.toArray(new String[0])
            );

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
        if(mHasBeenEdited && !mBodyEdit.getText().toString().isEmpty() && !mTitleEdit.getText()
                                                                                     .toString()
                                                                                     .isEmpty()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_discard_changes);
            builder.setPositiveButton(R.string.action_yes, (dialogInterface, i) -> {
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
                mDoneButton.postDelayed(super::finish, 150);
            });
            builder.setNegativeButton(R.string.action_no, null);
            final Dialog deleteDialog = builder.create();
            deleteDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            deleteDialog.show();
        } else {
            if(mKeyBoardChecker.isKeyboardOpen()) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
                mDoneButton.postDelayed(super::finish, 150);
            } else {
                super.finish();
            }
        }
    }
}
