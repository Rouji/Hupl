package eu.imouto.hupl.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.imouto.hupl.data.UploaderImporter;
import eu.imouto.hupl.data.UploaderEntry;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadService;

public class ChooseUploaderActivity extends DrawerActivity
    implements AdapterView.OnItemClickListener
{
    private UploaderDB uploaderDB;
    private List<FileUriOrText> fileList = new ArrayList<>();

    private static final int READ_EXTERNAL_STORAGE_REQUEST = 1;

    private ListView listView;
    private ChooseUploaderAdapter upAdapter;
    private CheckBox enableResize;
    private TextView errorText;

    private String[] neededPermissions = null;

    @Override
    int onInflateContent() {
        return R.layout.activity_choose_uploader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        }
        else {
            neededPermissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = sp.getBoolean("isFirstRun", true);
        if (firstRun)
        {
            sp.edit().putBoolean("isFirstRun", false).commit();
            try
            {
                new UploaderImporter(this).importFromAssets();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        upAdapter = new ChooseUploaderAdapter(this, new ArrayList<UploaderEntry>());

        listView = (ListView) findViewById(R.id.uploaderList);
        listView.setAdapter(upAdapter);
        listView.setOnItemClickListener(this);

        enableResize = (CheckBox)findViewById(R.id.enableResize);
        errorText = (TextView)findViewById(R.id.errorText);

        uploaderDB = new UploaderDB(getApplicationContext());

        //get the file's uri, if one was provided
        Intent recvIn = getIntent();
        if (recvIn == null || recvIn.getAction() == null)
            return;

        fileList.clear();
        if (recvIn.getAction().equals(Intent.ACTION_SEND))
        {
            fileList.add(
                    new FileUriOrText(
                        recvIn.getParcelableExtra(Intent.EXTRA_STREAM),
                        recvIn.getStringExtra(Intent.EXTRA_TEXT)
                    )
            );
        }
        else if (recvIn.getAction().equals(Intent.ACTION_SEND_MULTIPLE))
        {
            ArrayList<Uri> l = recvIn.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (l!=null) {
                for (Uri uri : l) {
                    fileList.add(new FileUriOrText(uri, null));
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String perm[], int[] grant) {
        super.onRequestPermissionsResult(requestCode, perm, grant);
        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST) {
            if (grant.length > 0 && Arrays.stream(grant).allMatch(g -> g == PackageManager.PERMISSION_GRANTED)) {
                //got permission
                errorText.setVisibility(View.GONE);
                listView.setEnabled(true);
            } else {
                errorText.setVisibility(View.VISIBLE);
                listView.setEnabled(false);
            }
        }
    }

    private boolean havePermissions() {
        for(String perm : neededPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        String strEdit = getResources().getString(R.string.uploaders);
        String strChoose = getResources().getString(R.string.choose_uploader);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        //request permission(s)
        if (!havePermissions())
        {
            errorText.setVisibility(View.VISIBLE);
            listView.setEnabled(false);
        }
        else
        {
            errorText.setVisibility(View.GONE);
            listView.setEnabled(true);
        }

        if (!fileList.isEmpty())
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
        CharSequence buttons[] = new CharSequence[]
        {
                getResources().getString(R.string.new_http_uploader),
                getResources().getString(R.string.import_from_url)
        };

        Dialog.OnClickListener clickListener = new Dialog.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    startEditActivity(null);
                }
                else
                {
                    startImportActivity();
                }
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.new_uploader))
                .setItems(buttons, clickListener)
                .create().show();
    }

    private void startEditActivity(String uploaderName)
    {
        Intent in = new Intent(getApplicationContext(), EditHttpUploaderActivity.class);
        if (uploaderName != null)
            in.putExtra("UPLOADER_NAME", uploaderName);
        startActivity(in);
    }

    private void startImportActivity()
    {
        Intent in = new Intent(this, ImportActivity.class);
        startActivity(in);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        TextView nameView = (TextView)view.findViewById(R.id.name);
        String name = nameView.getText().toString();

        if (fileList.isEmpty())
        {
            startEditActivity(name);
        }
        else
        {
            boolean resize = enableResize.isChecked();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ChooseUploaderActivity.this);
            sp.edit().putBoolean("enableResize", resize).apply();


            for (FileUriOrText fileUri : fileList)
            {
                Intent in = new Intent(this, UploadService.class);
                in.setAction("eu.imouto.hupl.ACTION_QUEUE_UPLOAD");
                in.putExtra("uri", fileUri.uri);
                in.putExtra("text", fileUri.text);
                in.putExtra("uploader", name);
                in.putExtra("compress", resize);
                startService(in);
            }

            uploaderDB.updateLastUsed(name);
            fileList.clear();
            finish();
        }
    }

    public void permissionErrorClick(View view)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO
                    },
                    READ_EXTERNAL_STORAGE_REQUEST);
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    READ_EXTERNAL_STORAGE_REQUEST);
        }

        //open this app's settings
        //Intent in = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        //                       Uri.fromParts("package", getPackageName(), null));
        //in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //checkPerm = true;
        //startActivity(in);
    }

    private class ChooseUploaderAdapter extends ArrayAdapter<UploaderEntry>
    {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
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

            ((TextView)convertView.findViewById(R.id.name)).setText(entry.name);
            String lastUsed = String.format(getResources().getString(R.string.last_used), df.format(entry.lastUsed));
            ((TextView)convertView.findViewById(R.id.lastUsed)).setText(lastUsed);

            try
            {
                if (entry.json.has("type"))
                    ((TextView)convertView.findViewById(R.id.type)).setText(entry.json.getString("type"));
            }
            catch (JSONException e)
            {}

            return convertView;
        }
    }

    private static class FileUriOrText {
        public final Uri uri;
        public final String text;

        public FileUriOrText(Uri uri, String text) {
            this.uri = uri;
            this.text = text;
        }
    }
}
