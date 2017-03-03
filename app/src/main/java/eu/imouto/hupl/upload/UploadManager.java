package eu.imouto.hupl.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.SparseArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import eu.imouto.hupl.data.FileToUpload;
import eu.imouto.hupl.data.HistoryDB;
import eu.imouto.hupl.data.HistoryEntry;
import eu.imouto.hupl.ui.UploadNotification;
import eu.imouto.hupl.util.ImageResize;
import eu.imouto.hupl.util.StreamUtil;
import eu.imouto.hupl.util.UriResolver;

public class UploadManager
{
    private static UploadManager instance = new UploadManager();
    private SparseArray<Upload> currentUploads = new SparseArray<>(5);

    public static UploadManager getInstance()
    {
        return instance;
    }

    private UploadManager()
    {
    }

    public void startUpload(Context context, Uri uri, String uploaderName, boolean compress)
    {
        FileToUpload file = UriResolver.uriToFile(context, uri);
        Uploader up = UploaderFactory.getUploaderByName(context, uploaderName, file);
        Upload u = new Upload(context, up, compress);
        currentUploads.put(file.getId(), u);
        u.start();
    }

    public void cancelUpload(int fileId)
    {
        Upload up = currentUploads.get(fileId);
        if (up != null)
            up.cancel();
    }

    public void removeUpload(int fileId)
    {
        currentUploads.delete(fileId);
    }

    private class Upload implements UploadProgressReceiver
    {
        private UploadNotification not;
        private Uploader up;
        private HistoryEntry hist = new HistoryEntry();
        private Context context;

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
            not.cancel();
            removeUpload(up.getFileToUpload().getId());
        }
    }
}
