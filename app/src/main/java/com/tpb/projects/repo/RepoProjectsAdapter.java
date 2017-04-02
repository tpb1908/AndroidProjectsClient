package com.tpb.projects.repo;

import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tpb.github.data.APIHandler;
import com.tpb.github.data.Loader;
import com.tpb.github.data.models.DataModel;
import com.tpb.github.data.models.Project;
import com.tpb.github.data.models.Repository;
import com.tpb.github.data.models.State;
import com.tpb.mdtext.imagegetter.HttpImageGetter;
import com.tpb.mdtext.mdtextview.MarkdownTextView;
import com.tpb.projects.R;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.fragments.RepoProjectsFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 25/03/17.
 */

public class RepoProjectsAdapter extends RecyclerView.Adapter<RepoProjectsAdapter.ProjectViewHolder> implements Loader.ListLoader<Project> {

    private ArrayList<Project> mProjects = new ArrayList<>();
    private Loader mLoader;
    private Repository mRepo;
    private RepoProjectsFragment mParent;
    private SwipeRefreshLayout mRefresher;

    public RepoProjectsAdapter(RepoProjectsFragment parent, SwipeRefreshLayout refresher) {
        mLoader = new Loader(parent.getContext());
        mParent = parent;
        mRefresher = refresher;
    }

    public void setRepository(Repository repo) {
        mRefresher.setRefreshing(true);
        mLoader.loadProjects(this, repo.getFullName());
        mRepo = repo;
    }

    public void reload() {
        final int oldSize = mProjects.size();
        mProjects.clear();
        notifyItemRangeRemoved(0, oldSize);
        mLoader.loadProjects(this, mRepo.getFullName());
    }

    @Override
    public void listLoadComplete(List<Project> projects) {
        mProjects.clear();
        mProjects.addAll(projects);
        notifyItemRangeChanged(0, mProjects.size());
        mRefresher.setRefreshing(false);
    }

    @Override
    public void listLoadError(APIHandler.APIError error) {

    }

    public void updateProject(Project project) {
        final int index = mProjects.indexOf(project);
        if(index != -1) {
            mProjects.set(index, project);
            notifyItemChanged(index);
        }
    }

    public void addProject(Project project) {
        mProjects.add(0, project);
        notifyItemInserted(0);
    }

    public void removeProject(Project project) {
        final int index = mProjects.indexOf(project);
        if(index != -1) {
            mProjects.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(parent.getContext())
                                                   .inflate(R.layout.viewholder_project, parent,
                                                           false
                                                   ));
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        final Project p = mProjects.get(position);
        holder.mName.setText(p.getName());
        holder.mName.setCompoundDrawablesWithIntrinsicBounds(
                p.getState() == State.OPEN ? R.drawable.ic_state_open : R.drawable.ic_state_closed,
                0, 0, 0
        );
        holder.mLastUpdate.setText(
                String.format(
                        holder.itemView.getContext().getString(R.string.text_last_updated),
                        DateUtils.getRelativeTimeSpanString(p.getUpdatedAt())
                )
        );
        if(!(DataModel.JSON_NULL.equals(p.getBody()) || p.getBody().isEmpty())) {
            holder.mBody.setVisibility(View.VISIBLE);
            holder.mBody.setMarkdown(
                    mProjects.get(holder.getAdapterPosition()).getBody(),
                    new HttpImageGetter(holder.mBody, holder.mBody),
                    null
            );
        }
        holder.itemView.setOnClickListener(v -> {
            final Intent i = new Intent(mParent.getContext(), ProjectActivity.class);
            i.putExtra(mParent.getString(R.string.parcel_project), p);
            mParent.startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            mParent.getActivity(),
                            holder.mName,
                            mParent.getString(R.string.transition_title)
                    ).toBundle()
            );
            i.putExtra(mParent.getString(R.string.intent_project_number), p.getNumber());
        });
        holder.mMenu.setOnClickListener(v -> mParent.showMenu(holder.mMenu, p));

    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;
        @BindView(R.id.project_body) MarkdownTextView mBody;
        @BindView(R.id.project_menu_button) ImageButton mMenu;

        ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mBody.setConsumeNonUrlClicks(false);
        }

    }
}
