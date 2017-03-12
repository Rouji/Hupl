package eu.imouto.hupl.ui;

import android.content.Intent;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.R;
import eu.imouto.hupl.data.UploaderEntry;

public class EditHttpUploaderActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener
{
    private boolean isNew;
    private String oldName;
    private UploaderDB db;
    private UploaderEntry entry;
    private final static String[] jsonPrefs =new String[]
    {
        "targetUrl",
        "fileParam",
        "responseRegex",
        "authUser",
        "authPass"
    };
    private final static Map<String, String> jsonDefaults = initDefaults();
    private static Map<String, String> initDefaults()
    {
        HashMap<String, String> m = new HashMap<>();
        m.put("targetUrl", "https://example.com/");
        m.put("fileParam", "file");
        return m;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_http_uploader);

        //add the settings view
        addPreferencesFromResource(R.xml.http_uploader_settings);

        db = new UploaderDB(this);

        Intent in = getIntent();
        String uploaderName = in.getStringExtra("UPLOADER_NAME");
        if (uploaderName == null)
        {
            isNew = true;
            entry = new UploaderEntry();
            entry.name = "";
            entry.json = new JSONObject();
            try
            {
                entry.json.put("type", "http");
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            isNew = false;
            entry = db.getUploaderByName(uploaderName);
            if (entry == null)
            {
                finish();
                return;
            }
            oldName = entry.name;
        }


        EditTextPreference textPref = (EditTextPreference)findPreference("name");
        textPref.setText(entry.name);
        textPref.setSummary(textPref.getText());
        textPref.setOnPreferenceChangeListener(this);
        for (String p : jsonPrefs)
        {
            textPref = (EditTextPreference)findPreference(p);
            if (textPref == null)
                continue;

            if (isNew)
            {
                String def = jsonDefaults.get(p);
                if (def == null)
                    def = "";
                textPref.setText(def);
            }
            else
            {
                try
                {
                    if (entry.json.has(p))
                        textPref.setText(entry.json.getString(p));
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
            textPref.setSummary(textPref.getText());
            textPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        if (preference instanceof EditTextPreference)
            preference.setSummary((CharSequence)newValue);
        return true;
    }

    public void onDeleteClick(View v)
    {
        if (isNew)
            finish();
        else
        {
            db.deleteUploader(entry.name);
            finish();
        }
    }

    public void onSaveClick(View v)
    {
        EditTextPreference pref = (EditTextPreference)findPreference("name");
        entry.name = pref.getText();

        if (entry.name.isEmpty())
        {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNew && db.getUploaderByName(entry.name) != null)
        {
            Toast.makeText(this, "Uploader with name " + entry.name + " already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String p : jsonPrefs)
        {
            pref = (EditTextPreference)findPreference(p);
            if (pref == null)
                continue;

            try
            {
                String txt = pref.getText();
                if (txt != null && !txt.isEmpty())
                    entry.json.put(p, txt);
                else if (entry.json.has(p))
                    entry.json.remove(p);
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        db.saveUploader(entry, oldName);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
