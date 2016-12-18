package com.tpb.projects.repo;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.tpb.projects.R;
import com.tpb.projects.data.auth.models.Project;

/**
 * Created by theo on 17/12/16.
 */

public class ProjectDialog extends DialogFragment {
    private Project mProject;
    private EditText mNameEdit;
    private EditText mDescriptionEdit;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_project, null);
        mNameEdit = (EditText) view.findViewById(R.id.project_name_edit);
        mDescriptionEdit = (EditText) view.findViewById(R.id.project_description_edit);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);
        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
           dismiss();
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            dismiss();
        });
        if(getArguments() != null && getArguments().getParcelable(getContext().getString(R.string.parcel_project)) != null) {
            mProject = getArguments().getParcelable(getContext().getString(R.string.parcel_project));
            builder.setTitle(R.string.title_edit_project);
            mNameEdit.setText(mProject.getName());
            mDescriptionEdit.setText(mProject.getBody());
        } else {
            mProject = new Project();
            builder.setTitle(R.string.title_new_project);
        }
        return builder.create();
    }



}
