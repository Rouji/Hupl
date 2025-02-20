package eu.imouto.hupl.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class StringMapPreference extends EditTextPreference {
    public static final String SEPARATOR = ":";

    public StringMapPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public StringMapPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StringMapPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StringMapPreference(Context context) {
        super(context);
    }

    public void setObj(JSONObject obj) {
        Map<String, String> map = new HashMap<>();
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                map.put(key, obj.getString(key));
            } catch (JSONException ignored) {
            }
        }
        setMap(map);
    }

    public JSONObject getObj() {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, String> entry : getMap().entrySet()) {
            try {
                obj.put(entry.getKey(), entry.getValue());
            } catch (JSONException ignored) {
            }
        }
        return obj;
    }

    public void setMap(Map<String, String> map) {
        String text = map.entrySet()
                .stream()
                .map(entry -> entry.getKey() + SEPARATOR + " " + entry.getValue())
                .collect(Collectors.joining("\n"));
        setText(text);
    }

    public Map<String, String> getMap() {
        String text = getText();
        String[] lines = text.split("\n");
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split(SEPARATOR,2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }
}
