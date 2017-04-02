package com.tpb.projects.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;

import com.tpb.projects.R;
import com.tpb.projects.editors.KeyboardDismissingDialogFragment;

/**
 * Created by theo on 01/01/17.
 */

public class ShortcutDialog extends KeyboardDismissingDialogFragment {

    private ShortcutDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getArguments().getInt(getString(R.string.intent_title_res)));
        final View view = getActivity().getLayoutInflater()
                                       .inflate(R.layout.dialog_save_shortcut, null);
        builder.setView(view);
        final EditText name = (EditText) view.findViewById(R.id.shortcut_name);
        name.setText(getArguments().getString(getString(R.string.intent_name)));

        final boolean showCheckbox = getArguments().getBoolean(getString(R.string.intent_drawable));
        if(showCheckbox)
            view.findViewById(R.id.shortcut_use_user_image).setVisibility(View.VISIBLE);

        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            if(mListener != null && !name.getText().toString().isEmpty()) {
                if(showCheckbox) {
                    mListener.onPositive(name.getText().toString(),
                            ((CheckBox) view.findViewById(R.id.shortcut_use_user_image)).isChecked()
                    );
                } else {
                    mListener.onPositive(name.getText().toString(), false);
                }
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);

        final Dialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            //Show keyboard
            final InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
        });

        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    public void setListener(ShortcutDialogListener listener) {
        mListener = listener;
    }

    public interface ShortcutDialogListener {

        void onPositive(String name, boolean iconFlag);

    }
}
