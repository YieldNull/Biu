package com.bbbbiu.biu.server.upload;


import com.bbbbiu.biu.server.HttpRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * @author victor & ritchieGitHub
 */
public class HttpdFileUpload extends FileUpload {

    public static final boolean isMultipartContent(HttpRequest request) {
        return request.getMethod() == HttpRequest.Method.POST && FileUploadBase.isMultipartContent(new RequestContext(request));
    }

    public HttpdFileUpload(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(HttpRequest request) throws FileUploadException {
        return this.parseRequest(new RequestContext(request));
    }

    public Map<String, List<FileItem>> parseParameterMap(HttpRequest request) throws FileUploadException {
        return this.parseParameterMap(new RequestContext(request));
    }

    public FileItemIterator getItemIterator(HttpRequest request) throws FileUploadException, IOException {
        return super.getItemIterator(new RequestContext(request));
    }

}
