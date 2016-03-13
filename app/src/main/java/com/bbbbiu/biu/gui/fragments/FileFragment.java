package com.bbbbiu.biu.gui.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileListAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

public class FileFragment extends Fragment implements OnBackPressedListener {
    private static final String TAG = FileFragment.class.getSimpleName();


    private FileListAdapter mArrayAdapter;
    private RecyclerView mRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onBackPressed() {
        return mArrayAdapter.quitDir();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_by_time:
                mArrayAdapter.setSortingComparator(FileListAdapter.COMPARATOR_TIME);
                break;
            case R.id.action_sort_by_size:
                mArrayAdapter.setSortingComparator(FileListAdapter.COMPARATOR_SIZE);
                break;
            case R.id.action_sort_by_name:
                mArrayAdapter.setSortingComparator(FileListAdapter.COMPARATOR_NAME);
                break;
            case R.id.action_show_hidden:
                if (mArrayAdapter.isShowHidden()) {
                    mArrayAdapter.setShowHidden(false);
                    item.setTitle(getString(R.string.action_show_hidden));
                } else {
                    mArrayAdapter.setShowHidden(true);
                    item.setTitle(getString(R.string.action_not_show_hidden));
                }
                break;
            case R.id.action_choose_or_dismiss:
                if (mArrayAdapter.isOnChoosing()) {
                    mArrayAdapter.setOnChoosing(false);
                    item.setTitle(getString(R.string.action_choose));
                } else {
                    mArrayAdapter.setOnChoosing(true);
                    item.setTitle(getString(R.string.action_choose_dismiss));
                }
                break;
            case R.id.action_choose_all:
                mArrayAdapter.setFileAllChosen();
                break;
            case R.id.action_search:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file, container, false);

        File rootDir = Environment.getExternalStorageDirectory();
        if (rootDir == null) {
            rootDir = Environment.getRootDirectory();
        }

        mArrayAdapter = new FileListAdapter(getContext(), rootDir);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_file);
        mRecyclerView.setAdapter(mArrayAdapter);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).build());

        return view;
    }


}