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
            case R.id.action_choosing_dismiss:
                mArrayAdapter.dismissChoosing();
                break;

            case R.id.action_choose_all:
                mArrayAdapter.setFileAllChosen();
                break;
            
            case R.id.action_search:
                break;
            default:
                break;
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