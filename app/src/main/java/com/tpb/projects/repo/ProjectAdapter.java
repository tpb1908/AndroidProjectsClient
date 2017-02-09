/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.repo;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.animatingrecyclerview.AnimatingRecycler;
import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Editor;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.util.Constants;
import com.tpb.projects.util.Data;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 17/12/16.
 */

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> implements Loader.ProjectsLoader {
    private static final String TAG = ProjectAdapter.class.getSimpleName();

    private ArrayList<Project> mProjects = new ArrayList<>();
    private final ProjectEditor mEditor;
    private final AnimatingRecycler mRecycler;
    private boolean canAccessRepo = false;



    public ProjectAdapter(ProjectEditor editor, AnimatingRecycler recycler) {
        mEditor = editor;
        mRecycler = recycler;
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int pos = viewHolder.getAdapterPosition();
                editor.deleteProject(mProjects.get(pos), new Editor.ProjectDeletionListener() {
                    @Override
                    public void projectDeleted(Project project) {
                        mProjects.remove(pos);
                        notifyItemRemoved(pos);
                    }

                    @Override
                    public void projectDeletionError(APIHandler.APIError error) {

                    }
                });
                notifyItemChanged(pos);
            }


        }).attachToRecyclerView(recycler);
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_project, parent, false));
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        holder.mName.setText(mProjects.get(position).getName());
        holder.mLastUpdate.setText(
                String.format(
                        holder.itemView.getContext().getString(R.string.text_last_updated),
                        DateUtils.getRelativeTimeSpanString(mProjects.get(position).getUpdatedAt())
                )
        );
        if(!(Constants.JSON_NULL.equals(mProjects.get(position).getBody()) || mProjects.get(position).getBody().isEmpty())) {
            holder.mBody.setVisibility(View.VISIBLE);
            holder.mBody.setHtml(
                    Data.parseMD(mProjects.get(holder.getAdapterPosition()).getBody()),
                    new HtmlHttpImageGetter(holder.mBody)
            );
        }
    }

    void enableEditAccess() {
        canAccessRepo = true;
        if(mProjects.size() > 0) notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    @Override
    public void projectsLoaded(Project[] projects) {
        Log.i(TAG, "projectsLoaded: " + Arrays.toString(projects));
        mProjects = new ArrayList<>(Arrays.asList(projects));
        notifyDataSetChanged();
        mRecycler.enableAnimation();
    }

    @Override
    public void projectsLoadError(APIHandler.APIError error) {

    }

    void clearProjects() {
        mProjects.clear();
        notifyDataSetChanged();
        mRecycler.enableAnimation();
    }

    void addProject(Project project) {
        mProjects.add(0, project);
        notifyItemInserted(0);
    }

    void updateProject(Project project) {
        final int pos = mProjects.indexOf(project);
        if(pos != -1) {
            mProjects.set(pos, project);
            notifyItemChanged(pos);
        }
        Log.i(TAG, "updateProject: At " + pos);
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;
        @BindView(R.id.project_body) HtmlTextView mBody;

        ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.findViewById(R.id.project_edit_button).setOnClickListener((v) -> mEditor.editProject(mProjects.get(getAdapterPosition())));
            view.findViewById(R.id.project_edit_button).setVisibility(canAccessRepo ? View.VISIBLE : View.INVISIBLE);
            view.setOnClickListener((v) -> mEditor.openProject(mProjects.get(getAdapterPosition()), mName));
            mBody.setShowUnderLines(false);
        }

    }

    interface ProjectEditor {

        void openProject(Project project, View name);

        void editProject(Project project);

        void deleteProject(Project project, Editor.ProjectDeletionListener listener);
    }

}
