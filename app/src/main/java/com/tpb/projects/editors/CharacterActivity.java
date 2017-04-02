package com.tpb.projects.editors;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.common.BaseActivity;
import com.tpb.projects.util.SettingsActivity;
import com.tpb.projects.util.input.DumbTextChangeWatcher;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 24/03/17.
 */

public class CharacterActivity extends BaseActivity {

    public static final int REQUEST_CODE_INSERT_CHARACTER = 7438;

    @BindView(R.id.search_title) TextView mTitle;
    @BindView(R.id.search_recycler) RecyclerView mRecycler;
    @BindView(R.id.search_search_box) EditText mSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsActivity.Preferences prefs = SettingsActivity.Preferences
                .getPreferences(this);
        setTheme(prefs.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme);
        setContentView(R.layout.activity_simple_search);
        ButterKnife.bind(this);
        mTitle.setText(R.string.title_activity_characters);
        mSearch.setHint(R.string.hint_search_characters);
        mRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        final CharacterAdapter adapter = new CharacterAdapter();
        mRecycler.setAdapter(adapter);
        mSearch.addTextChangedListener(new DumbTextChangeWatcher() {
            @Override
            public void textChanged() {
                adapter.filter(mSearch.getText().toString().toUpperCase());
            }
        });
        AsyncTask.execute(() -> {
            final ArrayList<Pair<String, String>> characters = new ArrayList<>();
            final int length = Character.MAX_CODE_POINT - Character.MIN_CODE_POINT;
            int lastIndex = 0;
            for(int i = Character.MIN_CODE_POINT; i < Character.MAX_CODE_POINT; i++) {
                if(Character.isDefined(i) && !Character.isISOControl(i)) {
                    characters.add(Pair.create(String.valueOf((char) i), Character.getName(i)));
                    // 50 gives ~10 chunks
                    if((characters.size() - lastIndex) > length / 50) {
                        final int start = lastIndex;
                        adapter.addCharacters(characters.subList(start, characters.size()));
                        lastIndex = characters.size();
                    }
                }
            }

        });
    }

    class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

        private final ArrayList<Pair<String, String>> mCharacters = new ArrayList<>();
        private final ArrayList<Pair<String, String>> mFilteredCharacters = new ArrayList<>();

        void addCharacters(List<Pair<String, String>> characters) {
            mCharacters.addAll(characters);
            final int originalSize = mFilteredCharacters.size();
            mFilteredCharacters.addAll(characters);
            CharacterActivity.this.runOnUiThread(() -> {
                notifyItemRangeInserted(originalSize, mFilteredCharacters.size());
            });
        }

        void filter(String query) {
            if(query.isEmpty()) {
                mFilteredCharacters.addAll(mCharacters);
                return;
            }
            mFilteredCharacters.clear();
            for(Pair<String, String> p : mCharacters) {
                if(p.second.contains(query)) mFilteredCharacters.add(p);
            }
            notifyDataSetChanged();
        }

        private void choose(int pos) {
            CharacterActivity.this.choose(mFilteredCharacters.get(pos).first);
        }

        @Override
        public CharacterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CharacterViewHolder(LayoutInflater.from(parent.getContext())
                                                         .inflate(R.layout.viewholder_text, parent,
                                                                 false
                                                         ));
        }

        @Override
        public void onBindViewHolder(CharacterViewHolder holder, int position) {
            holder.mCharacter.setText(mFilteredCharacters.get(position).first);
            holder.mName.setText(mFilteredCharacters.get(position).second);
        }

        @Override
        public int getItemCount() {
            return mFilteredCharacters.size();
        }

        class CharacterViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.text_content) TextView mCharacter;
            @BindView(R.id.text_info) TextView mName;

            CharacterViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(v -> choose(getAdapterPosition()));
            }
        }

    }

    protected void choose(String c) {
        final Intent result = new Intent();
        result.putExtra(getString(R.string.intent_character), c);
        setResult(RESULT_OK, result);
        finish();
    }

}
