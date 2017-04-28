package com.tpb.projects.editors;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.tpb.github.data.models.Comment;
import com.tpb.github.data.models.Issue;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.projects.R;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.input.KeyBoardVisibilityChecker;
import com.tpb.projects.util.input.SimpleTextChangeWatcher;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 14/02/17.
 */

public class CommentEditor extends EditorActivity {

    public static final int REQUEST_CODE_NEW_COMMENT = 1799;
    public static final int REQUEST_CODE_EDIT_COMMENT = 5734;
    public static final int REQUEST_CODE_COMMENT_FOR_STATE = 1400;

    @BindView(R.id.comment_body_edit) MarkdownEditText mEditor;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;
    private KeyBoardVisibilityChecker mKeyBoardChecker;

    private boolean mHasBeenEdited;

    private Comment mComment;
    private Issue mIssue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_comment_editor);
        stub.inflate();

        //Bind after inflating the stub
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();

        if(launchIntent.hasExtra(getString(R.string.parcel_comment))) {
            mComment = launchIntent.getParcelableExtra(getString(R.string.parcel_comment));
            mEditor.setText(mComment.getBody());
        }
        if(launchIntent.hasExtra(getString(R.string.parcel_issue))) {
            mIssue = launchIntent.getParcelableExtra(getString(R.string.parcel_issue));
        }
        mEditor.addTextChangedListener(new SimpleTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited |= mEditor.isEditing();
            }
        });

        new MarkdownButtonAdapter(this, mEditButtons,
                new MarkdownButtonAdapter.MarkdownButtonListener() {
                    @Override
                    public void snippetEntered(String snippet, int relativePosition) {
                        if(mEditor.hasFocus() && mEditor.isEnabled() && mEditor.isEditing()) {
                            Util.insertString(mEditor, snippet, relativePosition);
                        }
                    }

                    @Override
                    public String getText() {
                        return mEditor.getInputText().toString();
                    }

                    @Override
                    public void previewCalled() {
                        if(mEditor.isEditing()) {
                            mEditor.saveText();
                            final String repo = mIssue == null ? null : mIssue.getRepoFullName();
                            mEditor.disableEditing();
                            mEditor.setMarkdown(
                                    Markdown.formatMD(mEditor.getInputText().toString(), repo),
                                    new HttpImageGetter(mEditor)
                            );
                        } else {
                            mEditor.restoreText();
                            mEditor.enableEditing();
                        }
                    }
                }
        );
        mKeyBoardChecker = new KeyBoardVisibilityChecker(findViewById(android.R.id.content));

    }

    @Override
    protected void emojiChosen(String emoji) {
        Util.insertString(mEditor, String.format(":%1$s:", emoji));
    }

    @Override
    protected void insertString(String c) {
        Util.insertString(mEditor, c);
    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent done = new Intent();
        if(mComment == null) mComment = new Comment();
        mComment.setBody(mEditor.getInputText().toString());
        done.putExtra(getString(R.string.parcel_comment), mComment);
        if(mIssue != null) done.putExtra(getString(R.string.parcel_issue), mIssue);
        setResult(RESULT_OK, done);
        mHasBeenEdited = false;
        finish();
    }

    @OnClick(R.id.markdown_editor_discard)
    void onDiscard() {
        onBackPressed();
    }

    @Override
    void imageLoadComplete(String url) {
        Util.insertString(mEditor, url);
    }

    @Override
    void imageLoadException(IOException ioe) {

    }

    @Override
    public void finish() {
        if(mHasBeenEdited && !mEditor.getText().toString().isEmpty()) {
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
