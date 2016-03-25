package com.bbbbiu.biu.http.client;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;

/**
 * 从公网服务器获取到的文件列表。
 * <p/>
 * 由于要从{@link com.bbbbiu.biu.service.PollingService}通过{@link ResultReceiver} 传到Activity,故实现了{@link Parcelable}接口
 */
public class FileItem implements Parcelable {
    public String name;
    public String url;
    public long size;
    public String uid;

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public long getSize() {
        return size;
    }

    public String getUid() {
        return uid;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileItem && hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.url);
        dest.writeLong(this.size);
        dest.writeString(this.uid);
    }

    public FileItem() {
    }

    protected FileItem(Parcel in) {
        this.name = in.readString();
        this.url = in.readString();
        this.size = in.readLong();
        this.uid = in.readString();
    }

    public static final Parcelable.Creator<FileItem> CREATOR = new Parcelable.Creator<FileItem>() {
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
