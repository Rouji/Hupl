package eu.imouto.hupl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;

public class HostDB
{
    private SQLiteDatabase m_db;

    private final String m_tableName = "hosts";

    private final String m_createQuery =
            "CREATE TABLE IF NOT EXISTS hosts("+
                    "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                    "title VARCHAR,"+
                    "hostUrl VARCHAR,"+
                    "fileParam VARCHAR,"+
                    "authUser VARCHAR,"+
                    "authPass VARCHAR);";

    private final String m_addDefaultsQuery =
            "INSERT INTO hosts(title,hostUrl,fileParam,authUser,authPass)"+
                    "VALUES ('Imouto','https://filedump.imouto.eu/','file','','');";

    public HostDB(Context context)
    {
        m_db = context.openOrCreateDatabase("hupl", Context.MODE_PRIVATE, null);

        //check if the table exists
        try
        {
            Cursor cursor = m_db.rawQuery("SELECT * FROM hosts LIMIT 1", null);
            if (cursor == null || !cursor.moveToFirst()) {
                createTable();
            }
        }
        catch (SQLiteException ex)
        {
            createTable();
        }
    }

    private void createTable()
    {
        //create table and add default host(s)
        m_db.execSQL(m_createQuery);
        m_db.execSQL(m_addDefaultsQuery);
    }

    public Host getHostById(String id)
    {
        Cursor cursor = m_db.query(m_tableName,
                new String[]{"title","hostUrl","fileParam","authUser","authPass"},
                "id = ?",
                new String[]{id},
                null,null,null);

        if (cursor == null || cursor.getColumnCount() < 1)
        {
            return null;
        }
        cursor.moveToFirst();

        Host host = new Host();
        host.id = id;
        host.title = cursor.getString(0);
        host.hostUrl = cursor.getString(1);
        host.fileParam = cursor.getString(2);
        host.authUser = cursor.getString(3);
        host.authPass = cursor.getString(4);

        return host;
    }

    public ArrayList<Host> getAllHosts()
    {
        ArrayList<Host> list = new ArrayList<>();

        Cursor cursor = m_db.query(m_tableName,
                new String[]{"id","title","hostUrl","fileParam","authUser","authPass"},
                null,null,null,null,null);

        if (cursor == null || cursor.getColumnCount() < 1)
        {
            return list;
        }

        while (cursor.moveToNext())
        {
            Host host = new Host();
            host.id = cursor.getString(0);
            host.title = cursor.getString(1);
            host.hostUrl = cursor.getString(2);
            host.fileParam = cursor.getString(3);
            host.authUser = cursor.getString(4);
            host.authPass = cursor.getString(5);
            list.add(host);
        }
        return list;
    }

    public boolean deleteHost(String id)
    {
        m_db.delete("hosts","id = ?", new String[] {id});
        return true;
    }

    public Host createHost()
    {
        Host host = new Host();
        host.title = "New Host";
        host.hostUrl = "https://example.com/";
        host.fileParam = "file";

        ContentValues cv = new ContentValues();
        cv.put("title", host.title);
        cv.put("hostUrl", host.hostUrl);
        cv.put("fileParam",host.fileParam);
        long id = m_db.insert(m_tableName,null, cv);
        if (id == -1)
        {
            return null;
        }

        host.id = String.valueOf(id);

        return host;
    }

    public boolean saveHost(Host host)
    {
        if (host == null)
            return false;

        try {
            String sql = "UPDATE hosts SET " +
                    "title = \"" + host.title + "\" " +
                    "hostUrl = \"" + host.hostUrl + "\" " +
                    "fileParam = \"" + host.fileParam + "\" " +
                    "authUser = \"" + host.authUser + "\" " +
                    "authPass = \"" + host.authPass + "\" " +
                    " WHERE id = " + host.id;
            m_db.execSQL(sql);
        }
        catch (Exception ex)
        {
            Log.e("hostdb",ex.getMessage());
            return false;
        }
        return true;
    }

    public boolean setTitle(String id, String title) {return updateField(id,"title",title);}
    public boolean setHostUrl(String id, String url) {return updateField(id,"hostUrl",url);}
    public boolean setFileParam(String id, String param) {return updateField(id,"fileParam",param);}
    public boolean setAuthUser(String id, String user) {return updateField(id,"authUser",user);}
    public boolean setAuthPass(String id, String pass) {return updateField(id,"authPass",pass);}


    private boolean updateField(String id, String fieldName, String value)
    {
        try {
            String sql = "UPDATE hosts SET " + fieldName + " = \"" + value + "\" WHERE id = " + id;
            m_db.execSQL(sql);
        }
        catch (Exception ex)
        {
            Log.e("hostdb",ex.getMessage());
            return false;
        }
        return true;
    }
}
