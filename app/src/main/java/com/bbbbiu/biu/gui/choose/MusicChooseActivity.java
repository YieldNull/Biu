package com.bbbbiu.biu.gui.choose;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.bbbbiu.biu.gui.adapters.choose.MusicContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.bbbbiu.biu.util.Music;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicChooseActivity extends ChooseBaseActivity{


    private List<Music> musicList;
    private Music music;
    public static String TAG = MusicChooseActivity.class.getSimpleName();
    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new LinearContentLayoutManager(this);
    }

    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        initList();

        return new MusicContentAdapter(this,musicList);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    public void onFileChosen(File file) {

    }

    @Override
    public void onFileDismissed(File file) {

    }
    private void initList() {
        getMusicInfo(MusicChooseActivity.this);
    }
    private  List<Music> getMusicInfo(Context context)
    {
        musicList = new ArrayList<Music>();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        String title;
        String author;
        String duration;
        String path;
        long time=0;
        for (int i=0;i<cursor.getCount();i++)
        {

            cursor.moveToNext();
            title =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            author = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            time = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

            duration = formatTime(time);
            Log.d(TAG,title+" : " + path );

            music = new Music(title,author,duration);
            musicList.add(music);


        }

        musicList.add(music);
        Log.d(TAG,"------->"+cursor.getCount());
        return musicList;
    }
    private String formatTime(long time)
    {
        String min = time/(1000 * 60) + "";
        String sec = time%(1000 * 60) + "";
        if (min.length()<2)
            min = "0"+min;
        if (sec.length()==4)
            sec = "0"+sec;
        else if(sec.length()<=3)
            sec = "00"+sec;
        return min + ":" + sec.trim().substring(0,2);

    }

    @Override
    protected void onSendIOSClicked() {

    }

    @Override
    protected void onSendAndroidClicked() {

    }

    @Override
    protected void onSendComputerClicked() {

    }
}
