package com.bbbbiu.biu.gui.transfer;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.bbbbiu.biu.util.StorageUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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


    public FileItem() {
    }

    public FileItem(String uri, String name, long size) {
        this.uri = uri;
        this.name = name;
        this.size = size;
    }


    public InputStream inputStream(Context context) throws FileNotFoundException {
        Uri uri = Uri.parse(this.uri);

        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            return new FileInputStream(uri.getPath());
        } else {
            return context.getContentResolver().openInputStream(uri);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileItem && o.hashCode() == hashCode();
    }


    @Override
    public int hashCode() {
        return uri.hashCode();
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
