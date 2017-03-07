package eu.imouto.hupl.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.data.UploaderEntry;
import eu.imouto.hupl.util.SimpleDownload;

public class ImportActivity extends Activity
{
    private Uri uri;
    private File downloadedFile = null;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        Intent in = getIntent();
        if (in == null)
        {
            finish();
            return;
        }

        uri = in.getData();

        ((TextView)findViewById(R.id.url)).setText(uri.toString());

        status = (TextView)findViewById(R.id.status);


        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                downloadedFile = new SimpleDownload(getCacheDir().toString()).download(uri.toString());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid)
            {
                super.onPostExecute(aVoid);
                importFile();
            }
        }.execute();
    }


    private void importFile()
    {
        if (downloadedFile == null)
        {
            status.setText("Couldn't retrieve the file");
        }

        String jsonStr = "";
        try
        {
            FileInputStream in = new FileInputStream(downloadedFile.toString());
            byte[] buffer = new byte[1024];
            int n = -1;
            while ((n = in.read(buffer)) != -1)
            {
                jsonStr += new String(buffer, 0, n);
            }
        }
        catch (IOException ex)
        {
            status.setText("Couldn't read the file: " + ex.getMessage());
            return;
        }

        JSONObject obj;
        try
        {
            obj = new JSONObject(jsonStr);
        }
        catch (JSONException ex)
        {
            status.setText("Invalid JSON: " + ex.getMessage());
            return;
        }

        if (!obj.has("type"))
        {
            status.setText("File doesn't contain a valid uploader definition");
        }

        final UploaderEntry entry = new UploaderEntry();
        entry.name = downloadedFile.getName();
        entry.name = entry.name.substring(entry.name.lastIndexOf('.')+1, entry.name.length());
        entry.json = obj;

        if (obj.has("name"))
        {
            try
            {
                entry.name = obj.getString("name");
                obj.remove("name");
            }
            catch (JSONException e)
            {}
        }

        UploaderDB db = new UploaderDB(this);
        while (db.getUploaderByName(entry.name) != null)
        {
            status.setText(String.format("\"%1$s\" already exists, renaming to \"%1$s_\"", entry.name));
            entry.name += "_";
        }

        db.saveUploader(entry);


        CharSequence buttons[] = new CharSequence[]
        {
                getResources().getString(R.string.edit),
                getResources().getString(R.string.close)
        };

        Dialog.OnClickListener clickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == 0)
                {
                    Intent in = new Intent(getApplicationContext(), EditHttpUploaderActivity.class);
                    in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    in.putExtra("UPLOADER_NAME", entry.name);
                    startActivity(in);
                }
                else
                {
                    finish();
                }
            }
        };

        new AlertDialog.Builder(this).
                setTitle(String.format(getResources().getString(R.string.uploader_imported), entry.name)).
                setItems(buttons, clickListener).
                create().show();
    }

}
