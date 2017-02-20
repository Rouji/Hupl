package eu.imouto.hupl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtil
{
    private static final int BUFFER_SIZE = 1024;

    //reads an entire inputstream to a byte[]
    public static byte[] readAllBytes(InputStream in) throws IOException
    {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];

        int len = 0;
        while ((len = in.read(buffer)) != -1)
        {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }
}
