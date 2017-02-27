package org.sufficientlysecure.htmltext.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Theme;

import org.sufficientlysecure.htmltext.handlers.CodeClickHandler;
import org.sufficientlysecure.htmltextview.R;

/**
 * Created by theo on 27/02/17.
 */

public class CodeDialog implements CodeClickHandler {

    private Context mContext;

    public CodeDialog(Context context) {
        mContext = context;
    }

    @Override
    public void codeClicked(String code) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        final View view = inflater.inflate(R.layout.dialog_code, null);

        builder.setView(view);

        final HighlightJsView wv = (HighlightJsView) view.findViewById(R.id.dialog_highlight_view);
        wv.setTheme(Theme.ANDROID_STUDIO);
        wv.setSource(code);
        final Dialog dialog = builder.create();

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

}
