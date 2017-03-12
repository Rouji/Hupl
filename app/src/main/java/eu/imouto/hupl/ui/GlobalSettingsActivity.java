package eu.imouto.hupl.ui;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import java.util.LinkedList;
import java.util.List;

import eu.imouto.hupl.R;

public class GlobalSettingsActivity extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener
{
    UploadNotification n;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);
        addPreferencesFromResource(R.xml.global_settings);

        List<Preference> prefs = getPreferenceList(getPreferenceScreen(), new LinkedList<Preference>());
        for (Preference p : prefs)
        {
            if (p instanceof EditTextPreference)
            {
                String val = ((EditTextPreference)p).getText();
                p.setSummary(val);
            }
            p.setOnPreferenceChangeListener(this);
        }
    }

    private List<Preference> getPreferenceList(Preference p, List<Preference> list)
    {
        if (p instanceof PreferenceCategory || p instanceof PreferenceScreen)
        {
            PreferenceGroup g = (PreferenceGroup) p;
            int pCount = g.getPreferenceCount();
            for (int i = 0; i < pCount; i++)
            {
                getPreferenceList(g.getPreference(i), list);
            }
        }
        else
        {
            list.add(p);
        }
        return list;
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object newValue)
    {
        if (p instanceof EditTextPreference)
        {
            p.setSummary((CharSequence) newValue);
        }
        return true;
    }
}
