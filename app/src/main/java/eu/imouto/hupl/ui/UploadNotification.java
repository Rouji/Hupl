package eu.imouto.hupl.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Random;

import eu.imouto.hupl.R;
import eu.imouto.hupl.service.UtilService;
import eu.imouto.hupl.upload.UploadService;
import eu.imouto.hupl.util.Humanify;

public class UploadNotification
{
    private final static Random rnd = new Random();
    private Context context;
    private int notId = -1;
    private NotificationManager notMgr;
    private NotificationCompat.Builder notBldr;
    private NotificationCompat.BigPictureStyle bigPicStyle;

    private String fileName;

    public boolean lights = false;
    public boolean vibrate = false;

    public UploadNotification(Context context)
    {
        this.context = context;

        // set up a notification channel for Android O and newer
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            channelId = "upload_service";
            NotificationChannel chan = new NotificationChannel(channelId,
                                                               "Upload Service",
                                                               NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(0xFF0095FF);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(chan);
        }

        newId();

        notMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notBldr = new NotificationCompat.Builder(context, channelId)
            .setColor(0xFF0095FF)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(0, 0, false)
            .addAction(0, str(R.string.cancel), createCancelPendingIntent())
            .setOngoing(false)
            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
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

    public void setFileName(String name)
    {
        fileName = name;
    }

    public void setThumbnail(Bitmap bitmap)
    {
        if (bitmap == null)
            return;

        bigPicStyle = new NotificationCompat.BigPictureStyle();
        bigPicStyle.bigPicture(bitmap);
        notBldr.setStyle(bigPicStyle);
    }

    public void progress(long uploaded, long fileSize)
    {
        notBldr.setOngoing(true);

        if (fileName != null)
            notBldr.setContentTitle(fileName);
        else
            notBldr.setContentTitle(str(R.string.notification_status_uploading));

        if (fileSize == -1)
        {
            notBldr.setProgress(0, 0, true);
            setContentText(Humanify.byteCount(uploaded));
        } else
        {
            //setProgress doesn't take longs :(
            int prog = (int) (((float) uploaded / fileSize) * 1000.0f);

            String progText = Humanify.byteCount(uploaded) + "/" + Humanify.byteCount(fileSize);
            notBldr.setProgress(1000, prog, false);
            setContentText(progText);
        }

        show();
    }

    public void success(String downloadLink)
    {
        clearActions();
        notBldr.setSmallIcon(R.drawable.ic_cloud_done)

            .addAction(0,
                       str(R.string.share),
                       createSharePendingIntent(downloadLink))

            .addAction(0,
                       str(R.string.copy),
                       createCopyPendingIntent(downloadLink))

            .addAction(0,
                       str(R.string.open),
                       createOpenPendingIntent(downloadLink))

            .setContentTitle(str(R.string.notification_status_complete) + ": " + fileName)
            .setProgress(0, 0, false)
            .setOngoing(false);
        setContentText(downloadLink);

        if (vibrate)
            notBldr.setVibrate(new long[]{0,200});
        if (lights)
            notBldr.setLights(0xff4444ff, 1000, 2000);

        show();
    }

    public void error(String title, String message)
    {
        clearActions();
        notBldr.setSmallIcon(R.drawable.ic_error_outline)
            .setContentTitle(str(R.string.notification_status_failed) + ": " + fileName);
        setContentText(title);

        //apply expandable layout for (potentially) long error messages
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(message);
        notBldr.setStyle(bigTextStyle)
            .setOngoing(false);

        show();
    }

    public void cancel()
    {
        clearActions();
        notBldr.setSmallIcon(R.drawable.ic_error_outline)
                .setContentTitle(str(R.string.cancelled))
                .setProgress(0, 0, false)
                .setOngoing(false);
        setContentText(fileName);
        show();
    }

    public void show()
    {
        notMgr.notify(notId, notBldr.build());
    }

    public void close()
    {
        notMgr.cancel(notId);
    }

    private void clearActions()
    {
        notBldr.mActions.clear();
    }

    private String str(int id)
    {
        return context.getString(id);
    }

    private void setContentText(String str)
    {
        notBldr.setContentText(str);
        if (bigPicStyle != null)
            bigPicStyle.setSummaryText(str);
    }


    //intents for the notification's buttons

    private PendingIntent createSharePendingIntent(String url)
    {
        Intent in = new Intent(Intent.ACTION_SEND);
        in.setType("text/plain");
        in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        in.putExtra(Intent.EXTRA_TEXT, url);
        in = Intent.createChooser(in, str(R.string.share_chooser_title));

        return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), in, 0);
    }

    private PendingIntent createOpenPendingIntent(String url)
    {
        Intent in = new Intent(Intent.ACTION_VIEW);
        in.setData(Uri.parse(url));

        return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), in, 0);
    }

    private PendingIntent createCopyPendingIntent(String url)
    {
        Intent in = UtilService.copyToClipboardIntent(context, url);
        in.setType(url); //ugly hack so pending intents don't overwrite each other
        return PendingIntent.getService(context, 0, in, 0);
    }

    private PendingIntent createCancelPendingIntent()
    {
        Intent in = new Intent(context, UploadService.class);
        in.setAction("eu.imouto.hupl.ACTION_CANCEL");
        return PendingIntent.getService(context, (int) System.currentTimeMillis(), in, 0);
    }
}
