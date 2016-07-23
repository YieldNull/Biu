package com.yieldnull.httpd.upload;


import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.ProgressListener;
import com.yieldnull.httpd.ProgressNotifier;
import com.yieldnull.httpd.util.FileItemHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

class FileItemIterator {

    private final MultipartStream multipart;
    private final ProgressNotifier notifier;

    private final byte[] boundary;

    private FileItemStream currentItem;

    private String currentFieldName;

    /**
     * Whether the current item may still be read.
     */
    private boolean itemValid;

    /**
     * Whether we have seen the end of the file.
     */
    private boolean eof;

    public FileItemIterator(HttpRequest request, ProgressListener listener) throws IOException {
        String contentType = request.getContentType();

        if ((contentType == null)
                || (!contentType.toLowerCase(Locale.ENGLISH).startsWith(FileUpload.MULTIPART))) {
            throw new IOException("Invalid Content-Type");
        }

        InputStream inputStream = request.getInputStream();
        long requestSize = request.contentLength();

        boundary = getBoundary(contentType);
        if (boundary == null) {
            throw new IOException("No boundary found");
        }

        notifier = new ProgressNotifier(listener, requestSize);
        multipart = new MultipartStream(inputStream, boundary, notifier);
        multipart.setHeaderEncoding(request.getCharacterEncoding());

        findNextItem();
    }

    public FileItemStream next() throws IOException {
        if (eof || (!itemValid && !hasNext())) {
            throw new NoSuchElementException();
        }
        itemValid = false;
        return currentItem;
    }

    public boolean hasNext() throws IOException {
        return !eof && (itemValid || findNextItem());
    }


    private boolean findNextItem() throws IOException {
        if (eof) {
            return false;
        }
        if (currentItem != null) {
            currentItem.close();
            currentItem = null;
        }
        while (true) {
            boolean nextPart;
            nextPart = multipart.skipPreamble();
            if (!nextPart) {
                if (currentFieldName == null) {
                    // Outer multipart terminated -> No more data
                    eof = true;
                    return false;
                }
                // Inner multipart terminated -> Return to parsing the outer
                multipart.setBoundary(boundary);
                currentFieldName = null;
                continue;
            }
            FileItemHeaders headers = getParsedHeaders(multipart.readHeaders());
            if (currentFieldName == null) {
                // We're parsing the outer multipart
                String fieldName = getFieldName(headers);
                if (fieldName != null) {
                    String subContentType = headers.getHeader(FileUpload.CONTENT_TYPE);
                    if (subContentType != null
                            && subContentType.toLowerCase(Locale.ENGLISH)
                            .startsWith(FileUpload.MULTIPART_MIXED)) {
                        currentFieldName = fieldName;
                        // Multiple files associated with this field name
                        byte[] subBoundary = getBoundary(subContentType);
                        multipart.setBoundary(subBoundary);
                        continue;
                    }
                    String fileName = getFileName(headers);
                    currentItem = new FileItemStream(fileName,
                            fieldName, headers.getHeader(FileUpload.CONTENT_TYPE),
                            fileName == null, multipart);
                    currentItem.setHeaders(headers);
                    notifier.noteItem();
                    itemValid = true;
                    return true;
                }
            } else {
                String fileName = getFileName(headers);
                if (fileName != null) {
                    currentItem = new FileItemStream(fileName,
                            currentFieldName,
                            headers.getHeader(FileUpload.CONTENT_TYPE),
                            false, multipart);
                    currentItem.setHeaders(headers);
                    notifier.noteItem();
                    itemValid = true;
                    return true;
                }
            }
            multipart.discardBodyData();
        }
    }

