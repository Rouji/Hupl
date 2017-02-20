package eu.imouto.hupl.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;

public class HistoryActivity extends AppCompatActivity
{
    private ListView listView;
    private HistoryDB histDb;
    private HistoryAdapter histAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        histAdapter = new HistoryAdapter(this, new ArrayList<HistoryEntry>());

        listView = (ListView) findViewById(R.id.historyList);
        listView.setAdapter(histAdapter);

        histDb = new HistoryDB(getApplicationContext());

        setTitle("History");

        //fill the listview with data from sql
        updateListView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        updateListView();
    }

    private void updateListView()
    {
        histAdapter.clear();
        histAdapter.addAll(histDb.getAllEntries());
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
            String link = entry.link;
            if (link.startsWith("http"))
            {
                if (link.charAt(4) == 's')
                    link = link.substring(8);
                else
                    link = link.substring(7);
            }

            ((TextView)convertView.findViewById(R.id.historyRowId)).setText(String.valueOf(entry.id));
            ((TextView)convertView.findViewById(R.id.historyRowLink)).setText(link);
            ((TextView)convertView.findViewById(R.id.historyRowFilename)).setText(entry.originalName);
            ((TextView)convertView.findViewById(R.id.historyRowDate)).setText(df.format(entry.uploadDate));
            ((ImageView)convertView.findViewById(R.id.historyRowThumbnail)).setImageBitmap(entry.thumbnail);

            return convertView;
        }
    }
}
