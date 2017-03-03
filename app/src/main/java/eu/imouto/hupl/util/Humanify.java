package eu.imouto.hupl.util;

public class Humanify
{
    private static final String prefix = "KMGTPE"; //Kilo, Mega, Giga, ...

    private Humanify(){}

    public static String byteCount(long bytes)
    {
        if (bytes < 1024)
            return bytes + " B";

        int exp = (int)(Math.log(bytes) / Math.log(1024));
        if (exp >= prefix.length())
            exp = prefix.length();

        double n = bytes / Math.pow(1024, exp);

        return String.format("%.1f %siB", n, prefix.charAt(exp-1));
    }

}
