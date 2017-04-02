package com.tpb.projects.editors;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidnetworking.error.ANError;
import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.Uploader;
import com.tpb.github.data.models.Milestone;
import com.tpb.projects.BuildConfig;
import com.tpb.projects.R;
import com.tpb.mdtext.Markdown;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.UI;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.input.DumbTextChangeWatcher;
import com.tpb.projects.util.input.KeyBoardVisibilityChecker;

import com.tpb.mdtext.dialogs.CodeDialog;
import com.tpb.mdtext.dialogs.ImageDialog;
import com.tpb.mdtext.views.MarkdownEditText;
import com.tpb.mdtext.imagegetter.HttpImageGetter;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by theo on 25/02/17.
 */

public class MilestoneEditor extends EditorActivity implements Loader.ItemLoader<Milestone> {
    private static final String TAG = MilestoneEditor.class.getSimpleName();

    public static final int REQUEST_CODE_NEW_MILESTONE = 810;
    public static final int REQUEST_CODE_EDIT_MILESTONE = 369;

    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.milestone_date_layout) View mDateLayout;
    @BindView(R.id.milestone_clear_date_button) Button mClearDateButton;
    @BindView(R.id.milestone_description_edit) MarkdownEditText mDescriptionEditor;
    @BindView(R.id.milestone_title_edit) EditText mTitleEditor;
    @BindView(R.id.milestone_due_date) TextView mDueDate;

    private ProgressDialog mLoadingDialog;
    private KeyBoardVisibilityChecker mKeyBoardChecker;

    private boolean mIsEditing = false;
    private Milestone mLaunchMilestone;
    private String mFullRepoName;

    private long mDueOn = 0;

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

        stub.setLayoutResource(R.layout.stub_milestone_editor);
        stub.inflate();
        ButterKnife.bind(this);

        mLoadingDialog = new ProgressDialog(this);

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.parcel_milestone))) {
            mIsEditing = true;
            loadComplete(launchIntent.getParcelableExtra(getString(R.string.parcel_milestone)));
        } else if(launchIntent.hasExtra(getString(R.string.intent_repo)) && launchIntent
                .hasExtra(getString(R.string.intent_milestone_number))) {
            mFullRepoName = launchIntent.getStringExtra(getString(R.string.intent_repo));
            final int number = launchIntent
                    .getIntExtra(getString(R.string.intent_milestone_number), -1);
            mLoadingDialog.setTitle(R.string.text_milestone_loading);
            mLoadingDialog.setCanceledOnTouchOutside(false);
            mLoadingDialog.show();
            mIsEditing = true;
            new Loader(this).loadMilestone(this, mFullRepoName, number);
        } else if(launchIntent.hasExtra(getString(R.string.intent_repo))) {
            //TODO Create new milestone
        } else {
            finish();
        }


        mDueDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(MilestoneEditor.this, (view, year, month, dayOfMonth) -> {
                final Calendar chosen = Calendar.getInstance();
                chosen.set(Calendar.YEAR, year);
                chosen.set(Calendar.MONTH, month);
                chosen.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDueOn = chosen.getTimeInMillis();
                mClearDateButton.setVisibility(View.VISIBLE);
                setDueDate();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        new MarkdownButtonAdapter(this, mEditButtons,
                new MarkdownButtonAdapter.MarkDownButtonListener() {
                    @Override
                    public void snippetEntered(String snippet, int relativePosition) {
                        if(mTitleEditor.hasFocus()) {
                            final int start = Math.max(mTitleEditor.getSelectionStart(), 0);
                            mTitleEditor.getText().insert(start, snippet);
                            mTitleEditor.setSelection(start + relativePosition);
                        }
                    }

                    @Override
                    public String getText() {
                        if(mTitleEditor.isFocused()) return mTitleEditor.getText().toString();
                        if(mDescriptionEditor.isFocused())
                            return mDescriptionEditor.getInputText().toString();
                        return "";
                    }

                    @Override
                    public void previewCalled() {
                        if(mDescriptionEditor.isEditing()) {
                            mDescriptionEditor.saveText();
                            mDescriptionEditor.setMarkdown(
                                    Markdown.formatMD(mDescriptionEditor.getText().toString(), null),
                                    new HttpImageGetter(mDescriptionEditor, mDescriptionEditor)
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

        mKeyBoardChecker = new KeyBoardVisibilityChecker(content,
                new KeyBoardVisibilityChecker.KeyBoardVisibilityListener() {
                    @Override
                    public void keyboardShown() {
                        mDateLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void keyboardHidden() {
                        mDateLayout.postDelayed(() -> mDateLayout.setVisibility(View.VISIBLE), 100);
                    }
                }
        );

        mTitleEditor.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = mHasBeenEdited || mDescriptionEditor.isEditing();
            }
        });
        mDescriptionEditor.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = true;
            }
        });
        mDescriptionEditor.setCodeClickHandler(new CodeDialog(this));
        mDescriptionEditor.setImageHandler(new ImageDialog(this));
    }

    @Override
    public void loadComplete(Milestone milestone) {
        mLaunchMilestone = milestone;
        mTitleEditor.setText(milestone.getTitle());
        mLoadingDialog.hide();
        mDescriptionEditor.setFocusable(true);
        mDescriptionEditor.setFocusableInTouchMode(true);
        mDescriptionEditor.setEnabled(true);
        mDescriptionEditor.setText(milestone.getDescription());
        if(milestone.getDueOn() != 0) {
            mDueOn = milestone.getDueOn();
            mClearDateButton.setVisibility(View.VISIBLE);
            setDueDate();
        }
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @OnClick(R.id.milestone_clear_date_button)
    void onClearDate() {
        mDueDate.setText(null);
        mClearDateButton.setVisibility(View.GONE);
        mDueOn = 0;
    }

    private void setDueDate() {
        final java.text.DateFormat df = DateFormat.getLongDateFormat(this);
        mDueDate.setText(df.format(new Date(mDueOn)));
    }

    @OnClick(R.id.markdown_editor_done)
    void onDone() {
        final Intent data = new Intent();
        data.putExtra(getString(R.string.intent_milestone_title),
                mTitleEditor.getText().toString()
        );
        data.putExtra(getString(R.string.intent_milestone_description),
                mDescriptionEditor.getInputText().toString()
        );
        data.putExtra(getString(R.string.intent_milestone_due_on), mDueOn);
        if(mIsEditing) {
            //TODO Check valid state
            data.putExtra(getString(R.string.intent_milestone_number),
                    mLaunchMilestone.getNumber()
            );
        }
        setResult(RESULT_OK, data);
        finish();


    }

    @OnClick(R.id.markdown_editor_discard)
    void onDiscard() {
        finish();
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
                                           final int start = Math.max(mDescriptionEditor.getSelectionStart(), 0);
                                           mDescriptionEditor.getText().insert(start, snippet);
                                           mDescriptionEditor.setSelection(start + snippet.indexOf("]"));
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
        Util.insertString(mDescriptionEditor, String.format(":%1$s", emoji));
    }

    @Override
    protected void characterChosen(String c) {
        Util.insertString(mDescriptionEditor, c);
    }
}
