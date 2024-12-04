package eu.imouto.hupl.upload;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import eu.imouto.hupl.data.FileToUpload;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.data.UploaderEntry;

class UploaderFactory
{
    public static Uploader getUploaderByName(Context context, String name, FileToUpload file)
    {
        UploaderDB db = new UploaderDB(context);
        UploaderEntry entry = db.getUploaderByName(name);
        if (entry == null)
            return null;

        String type = getStr(entry.json, "type");
        if (type.equals("http"))
        {
            HttpUploader up = new HttpUploader(file);
            up.name = entry.name;
            up.targetUrl = getStr(entry.json, "targetUrl");
            up.fileParam = getStr(entry.json, "fileParam");
            up.responseRegex = getStr(entry.json, "responseRegex");
            up.authUser = getStr(entry.json, "authUser");
            up.authPass = getStr(entry.json, "authPass");
            up.disableChunkedTransfer = getBool(entry.json, "disableChunkedTransfer", false);
            up.extraParams = getMap(entry.json, "extraParams");
            up.headers = getHeaders(entry.json, "headers");
            return up;
        }
        return null;
    }

    private static Map<String, String> getMap(JSONObject obj, String name)
    {
        Map<String, String> map = new HashMap<>();
        try
        {
            JSONObject subObj = obj.getJSONObject(name);
            Iterator<String> keys = subObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, subObj.getString(key));
            }
        }
        catch (JSONException ex)
        {}
        return map;
    }

    private static boolean getBool(JSONObject obj, String name, boolean defaultVal)
    {
        boolean b = defaultVal;
        try
        {
            b = obj.getBoolean(name);
        }
        catch (JSONException ex)
        {}
        return b;
    }

    private static String getStr(JSONObject obj, String name)
    {
        String str = null;
        try
        {
            str = obj.getString(name);
        }
        catch (JSONException ex)
        {}
        return str;
    }

    private static Map<String, String> getHeaders(JSONObject obj, String name)
    {
        Map<String, String> headers = new HashMap<>();
        String headersString = getStr(obj, "headers");
        if (headersString == null || headersString.isBlank()) {
            return headers;
        }


        String[] lines = headersString.split(Pattern.quote("\n"));
        for (String line : lines) {
            String[] lineSplit = line.split(Pattern.quote(":"));
            if (lineSplit.length != 2) {
                continue;
            }

            String key = lineSplit[0].trim();
            String value = lineSplit[1].trim();
            headers.put(key, value);
        }

        return headers;
    }
}
