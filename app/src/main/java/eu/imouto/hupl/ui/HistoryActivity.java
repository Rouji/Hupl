package eu.imouto.hupl.ui;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;

import eu.imouto.hupl.R;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;

public class HistoryActivity extends DrawerActivity
{
    private RecyclerView recyclerView;
    private HistoryDB histDb;
    private HistoryListAdapter histAdapter;
    private Bitmap defaultThumb;

    @Override
    int onInflateContent() {
        return R.layout.activity_history;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.history));

        histAdapter = new HistoryListAdapter(this);

        recyclerView = findViewById(R.id.historyList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(histAdapter);

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

    private void showHistoryDialog(HistoryEntry entry)
    {
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

    private class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder>
    {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
        private final ArrayList<HistoryEntry> entries;

        public HistoryListAdapter(Context context)
        {
            this.entries = new ArrayList<>();
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_row, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position)
        {
            HistoryEntry entry = entries.get(position);

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

            holder.idText.setText(String.valueOf(entry.id));
            holder.linkText.setText(link);
            holder.filenameText.setText(entry.originalName);
            holder.dateText.setText(df.format(entry.uploadDate));
            holder.thumbnail.setImageBitmap(thumb);

            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION)
                    {
                        showHistoryDialog(entries.get(pos));
                    }
                }
            });
        }

        @Override
        public int getItemCount()
        {
            return entries.size();
        }

        public void clear()
        {
            entries.clear();
            notifyDataSetChanged();
        }

        public void addAll(ArrayList<HistoryEntry> newEntries)
        {
            entries.addAll(newEntries);
            notifyDataSetChanged();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder
        {
            TextView idText;
            TextView linkText;
            TextView filenameText;
            TextView dateText;
            ImageView thumbnail;

            HistoryViewHolder(View itemView)
            {
                super(itemView);
                idText = itemView.findViewById(R.id.historyRowId);
                linkText = itemView.findViewById(R.id.historyRowLink);
                filenameText = itemView.findViewById(R.id.historyRowFilename);
                dateText = itemView.findViewById(R.id.historyRowDate);
                thumbnail = itemView.findViewById(R.id.historyRowThumbnail);
            }
        }
    }

    private void deleteEntry(int id)
    {
        histDb.deleteEntry(id);
        updateListView();
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
