package com.tpb.projects.editors;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.androidnetworking.error.ANError;
import com.tpb.projects.R;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.Uploader;
import com.tpb.projects.data.models.Comment;
import com.tpb.projects.data.models.Issue;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 14/02/17.
 */

public class CommentEditor extends ImageLoadingActivity {
    private static final String TAG = CommentEditor.class.getSimpleName();

    public static final int REQUEST_CODE_NEW_COMMENT = 1799;
    public static final int REQUEST_CODE_EDIT_COMMENT = 5734;
    public static final int REQUEST_CODE_COMMENT_FOR_STATE = 1400;

    @BindView(R.id.comment_body_edit) EditText mEditor;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButton;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;

    private boolean mHasBeenEdited;

    private Comment mComment;
    private Issue mIssue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_markdown_editor);

        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_comment_editor);
        stub.inflate();

        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();


        if(launchIntent.hasExtra(getString(R.string.parcel_comment))) {
            mComment = launchIntent.getParcelableExtra(getString(R.string.parcel_comment));
            mEditor.setText(mComment.getBody());
        }
        if(launchIntent.hasExtra(getString(R.string.parcel_issue))) {
            mIssue = launchIntent.getParcelableExtra(getString(R.string.parcel_issue));
        }

        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mHasBeenEdited = true;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
            @Override
            public void snippetEntered(String snippet, int relativePosition) {
                insertString(snippet, relativePosition);
            }

            @Override
            public String getText() {
                return mEditor.getText().toString();
            }
        });

    }

    private void insertString(String snippet, int relativePosition) {
        if(mEditor.hasFocus() && mEditor.isEnabled()) {
            final int start = Math.max(mEditor.getSelectionStart(), 0);
            mEditor.getText().insert(start, snippet);
            mEditor.setSelection(start + relativePosition);
        }
    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent done = new Intent();
        if(mComment == null) mComment = new Comment();
        mComment.setBody(mEditor.getText().toString());
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
    void imageLoadComplete(String image64) {
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(() -> mUploadDialog.show());
        new Uploader().uploadImage(new Uploader.ImgurUploadListener() {
            @Override
            public void imageUploaded(String link) {
                Log.i(TAG, "imageUploaded: Image uploaded " + link);
                mUploadDialog.cancel();
                final String snippet = String.format(getString(R.string.text_image_link), link);
                insertString(snippet, snippet.indexOf("]"));
            }

            @Override
            public void uploadError(ANError error) {

            }
        }, image64, (bytesUploaded, totalBytes) -> mUploadDialog.setProgress(Math.round((100 * bytesUploaded) / totalBytes)));
    }

    @Override
    void imageLoadException(IOException ioe) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void finish() {
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
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
            mDoneButton.postDelayed(super::finish, 150);
        }
    }

}
