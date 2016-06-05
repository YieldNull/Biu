package com.bbbbiu.biu.gui.transfer;

import android.os.Parcel;
import android.os.Parcelable;

import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;

/**
 * Created by YieldNull at 4/26/16
 */
public class FileItem implements Parcelable {
    public String uri;
    public String name;
    public long size;

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uri);
        dest.writeString(this.name);
        dest.writeLong(this.size);
    }

    public FileItem() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileItem && ((FileItem) o).uri.equals(uri);
    }

    public FileItem(String uri, String name, long size) {
        this.uri = uri;
        this.name = name;
        this.size = size;
    }

    @Override
    public int hashCode() {
        File file = new File(uri);
        return file.exists() ? file.hashCode() : super.hashCode();
    }

    protected FileItem(Parcel in) {
        this.uri = in.readString();
        this.name = in.readString();
        this.size = in.readLong();
    }

    public String readableSize() {
        return StorageUtil.getReadableSize(size);
    }


    public static final Creator<FileItem> CREATOR = new Creator<FileItem>() {
        @Override
        public FileItem createFromParcel(Parcel source) {
            return new FileItem(source);
        }

        @Override
        public FileItem[] newArray(int size) {
            return new FileItem[size];
        }
    };
}
