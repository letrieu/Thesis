package k2013.fit.hcmus.thesis.id539621.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import k2013.fit.hcmus.thesis.id539621.R;
import k2013.fit.hcmus.thesis.id539621.adapter.GameSelectionAdapter;

public class GameSelectionActivity extends BaseActivity {

    private GameSelectionAdapter mAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_selection);

        mRecyclerView = (RecyclerView) findViewById(R.id.gameselection_recyclerview);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setHasFixedSize(false);

        mAdapter = new GameSelectionAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }
}
