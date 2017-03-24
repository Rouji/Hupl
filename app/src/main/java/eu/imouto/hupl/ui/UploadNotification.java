package eu.imouto.hupl.ui;

import android.app.Notification;
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
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Random;

import eu.imouto.hupl.R;
import eu.imouto.hupl.upload.UploadService;
import eu.imouto.hupl.util.Humanify;

public class UploadNotification
{
    private final static Random rnd = new Random();
    private Context context;
    private int notId = -1;
    private NotificationManager notMgr;
    private NotificationCompat.Builder notBldr;

    private String fileName;

    public boolean lights = false;
    public boolean vibrate = false;

    public UploadNotification(Context context)
    {
        this.context = context;

        newId();

        notMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notBldr = new NotificationCompat.Builder(context)
            .setColor(0xFF0095FF)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(0, 0, false)
            .addAction(0, str(R.string.cancel), createCancelPendingIntent())
            .setOngoing(false);
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

    public void setFileName(String name)
    {
        fileName = name;
    }

    public void setThumbnail(Bitmap bitmap)
    {
        notBldr.setLargeIcon(bitmap);
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
            notBldr.setContentText(Humanify.byteCount(uploaded))
                .setProgress(0, 0, true);
        } else
        {
            //setProgress doesn't take longs :(
            int prog = (int) (((float) uploaded / fileSize) * 1000.0f);

            String progText = Humanify.byteCount(uploaded) + "/" + Humanify.byteCount(fileSize);
            notBldr.setContentText(progText)
                .setProgress(1000, prog, false);
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
            .setContentText(downloadLink)
            .setProgress(0, 0, false)
            .setOngoing(false);

        if (vibrate)
            notBldr.setVibrate(new long[]{0,200});
        if (lights)
            notBldr.setLights(0xff4444ff, 1000, 2000);

        show();
    }

    public void error(String error)
    {
        clearActions();
        notBldr.setSmallIcon(R.drawable.ic_error_outline)
            .setContentTitle(str(R.string.notification_status_failed) + ": " + fileName)
            .setContentText(error);

//        notBldr.addAction(0, "Retry", null); //TODO: retry action

        //apply expandable layout for (potentially) long error messages
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.bigText(error);
        notBldr.setStyle(bigTextStyle)
            .setOngoing(false);

        show();
    }

    public void cancel()
    {
        clearActions();
        notBldr.setSmallIcon(R.drawable.ic_error_outline)
                .setContentTitle(str(R.string.cancelled))
                .setContentText(fileName)
                .setProgress(0, 0, false)
                .setOngoing(false);

//        notBldr.addAction(0, "Retry", null); //TODO retry action
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
        CopyBroadcastReceiver receiver = new CopyBroadcastReceiver(url);
        IntentFilter filter = new IntentFilter("eu.imouto.hupl.ACTION_COPY");
        context.registerReceiver(receiver, filter);//TODO: probably should unregister these receivers at some point?

        Intent in = new Intent("eu.imouto.hupl.ACTION_COPY");

        return PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), in, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent createCancelPendingIntent()
    {
        Intent in = new Intent(context, UploadService.class);
        in.setAction("eu.imouto.hupl.ACTION_CANCEL");
        return PendingIntent.getService(context, (int) System.currentTimeMillis(), in, 0);
    }

    private class CopyBroadcastReceiver extends BroadcastReceiver
    {
        private String url;

        public CopyBroadcastReceiver(String url)
        {
            this.url = url;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String strCopied = context.getResources().getString(R.string.toast_url_copied);
            ClipboardManager clipMgr = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("url", url);
            clipMgr.setPrimaryClip(data);
            Toast.makeText(context, strCopied, Toast.LENGTH_SHORT).show();

            //hide notification tray
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }
}
