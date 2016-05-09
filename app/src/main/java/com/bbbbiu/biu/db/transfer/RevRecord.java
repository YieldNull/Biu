package com.bbbbiu.biu.db.transfer;

import android.content.Context;

import com.bbbbiu.biu.util.StorageUtil;
import com.orm.SugarRecord;

import java.io.File;

/**
 * Created by YieldNull at 5/9/16
 */
public class RevRecord extends SugarRecord {
    private static final String TAG = RevRecord.class.getSimpleName();

    public long timestamp;
    public String name;
    public long size;

    public RevRecord() {
    }

    public RevRecord(long timestamp, String name, long size) {
        this.timestamp = timestamp;
        this.name = name;
        this.size = size;
    }

    public File getFile(Context context) {
        return new File(StorageUtil.getDownloadDir(context), name);
    }
}
