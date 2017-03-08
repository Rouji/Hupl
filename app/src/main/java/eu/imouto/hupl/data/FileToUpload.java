package eu.imouto.hupl.data;

import android.net.Uri;

import java.io.InputStream;
import java.util.Random;

public class FileToUpload
{
    private static final String[] IMAGE_STRINGS = new String[] {"jpg", "jpeg", "png", "gif"};

    private int id;
    public String fileName;
    public InputStream stream;
    public String mime;
    public Uri origUri;

    public FileToUpload()
    {
        Random rnd = new Random();
        id = rnd.nextInt();
    }

    public int getId()
    {
        return id;
    }

    public boolean isImage()
    {
        if (mime != null && mime.contains("image/"))
            return true;

        for (String s : IMAGE_STRINGS)
        {
            if (fileName.endsWith(s))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof FileToUpload)) return false;
        FileToUpload other = (FileToUpload) o;
        return other.id == this.id;
    }
}
