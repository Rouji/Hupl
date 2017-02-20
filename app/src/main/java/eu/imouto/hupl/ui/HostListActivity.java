package eu.imouto.hupl.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.imouto.hupl.data.Host;
import eu.imouto.hupl.data.HostDB;
import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadManager;

public class HostListActivity extends AppCompatActivity
{
    private HostDB m_db;
    private Uri m_fileUri;

    private ListView m_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_list);

        m_lv = (ListView) findViewById(R.id.hostEditList);

        m_db = new HostDB(getApplicationContext());

        //get the file's uri, if one was provided
        m_fileUri = null;
        Intent recvIn = getIntent();
        if (recvIn.getAction().equalsIgnoreCase(Intent.ACTION_SEND))
        {
            m_fileUri = (Uri) recvIn.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        //fill the listview with data from sql
        updateListView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //reload the list, when we resume after editing an entry
        updateListView();
    }

    private void updateListView()
    {
        ListView lv = (ListView) findViewById(R.id.hostEditList);
        String strEdit = getResources().getString(R.string.actionbar_title_edit_hosts);
        String strChoose = getResources().getString(R.string.actionbar_title_choose_upload_host);

        if (m_fileUri != null)
        {
            //we're sending stuff somewhere
            setTitle(strChoose);
            lv.setOnItemClickListener(new SendItemClickListener());
        }
        else
        {
            //activity was opened without an intent, so we're looking to edit entries
            setTitle(strEdit);
            lv.setOnItemClickListener(new EditItemClickListener());
        }

        ArrayList<Map<String,String>> l = new ArrayList<>();
        HashMap<String,String> m;

        ArrayList<Host> hosts = m_db.getAllHosts();
        for (Host h:hosts)
        {
            m = new HashMap<>();
            m.put("id", h.id);
            m.put("title", h.title);
            m.put("url", h.hostUrl);
            l.add(m);
        }

        String[] from = {"id", "title", "url"};
        int[] to = {R.id.hostRowId, R.id.hostRowTitle, R.id.hostRowUrl};
        SimpleAdapter ad = new SimpleAdapter(this, l, R.layout.host_row,from,to);
        lv.setAdapter(ad);
    }

    public void addHostClick(View v)
    {
        Host host = m_db.createHost();
        if (host != null)
        {
            startEditActivity(host.id);
        }

        updateListView();
    }

    private void startEditActivity(String hostId)
    {
        Intent in = new Intent(getApplicationContext(), HostEditActivity.class);
        in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.putExtra("HOST_ID", hostId);
        startActivity(in);
    }

    private class EditItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            TextView tv = (TextView)view.findViewById(R.id.hostRowId);
            String id = tv.getText().toString();

            startEditActivity(id);
        }
    }

    private class SendItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            TextView tv = (TextView)view.findViewById(R.id.hostRowId);
            String id = tv.getText().toString();

            Host host = m_db.getHostById(id);
            if (host == null)
            {
                finish();
                return;
            }

            uploadFile(m_fileUri, host);
            finish();
        }
    }

    private void uploadFile(Uri fileUri, Host host)
    {
        UploadManager.getInstance().startUpload(getApplicationContext(), fileUri, host.title, true);

    }
}
