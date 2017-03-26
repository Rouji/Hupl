package eu.imouto.hupl.util;

import java.util.Random;

public class RandomString
{
    private static final Random rnd = new Random();
    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";

    public static String next(int len)
    {
        StringBuilder sb = new StringBuilder(len);
        int csLen = CHARSET.length();

        for (int i = 0; i < len; ++i)
        {
            sb.append(CHARSET.charAt(rnd.nextInt(csLen)));
        }

        return sb.toString();
    }
}
