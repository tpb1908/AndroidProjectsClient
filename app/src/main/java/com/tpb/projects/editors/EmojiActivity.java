package com.tpb.projects.editors;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.SettingsActivity;
import com.tpb.projects.util.BaseActivity;
import com.tpb.projects.util.Util;
import com.tpb.projects.util.input.DumbTextChangeWatcher;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 23/03/17.
 */

public class EmojiActivity extends BaseActivity {

    public static final int REQUEST_CODE_CHOOSE_EMOJI = 666;
    private static final String PREFS_COMMON_EMOJIS = "COMMON_EMOJIS";

    private SharedPreferences mCommonEmojis;
    @BindView(R.id.emoji_recycler) RecyclerView mRecycler;
    @BindView(R.id.emoji_search_box) EditText mSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_emoji);
        ButterKnife.bind(this);

        mCommonEmojis = getSharedPreferences(PREFS_COMMON_EMOJIS, MODE_PRIVATE);
        mRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        final EmojiAdapter adapter = new EmojiAdapter(mCommonEmojis);
        mRecycler.setAdapter(adapter);
        mSearch.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                adapter.filter(mSearch.getText().toString().toLowerCase());
            }
        });
    }

    class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {

        private ArrayList<Emoji> mEmojis = new ArrayList<>();
        private ArrayList<Emoji> mFilteredEmojis = new ArrayList<>();
        
        EmojiAdapter(SharedPreferences prefs) {
            mEmojis.addAll(EmojiManager.getAll());
            if(prefs.getString("common", null) != null) {
                final String[] common = prefs.getString("common", "").split(",");
                for(String s : common) {
                    final Emoji e = EmojiManager.getForAlias(s);
                    if(e != null) mEmojis.add(0, e);
                }
            }
            mFilteredEmojis.addAll(mEmojis);
        }

        void filter(String query) {
            if(query.isEmpty()) {
                mFilteredEmojis.addAll(mEmojis);
                return;
            }
            mFilteredEmojis.clear();
            for(Emoji e : mEmojis) {
                for(String s : e.getAliases()) {
                    if(s.contains(query)) {
                        mFilteredEmojis.add(e);
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }

        private void choose(int pos) {
            EmojiActivity.this.choose(mFilteredEmojis.get(pos).getAliases().get(0));
        }

        @Override
        public EmojiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new EmojiViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_emoji, parent, false));
        }

        @Override
        public void onBindViewHolder(EmojiViewHolder holder, int position) {
            holder.mEmoji.setText(mFilteredEmojis.get(position).getUnicode());
            holder.mName.setText(String.format(":%1$s:", mFilteredEmojis.get(position).getAliases().get(0)));
        }

        @Override
        public int getItemCount() {
            return mFilteredEmojis.size();
        }

        class EmojiViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.emoji_text) TextView mEmoji;
            @BindView(R.id.emoji_name) TextView mName;
            
            EmojiViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(v -> choose(getAdapterPosition()));
            }
        }

    }

    private void choose(String emoji) {
        String common = "";
        if(mCommonEmojis.getString("common", null) != null) {
            String current = mCommonEmojis.getString("common", "");
            if(current.contains(emoji)) {
                final int index = current.indexOf(emoji);
                current = current.substring(0, index) + current.substring(index + emoji.length());
            }
            if(Util.instancesOf(current, ",") > 5) {
                current = current.substring(current.indexOf(',') + 1);
            }
            common += current;
        }
        common += "," + emoji;
        mCommonEmojis.edit().putString("common", common).apply();

        final Intent result = new Intent();
        result.putExtra(getString(R.string.intent_emoji), emoji);
        setResult(RESULT_OK, result);
        finish();
    }

}
