package com.tpb.projects.editors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.widget.AbsListView;
import android.widget.ListView;

import com.tpb.projects.R;

/**
 * Created by theo on 29/12/16.
 */

public class MultiChoiceDialog extends KeyboardDismissingDialogFragment {

    private MultiChoiceDialogListener listener;
    private String[] choices;
    private boolean[] checked;
    private ListView listView;
    private int[] colors;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final Bundle arguments = getArguments();
        final int titleRes = arguments.getInt(getContext().getString(R.string.intent_title_res));
        builder.setTitle(titleRes);
        builder.setMultiChoiceItems(choices, checked, (dialogInterface, i, b) -> checked[i] = b);
        builder.setPositiveButton(R.string.action_ok, (dialogInterface, i) -> {
            if(listener != null) listener.ChoicesComplete(choices, checked);
        });
        builder.setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {
            if(listener != null) listener.ChoicesCancelled();
        });
        final AlertDialog dialog = builder.create();
        listView = dialog.getListView();

        dialog.setOnShowListener(dialogInterface -> {
            if(colors != null) {
                listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                    }

                    /*
                    We have to get the views after they are bound, so we wait for a scroll
                    and then iterate through the views on screen
                     */

                    @Override
                    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {
                        for(int i = 0; i < visibleCount; i++) {
                            try {
                                ((AppCompatCheckedTextView) listView.getChildAt(i)).setTextColor(colors[firstVisible + i]);
                            } catch(ClassCastException ignored) {
                            }
                        }
                    }
                });
            }
        });

        return dialog;
    }

    public void setTextColors(int[] colors) {
        this.colors = colors;
    }

    public void setChoices(String[] choices, boolean[] checked) {
        this.choices = choices;
        this.checked = checked;
    }

    public void setListener(MultiChoiceDialogListener listener) {
        this.listener = listener;
    }

    public interface MultiChoiceDialogListener {

        void ChoicesComplete(String[] choices, boolean[] checked);

        void ChoicesCancelled();

    }
}
