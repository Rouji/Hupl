package eu.imouto.hupl.upload;

import eu.imouto.hupl.data.FileToUpload;

abstract class Uploader implements Runnable
{
    protected UploadProgressReceiver progressReceiver;
    protected FileToUpload fileToUpload;

    public String name;

    public Uploader(FileToUpload file) {fileToUpload = file;}
    public FileToUpload getFileToUpload() {return fileToUpload;}

    public void run()
    {
        uploadFile(fileToUpload);
    }

    public void setProgessReceiver(UploadProgressReceiver receiver)
    {
        progressReceiver = receiver;
    }

    protected abstract void uploadFile(FileToUpload file);

    public abstract void cancel();
}
