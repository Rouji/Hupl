package eu.imouto.hupl.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.imouto.hupl.data.UploaderEntry;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadManager;

public class ChooseUploaderActivity extends DrawerActivity
{
    private UploaderDB uploaderDB;
    private Uri fileUri;

    private ListView listView;
    private CheckBox enableResize;

    @Override
    int onInflateContent()
    {
        return R.layout.activity_choose_uploader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        listView = (ListView) findViewById(R.id.uploaderList);
        enableResize = (CheckBox)findViewById(R.id.enableResize);

        uploaderDB = new UploaderDB(getApplicationContext());

        //get the file's uri, if one was provided
        fileUri = null;
        Intent recvIn = getIntent();
        if (recvIn != null &&
                recvIn.getAction() != null &&
                recvIn.getAction().equalsIgnoreCase(Intent.ACTION_SEND))
        {
            fileUri = recvIn.getParcelableExtra(Intent.EXTRA_STREAM);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String strEdit = getResources().getString(R.string.uploaders);
        String strChoose = getResources().getString(R.string.choose_uploader);

        if (fileUri != null)
        {
            //we're sending stuff somewhere
            setTitle(strChoose);
            listView.setOnItemClickListener(new SendItemClickListener());

            enableResize.setVisibility(View.VISIBLE);
            findViewById(R.id.newUploader).setVisibility(View.GONE);
        }
        else
        {
            //activity was opened without an intent, so we're looking to edit entries
            setTitle(strEdit);
            listView.setOnItemClickListener(new EditItemClickListener());

            enableResize.setVisibility(View.GONE);
            findViewById(R.id.newUploader).setVisibility(View.VISIBLE);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        enableResize.setChecked(sp.getBoolean("enableResize", false));

        ArrayList<Map<String,String>> l = new ArrayList<>();
        HashMap<String,String> m;

        ArrayList<UploaderEntry> uploaderEntries = uploaderDB.getAllUploaders();
        if (uploaderEntries == null)
            return;

        for (UploaderEntry entry: uploaderEntries)
        {
            m = new HashMap<>();
            m.put("name", entry.name);
            l.add(m);
        }

        String[] from = {"name"};
        int[] to = {R.id.uploaderRowName};
        SimpleAdapter ad = new SimpleAdapter(this, l, R.layout.uploader_row, from, to);
        listView.setAdapter(ad);
    }

    public void newUploaderClick(View v)
    {
        startEditActivity(null);
    }

    private void startEditActivity(String uploaderName)
    {
        Intent in = new Intent(getApplicationContext(), EditHttpUploaderActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (uploaderName != null)
            in.putExtra("UPLOADER_NAME", uploaderName);
        startActivity(in);
    }

    private class EditItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            TextView tv = (TextView)view.findViewById(R.id.uploaderRowName);
            String name = tv.getText().toString();

            startEditActivity(name);
        }
    }

    private class SendItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            TextView tv = (TextView)view.findViewById(R.id.uploaderRowName);
            String name = tv.getText().toString();


            boolean resize = enableResize.isChecked();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ChooseUploaderActivity.this);
            sp.edit().putBoolean("enableResize", resize).apply();


            UploadManager.getInstance().startUpload(getApplicationContext(), fileUri, name, resize);
            finish();
        }
    }
}
