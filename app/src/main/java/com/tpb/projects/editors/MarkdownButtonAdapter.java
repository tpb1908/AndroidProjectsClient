package com.tpb.projects.editors;

import android.content.Intent;
import android.support.annotation.DrawableRes;
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

    private final EditorActivity mParent;
    private final LinearLayout mScrollView;
    private final MarkdownButtonListener mListener;

    MarkdownButtonAdapter(EditorActivity parent, @NonNull LinearLayout scrollView, @NonNull MarkdownButtonListener listener) {
        mParent = parent;
        mScrollView = scrollView;
        mListener = listener;
        initViews();
    }

    private void initViews() {
        ImageButton preview = createImageButton(R.drawable.ic_preview);
        preview.setOnClickListener((v) -> mListener.previewCalled());

        preview = createImageButton(R.drawable.ic_insert_link);
        preview.setOnClickListener((v) -> showInsertLinkDialog());

        preview = createImageButton(R.drawable.ic_photo);
        preview.setOnClickListener((v) -> mParent.showImageUploadDialog());

        preview = createImageButton(R.drawable.ic_format_bold);
        preview.setOnClickListener((v) -> mListener.snippetEntered("****", 2));

        preview = createImageButton(R.drawable.ic_format_italic);
        preview.setOnClickListener((v) -> mListener.snippetEntered("**", 1));

        preview = createImageButton(R.drawable.ic_format_strikethrough);
        preview.setOnClickListener((v) -> mListener.snippetEntered("~~~~", 2));

        preview = createImageButton(R.drawable.ic_check_box_checked);
        preview.setOnClickListener((v) -> mListener.snippetEntered(" [x] ", 5));

        preview = createImageButton(R.drawable.ic_check_box_empty);
        preview.setOnClickListener((v) -> mListener.snippetEntered(" [] ", 4));

        preview = createImageButton(R.drawable.ic_horizontal_rule);
        preview.setOnClickListener((v) -> mListener.snippetEntered("\n---\n ", 5));

        preview = createImageButton(R.drawable.ic_format_list_bulleted);
        preview.setOnClickListener((v) -> mListener.snippetEntered(" * ", 3));

        preview = createImageButton(R.drawable.ic_format_list_numbered);
        preview.setOnClickListener((v) -> mListener.snippetEntered(" 1. ", 3));

        preview = createImageButton(R.drawable.ic_format_quote);
        preview.setOnClickListener((v) -> mListener.snippetEntered("> ", 2));

        preview = createImageButton(R.drawable.ic_format_code);
        preview.setOnClickListener((v) -> mListener.snippetEntered("```\n\n```", 4));

        preview = createImageButton(R.drawable.ic_emoticon);
        preview.setOnClickListener((v) -> showInsertEmoticonActivity());

        preview = createImageButton(R.drawable.ic_character);
        preview.setOnClickListener((v) -> showInsertCharacterActivity());
    }

    private ImageButton createImageButton(@DrawableRes int resId) {
        final ImageButton ib = (ImageButton) LayoutInflater
                .from(mParent)
                .inflate(
                        R.layout.shard_markdown_button,
                        mScrollView,
                        false
                );
        ib.setImageResource(resId);
        mScrollView.addView(ib);
        return ib;
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
            mListener.snippetEntered(
                    String.format(
                            mParent.getString(R.string.text_md_link),
                            text.getText().toString(),
                            url.getText().toString()
                    ),
                    0
            );
        });
        builder.setNegativeButton(R.string.action_cancel, null);

        builder.create().show();

    }

    private void showInsertEmoticonActivity() {
        mParent.startActivityForResult(new Intent(mParent, EmojiActivity.class),
                EmojiActivity.REQUEST_CODE_CHOOSE_EMOJI
        );
    }

    private void showInsertCharacterActivity() {
        mParent.startActivityForResult(new Intent(mParent, CharacterActivity.class),
                CharacterActivity.REQUEST_CODE_INSERT_CHARACTER
        );
    }

    interface MarkdownButtonListener {

        void snippetEntered(String snippet, int relativePosition);

        String getText();

        void previewCalled();

    }

}