    /**
     * 从 HTTP 请求头`Content-Type`中获取各个文件的分隔符`boundary`。 形如：
     * <p/>
     * Content-Type:multipart/form-data; boundary=----WebKitFormBoundarylg8DZpyWws8BAd5W
     * <p/>
     * 上传的各个文件在请求体中以`------WebKitFormBoundarylg8DZpyWws8BAd5W`开头
     *
     * @param contentType Content-Type的值
     * @return 以byte[]返回的boundary
     */
    private byte[] getBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);

        Map<String, String> params = parser.parse(contentType, new char[]{';', ','});
        String boundaryStr = params.get("boundary");

        return boundaryStr == null ? null : boundaryStr.getBytes();
    }


    /**
     * 从Entity Header `Content-disposition`中获取文件名
     *
     * @param headers HTTP 请求头
     * @return 没有指定文件名则返回空串“”
     */
    private String getFileName(FileItemHeaders headers) {
        String contentDisposition = headers.getHeader(FileUpload.CONTENT_DISPOSITION);
        if (contentDisposition == null) {
            return null;
        }

        String fileName = null;
        String cdl = contentDisposition.toLowerCase(Locale.ENGLISH);

        if (cdl.startsWith(FileUpload.FORM_DATA) || cdl.startsWith(FileUpload.ATTACHMENT)) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);

            Map<String, String> params = parser.parse(contentDisposition, ';');
            if (params.containsKey("filename")) {
                fileName = params.get("filename");
                if (fileName != null) {
                    fileName = fileName.trim();
                } else {
                    fileName = "";
                }
            }
        }
        return fileName;
    }

    /**
     * 从请求体中获取`content-disposition`中的字段名（input 的name属性）
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @return The field name for the current <code>encapsulation</code>.
     */
    private String getFieldName(FileItemHeaders headers) {
        String contentDisposition = headers.getHeader(FileUpload.CONTENT_DISPOSITION);

        String fieldName = null;
        if (contentDisposition != null
                && contentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FileUpload.FORM_DATA)) {

            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);

            Map<String, String> params = parser.parse(contentDisposition, ';');
            fieldName = params.get("name");
            if (fieldName != null) {
                fieldName = fieldName.trim();
            }
        }
        return fieldName;
    }

    /**
     * <p> Parses the <code>header-part</code> and returns as key/value
     * pairs.
     * <p/>
     * <p> If there are multiple headers of the same names, the name
     * will map to a comma-separated list containing the values.
     *
     * @param headerPart The <code>header-part</code> of the current
     *                   <code>encapsulation</code>.
     * @return A <code>Map</code> containing the parsed HTTP request headers.
     */
    private FileItemHeaders getParsedHeaders(String headerPart) {
        final int len = headerPart.length();
        FileItemHeaders headers = new FileItemHeaders();
        int start = 0;
        while (true) {
            int end = parseEndOfLine(headerPart, start);
            if (start == end) {
                break;
            }
            StringBuilder header = new StringBuilder(headerPart.substring(start, end));
            start = end + 2;
            while (start < len) {
                int nonWs = start;
                while (nonWs < len) {
                    char c = headerPart.charAt(nonWs);
                    if (c != ' ' && c != '\t') {
                        break;
                    }
                    ++nonWs;
                }
                if (nonWs == start) {
                    break;
                }
                // Continuation line found
                end = parseEndOfLine(headerPart, nonWs);
                header.append(" ").append(headerPart.substring(nonWs, end));
                start = end + 2;
            }
            parseHeaderLine(headers, header.toString());
        }
        return headers;
    }

    /**
     * Skips bytes until the end of the current line.
     *
     * @param headerPart The headers, which are being parsed.
     * @param end        Index of the last byte, which has yet been
     *                   processed.
     * @return Index of the \r\n sequence, which indicates
     * end of line.
     */
    private int parseEndOfLine(String headerPart, int end) {
        int index = end;
        while (true) {
            int offset = headerPart.indexOf('\r', index);
            if (offset == -1 || offset + 1 >= headerPart.length()) {
                throw new IllegalStateException(
                        "Expected headers to be terminated by an empty line.");
            }
            if (headerPart.charAt(offset + 1) == '\n') {
                return offset;
            }
            index = offset + 1;
        }
    }

    /**
     * Reads the next header line.
     *
     * @param headers String with all headers.
     * @param header  Map where to store the current header.
     */
    private void parseHeaderLine(FileItemHeaders headers, String header) {
        final int colonOffset = header.indexOf(':');
        if (colonOffset == -1) {
            // This header line is malformed, skip it.
            return;
        }
        String headerName = header.substring(0, colonOffset).trim();
        String headerValue =
                header.substring(header.indexOf(':') + 1).trim();
        headers.addHeader(headerName, headerValue);
    }

}
