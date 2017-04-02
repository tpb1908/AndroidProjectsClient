package com.tpb.mdtext.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.pddstudio.highlightjs.HighlightJsView;
import com.pddstudio.highlightjs.models.Language;
import com.pddstudio.highlightjs.models.Theme;

import com.tpb.mdtext.handlers.CodeClickHandler;
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
    public void codeClicked(String code, @Nullable String language) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        final View view = inflater.inflate(R.layout.dialog_code, null);

        builder.setView(view);

        final HighlightJsView wv = (HighlightJsView) view.findViewById(R.id.dialog_highlight_view);
        wv.setTheme(Theme.ANDROID_STUDIO);

        if(language != null && !language.isEmpty()) wv.setHighlightLanguage(getLanguage(language));
        wv.setSource(code);
        final Dialog dialog = builder.create();

        dialog.getWindow()
              .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }


    private static Language getLanguage(String lang) {
        for(Language l : Language.values()) {
            if(l.toString().equalsIgnoreCase(lang)) return l;
        }
        return Language.AUTO_DETECT;
    }
}
