package com.bbbbiu.biu.server.upload;

import com.bbbbiu.biu.server.HttpRequest;

import java.io.IOException;
import java.io.InputStream;


public class RequestContext {
    private HttpRequest request;

    public RequestContext(HttpRequest request) {
        this.request = request;
    }

    public long contentLength() {
        long size;
        try {
            String cl1 = request.getHeaders().get("content-length");
            size = Long.parseLong(cl1);
        } catch (NumberFormatException var4) {
            size = -1L;
        }

        return size;
    }

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public String getContentType() {
        return this.request.getHeaders().get("content-type");
    }

    public int getContentLength() {
        return (int) contentLength();
    }

    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }
}
