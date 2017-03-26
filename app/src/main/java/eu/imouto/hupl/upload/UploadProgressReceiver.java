package eu.imouto.hupl.upload;

interface UploadProgressReceiver
{
    void onUploadProgress(int uploaded, int fileSize);
    void onUploadFinished(String fileLink);
    void onUploadFailed(String error);
    void onUploadCancelled();
}