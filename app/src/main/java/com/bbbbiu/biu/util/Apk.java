package com.bbbbiu.biu.util;

import android.graphics.Bitmap;

/**
 * Created by fangdongliang on 16/3/24.
 */
public class Apk{
    private String name;
    private Bitmap bitmap;
    public Apk(String name,Bitmap bitmap)
    {
        this.name = name;
        this.bitmap = bitmap;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;
    }
    public String getName()
    {
        return name;
    }
    public Bitmap getBitmap()
    {
        return bitmap;
    }

}
