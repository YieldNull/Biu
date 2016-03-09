package com.bbbbiu.biu.gui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileListBaseAdapter;

import java.io.File;

public class FileFragment extends Fragment implements OnBackPressedListener {
    private static final String TAG = FileFragment.class.getSimpleName();


    private FileListBaseAdapter arrayAdapter;
    private ListView listView;

    @Override
    public boolean onBackPressed() {
        return arrayAdapter.quitDir();
    }


    public interface OnFileOptionClickListener {
        void onFileOptionClicked();
    }

    public interface OnOutsideClickListener {
        void onOutsideClicked();
    }


    private OnFileOptionClickListener mOnFileOptionClickListener;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mOnFileOptionClickListener = (OnFileOptionClickListener) context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file, container, false);

        File rootDir = Environment.getExternalStorageDirectory();

        arrayAdapter = new FileListBaseAdapter(getContext(), rootDir);

        listView = (ListView) view.findViewById(R.id.listView_file);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                arrayAdapter.handleItemClick();
            }
        });
        return view;
    }
}