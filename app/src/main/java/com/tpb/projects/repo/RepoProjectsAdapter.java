package com.tpb.projects.repo;

import android.content.Intent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tpb.projects.R;
import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.DataModel;
import com.tpb.projects.data.models.Project;
import com.tpb.projects.data.models.Repository;
import com.tpb.projects.markdown.Markdown;
import com.tpb.projects.project.ProjectActivity;
import com.tpb.projects.repo.fragment.RepoProjectsFragment;

import org.sufficientlysecure.htmltext.htmltextview.HtmlTextView;
import org.sufficientlysecure.htmltext.imagegetter.HtmlHttpImageGetter;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by theo on 25/03/17.
 */

public class RepoProjectsAdapter extends RecyclerView.Adapter<RepoProjectsAdapter.ProjectViewHolder> implements  Loader.GITModelsLoader<Project> {

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
    public void loadComplete(Project[] data) {
        mProjects.clear();
        mProjects.addAll(Arrays.asList(data));
        notifyItemRangeChanged(0, mProjects.size());
        mRefresher.setRefreshing(false);
    }

    @Override
    public void loadError(APIHandler.APIError error) {

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
        if(!(DataModel.JSON_NULL.equals(mProjects.get(position).getBody()) || mProjects.get(position).getBody().isEmpty())) {
            holder.mBody.setVisibility(View.VISIBLE);
            holder.mBody.setHtml(
                    Markdown.parseMD(mProjects.get(holder.getAdapterPosition()).getBody()),
                    new HtmlHttpImageGetter(holder.mBody, holder.mBody),
                    null
            );
        }
        holder.itemView.setOnClickListener(v -> {
            final Intent i = new Intent(mParent.getContext(), ProjectActivity.class);
            i.putExtra(mParent.getString(R.string.parcel_project), mProjects.get(position));
            mParent.startActivity(i,
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            mParent.getActivity(),
                            holder.mName,
                            mParent.getString(R.string.transition_title)
                    ).toBundle()
            );
            i.putExtra(mParent.getString(R.string.intent_project_number), mProjects.get(position).getNumber());
        });
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.project_name) TextView mName;
        @BindView(R.id.project_last_updated) TextView mLastUpdate;
        @BindView(R.id.project_body) HtmlTextView mBody;

        ProjectViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mBody.setConsumeNonUrlClicks(false);
        }

    }
}
