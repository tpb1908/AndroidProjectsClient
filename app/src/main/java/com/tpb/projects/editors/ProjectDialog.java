package com.tpb.projects.editors;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mittsu.markedview.MarkedView;
import com.tpb.projects.R;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.util.input.DumbTextChangeWatcher;

/**
 * Created by theo on 17/12/16.
 */

public class ProjectDialog extends DialogFragment {
    private static final String TAG = ProjectDialog.class.getSimpleName();

    private ProjectListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_project, null);
        final EditText nameEdit = (EditText) view.findViewById(R.id.project_name_edit);
        final EditText descriptionEdit = (EditText) view.findViewById(R.id.project_description_edit);
        final MarkedView descriptionMarkDown = (MarkedView) view.findViewById(R.id.project_description_markdwon);
        final Project project;
        final boolean isNewProject;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);

        if(getArguments() != null && getArguments().getParcelable(getContext().getString(R.string.parcel_project)) != null) {
            project = getArguments().getParcelable(getContext().getString(R.string.parcel_project));
            builder.setTitle(R.string.title_edit_project);
            nameEdit.setText(project.getName());
            descriptionEdit.setText(project.getBody());
            descriptionMarkDown.setMDText(project.getBody());
            isNewProject = false;
        } else {
            project = new Project();
            builder.setTitle(R.string.title_new_project);
            isNewProject = true;
        }

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            project.setName(nameEdit.getText().toString());
            project.setBody(descriptionEdit.getText().toString());
            if(mListener != null) mListener.projectEditDone(
                    project,
                    isNewProject
            );
            dismiss();
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(mListener != null) mListener.projectEditCancelled();
            dismiss();
        });
        builder.setOnDismissListener(dialogInterface -> {
            if(mListener != null) mListener.projectEditCancelled();
        });
        descriptionEdit.addTextChangedListener(new DumbTextChangeWatcher() {
            final Handler updateHandler = new Handler();
            long lastUpdated;

            @Override
            public void textChanged() {
                lastUpdated = System.currentTimeMillis();
                updateHandler.postDelayed(() -> {
                    if(System.currentTimeMillis() - lastUpdated >= 190) {
                        descriptionMarkDown.setMDText(descriptionEdit.getText().toString());
                        descriptionMarkDown.reload();
                    }
                }, 200);
            }
        });

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            final InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(nameEdit, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    public void setListener(ProjectListener listener) {
        mListener = listener;
    }

    public interface ProjectListener {

        void projectEditDone(Project project, boolean isNewProject);

        void projectEditCancelled();

    }

}
