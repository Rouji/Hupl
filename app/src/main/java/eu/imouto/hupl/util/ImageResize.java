package eu.imouto.hupl.util;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * Util class for resizing and compressing images
 * Note: Compression outputs JPEG.
 */
public class ImageResize
{
    private static final int THUMBNAIL_SIZE = 1024;
    private static final int DEFAULT_QUALITY = 70;

    public static Bitmap resizeToFit(Bitmap bitmap, int width, int height)
    {
        if (bitmap == null)
            return null;

        if (bitmap.getWidth() > width || bitmap.getHeight() > height)
        {
            float srcRat = bitmap.getWidth() / (float) bitmap.getHeight();
            float dstRat = width / (float) height;
            float scale = dstRat > srcRat ? height / (float) bitmap.getHeight() : width / (float) bitmap.getWidth();

            return Bitmap.createScaledBitmap(
                    bitmap,
                    Math.round(scale * bitmap.getWidth()),
                    Math.round(scale * bitmap.getHeight()),
                    true);
        }
        else
        {
            return bitmap;
        }
    }

    public static Bitmap thumbnail(Bitmap bitmap)
    {
        return resizeToFit(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
    }

    public static byte[] compress(Bitmap bitmap, int quality)
    {
        if (bitmap == null)
            return null;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream);

        return outStream.toByteArray();
    }
}
