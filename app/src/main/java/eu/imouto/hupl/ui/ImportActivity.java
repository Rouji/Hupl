package eu.imouto.hupl.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.UploaderDB;
import eu.imouto.hupl.data.UploaderEntry;
import eu.imouto.hupl.data.UploaderImporter;
import eu.imouto.hupl.util.SimpleDownload;

public class ImportActivity extends Activity
{
    private Uri uri;
    private File downloadedFile = null;
    private TextView url;
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

        url = (TextView) findViewById(R.id.url);
        status = (TextView) findViewById(R.id.status);
        uri = in.getData();

        if (uri != null)
        {
            url.setText(uri.toString());
            startDownloadTask();
        }
        else
        {
            showUrlDiag();
        }
    }

    private void addLine(String line)
    {
        status.setText(status.getText() + "\n" + line);
    }

    private void showUrlDiag()
    {
        final EditText txtUrl = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.enter_config_url))
                .setView(txtUrl)
                .setPositiveButton(getResources().getString(R.string.uploader_import), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        uri = Uri.parse(txtUrl.getText().toString());
                        if (uri != null)
                        {
                            startDownloadTask();
                        }
                    }
                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        finish();
                    }
                })
                .show();
    }

    private void startDownloadTask()
    {
        url.setText(uri.toString());
       addLine("Downloading file...");

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
            addLine("Couldn't retrieve the file");
        }

        final UploaderEntry entry;
        try
        {
            entry = new UploaderImporter(this).dbEntryFromFile(downloadedFile.toString());
        }
        catch (IOException e)
        {
            addLine("Couldn't read the file: " + e.getMessage());
            return;
        }
        catch (JSONException e)
        {
            addLine("Invalid JSON: " + e.getMessage());
            return;
        }

        if (entry == null)
        {
            addLine("File doesn't contain a valid uploader definition");
            return;
        }

        UploaderDB db = new UploaderDB(this);
        while (db.getUploaderByName(entry.name) != null)
        {
            addLine(String.format("\"%1$s\" already exists, renaming to \"%1$s_\"", entry.name));
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
                    finish();
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
