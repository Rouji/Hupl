package eu.imouto.hupl.ui;

import android.preference.PreferenceActivity;
import android.os.Bundle;

import eu.imouto.hupl.R;

public class GlobalSettingsActivity extends PreferenceActivity
{
    UploadNotification n;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);
        addPreferencesFromResource(R.xml.global_settings);
    }
}
