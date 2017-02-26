package com.tpb.projects.editors;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.data.models.Milestone;
import com.tpb.projects.util.DumbTextChangeWatcher;
import com.tpb.projects.util.KeyBoardVisibilityChecker;
import com.tpb.projects.util.UI;

import java.io.IOException;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 25/02/17.
 */

public class MilestoneEditor extends ImageLoadingActivity implements Loader.GITLoader<Milestone> {
    private static final String TAG = MilestoneEditor.class.getSimpleName();

    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;
    @BindView(R.id.milestone_date_layout) View mDateLayout;
    @BindView(R.id.milestone_clear_date_button) Button mClearDateButton;
    @BindView(R.id.milestone_description_edit) EditText mDescriptionEditor;
    @BindView(R.id.milestone_title_edit) EditText mTitleEditor;
    @BindView(R.id.milestone_due_date) TextView mDueDate;

    private KeyBoardVisibilityChecker mKeyBoardChecker;

    private boolean mHasBeenEdited = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Transparent_Dark : R.style.AppTheme_Transparent);
        setContentView(R.layout.activity_markdown_editor);
        UI.setStatusBarColor(getWindow(), getResources().getColor(R.color.colorPrimaryDark));
        final ViewStub stub = (ViewStub) findViewById(R.id.editor_stub);

        stub.setLayoutResource(R.layout.stub_milestone_editor);
        stub.inflate();
        ButterKnife.bind(this);

        final Intent launchIntent = getIntent();
        if(launchIntent.hasExtra(getString(R.string.parcel_milestone))) {
            final Milestone m = launchIntent.getParcelableExtra(getString(R.string.parcel_milestone));
            loadComplete(m);
        } else if(launchIntent.hasExtra(getString(R.string.intent_repo)) && launchIntent.hasExtra(getString(R.string.intent_milestone_number))) {
            final String repo = launchIntent.getStringExtra(getString(R.string.intent_repo));
            final int number = launchIntent.getIntExtra(getString(R.string.intent_milestone_number), -1);
            Log.i(TAG, "onCreate: Loading milestone "+ number);
            new Loader(this).loadMilestone(this, repo, number);
        } else {
            finish();
        }


        mDueDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(MilestoneEditor.this, (view, year, month, dayOfMonth) -> {

            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
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
                if(mDescriptionEditor.isFocused()) mDescriptionEditor.getText().toString();
                return "";
            }
        });

        final View content = findViewById(android.R.id.content);
        content.setVisibility(View.VISIBLE);

        mKeyBoardChecker = new KeyBoardVisibilityChecker(content, new KeyBoardVisibilityChecker.KeyBoardVisibilityListener() {
            @Override
            public void keyboardShown() {
                mDateLayout.setVisibility(View.GONE);
            }

            @Override
            public void keyboardHidden() {
                mDateLayout.postDelayed(() -> mDateLayout.setVisibility(View.VISIBLE), 100);
            }
        });

        mTitleEditor.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = true;
            }
        });
        mDescriptionEditor.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                mHasBeenEdited = true;
            }
        });
    }

    @Override
    public void loadComplete(Milestone... milestone) {
        Log.i(TAG, "milestoneLoaded: " + milestone[0]);
    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }

    @Override
    void imageLoadComplete(String image64) {

    }

    @Override
    void imageLoadException(IOException ioe) {

    }
}
