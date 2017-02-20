package eu.imouto.hupl.upload;

import eu.imouto.hupl.data.FileToUpload;

public class UploaderFactory
{
    public static Uploader getUploaderByName(String name, FileToUpload file)
    {
        //TODO
        HttpUploader up = new HttpUploader(file);
        up.name = name;
        up.targetUrl = "https://x0.at/";
        up.fileParam = "file";
        return up;
    }
}
