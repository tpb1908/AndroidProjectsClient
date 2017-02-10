package com.tpb.projects.dialogs;

import android.app.ProgressDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.SettingsActivity;
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


    @BindView(R.id.issue_title_edit) TextView mTitleEdit;
    @BindView(R.id.markdown_editor_discard) Button mDiscardButon;
    @BindView(R.id.markdown_editor_done) Button mDoneButton;
    @BindView(R.id.issue_labels_text) TextView mLabelsText;
    @BindView(R.id.issue_assignees_text) HtmlTextView mAssigneesText;
    @BindView(R.id.issue_information_layout) View mInfoLayout;
    @BindView(R.id.markdown_edit_buttons) LinearLayout mEditButtons;

    private final ArrayList<String> mAssignees = new ArrayList<>();
    private final ArrayList<String> mSelectedLabels = new ArrayList<>();

    private String repoFullName;

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

        content.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            final Rect r = new Rect();
            content.getWindowVisibleDisplayFrame(r);
            final int screenHeight = content.getRootView().getHeight();

            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            final int keypadHeight = screenHeight - r.bottom;

            Log.d(TAG, "keypadHeight = " + keypadHeight);

            if(keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                Log.i(TAG, "onGlobalLayout: Keyboard open");
                mInfoLayout.setVisibility(View.GONE);
                // keyboard is opened
            }
            else {
                Log.i(TAG, "onGlobalLayout: Keyboard closed");
                mInfoLayout.setVisibility(View.VISIBLE);
                // keyboard is closed
            }
        });

        new MarkdownButtonAdapter(this, mEditButtons, new MarkdownButtonAdapter.MarkDownButtonListener() {
            @Override
            public void snippetEntered(String snippet) {

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
        mAssigneesText.setHtml(builder.toString());
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
        mLabelsText.setText(builder, TextView.BufferType.SPANNABLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
