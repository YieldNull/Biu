package com.bbbbiu.biu.lib.util;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by YieldNull at 4/22/16
 */
public class UploadRequestBody extends RequestBody {
    MediaType contentType;
    File file;
    ProgressNotifier notifier;

    final long DEFAULT_SEGMENT_SIZE = 8192;

    public UploadRequestBody(MediaType contentType, File file, ProgressNotifier notifier) {
        this.contentType = contentType;
        this.file = file;
        this.notifier = notifier;
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        System.setProperty("http.keepAlive", "false");
        Source source = null;
        try {
            source = Okio.source(file);
            //sink.writeAll(source);

            int read;
            while ((read = (int) source.read(sink.buffer(), DEFAULT_SEGMENT_SIZE)) != -1) {
                sink.flush();
                notifier.noteBytesRead(read);
            }
        } finally {
            Util.closeQuietly(source);
        }
    }
}
