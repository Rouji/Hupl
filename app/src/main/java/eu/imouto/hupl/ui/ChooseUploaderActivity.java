package eu.imouto.hupl.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import eu.imouto.hupl.data.UploaderEntry;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadManager;

public class ChooseUploaderActivity extends DrawerActivity
    implements AdapterView.OnItemClickListener
{
    private UploaderDB uploaderDB;
    private Uri fileUri;

    private ListView listView;
    private ChooseUploaderAdapter upAdapter;
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

        upAdapter = new ChooseUploaderAdapter(this, new ArrayList<UploaderEntry>());

        listView = (ListView) findViewById(R.id.uploaderList);
        listView.setAdapter(upAdapter);
        listView.setOnItemClickListener(this);

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
            enableResize.setVisibility(View.VISIBLE);
            findViewById(R.id.newUploader).setVisibility(View.GONE);
        }
        else
        {
            //activity was opened without an intent, so we're looking to edit entries
            setTitle(strEdit);
            enableResize.setVisibility(View.GONE);
            findViewById(R.id.newUploader).setVisibility(View.VISIBLE);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        enableResize.setChecked(sp.getBoolean("enableResize", false));

        upAdapter.clear();
        ArrayList<UploaderEntry> entries = uploaderDB.getAllUploaders();
        if (entries != null)
            upAdapter.addAll(entries);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        TextView nameView = (TextView)view.findViewById(R.id.uploaderRowName);
        String name = nameView.getText().toString();

        if (fileUri == null)
        {
            startEditActivity(name);
        }
        else
        {
            boolean resize = enableResize.isChecked();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ChooseUploaderActivity.this);
            sp.edit().putBoolean("enableResize", resize).apply();


            UploadManager.getInstance().startUpload(getApplicationContext(), fileUri, name, resize);
            finish();
        }
    }

    private class ChooseUploaderAdapter extends ArrayAdapter<UploaderEntry>
    {
        public ChooseUploaderAdapter(Context context, ArrayList<UploaderEntry> entries)
        {
            super(context, 0, entries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            UploaderEntry entry = getItem(position);
            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.uploader_row, parent, false);

            ((TextView)convertView.findViewById(R.id.uploaderRowName)).setText(entry.name);
            return convertView;
        }
    }
}
