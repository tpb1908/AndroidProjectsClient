package com.tpb.projects.dialogs;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.SettingsActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 07/02/17.
 */

public class IssueEditor extends AppCompatActivity {
    private static final String TAG = IssueEditor.class.getSimpleName();


    @BindView(R.id.issue_title_edit) TextView mTitleEdit;
    @BindView(R.id.issue_add_assignees_button) Button mAssigneesButton;
    @BindView(R.id.issue_add_labels_button) Button mLabelsButton;

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

        final View content = findViewById(android.R.id.content);

        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                content.getWindowVisibleDisplayFrame(r);
                int screenHeight = content.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                Log.d(TAG, "keypadHeight = " + keypadHeight);

                if(keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    Log.i(TAG, "onGlobalLayout: Keyboard open");
                    mAssigneesButton.setVisibility(View.GONE);
                    mLabelsButton.setVisibility(View.GONE);
                    // keyboard is opened
                }
                else {
                    Log.i(TAG, "onGlobalLayout: Keyboard closed");
                    mAssigneesButton.setVisibility(View.VISIBLE);
                    mLabelsButton.setVisibility(View.VISIBLE);
                    // keyboard is closed
                }
            }
        });

    }
}
