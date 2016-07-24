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

        boundary = getBoundary(contentType);
        if (boundary == null) {
            throw new IOException("No boundary found");
        }

        InputStream inputStream = request.getInputStream();

        notifier = new ProgressNotifier(listener, request.contentLength());
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


    /**
     * 寻找下一个Entity
     *
     * @return 是否找到
     * @throws IOException
     */
    private boolean findNextItem() throws IOException {
        if (eof) {
            return false;
        }

        if (currentItem != null) {
            currentItem.close();
            currentItem = null;
        }

        while (true) {
            boolean nextPart = multipart.skipPreamble();

            if (!nextPart) {
                if (currentFieldName == null) {
                    // Outer multipart terminated -> No more data
                    eof = true;
                    return false;
                } else {
                    // Inner multipart terminated -> Return to parsing the outer
                    multipart.setBoundary(boundary);
                    currentFieldName = null;
                    continue;
                }
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
                    } else {
                        String fileName = getFileName(headers);
                        currentItem = new FileItemStream(fileName,
                                fieldName, headers.getHeader(FileUpload.CONTENT_TYPE),
                                fileName == null, multipart);
                        currentItem.setHeaders(headers);
                        notifier.noteItem();
                        itemValid = true;
                        return true;
                    }
                }
            } else {
                // We're parsing the inner multipart
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
     * 从Entity Header中获取`content-disposition`中的字段名（input的name属性）
     *
     * @param headers Entity header
     * @return field name of current entity
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
     * 将Entity Header 解析成键值对，若Header名相同，则将不同的value值用逗号隔开
     *
     * @param headerPart Entity Header
     * @return 解析后所得键值对
     */
    private FileItemHeaders getParsedHeaders(String headerPart) {
        final int len = headerPart.length();

        FileItemHeaders headers = new FileItemHeaders();
        int start = 0;

        while (true) {
            int end = findLineEnd(headerPart, start);
            if (start == end) { // 表示有两个换行符相连，Entity Header 结束，后面是Entity Body.
                break;
            }

            // Header 中可能有换行符，例如（不限于两行）
            // Content-Type: multipart/form-data;
            //              boundary="----=_Part_293427_735306028.1445485521749"

            StringBuilder builder = new StringBuilder(headerPart.substring(start, end));
            start = end + 2;
            while (start < len) {
                int nonWs = start;

                // 略过行首空白字符
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

                // 将下一行的内容加进去
                end = findLineEnd(headerPart, nonWs);
                builder.append(" ").append(headerPart.substring(nonWs, end));
                start = end + 2;
            }
            parseHeaderLine(headers, builder.toString());
        }
        return headers;
    }


    /**
     * 获取下一个换行符的位置
     *
     * @param headerPart Entity Header
     * @param start      从何处开始
     * @return "\r\n"的位置，以"\r"算
     */
    private int findLineEnd(String headerPart, int start) {
        int index = start;
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
     * 解析读取到的一条header
     *
     * @param headers   所有已解析的header
     * @param headerStr 待解析的header
     */
    private void parseHeaderLine(FileItemHeaders headers, String headerStr) {
        final int colonOffset = headerStr.indexOf(':');
        if (colonOffset == -1) {
            // 不合法
            return;
        }
        String headerName = headerStr.substring(0, colonOffset).trim();
        String headerValue =
                headerStr.substring(headerStr.indexOf(':') + 1).trim();
        headers.addHeader(headerName, headerValue);
    }

}
