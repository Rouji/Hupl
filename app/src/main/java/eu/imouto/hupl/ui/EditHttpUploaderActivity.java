package eu.imouto.hupl.ui;

import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceCategory;
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
        "authPass",
        "disableChunkedTransfer"
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


        Preference pref = findPreference("name");
        ((EditTextPreference)pref).setText(entry.name);
        pref.setSummary(((EditTextPreference)pref).getText());
        pref.setOnPreferenceChangeListener(this);
        for (String p : jsonPrefs)
        {
            pref = findPreference(p);
            if (pref == null)
                continue;

            if (isNew)
            {
                String def = jsonDefaults.get(p);
                if (pref instanceof EditTextPreference)
                    ((EditTextPreference)pref).setText(def == null ? "" : def);
                if (pref instanceof CheckBoxPreference)
                    ((CheckBoxPreference)pref).setChecked(false);
            }
            else
            {
                try
                {
                    if (entry.json.has(p))
                    {
                        if (pref instanceof EditTextPreference)
                            ((EditTextPreference)pref).setText(entry.json.getString(p));
                        else if (pref instanceof CheckBoxPreference)
                            ((CheckBoxPreference)pref).setChecked(entry.json.getBoolean(p));
                    }
                }
                catch (JSONException e)
                {
                }
            }
            if (pref instanceof EditTextPreference)
                pref.setSummary(((EditTextPreference)pref).getText());
            pref.setOnPreferenceChangeListener(this);
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
        Preference pref = findPreference("name");
        entry.name = ((EditTextPreference)pref).getText();

        if (entry.name.isEmpty())
        {
            Toast.makeText(this, getResources().getString(R.string.please_enter_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNew && db.getUploaderByName(entry.name) != null)
        {
            Toast.makeText(this, String.format(getResources().getString(R.string.uploader_exists), entry.name), Toast.LENGTH_SHORT).show();
            return;
        }

        for (String p : jsonPrefs)
        {
            pref = findPreference(p);
            if (pref == null)
                continue;

            if (entry.json.has(p))
                entry.json.remove(p);

            try
            {
                if (pref instanceof EditTextPreference)
                {
                    String txt = ((EditTextPreference)pref).getText();
                    if (txt != null && !txt.isEmpty())
                        entry.json.put(p, txt);
                }
                else if (pref instanceof CheckBoxPreference)
                {
                    entry.json.put(p, ((CheckBoxPreference)pref).isChecked());
                }
            }
            catch (JSONException e)
            {}
        }

        db.saveUploader(entry, oldName);
        Toast.makeText(this, getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
        finish();
    }
}
