package eu.imouto.hupl.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import eu.imouto.hupl.util.ImageResize;

public class HistoryDB extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "history";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int THUMBNAIL_QUALITY = 70;


    public HistoryDB(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //create table
        db.execSQL(
            "CREATE TABLE history(" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "upload_date STRING," +
            "uploader STRING," +
            "link STRING," +
            "orig_name STRING," +
            "mime STRING," +
            "thumbnail BLOB);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {}

    public void deleteEntry(int id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("history", "id = ?", new String[] {String.valueOf(id)});
    }

    public void deleteEntry(HistoryEntry entry)
    {
        deleteEntry(entry.id);
    }

    public void addEntry(HistoryEntry entry)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        db.insert("history", null, entryToCV(entry));
    }

    public ArrayList<HistoryEntry> getAllEntries()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM history ORDER BY id DESC;", null);
        if (!c.moveToFirst())
            return null;

        ArrayList<HistoryEntry> entries = new ArrayList<>(c.getCount());
        do
        {
            HistoryEntry he = rowToEntry(c);
            if (he != null)
                entries.add(he);
        } while(c.moveToNext());

        return entries;
    }

    private static HistoryEntry rowToEntry(Cursor c)
    {
        SimpleDateFormat df = new SimpleDateFormat(DATETIME_FORMAT);

        HistoryEntry he = new HistoryEntry();
        try
        {
            he.id = c.getInt(0);
            he.uploadDate = df.parse(c.getString(1));
            he.uploader = c.getString(2);
            he.link = c.getString(3);
            he.originalName = c.getString(4);
            he.mime = c.getString(5);
            byte[] jpeg = c.getBlob(6);
            if (jpeg != null)
                he.thumbnail = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        }
        catch (ParseException ex)
        {
            return null;
        }

        return he;
    }

    private static ContentValues entryToCV(HistoryEntry entry)
    {
        SimpleDateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
        byte[] thumb = ImageResize.compress(entry.thumbnail, THUMBNAIL_QUALITY);

        ContentValues cv = new ContentValues(7);
        cv.put("upload_date", df.format(entry.uploadDate));
        cv.put("uploader", entry.uploader);
        cv.put("link", entry.link);
        cv.put("orig_name", entry.originalName);
        cv.put("mime", entry.mime);
        cv.put("thumbnail", thumb);

        return cv;
    }
}
