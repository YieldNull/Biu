package com.yieldnull.httpd.upload;

import com.yieldnull.httpd.util.FileItemHeaders;
import com.yieldnull.httpd.util.Streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Default implementation of {@link FileItemStream}.
 */
class FileItemStream {

    private final String contentType;   // 文件 Content-Type
    private final String fieldName;     // 文件字段名（input标签的 name属性）
    private final String name;          // 文件名

    private final boolean formField;    // 是否是以表单上传

    private final InputStream stream;   // 文件输入流

    private FileItemHeaders headers;    // request payload 中各个文件的描述头

    /**
     * Creates a new instance.
     *
     * @param pName        The items file name, or null.
     * @param pFieldName   The items field name.
     * @param pContentType The items content type, or null.
     * @param pFormField   Whether the item is a form field.
     * @throws IOException Creating the file item failed.
     */
    FileItemStream(String pName, String pFieldName, String pContentType,
                   boolean pFormField, MultipartStream multi) throws IOException {
        name = pName;
        fieldName = pFieldName;
        contentType = pContentType;
        formField = pFormField;

        stream = multi.newInputStream();
    }

    public String getContentType() {
        return contentType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isFormField() {
        return formField;
    }

    public String getName() {
        return Streams.checkFileName(name);
    }

    public FileItemHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(FileItemHeaders pHeaders) {
        headers = pHeaders;
    }

    public InputStream openStream() throws IOException {
        return stream;
    }

    public void close() throws IOException {
        stream.close();
    }
}
