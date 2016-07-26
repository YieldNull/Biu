package com.yieldnull.httpd;

public class ProgressNotifier {

    private final ProgressListener listener;

    private final long contentLength; // 未知则为-1

    private long bytesRead;

    private String fileUri;

    public ProgressNotifier(String pFileUri, ProgressListener pListener, long pContentLength) {
        listener = pListener;
        contentLength = pContentLength;
        fileUri = pFileUri;
    }

    public void noteBytesRead(int pBytes) {
        bytesRead += pBytes;
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.update(fileUri, bytesRead, contentLength);
        }
    }

}
