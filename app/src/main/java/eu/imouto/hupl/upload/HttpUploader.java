package eu.imouto.hupl.upload;

import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.imouto.hupl.data.FileToUpload;

public class HttpUploader extends Uploader
{
    private final static String EOL = "\r\n";
    private final static String BOUNDARY = "--------adc3da76e82a06e9ea5e31b36aab8bda";
    private final static String HYPHENS = "--";
    private final static int BUFFER_SIZE = 1024*8;

    private boolean run = true;

    public String targetUrl;
    public String authUser;
    public String authPass;
    public String fileParam;
    public String responseRegex;

    public HttpUploader(FileToUpload file)
    {
        super(file);
    }

    @Override
    protected void uploadFile(FileToUpload file)
    {
        StringBuilder responseBuilder = new StringBuilder();
        int fileSize = -1;
        try
        {
            fileSize = file.stream.available();
        }
        catch (IOException e)
        {}

        HttpURLConnection connection;
        DataOutputStream outputStream;
        DataInputStream inputStream;

        int bytesRead;
        byte[] buffer = new byte[BUFFER_SIZE];

        try
        {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            //setup http header
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+ BOUNDARY);
            connection.setRequestProperty("Transfer-Encoding", "chunked");
            connection.setChunkedStreamingMode(BUFFER_SIZE);

            //set basic auth
            if (authUser != null && !authUser.isEmpty() &&
                authPass != null && !authPass.isEmpty())
            {
                String auth = "Basic " + Base64.encodeToString((authUser+":"+authPass).getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", auth);
            }

            //write multipart header
            outputStream = new DataOutputStream( connection.getOutputStream() );
            outputStream.writeBytes(HYPHENS + BOUNDARY + EOL);
            outputStream.writeBytes("Content-Disposition: form-data; name=\""+fileParam+"\";filename=\"" + file.fileName +"\"" + EOL);
            outputStream.writeBytes(EOL);

            //write file contents
            int written = 0;
            while (run && (bytesRead = file.stream.read(buffer, 0, BUFFER_SIZE)) > 0)
            {
                outputStream.write(buffer, 0, bytesRead);
                written += bytesRead;
                if (progressReceiver != null)
                    progressReceiver.onUploadProgress(written, fileSize);
            }

            if (run)
            {
                //write multipart footer
                outputStream.writeBytes(EOL);
                outputStream.writeBytes(HYPHENS + BOUNDARY + HYPHENS + EOL);
            }

            outputStream.flush();
            outputStream.close();

            int status = -1;
            String response = "";

            if (run)
            {
                //read response body
                inputStream = new DataInputStream(connection.getInputStream());
                while ((bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE)) > 0)
                {
                    responseBuilder.append(new String(buffer, 0, bytesRead));
                }

                status = connection.getResponseCode();
                response = responseBuilder.toString();
            }


            if (progressReceiver != null)
            {
                if (!run)
                {
                    progressReceiver.onUploadCancelled();
                }
                else if (status == 200)
                {
                    String parsed = parseResponse(response);
                    if (parsed == null)
                        progressReceiver.onUploadFailed("No regex match found");
                    else
                        progressReceiver.onUploadFinished(parsed);
                }
                else
                {
                    progressReceiver.onUploadFailed("HTTP status " + status);
                }
            }
        }
        catch (Exception ex)
        {
            if (progressReceiver != null)
                progressReceiver.onUploadFailed("Exception: " + ex.getMessage());
        }
    }

    private String parseResponse(String resp)
    {
        if (responseRegex == null || responseRegex.isEmpty())
            return resp;

        Pattern pat = Pattern.compile(responseRegex);
        Matcher matcher = pat.matcher(resp);
        if (!matcher.find())
            return null;
        if (matcher.groupCount() > 0)
            return matcher.group(1);
        return null;
    }

    @Override
    public void cancel()
    {
        run = false;
    }
}
