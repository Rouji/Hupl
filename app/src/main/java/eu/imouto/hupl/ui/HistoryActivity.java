package eu.imouto.hupl.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;

public class HistoryActivity extends DrawerActivity
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

        setTitle("History");

        histAdapter = new HistoryAdapter(this, new ArrayList<HistoryEntry>());

        listView = (ListView) findViewById(R.id.historyList);
        listView.setAdapter(histAdapter);

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
        DateFormat df;

        public HistoryAdapter(Context context, ArrayList<HistoryEntry> entries)
        {
            super(context, 0, entries);
            df = DateFormat.getDateInstance(DateFormat.LONG);
        }

        @NonNull
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
            if (link.startsWith("http"))
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
}
