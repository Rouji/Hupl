package eu.imouto.hupl.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UploaderDB extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "uploaders";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT);

    public UploaderDB(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(
            "CREATE TABLE uploaders(" +
            "name STRING PRIMARY KEY," +
            "last_used STRING," +
            "json STRING NOT NULL);"
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }

    public UploaderEntry getUploaderByName(String name)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM uploaders WHERE name = ?", new String[]{name});
        if (!c.moveToFirst())
            return null;
        db.close();
        return rowToEntry(c);
    }

    public ArrayList<UploaderEntry> getAllUploaders()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM uploaders ORDER BY datetime(last_used) DESC;", null);
        if (!c.moveToFirst())
            return null;

        ArrayList<UploaderEntry> entries = new ArrayList<>(c.getCount());
        do
        {
            UploaderEntry ue = rowToEntry(c);
            if (ue != null)
                entries.add(ue);
        } while(c.moveToNext());

        db.close();
        return entries;
    }

    public void updateLastUsed(String name)
    {
        SQLiteDatabase db = getWritableDatabase();
        Date now = new Date();

        ContentValues cv = new ContentValues();
        cv.put("last_used", dateFormat.format(now));
        db.update("uploaders", cv, "name = ?", new String[] {name});
        db.close();
    }

    public void deleteUploader(String name)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("uploaders", "name = ?", new String[]{name});
        db.close();
    }

    public void saveUploader(UploaderEntry uploader)
    {
        saveUploader(uploader, null);
    }

    public void saveUploader(UploaderEntry uploader, String oldName)
    {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues(3);
        cv.put("name", uploader.name);
        cv.put("last_used", dateFormat.format(uploader.lastUsed));
        cv.put("json", uploader.json.toString());
        if (oldName == null)
            db.replace("uploaders", null, cv);
        else
            db.update("uploaders", cv, "name = ?", new String[]{oldName});
        db.close();
    }

    private static UploaderEntry rowToEntry(Cursor c)
    {
        UploaderEntry ue = new UploaderEntry();

        ue.name = c.getString(0);
        try
        {
            ue.lastUsed = dateFormat.parse(c.getString(1));
        }
        catch (ParseException e)
        {
            ue.lastUsed = new Date(1,1,1);
        }
        try
        {
            ue.json = new JSONObject(c.getString(2));
        }
        catch (JSONException e)
        {
            ue.json =  new JSONObject();
        }

        return ue;
    }
}
