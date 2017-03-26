package eu.imouto.hupl.data;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import eu.imouto.hupl.util.StreamUtil;

public class UploaderImporter
{
    private Context context;
    public UploaderImporter(Context context)
    {
        this.context = context;
    }

    public UploaderEntry dbEntryFromFile(String path) throws IOException, JSONException
    {
        return dbEntryFromStream(new FileInputStream(path), path);
    }

    public UploaderEntry dbEntryFromStream(InputStream stream, String filePath) throws IOException, JSONException
    {
        String jsonStr;
        jsonStr = new String(StreamUtil.readAllBytes(stream));

        JSONObject obj;
        obj = new JSONObject(jsonStr);

        if (!obj.has("type"))
        {
            return null;
        }

        final UploaderEntry entry = new UploaderEntry();
        entry.name = new File(filePath).getName();
        entry.name = entry.name.substring(0, entry.name.lastIndexOf('.'));
        entry.json = obj;

        if (obj.has("name"))
        {
            try
            {
                entry.name = obj.getString("name");
                obj.remove("name");
            }
            catch (JSONException e)
            {}
        }

        return entry;
    }

    public void importFromAssets() throws IOException
    {
        AssetManager ass = context.getAssets();
        List<String> files = findUploaderFiles(ass, "uploaders", new LinkedList<String>());
        UploaderDB db = new UploaderDB(context);

        for (String file : files)
        {
            UploaderEntry entry = null;
            try
            {
                entry = dbEntryFromStream(ass.open(file), file);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
                continue;
            }

            if (db.getUploaderByName(entry.name) == null)
            {
                db.saveUploader(entry);
            }
        }
    }

    private List<String> findUploaderFiles(AssetManager ass, String baseDir, List<String> files) throws IOException
    {
        String[] names = ass.list(baseDir);
        for (String n : names)
        {
            if (!n.contains(".")) //is a folder
            {
                findUploaderFiles(ass, baseDir+"/"+n, files);
            }
            else
            {
                files.add(baseDir + "/" + n);
            }
        }

        return files;
    }
}
