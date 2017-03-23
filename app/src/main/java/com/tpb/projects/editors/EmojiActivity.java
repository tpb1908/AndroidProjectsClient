package com.tpb.projects.editors;

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

    @BindView(R.id.emoji_recycler) RecyclerView mRecycler;
    @BindView(R.id.emoji_search_box) EditText mSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences.getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_emoji);
        ButterKnife.bind(this);
        mRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        final EmojiAdapter adapter = new EmojiAdapter();
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
        
        EmojiAdapter() {

            mEmojis.addAll(EmojiManager.getAll());
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
                    if(s.startsWith(query)) {
                        mFilteredEmojis.add(e);
                        break;
                    }
                }
            }
            notifyDataSetChanged();
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
            }
        }

    }

}
