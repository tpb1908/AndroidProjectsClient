package com.tpb.projects.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.tpb.projects.R;

/**
 * Created by theo on 10/02/17.
 */

public class MarkdownButtonAdapter {

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
        mScrollView.addView(preview);
        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_insert_link);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_bold);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_italic);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_strikethrough);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_bulleted);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_numbered);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_quote);
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mContext).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_code);
        mScrollView.addView(preview);


    }


    public interface MarkDownButtonListener {

        void snippetEntered(String snippet);

    }

}
