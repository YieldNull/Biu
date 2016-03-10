package com.bbbbiu.biu.gui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.FileSelectActivity;
import com.bbbbiu.biu.gui.adapters.FileListBaseAdapter;

import java.io.File;

public class FileFragment extends Fragment implements OnBackPressedListener {
    private static final String TAG = FileFragment.class.getSimpleName();


    private FileListBaseAdapter arrayAdapter;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onBackPressed() {
        return arrayAdapter.quitDir();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_by_time:
                arrayAdapter.setSortingComparator(FileListBaseAdapter.COMPARATOR_TIME);
                break;
            case R.id.action_sort_by_size:
                arrayAdapter.setSortingComparator(FileListBaseAdapter.COMPARATOR_SIZE);
                break;
            case R.id.action_sort_by_name:
                arrayAdapter.setSortingComparator(FileListBaseAdapter.COMPARATOR_NAME);
                break;
            case R.id.action_show_hidden:
                if (arrayAdapter.isShowHidden()) {
                    arrayAdapter.setShowHidden(false);
                    item.setTitle("显示隐藏文件");
                } else {
                    arrayAdapter.setShowHidden(true);
                    item.setTitle("不显示隐藏文件");
                }
                break;
            case R.id.action_select_or_dismiss:
                if (arrayAdapter.isOnSelecting()) {
                    arrayAdapter.setOnSelecting(false);
                    item.setTitle("选择");
                } else {
                    arrayAdapter.setOnSelecting(true);
                    item.setTitle("清除选择");
                }
                break;
            case R.id.action_select_all:
                arrayAdapter.setFileAllSelected();
                break;
            case R.id.action_search:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
                File file = (File) arrayAdapter.getItem(position);

                boolean isSelected = arrayAdapter.isFileSelected(position);

                if (isSelected) {
                    arrayAdapter.setFileSelected(position, false);
                } else {
                    if (file.isDirectory()) {
                        arrayAdapter.enterDir(file);

                    } else {
                        arrayAdapter.setFileSelected(position, true);
                    }
                }
            }
        });

        return view;
    }
}