package com.tpb.projects.editors;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;

/**
 * Created by theo on 24/12/16.
 */

public class FullScreenDialog extends KeyboardDismissingDialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final WebView wv = new WebView(getContext());
        wv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final ProgressBar pb = new ProgressBar(getContext());

        final AlertDialog ad = new AlertDialog.Builder(getActivity()).setView(pb).create();
        if(getArguments() != null && getArguments().containsKey(getString(R.string.intent_markdown))) {
            final String markdown = getArguments().getString(getString(R.string.intent_markdown));

            final Loader.MarkDownRenderLoader loader = new Loader.MarkDownRenderLoader() {
                @Override
                public void rendered(String html) {
                    ad.setContentView(wv);

                    wv.getSettings().setJavaScriptEnabled(true);
                    wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                }

                @Override
                public void renderError(APIHandler.APIError error) {
                    ad.dismiss();
                }
            };

            if(getArguments().containsKey(getString(R.string.intent_repo))) {
                final String repo = getArguments().getString(getString(R.string.intent_repo));
                new Loader(getContext()).renderMarkDown(loader, markdown, repo);
            } else {
                new Loader(getContext()).renderMarkDown(loader, markdown);
            }
        }


        return ad;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
