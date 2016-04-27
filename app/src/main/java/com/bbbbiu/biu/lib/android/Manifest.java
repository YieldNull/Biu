package com.bbbbiu.biu.lib.android;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by YieldNull at 4/22/16
 */
public class Manifest implements Iterable<Manifest.Item> {

    public List<Item> manifest = new ArrayList<>();

    public void addItem(Item item) {
        manifest.add(item);
    }

    @Override
    public Iterator<Item> iterator() {
        return manifest.iterator();
    }

    public static class Item {
        public String path; // 因为文件名可能相同，就用绝对路径表示了
        public long size;

        public Item(String path, long size) {
            this.path = path;
            this.size = size;
        }

        public String getName() {
            return new File(path).getName();
        }
    }
}
