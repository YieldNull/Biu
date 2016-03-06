package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;

import java.util.List;

public class PopupArrayAdapter extends ArrayAdapter {
    public static class PopupItem {
        private int drawableId;
        private int titleId;

        public int getDrawableId() {
            return drawableId;
        }

        public int getTitleId() {
            return titleId;
        }

        public PopupItem(int drawableId, int titleId) {
            this.drawableId = drawableId;
            this.titleId = titleId;
        }
    }

    public PopupArrayAdapter(Context context, List objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PopupItem item = (PopupItem) getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.popup_window_item, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.textView_popup_window);
        textView.setText(getContext().getString(item.getTitleId()));

        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView_popup_window);
        imageView.setImageResource(item.getDrawableId());
        return convertView;
    }
}
