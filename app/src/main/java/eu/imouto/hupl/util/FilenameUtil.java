package eu.imouto.hupl.util;

public class FilenameUtil
{
    public static String replaceExtension(String filename, String newExt)
    {
        int idx = filename.lastIndexOf(".");
        if (idx < 0)
            return filename;
        String sub = filename.substring(0,idx+1);
        return sub + newExt;
    }
}
