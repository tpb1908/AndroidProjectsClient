package com.tpb.projects.editors;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.mdtext.webview.MarkdownWebView;
import com.tpb.projects.R;

/**
 * Created by theo on 24/12/16.
 */

public class FullScreenDialog extends KeyboardDismissingDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MarkdownWebView wv = new MarkdownWebView(getContext());
        wv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        final ProgressBar pb = new ProgressBar(getContext());

        final AlertDialog ad = new AlertDialog.Builder(getActivity()).setView(pb).create();
        if(getArguments() != null && getArguments()
                .containsKey(getString(R.string.intent_markdown))) {
            final String markdown = getArguments().getString(getString(R.string.intent_markdown));

            final Loader.ItemLoader<String> loader = new Loader.ItemLoader<String>() {
                @Override
                public void loadComplete(String html) {
                    ad.setContentView(wv);
                    wv.enableDarkTheme();
                    wv.setMarkdown(html);
                }

                @Override
                public void loadError(APIHandler.APIError error) {
                    ad.dismiss();
                }
            };

            if(getArguments().containsKey(getString(R.string.intent_repo))) {
                final String repo = getArguments().getString(getString(R.string.intent_repo));
                Loader.getLoader(getContext()).renderMarkDown(loader, markdown, repo);
            } else {
                Loader.getLoader(getContext()).renderMarkDown(loader, markdown);
            }
        }


        return ad;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
