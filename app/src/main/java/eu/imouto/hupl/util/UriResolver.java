package eu.imouto.hupl.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.InputStream;

import eu.imouto.hupl.data.FileToUpload;

public class UriResolver
{
    private static final int RANDOM_FILENAME_LENGTH = 4;

    public static FileToUpload uriToFile(Context context, Uri uri)
    {
        ContentResolver res = context.getContentResolver();
        InputStream inStream;

        try
        {
            inStream = res.openInputStream(uri);
        }
        catch (FileNotFoundException ex)
        {
            return null;
        }


        //try to get the original filename and extension
        String fileName = fileNameFromUri(context, uri);

        //get the mime type
        String mime = res.getType(uri);

        //if the original filename can't be found,
        //generate a randomised file name and try to find an extension using mime type
        if (fileName == null)
        {
            MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
            String ext = mimeMap.getExtensionFromMimeType(mime);

            //assume a generic binary format, if no mime type matches
            if (ext == null)
                ext = "bin";

            fileName = RandomString.next(RANDOM_FILENAME_LENGTH)+"."+ext;
        }

        FileToUpload file = new FileToUpload();
        file.fileName = fileName;
        file.stream = inStream;
        file.mime = mime;
        file.origUri = uri;

        return file;
    }

    public static String fileNameFromUri(Context context, Uri uri)
    {
        String name = null;
        if(uri.getScheme().equals("content"))
        {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try
            {
                if (cursor != null && cursor.moveToFirst())
                {
                    name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
            finally
            {
                cursor.close();
            }
        }
        else if (uri.getScheme().equals("file"))
        {
            String path = uri.getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1)
            {
                name = path.substring(lastSlash+1);
            }
        }

        return name;
    }
}
