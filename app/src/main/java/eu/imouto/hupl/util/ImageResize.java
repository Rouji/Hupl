package eu.imouto.hupl.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Resizes images to a max. width/height and compresses them.
 * Note: Always outputs JPEG.
 */
public class ImageResize
{
    public int width;
    public int height;
    public int quality = 80;

    public ImageResize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public ImageResize(int width, int height, int quality)
    {
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    public InputStream resize(String filePath)
    {
        return resize(BitmapFactory.decodeFile(filePath));
    }

    public InputStream resize(InputStream stream)
    {
        return resize(BitmapFactory.decodeStream(stream));
    }

    public InputStream resize(Bitmap bitmap)
    {
        float srcRat = bitmap.getWidth()/(float)bitmap.getHeight();
        float dstRat = width/(float)height;
        float scale = dstRat > srcRat ? height / (float)bitmap.getHeight() : width / (float)bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                Math.round(scale * bitmap.getWidth()),
                Math.round(scale * bitmap.getHeight()),
                true);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream);

        return new ByteArrayInputStream(outStream.toByteArray());
    }
}
