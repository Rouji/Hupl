package eu.imouto.hupl.upload;

public class UploadException extends Exception
{
    private static String title;
    public UploadException(String title, String message)
    {
        super(message);
        this.title = title;
    }

    public static String getTitle()
    {
        return title;
    }
}
