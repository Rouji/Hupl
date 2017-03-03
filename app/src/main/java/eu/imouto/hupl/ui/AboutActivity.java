package eu.imouto.hupl.ui;

import android.os.Bundle;

import eu.imouto.hupl.R;

public class AboutActivity extends DrawerActivity
{

    @Override
    int onInflateContent()
    {
        return R.layout.activity_about;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getString(R.string.about));
    }
}
