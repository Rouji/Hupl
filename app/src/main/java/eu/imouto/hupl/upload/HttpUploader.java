package eu.imouto.hupl.upload;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private HttpURLConnection connection;

    public String targetUrl;
    public String authUser;
    public String authPass;
    public String fileParam;
    public String responseRegex;
    public boolean disableChunkedTransfer;

    public HttpUploader(FileToUpload file)
    {
        super(file);
    }

    @Override
    protected void uploadFile(FileToUpload file)
    {
        StringBuilder responseBuilder = new StringBuilder();
        int fileSize = -1;
        DataOutputStream outputStream;
        DataInputStream inputStream;
        int bytesRead = -1;
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream fileStream = file.stream;

        String multipartHeader = HYPHENS
                + BOUNDARY
                + EOL
                + "Content-Disposition: form-data; name=\"" + fileParam + "\";" +
                "filename=\"" + file.fileName + "\"" + EOL + EOL;
        String multipartFooter = EOL + EOL + HYPHENS + BOUNDARY + HYPHENS + EOL;

        try
        {
            fileSize = file.stream.available();
        }
        catch (IOException e)
        {}

        try
        {
            if (fileSize == -1 && disableChunkedTransfer)
            {
                fileSize = 0;
                ByteArrayOutputStream byteArr = new ByteArrayOutputStream();
                while ((bytesRead = file.stream.read(buffer, 0, BUFFER_SIZE)) > 0)
                {
                    byteArr.write(buffer, 0, bytesRead);
                    fileSize += bytesRead;
                }
                fileStream = new ByteArrayInputStream(byteArr.toByteArray());
            }


            int status = -1;
            String response = "";

            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            //setup http header
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+ BOUNDARY);
            if (disableChunkedTransfer)
            {
                connection.setUseCaches(false);
                connection.setFixedLengthStreamingMode(fileSize + multipartHeader.length() + multipartFooter.length());
            }
            else
            {
                connection.setRequestProperty("Transfer-Encoding", "chunked");
                connection.setChunkedStreamingMode(BUFFER_SIZE);
            }

            //set basic auth
            if (authUser != null && !authUser.isEmpty() &&
                authPass != null && !authPass.isEmpty())
            {
                String auth = "Basic " + Base64.encodeToString((authUser+":"+authPass).getBytes(), Base64.NO_WRAP);
                connection.setRequestProperty("Authorization", auth);
            }

            //write multipart header
            outputStream = new DataOutputStream( connection.getOutputStream() );
            outputStream.writeBytes(multipartHeader);

            //write file contents
            int written = 0;
            while (run && (bytesRead = fileStream.read(buffer, 0, BUFFER_SIZE)) > 0)
            {
                outputStream.write(buffer, 0, bytesRead);
                written += bytesRead;
                Log.d("uploader", "prog:"+written);
                if (progressReceiver != null)
                    progressReceiver.onUploadProgress(written, fileSize);
            }

            //write multipart footer
            outputStream.writeBytes(multipartFooter);

            outputStream.flush();
            outputStream.close();

            status = connection.getResponseCode();
            if (status != 200)
                throw new UploadException("Server Error", "Received status code HTTP " + status);

            //read response body
            inputStream = new DataInputStream(connection.getInputStream());
            while ((bytesRead = inputStream.read(buffer, 0, BUFFER_SIZE)) > 0)
            {
                responseBuilder.append(new String(buffer, 0, bytesRead));
            }

            response = responseBuilder.toString();


            String parsed = parseResponse(response);
            if (parsed == null)
                throw new UploadException("Response Parsing Error", "The following response did not match against the configured regex:\n" + response);

            progressReceiver.onUploadFinished(parsed);
        }
        catch (UploadException ex)
        {
            if (progressReceiver != null)
                progressReceiver.onUploadFailed(ex.getTitle(), ex.getMessage());
        }
        catch (Exception ex)
        {
            if (!run)
            {
                progressReceiver.onUploadCancelled();
            }
            else if (progressReceiver != null)
                progressReceiver.onUploadFailed("Unexpected Exception", ex.getMessage());
        }
        finally
        {
            if (connection != null)
                connection.disconnect();
            connection = null;
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

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (connection != null)
                    connection.disconnect();
            }
        }).start();
    }
}
