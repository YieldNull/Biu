package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.util.Music;

import java.util.List;

/**
 * Created by fangdongliang on 16/3/26.
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    Context mContext;
    LayoutInflater mInflater;
    List<Music> music_list;
    Music music;
    public MusicAdapter(Context context,List<Music> mlist)
    {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        music_list = mlist;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder myHolder =
                new MyViewHolder(mInflater.inflate(R.layout.music_item,null));
        return myHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        music = music_list.get(position);
        holder.tv_name.setText(music.getName());
        holder.tv_author.setText(music.getAuthor());
        holder.tv_length.setText(music.getLength());

    }

    @Override
    public int getItemCount() {
        return music_list.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView tv_name;
        TextView tv_author;
        TextView tv_length;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_name = (TextView) itemView.findViewById(R.id.music_name);
            tv_author= (TextView) itemView.findViewById(R.id.music_singer);
            tv_length = (TextView) itemView.findViewById(R.id.music_length);
        }
    }
}
