package eu.imouto.hupl.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;

public class HistoryActivity extends DrawerActivity
    implements AdapterView.OnItemClickListener
{
    private ListView listView;
    private HistoryDB histDb;
    private HistoryAdapter histAdapter;
    private Bitmap defaultThumb;

    @Override
    int onInflateContent()
    {
        return R.layout.activity_history;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.history));

        histAdapter = new HistoryAdapter(this, new ArrayList<HistoryEntry>());

        listView = (ListView) findViewById(R.id.historyList);
        listView.setAdapter(histAdapter);
        listView.setOnItemClickListener(this);

        histDb = new HistoryDB(getApplicationContext());

        defaultThumb = BitmapFactory.decodeResource(getResources(), R.drawable.ic_insert_drive_file);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //fill the listview with data from sql
        updateListView();
    }

    private void updateListView()
    {
        histAdapter.clear();
        ArrayList<HistoryEntry> entries =  histDb.getAllEntries();
        if (entries != null)
            histAdapter.addAll(entries);
    }

    private class HistoryAdapter extends ArrayAdapter<HistoryEntry>
    {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

        public HistoryAdapter(Context context, ArrayList<HistoryEntry> entries)
        {
            super(context, 0, entries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            HistoryEntry entry = getItem(position);
            if (convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.history_row, parent,false);
            }

            //strip protocol
            String link = entry.link == null ? "Null" : entry.link;
            if (link.startsWith("http") && link.length() > 12)
            {
                if (link.charAt(4) == 's')
                    link = link.substring(8);
                else
                    link = link.substring(7);
            }

            Bitmap thumb = entry.thumbnail;
            if (thumb == null)
                thumb = defaultThumb;

            ((TextView)convertView.findViewById(R.id.historyRowId)).setText(String.valueOf(entry.id));
            ((TextView)convertView.findViewById(R.id.historyRowLink)).setText(link);
            ((TextView)convertView.findViewById(R.id.historyRowFilename)).setText(entry.originalName);
            ((TextView)convertView.findViewById(R.id.historyRowDate)).setText(df.format(entry.uploadDate));
            ((ImageView)convertView.findViewById(R.id.historyRowThumbnail)).setImageBitmap(thumb);

            return convertView;
        }
    }

    private void deleteEntry(int id)
    {
        histDb.deleteEntry(id);
        updateListView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        HistoryEntry entry = histAdapter.getItem(position);

        CharSequence buttons[] = new CharSequence[]
        {
                getResources().getString(R.string.share),
                getResources().getString(R.string.copy),
                getResources().getString(R.string.open),
                getResources().getString(R.string.notification),
                getResources().getString(R.string.delete)
        };

        new AlertDialog.Builder(this).
        setTitle(entry.link).
        setItems(buttons, new DialogOnClickListener(entry)).
        create().show();
    }

    private class DialogOnClickListener implements DialogInterface.OnClickListener
    {
        private HistoryEntry entry;
        public DialogOnClickListener(HistoryEntry entry)
        {
            this.entry = entry;
        }

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            Context context = HistoryActivity.this;
            Intent in = null;
            switch (which)
            {
                case 0:
                    in = new Intent(Intent.ACTION_SEND);
                    in.setType("text/plain");
                    in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    in.putExtra(Intent.EXTRA_TEXT, entry.link);
                    in = Intent.createChooser(in, context.getResources().getString(R.string.share_chooser_title));
                    context.startActivity(in);
                    break;
                case 1:
                    ClipboardManager clipMgr = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData data = ClipData.newPlainText("url", entry.link);
                    clipMgr.setPrimaryClip(data);
                    Toast.makeText(context, context.getResources().getString(R.string.toast_url_copied), Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    in = new Intent(Intent.ACTION_VIEW);
                    in.setData(Uri.parse(entry.link));
                    try
                    {
                        context.startActivity(in);
                    }
                    catch (ActivityNotFoundException ex)
                    {
                        Toast.makeText(context, getResources().getString(R.string.no_activity), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 3:
                    UploadNotification not = new UploadNotification(context);
                    not.setFileName(entry.originalName);
                    not.setThumbnail(entry.thumbnail);
                    not.success(entry.link);
                    break;
                case 4:
                    HistoryActivity.this.deleteEntry(entry.id);
                    break;

            }
        }
    }

}
