package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.bbbbiu.biu.gui.fragments.FileFragment;

public class FileSelectPagerAdapter extends FragmentPagerAdapter {
    private Context context;
    private String tabTitles[] = new String[]{"文件", "文档", "图片", "音乐", "视频", "应用"};

    public FileSelectPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        return new FileFragment();
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
