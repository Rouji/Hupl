package eu.imouto.hupl.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import java.util.Queue;
import java.util.Random;

import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadService;

public class QueueNotification
{
    private final static Random rnd = new Random();
    private Context context;
    private int notId = -1;
    private NotificationManager notMgr;
    private NotificationCompat.Builder notBldr;

    private String fileName;

    public boolean lights = false;
    public boolean vibrate = false;

    public QueueNotification(Context context)
    {
        this.context = context;

        newId();

        notMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notBldr = new NotificationCompat.Builder(context)
            .setColor(0xFF0095FF)
            .setSmallIcon(R.drawable.ic_cloud_queue)
            .addAction(0, str(R.string.cancel_all), createCancelAllPendingIntent())
            .setOngoing(true);
        setThumbnail(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
    }

    public void newId()
    {
        notId = rnd.nextInt();
    }

    public int getId()
    {
        return notId;
    }

    public Notification getNotification()
    {
        return notBldr.build();
    }

    private void setThumbnail(Bitmap bitmap)
    {
        notBldr.setLargeIcon(bitmap);
    }

    public void setQueue(Queue<String> queuedFiles)
    {
        notBldr.setContentTitle(String.format(str(R.string.uploads_queued),queuedFiles.size()));

        String listTxt = "";
        for (String f : queuedFiles)
            listTxt += f + "\n";

        NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle();
        bts.bigText(listTxt);
        notBldr.setStyle(bts);
        show();
    }

    public void close()
    {
        notMgr.cancel(notId);
    }

    public void show()
    {
        notMgr.notify(notId, notBldr.build());
    }

    private String str(int id)
    {
        return context.getString(id);
    }

    private PendingIntent createCancelAllPendingIntent()
    {
        Intent in = new Intent(context, UploadService.class);
        in.setAction("eu.imouto.hupl.ACTION_CANCEL_ALL");
        return PendingIntent.getService(context, (int) System.currentTimeMillis(), in, 0);
    }
}
