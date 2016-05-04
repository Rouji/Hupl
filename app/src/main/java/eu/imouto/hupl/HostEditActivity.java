package eu.imouto.hupl;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class HostEditActivity extends PreferenceActivity
{
    private String m_hostId;
    private HostDB m_db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_edit);

        Intent recvIn = getIntent();
        m_hostId = recvIn.getStringExtra("HOST_ID");
        if (m_hostId == null)
        {
            finish();
            return;
        }

        m_db = new HostDB(getApplicationContext());

        Host host = m_db.getHostById(m_hostId);
        if (host == null)
        {
            finish();
            return;
        }


        //add the settings view
        addPreferencesFromResource(R.xml.host_settings);

        //set preference values and assign on-change listener
        EditTextPreference textPref = (EditTextPreference)findPreference("prefTitle");
        textPref.setOnPreferenceChangeListener(new TextPreferenceChangeListener());
        textPref.setText(host.title);

        textPref = (EditTextPreference)findPreference("prefUrl");
        textPref.setOnPreferenceChangeListener(new TextPreferenceChangeListener());
        textPref.setText(host.hostUrl);

        textPref = (EditTextPreference)findPreference("prefParam");
        textPref.setOnPreferenceChangeListener(new TextPreferenceChangeListener());
        textPref.setText(host.fileParam);

        textPref = (EditTextPreference)findPreference("prefUser");
        textPref.setOnPreferenceChangeListener(new TextPreferenceChangeListener());
        textPref.setText(host.authUser);

        textPref = (EditTextPreference)findPreference("prefPass");
        textPref.setOnPreferenceChangeListener(new TextPreferenceChangeListener());
        textPref.setText(host.authPass);

        Preference button = findPreference("prefDel");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                m_db.deleteHost(m_hostId);
                finish();
                return true;
            }
        });
    }

    private class TextPreferenceChangeListener implements EditTextPreference.OnPreferenceChangeListener
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o)
        {
            String key = preference.getKey();
            String value = (String) o;

            switch(key)
            {
                case "prefTitle": m_db.setTitle(m_hostId,value); break;
                case "prefUrl": m_db.setHostUrl(m_hostId,value); break;
                case "prefParam": m_db.setFileParam(m_hostId,value); break;
                case "prefUser": m_db.setAuthUser(m_hostId,value); break;
                case "prefPass": m_db.setAuthPass(m_hostId,value); break;
            }

            return true;
        }
    }
}
