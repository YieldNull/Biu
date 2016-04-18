package com.bbbbiu.biu.gui.adapters.choose;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.db.FileItem;
import com.bbbbiu.biu.util.db.ModelItem;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/18/16
 */
public class ArchiveContentAdapter extends ContentBaseAdapter {
    private static final String TAG = ArchiveContentAdapter.class.getSimpleName();

    public ArchiveContentAdapter(ChooseBaseActivity context) {
        super(context);

        if (!queryModelItems(ModelItem.TYPE_ARCHIVE)) {
        }
    }

    @Override
    public void cancelPicassoTask() {

    }

    @Override
    public RecyclerView.ViewHolder OnCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ArchiveViewHolder(inflater.inflate(R.layout.list_archive_item, parent, false));
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            ArchiveViewHolder holder = (ArchiveViewHolder) hd;
            FileItem item = (FileItem) getItemAt(position);

            holder.nameText.setText(item.getFile().getName());
            holder.infoText.setText(item.getSize());

            if (isItemChosen(position)) {
                holder.setItemStyleChosen();
            } else {
                holder.setItemStyleChoosing();
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setItemChosen(position);
                }
            });

        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;
            holder.headerText.setText(getHeaderText(position));
        }
    }

    class ArchiveViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImg;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionButton;

        public ArchiveViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public void setItemStyleChosen() {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            iconImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            iconImg.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
        }

        public void setItemStyleChoosing() {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            iconImg.setBackgroundDrawable(null);
            iconImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_archive));
        }
    }
}
