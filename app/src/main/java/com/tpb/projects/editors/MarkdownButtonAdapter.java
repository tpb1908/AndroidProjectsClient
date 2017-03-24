package com.tpb.projects.editors;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.tpb.projects.R;
import com.tpb.projects.util.UI;

/**
 * Created by theo on 10/02/17.
 */

class MarkdownButtonAdapter {
    private static final String TAG = MarkdownButtonAdapter.class.getSimpleName();

    private final EditorActivity mParent;
    private final LinearLayout mScrollView;
    private final MarkDownButtonListener mListener;

    MarkdownButtonAdapter(EditorActivity parent, @NonNull LinearLayout scrollView, @NonNull MarkDownButtonListener listener) {
        mParent = parent;
        mScrollView = scrollView;
        mListener = listener;
        initViews();
    }

    private void initViews() {
        ImageButton preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_preview);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.previewCalled();
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_insert_link);
        preview.setOnClickListener((v) -> showInsertLinkDialog());
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_photo);
        preview.setOnClickListener((v) -> mParent.showImageUploadDialog());
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_bold);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("****", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_italic);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("**", 1);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_strikethrough);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("~~~~", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_bulleted);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered(" * ", 3);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_list_numbered);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered(" 1. ", 3);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_quote);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("> ", 2);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_format_code);
        preview.setOnClickListener((v) -> {
            if(mListener != null) mListener.snippetEntered("```\n\n```", 4);
        });
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_emoticon);
        preview.setOnClickListener((v) -> showInsertEmoticonActivity());
        mScrollView.addView(preview);

        preview = (ImageButton) LayoutInflater.from(mParent).inflate(R.layout.shard_markdown_button, mScrollView, false);
        preview.setImageResource(R.drawable.ic_character);
        preview.setOnClickListener((v) -> showInsertCharacterActivity());
        mScrollView.addView(preview);
    }

    private void showInsertLinkDialog() {
        final LinearLayout wrapper = new LinearLayout(mParent);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPaddingRelative(UI.pxFromDp(16), 0, UI.pxFromDp(16), 0);

        final EditText text = new EditText(mParent);
        text.setHint(R.string.hint_url_description);
        wrapper.addView(text);

        final EditText url = new EditText(mParent);
        url.setHint(R.string.hint_url_url);
        wrapper.addView(url);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mParent);
        builder.setTitle(R.string.title_insert_link);
        builder.setView(wrapper);

        builder.setPositiveButton(R.string.action_insert, (v, di) -> {
            if(mListener != null) {
                mListener.snippetEntered(
                        String.format(
                                mParent.getString(R.string.text_md_link),
                                text.getText().toString(),
                                url.getText().toString()
                        ),
                        0);
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);

        builder.create().show();

    }

    private void showInsertEmoticonActivity() {
        mParent.startActivityForResult(new Intent(mParent, EmojiActivity.class), EmojiActivity.REQUEST_CODE_CHOOSE_EMOJI);
    }

    private void showInsertCharacterActivity() {
        mParent.startActivityForResult(new Intent(mParent, CharacterActivity.class), CharacterActivity.REQUEST_CODE_INSERT_CHARACTER);
    }

    interface MarkDownButtonListener {

        void snippetEntered(String snippet, int relativePosition);

        String getText();

        void previewCalled();

    }

}
