package com.tpb.projects.editors;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.tpb.github.data.models.Project;
import com.tpb.mdtext.Markdown;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.projects.R;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.input.SimpleTextChangeWatcher;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 25/03/17.
 */

public class ProjectEditor extends EditorActivity {

    public static final int REQUEST_CODE_NEW_PROJECT = 4591;
    public static final int REQUEST_CODE_EDIT_PROJECT = 1932;

    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.project_description_edit) MarkdownEditText mDescriptionEditor;
    @BindView(R.id.project_name_edit) EditText mNameEditor;

    private int mProjectNumber = -1;
    private boolean mHasBeenEdited = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(
                prefs.isDarkThemeEnabled() ? R.style.AppTheme_Transparent_Dark : R.style.AppTheme_Transparent);
        setContentView(R.layout.activity_markdown_editor);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_project_editor);
        stub.inflate();
        ButterKnife.bind(this);


        new MarkdownButtonAdapter(this, mEditButtons,
                new MarkdownButtonAdapter.MarkDownButtonListener() {
                    @Override
                    public void snippetEntered(String snippet, int relativePosition) {
                        if(mNameEditor.hasFocus()) {
                            final int start = Math.max(mNameEditor.getSelectionStart(), 0);
                            mNameEditor.getText().insert(start, snippet);
                            mNameEditor.setSelection(start + relativePosition);
                        }
                    }

                    @Override
                    public String getText() {
                        if(mNameEditor.isFocused()) return mNameEditor.getText().toString();
                        if(mDescriptionEditor.isFocused())
                            return mDescriptionEditor.getInputText().toString();
                        return "";
                    }

                    @Override
                    public void previewCalled() {
                        if(mDescriptionEditor.isEditing()) {
                            mDescriptionEditor.saveText();
                            mDescriptionEditor.setMarkdown(
                                    Markdown.formatMD(mDescriptionEditor.getText().toString(),
                                            null
                                    ),
                                    new HttpImageGetter(mDescriptionEditor)
                            );
                            mDescriptionEditor.disableEditing();
                        } else {
                            mDescriptionEditor.restoreText();
                            mDescriptionEditor.enableEditing();
                        }
                    }
                }
        );

        final View content = findViewById(android.R.id.content);
        content.setVisibility(View.VISIBLE);


        mNameEditor.addTextChangedListener(new SimpleTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = mHasBeenEdited || mDescriptionEditor.isEditing();
            }
        });
        mDescriptionEditor.addTextChangedListener(new SimpleTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = true;
            }
        });

        if(getIntent().hasExtra(getString(R.string.parcel_project))) {
            final Project project = getIntent()
                    .getParcelableExtra(getString(R.string.parcel_project));
            mProjectNumber = project.getId();
            mNameEditor.setText(project.getName());
            mDescriptionEditor.setText(project.getBody());
        }
    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent data = new Intent();
        if(mProjectNumber != -1) {
            data.putExtra(getString(R.string.intent_project_number), mProjectNumber);
        }
        data.putExtra(getString(R.string.intent_name), mNameEditor.getText().toString());
        data.putExtra(getString(R.string.intent_markdown), mDescriptionEditor.getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }

    @OnClick(R.id.markdown_editor_discard)
    void onDiscard() {
        finish();
    }

    @Override
    void imageLoadComplete(String url) {
        Util.insertString(mDescriptionEditor, url);
    }

    @Override
    void imageLoadException(IOException ioe) {

    }

    @Override
    protected void emojiChosen(String emoji) {
        Util.insertString(mDescriptionEditor, String.format(":%1$s", emoji));
    }

    @Override
    protected void insertString(String c) {
        Util.insertString(mDescriptionEditor, c);
    }
}
