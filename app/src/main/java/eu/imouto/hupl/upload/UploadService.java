package eu.imouto.hupl.upload;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.util.SparseArray;

import eu.imouto.hupl.ui.UploadNotification;

public class UploadService extends Service
{
    private static final String ACTION_FOO = "eu.imouto.hupl.service.action.FOO";
    private SparseArray<Upload> currentUploads = new SparseArray<>(5);

    public UploadService()
    {
        super();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public static void startActionFoo(Context context, String param1, String param2)
    {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_FOO);
        context.startService(intent);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        UploadNotification n = new UploadNotification(this, ++a);
        startForeground(a, n.getNotification());
        n.success("hurr.com");

        n = new UploadNotification(this, 29);
        n.progress(10, 100);
        return START_NOT_STICKY;
    }

    private class Upload implements UploadProgressReceiver
    {
        private UploadNotification not;
        private Uploader up;
        private HistoryEntry hist = new HistoryEntry();
        private Context context;
        private boolean cancelled = false;

        public Upload(Context context, Uploader uploader, boolean compress)
        {
            this.context = context;

            up = uploader;
            up.setProgessReceiver(this);

            FileToUpload ftu = up.getFileToUpload();

            not = new UploadNotification(context, up.getFileToUpload().getId());
            not.setFileName(ftu.fileName);

            //handle thumbnails (and compression) for images
            Bitmap thumb = null;
            if (ftu.isImage())
            {
                byte[] orig = new byte[0];
                try
                {
                    orig = StreamUtil.readAllBytes(ftu.stream);
                }
                catch (IOException e)
                {
                    not.error(e.getLocalizedMessage());
                    removeUpload(up.getFileToUpload().getId());
                }

                Bitmap bm = BitmapFactory.decodeByteArray(orig, 0, orig.length);
                thumb = ImageResize.thumbnail(bm);
                if (compress)
                {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    int w = Integer.parseInt(sp.getString("image_resize_width", "1000"));
                    int h = Integer.parseInt(sp.getString("image_resize_height", "1000"));
                    int q = Integer.parseInt(sp.getString("image_resize_quality", "70"));
                    bm = ImageResize.resizeToFit(bm, w, h);
                    orig = ImageResize.compress(bm, q);
                }
                ftu.stream = new ByteArrayInputStream(orig);
            }

            not.setThumbnail(thumb);

            hist.originalName = ftu.fileName;
            hist.mime = ftu.mime;
            hist.thumbnail = thumb;
            hist.uploader = uploader.name;
        }

        public void start()
        {
            new Thread(up).start();
        }

        public void cancel()
        {
            up.cancel();
        }

        @Override
        public void onUploadProgress(int uploaded, int fileSize)
        {
            if (!cancelled)
                not.progress(uploaded, fileSize);
        }

        @Override
        public void onUploadFinished(String fileLink)
        {
            hist.link = fileLink;
            HistoryDB histDb = new HistoryDB(context);
            histDb.addEntry(hist);

            not.success(fileLink);
            removeUpload(up.getFileToUpload().getId());
        }

        @Override
        public void onUploadFailed(String error)
        {
            not.error(error);
            removeUpload(up.getFileToUpload().getId());
        }

        @Override
        public void onUploadCancelled()
        {
            cancelled = true;
            not.cancel();
            removeUpload(up.getFileToUpload().getId());
        }
    }
}
