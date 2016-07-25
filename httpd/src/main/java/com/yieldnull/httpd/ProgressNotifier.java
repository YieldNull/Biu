package com.yieldnull.httpd;

public class ProgressNotifier {

    private final ProgressListener listener;

    private final long contentLength; // 未知则为-1

    private long bytesRead;

    public ProgressNotifier(ProgressListener pListener, long pContentLength) {
        listener = pListener;
        contentLength = pContentLength;
    }

    public void noteBytesRead(int pBytes) {
        bytesRead += pBytes;
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.update(bytesRead, contentLength);
        }
    }

}
