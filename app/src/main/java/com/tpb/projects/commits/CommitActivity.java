package com.tpb.projects.commits;

import com.tpb.projects.data.APIHandler;
import com.tpb.projects.data.Loader;
import com.tpb.projects.data.models.Commit;
import com.tpb.projects.util.CircularRevealActivity;

/**
 * Created by theo on 30/03/17.
 */

public class CommitActivity extends CircularRevealActivity implements Loader.ItemLoader<Commit> {


    @Override
    public void loadComplete(Commit data) {

    }

    @Override
    public void loadError(APIHandler.APIError error) {

    }
}
