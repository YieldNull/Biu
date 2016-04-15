package com.bbbbiu.biu.util.dbmodel;

import com.orm.SugarRecord;
import com.orm.dsl.Unique;

/**
 * Created by YieldNull at 4/15/16
 */
public class FileCate extends SugarRecord {
    @Unique
    public String path;
    public int type;

    public FileCate() {
    }

    public FileCate(String path, int type) {
        this.path = path;
        this.type = type;
    }
}
