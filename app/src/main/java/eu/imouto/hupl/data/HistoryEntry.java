package eu.imouto.hupl.data;

import android.graphics.Bitmap;

import java.util.Date;

public class HistoryEntry
{
    public int id = -1;
    public Date uploadDate = new Date();
    public String uploader = null;
    public String link = null;
    public String originalName = null;
    public String mime = null;
    public Bitmap thumbnail = null;
}
