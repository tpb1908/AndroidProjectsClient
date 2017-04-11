package com.tpb.mdtext.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.tpb.mdtext.handlers.TableClickHandler;
import com.tpb.mdtext.webview.MarkdownWebView;

import org.sufficientlysecure.htmltextview.R;

/**
 * Created by theo on 08/04/17.
 */

public class TableDialog implements TableClickHandler {

    private Context mContext;

    public TableDialog(Context context) {
        mContext = context;
    }

    @Override
    public void onClick(String html) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        final View view = inflater.inflate(R.layout.dialog_table, null);

        builder.setView(view);

        final MarkdownWebView wv = (MarkdownWebView) view.findViewById(R.id.dialog_web_view);
        wv.enableDarkTheme();
        wv.setMarkdown(html);
        final Dialog dialog = builder.create();

        dialog.getWindow()
              .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

}
