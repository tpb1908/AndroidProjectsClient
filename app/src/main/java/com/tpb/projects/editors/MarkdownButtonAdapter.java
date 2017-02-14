package com.tpb.projects.editors;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.tpb.projects.R;
import com.tpb.projects.util.UI;

/**
 * Created by theo on 10/02/17.
 */

public class MarkdownButtonAdapter {
    private static final String TAG = MarkdownButtonAdapter.class.getSimpleName();

    private Context mContext;
    private LinearLayout mScrollView;
    private MarkDownButtonListener mListener;

    public MarkdownButtonAdapter(Context context, @NonNull LinearLayout scrollView, @NonNull MarkDownButtonListener listener) {
        mContext = context;
        mScrollView = scrollView;
        mListener = listener;
        initViews();
    }

    private void initViews() {
        ImageButton preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_preview);
        preview.setOnClickListener((v) -> {
            if(mListener != null) {
                final FullScreenDialog dialog = new FullScreenDialog();
                final Bundle bundle = new Bundle();
                bundle.putString(mContext.getString(R.string.intent_markdown), mListener.getText());
                dialog.setArguments(bundle);
                dialog.show(((AppCompatActivity) mContext).getSupportFragmentManager(), TAG);
            }
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_insert_link);
        preview.setOnClickListener((v) -> showInsertLinkDialog());
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_bold);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("****", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_italic);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("**", 1);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_strikethrough);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("~~~~", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_bulleted);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered(" * ", 3);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_numbered);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered(" 1. ", 3);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_quote);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("> ", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_code);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("```\n\n```", 4);
        });
        mScrollView.addView(preview);


    }

    private void showInsertLinkDialog() {
        final LinearLayout wrapper = new LinearLayout(mContext);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPaddingRelative(UI.pxFromDp(16), 0, UI.pxFromDp(16), 0);

        final EditText text = new EditText(mContext);
        text.setHint("Text to show");
        wrapper.addView(text);

        final EditText url = new EditText(mContext);
        url.setHint("URL");
        wrapper.addView(url);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Insert link");
        builder.setView(wrapper);

        builder.setPositiveButton(R.string.action_insert, (v, di) -> {
            if(mListener != null) {
                mListener.snippetEntered(
                        String.format(
                            mContext.getString(R.string.text_md_link),
                            text.getText().toString(),
                            url.getText().toString()
                        ),
                -1);
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);

        builder.create().show();

    }

    public interface MarkDownButtonListener {

        void snippetEntered(String snippet, int relativePosition);

        String getText();

    }

}
