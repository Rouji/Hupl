package eu.imouto.hupl;

import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUploader
{
    public static class HttpResult
    {
        public int status = 0;
        public String response = "";
        public String exception = "";
    }

    public static class HttpHost
    {
        public String hostName = "";
        public String fileParamName = "file";
        public String authUser = "";
        public String authPass = "";
    }

    public static class FileToUpload
    {
        public String fileName;
        public InputStream inputStream;
    }

    private final static String eol = "\r\n";
    private final static String boundary = "--------adc3da76e82a06e9ea5e31b36aab8bda";
    private final static String hyphens = "--";
    private final static int bufferSize = 1024*1024;

    static HttpResult uploadFile(Host host, FileToUpload file)
    {
        HttpResult res = new HttpResult();
        StringBuilder responseBuilder = new StringBuilder();

        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;

        int bytesRead;
        byte[] buffer = new byte[bufferSize];

        try
        {
            URL url = new URL(host.hostUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            //setup http header
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
            connection.setChunkedStreamingMode(bufferSize);

            //set basic auth
            if (!host.authUser.isEmpty())
            {
                String auth = "Basic " + Base64.encodeToString((host.authUser+":"+host.authPass).getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", auth);
            }

            //write multipart header
            outputStream = new DataOutputStream( connection.getOutputStream() );
            outputStream.writeBytes(hyphens + boundary + eol);
            outputStream.writeBytes("Content-Disposition: form-data; name=\""+host.fileParam+"\";filename=\"" + file.fileName +"\"" + eol);
            outputStream.writeBytes(eol);

            int ov = 0;
            //write file contents
            while ((bytesRead = file.inputStream.read(buffer, 0, bufferSize)) > 0)
            {
                outputStream.write(buffer, 0, bytesRead);
            }

            //write multipart footer
            outputStream.writeBytes(eol);
            outputStream.writeBytes(hyphens + boundary + hyphens + eol);

            //read response body
            inputStream = new DataInputStream(connection.getInputStream());
            while ((bytesRead = inputStream.read(buffer,0,bufferSize)) > 0)
            {
                responseBuilder.append(new String(buffer, 0, bytesRead));
            }

            res.status = connection.getResponseCode();
            res.response = responseBuilder.toString();

            outputStream.flush();
            outputStream.close();
        }
        catch (Exception ex)
        {
            res.exception = ex.getMessage();
        }

        return res;
    }
}
