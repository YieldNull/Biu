package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by fangdongliang on 16/3/26.
 */
public class MusicContentAdapter extends ContentBaseAdapter {
    private static final String TAG = MusicContentAdapter.class.getSimpleName();
    Context mContext;
    LayoutInflater mInflater;
    List<Music> music_list;
    Music music;

    public MusicContentAdapter(Context context, List<Music> mlist) {
        super(context);

        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        music_list = mlist;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(mInflater.inflate(R.layout.list_music_item, null));
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        MyViewHolder holder = (MyViewHolder) hd;
        music = music_list.get(position);
        holder.tv_name.setText(music.getName());
        holder.tv_author.setText(music.getAuthor());
        holder.tv_length.setText(music.getLength());

    }

    @Override
    public int getItemCount() {
        return music_list.size();
    }

    @Override
    public List<File> getChosenFiles() {
        return null;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv_name;
        TextView tv_author;
        TextView tv_length;

        public MyViewHolder(View itemView) {
            super(itemView);
            tv_name = (TextView) itemView.findViewById(R.id.music_name);
            tv_author = (TextView) itemView.findViewById(R.id.music_singer);
            tv_length = (TextView) itemView.findViewById(R.id.music_length);
        }
    }

    /**
     * Created by fangdongliang on 16/3/26.
     */
    public static class Music {
        private String name;
        private String author;
        private String length;

        public Music(String name, String author, String length) {
            this.name = name;
            this.author = author;
            this.length = length;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setLength(String length) {
            this.length = length;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getName() {
            return name;
        }

        public String getAuthor() {
            return author;
        }

        public String getLength() {
            return length;
        }

    }
}
