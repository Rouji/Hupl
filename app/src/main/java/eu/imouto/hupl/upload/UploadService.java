package eu.imouto.hupl.upload;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import eu.imouto.hupl.data.FileToUpload;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;
import eu.imouto.hupl.ui.QueueNotification;
import eu.imouto.hupl.ui.UploadNotification;
import eu.imouto.hupl.util.FilenameUtil;
import eu.imouto.hupl.util.ImageResize;
import eu.imouto.hupl.util.StreamUtil;
import eu.imouto.hupl.util.UriResolver;

public class UploadService extends Service implements UploadProgressReceiver
{
    private QueueNotification queueNotification;
    private SharedPreferences pref;
    private int updatesPerSec;
    private UploadNotification notification;
    private Uploader uploader;
    private HistoryEntry historyEntry;
    private Queue<QueueEntry> uploadQueue = new LinkedList<>();
    private Thread uploaderThread;
    private HistoryDB histDb = new HistoryDB(this);
    private boolean uploading = false;
    private boolean dismissOnCancel = true;
    private long lastUpdate = 0;

    private class QueueEntry
    {
        public String uploader;
        public FileToUpload file;
        public boolean compress;
    }

    public UploadService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        pref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        updatesPerSec = Integer.parseInt(pref.getString("notification_updates_per_sec", "5"));
        dismissOnCancel = pref.getBoolean("notification_dismiss_on_cancel", dismissOnCancel);

        if (intent == null)
            return START_STICKY;

        String act = intent.getAction();
        if (act.equals("eu.imouto.hupl.ACTION_QUEUE_UPLOAD"))
        {
            QueueEntry e = new QueueEntry();
            e.uploader = intent.getStringExtra("uploader");
            e.compress = intent.getBooleanExtra("compress", false);
            Uri uri = intent.getParcelableExtra("uri");
            e.file = UriResolver.uriToFile(this, uri);
            if (e.file == null)
                return START_STICKY; //TODO: display an error or something

            uploadQueue.add(e);
            startUpload();
            updateQueueNotification();
        }
        else if(act.equals("eu.imouto.hupl.ACTION_CANCEL"))
        {
            if (uploader != null)
                uploader.cancel();
        }
        else if(act.equals("eu.imouto.hupl.ACTION_CANCEL_ALL"))
        {
            uploadQueue.clear();
            updateQueueNotification();
        }

        return START_STICKY;
    }

    private void startUpload()
    {
        if (uploading)
            return;

        QueueEntry e = uploadQueue.poll();
        if (e == null)
            return;


        uploader = UploaderFactory.getUploaderByName(this, e.uploader, e.file);
        if (uploader == null)
            return;
        uploader.setProgessReceiver(this);

        //handle thumbnails (and compression) for images
        Bitmap thumb = null;
        if (e.file.isImage())
        {
            byte[] orig = new byte[0];
            try
            {
                orig = StreamUtil.readAllBytes(e.file.stream);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                return;
            }

            Bitmap bm = BitmapFactory.decodeByteArray(orig, 0, orig.length);
            thumb = ImageResize.thumbnail(bm);
            if (e.compress)
            {
                int w = Integer.parseInt(pref.getString("image_resize_width", "1000"));
                int h = Integer.parseInt(pref.getString("image_resize_height", "1000"));
                int q = Integer.parseInt(pref.getString("image_resize_quality", "70"));
                bm = ImageResize.resizeToFit(bm, w, h);
                orig = ImageResize.compress(bm, q);
                e.file.fileName = FilenameUtil.replaceExtension(e.file.fileName, "jpg");
            }
            e.file.stream = new ByteArrayInputStream(orig);
        }

        notification = new UploadNotification(this);
        notification.lights = pref.getBoolean("notification_light", false);
        notification.vibrate = pref.getBoolean("notification_vibrate", false);
        notification.setFileName(e.file.fileName);
        notification.setThumbnail(thumb);

        historyEntry = new HistoryEntry();
        historyEntry.originalName = e.file.fileName;
        historyEntry.mime = e.file.mime;
        historyEntry.uploader = uploader.name;
        historyEntry.thumbnail = thumb;

        startForeground(notification.getId(), notification.getNotification());
        uploaderThread = new Thread(uploader);
        uploaderThread.start();
        uploading = true;
    }

    private void updateQueueNotification()
    {
        if (uploadQueue.isEmpty() && queueNotification != null)
        {
            queueNotification.close();
            queueNotification = null;
        }
        if (uploadQueue.isEmpty())
            return;

        if (queueNotification == null)
            queueNotification = new QueueNotification(this);

        Queue<String> files = new LinkedList<>();
        for (QueueEntry e : uploadQueue)
        {
            files.add(e.file.fileName);
        }
        queueNotification.setQueue(files);
    }

    @Override
    public void onUploadProgress(int uploaded, int fileSize)
    {
        long now = System.currentTimeMillis();
        if ((now - lastUpdate) > 1000/updatesPerSec)
        {
            notification.progress(uploaded, fileSize);
            lastUpdate = now;
        }
    }

    @Override
    public void onUploadFinished(String fileLink)
    {
        historyEntry.link = fileLink;
        histDb.addEntry(historyEntry);
        histDb.prune(Integer.parseInt(pref.getString("history_size", "1000")));

        notification.success(fileLink);
        uploading = false;
        stopFG();
        startUpload();
        updateQueueNotification();
    }

    @Override
    public void onUploadFailed(String title, String message)
    {
        notification.error(title, message);
        uploading = false;
        stopFG();
        startUpload();
        updateQueueNotification();
    }

    @Override
    public void onUploadCancelled()
    {
        uploading = false;
        stopFG();

        if (dismissOnCancel)
            notification.close();
        else
            notification.cancel();

        startUpload();
        updateQueueNotification();
    }

    private void stopFG()
    {
        stopForeground(true);
        notification.newId();
        notification.show();
    }
}
