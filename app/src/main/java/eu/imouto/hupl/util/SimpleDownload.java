package eu.imouto.hupl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SimpleDownload
{
    private static final int BUFFER_SIZE = 1024;
    private String downloadDir;

    public SimpleDownload(String downloadDir)
    {
        this.downloadDir = downloadDir;
    }

    public File download(String url)
    {
        try
        {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;


            File outFile = null;
            String disp = conn.getHeaderField("Content-Disposition");

            if (disp != null)
            {
                int index = disp.indexOf("filename=");
                if (index > 0)
                {
                    String name = disp.substring(index + 10, disp.length() - 1);
                    outFile = new File(downloadDir, name);
                }
            }
            else
            {
                String name = url.substring(url.lastIndexOf("/") + 1, url.length());
                outFile = new File(downloadDir, name);
            }

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(outFile);

            int read = -1;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }

            in.close();
            out.close();

            return outFile;
        }
        catch (IOException ex)
        {
            return null;
        }
    }
}
