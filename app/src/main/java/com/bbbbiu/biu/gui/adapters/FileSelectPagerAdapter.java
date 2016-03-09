package com.bbbbiu.biu.gui.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.bbbbiu.biu.gui.fragments.FileFragment;
import com.bbbbiu.biu.gui.fragments.MainFragment;

public class FileSelectPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = FileSelectPagerAdapter.class.getSimpleName();

    private String tabTitles[] = new String[]{"文件", "文档", "图片", "音乐", "视频", "应用"};

    private Fragment[] fragments = new Fragment[tabTitles.length];

    public FileSelectPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if (fragments[position] == null) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new FileFragment();
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                default:
                    fragment = new MainFragment();
                    break;
            }
            fragments[position] = fragment;
            return fragment;
        } else {
            return fragments[position];
        }
    }

    @Override
    public int getCount() {
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
