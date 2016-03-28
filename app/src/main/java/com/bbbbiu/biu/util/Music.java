package com.bbbbiu.biu.util;

/**
 * Created by fangdongliang on 16/3/26.
 */
public class Music {
    private String name;
    private String author;
    private String length;
    public Music(String name,String author,String length)
    {
        this.name = name;
        this.author = author;
        this.length = length;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public void setLength(String length)
    {
        this.length = length;
    }
    public void setAuthor(String author)
    {
        this.author = author;
    }
    public  String getName()
    {
        return name;
    }
    public String getAuthor()
    {
        return author;
    }
    public String getLength()
    {
        return length;
    }

}
