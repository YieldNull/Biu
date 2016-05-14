package com.bbbbiu.biu.db.transfer;

import android.content.Context;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YieldNull at 5/9/16
 */

@Table(database = RevRecord.MyDatabase.class)
public class RevRecord extends BaseModel {
    @Database(name = MyDatabase.NAME, version = MyDatabase.VERSION)
    public class MyDatabase {

        public static final String NAME = "history";

        public static final int VERSION = 1;
    }

    @PrimaryKey(autoincrement = true)
    public long id;

    @Column
    public long timestamp;

    @Column
    public String name;

    @Column
    public long size;

    public RevRecord() {
    }


    public static List<RevRecord> queryAll(Context context) {
        List<RevRecord> records = SQLite.select()
                .from(RevRecord.class)
                .queryList();

        File dir = StorageUtil.getDownloadDir(context);

        List<RevRecord> recordsToDelete = new ArrayList<>();

        for (RevRecord record : records) {
            File file = new File(dir, record.name);

            if (!file.exists()) {
                recordsToDelete.add(record);
            }
        }

        records.removeAll(recordsToDelete);

        return records;
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
