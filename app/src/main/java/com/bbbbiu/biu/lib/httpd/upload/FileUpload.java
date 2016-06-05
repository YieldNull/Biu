package com.bbbbiu.biu.lib.httpd.upload;

import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.upload.interfaces.Closeable;
import com.bbbbiu.biu.lib.httpd.upload.interfaces.FileItemIterator;
import com.bbbbiu.biu.lib.httpd.upload.interfaces.FileItemStream;
import com.bbbbiu.biu.lib.util.ProgressListener;
import com.bbbbiu.biu.lib.httpd.util.FileItemHeaders;
import com.bbbbiu.biu.lib.httpd.util.LimitedInputStream;
import com.bbbbiu.biu.lib.util.ProgressNotifier;
import com.bbbbiu.biu.lib.httpd.util.Streams;
import com.bbbbiu.biu.lib.httpd.upload.exceptions.*;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

public class FileUpload {


    public static final String CONTENT_TYPE = "Content-type";
    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String CONTENT_LENGTH = "Content-length";
    public static final String FORM_DATA = "form-data";
    public static final String ATTACHMENT = "attachment";
    public static final String MULTIPART = "multipart/";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String MULTIPART_MIXED = "multipart/mixed";


    private static final long sizeMax = -1;  // 请求报文最大大小
    private static final long fileSizeMax = -1;//4 * 1024 * 1024 * 1024; //单文件最大大小 4GB


    private ProgressListener listener;
    private FileItemFactory fileItemFactory;


    public FileItemFactory getFileItemFactory() {
        return fileItemFactory;
    }


    public ProgressListener getProgressListener() {
        return listener;
    }

    public void setProgressListener(ProgressListener pListener) {
        listener = pListener;
    }


    public FileUpload(FileItemFactory fileItemFactory) {
        this.fileItemFactory = fileItemFactory;
    }


    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.
     *
     * @param request The context for the request to be parsed.
     * @return A list of <code>FileItem</code> instances parsed from the
     * request, in the order that they were transmitted.
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     */
    public List<FileItem> parseRequest(HttpRequest request)
            throws FileUploadException {

        List<FileItem> items = new ArrayList<>();
        boolean successful = false;

        try {
            FileItemIterator iter = getItemIterator(request);
            FileItemFactory factory = getFileItemFactory();

            while (iter.hasNext()) {
                final FileItemStream item = iter.next();
                // Don't use getName() here to prevent an InvalidFileNameException.
                final String fileName = ((FileItemIteratorImpl.FileItemStreamImpl) item).name;
                FileItem fileItem = factory.createItem(item.getFieldName(), item.getContentType(),
                        item.isFormField(), fileName);
                items.add(fileItem);

                try {
                    Streams.copy(item.openStream(), fileItem.getOutputStream(), true, null);
                } catch (FileUploadIOException e) {
                    throw (FileUploadException) e.getCause();
                } catch (IOException e) {
                    throw new IOFileUploadException(format("Processing of %s request failed. %s",
                            MULTIPART_FORM_DATA, e.getMessage()), e);
                }

                final FileItemHeaders fih = item.getHeaders();
                fileItem.setHeaders(fih);
            }
            successful = true;
            return items;

        } catch (FileUploadIOException e) {
            throw (FileUploadException) e.getCause();
        } catch (IOException e) {
            throw new FileUploadException(e.getMessage(), e);
        } finally {
            if (!successful) {
                for (FileItem fileItem : items) {
                    try {
                        fileItem.delete();
                    } catch (Throwable e) {
                        // ignore it
                    }
                }
            }
        }
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.
     *
     * @param request The context for the request to be parsed.
     * @return An iterator to instances of <code>FileItemStream</code>
     * parsed from the request, in the order that they were
     * transmitted.
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     * @throws IOException         An I/O error occurred. This may be a network
     *                             error while communicating with the client or a problem while
     *                             storing the uploaded content.
     */
    public FileItemIterator getItemIterator(HttpRequest request)
            throws FileUploadException, IOException {
        try {
            return new FileItemIteratorImpl(request);
        } catch (FileUploadIOException e) {
            // unwrap encapsulated SizeException
            throw (FileUploadException) e.getCause();
        }
    }


    /**
     * 从 HTTP 请求头`Content-Type`中获取各个文件的分隔符`boundary`。 形如：
     * <p>
     * Content-Type:multipart/form-data; boundary=----WebKitFormBoundarylg8DZpyWws8BAd5W
     * <p>
     * 上传的各个文件在请求体中以`----WebKitFormBoundarylg8DZpyWws8BAd5W`开头
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
     * 从HTTP 请求体中获取文件名
     *
     * @param headers HTTP 请求头
     * @return 没有指定文件名则返回空串“”
     */
    private String getFileName(FileItemHeaders headers) {
        return getFileName(headers.getHeader(CONTENT_DISPOSITION));
    }


    /**
     * 从HTTP 请求头`Content-disposition` 中获取 文件名
     *
     * @param contentDisposition `Content-disposition` 的值
     * @return 没有指定文件名则返回空串“”
     */
    private String getFileName(String contentDisposition) {

        if (contentDisposition == null) {
            return null;
        }

        String fileName = null;
        String cdl = contentDisposition.toLowerCase(Locale.ENGLISH);

        if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
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
    protected String getFieldName(FileItemHeaders headers) {
        return getFieldName(headers.getHeader(CONTENT_DISPOSITION));
    }

    /**
     * 从请求体中获取`content-disposition`中的字段名（input 的name属性）
     *
     * @param pContentDisposition The content-dispositions header value.
     * @return The field jake
     */
    private String getFieldName(String pContentDisposition) {
        String fieldName = null;
        if (pContentDisposition != null
                && pContentDisposition.toLowerCase(Locale.ENGLISH).startsWith(FORM_DATA)) {
            ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);

            Map<String, String> params = parser.parse(pContentDisposition, ';');
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
     * <p>
     * <p> If there are multiple headers of the same names, the name
     * will map to a comma-separated list containing the values.
     *
     * @param headerPart The <code>header-part</code> of the current
     *                   <code>encapsulation</code>.
     * @return A <code>Map</code> containing the parsed HTTP request headers.
     */
    protected FileItemHeaders getParsedHeaders(String headerPart) {
        final int len = headerPart.length();
        FileItemHeaders headers = newFileItemHeaders();
        int start = 0;
        for (; ; ) {
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
     * Creates a new instance of {@link FileItemHeaders}.
     *
     * @return The new instance.
     */
    protected FileItemHeaders newFileItemHeaders() {
        return new FileItemHeaders();
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
        for (; ; ) {
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

    /**
     * The iterator, which is returned by
     * {@link FileUpload#getItemIterator(HttpRequest)}.
     */
    private class FileItemIteratorImpl implements FileItemIterator {

        /**
         * The multi part stream to process.
         */
        private final MultipartStream multi;

        /**
         * The notifier, which used for triggering the
         * {@link ProgressListener}.
         */
        private final ProgressNotifier notifier;

        /**
         * The boundary, which separates the various parts.
         */
        private final byte[] boundary;

        /**
         * The item, which we currently process.
         */
        private FileItemStreamImpl currentItem;

        /**
         * The current items field name.
         */
        private String currentFieldName;

        /**
         * Whether we are currently skipping the preamble.
         */
        private boolean skipPreamble;

        /**
         * Whether the current item may still be read.
         */
        private boolean itemValid;

        /**
         * Whether we have seen the end of the file.
         */
        private boolean eof;

        /**
         * Creates a new instance.
         *
         * @param request The request context.
         * @throws FileUploadException An error occurred while
         *                             parsing the request.
         * @throws IOException         An I/O error occurred.
         */
        public FileItemIteratorImpl(HttpRequest request)
                throws FileUploadException, IOException {

            String contentType = request.getContentType();

            //TODO
            if ((null == contentType)
                    || (!contentType.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART))) {
                throw new InvalidContentTypeException(
                        format("the request doesn't contain a %s or %s stream, content type header is %s",
                                MULTIPART_FORM_DATA, MULTIPART_MIXED, contentType));
            }

            InputStream input = request.getInputStream();


            final long requestSize = request.contentLength();

            // TODO
            if (sizeMax >= 0) {
                if (requestSize != -1 && requestSize > sizeMax) {
                    throw new SizeLimitExceededException(
                            format("the request was rejected because its size (%s) exceeds the configured maximum (%s)",
                                    requestSize, sizeMax),
                            requestSize, sizeMax);
                }

                input = new LimitedInputStream(input, sizeMax) {
                    @Override
                    protected void raiseError(long pSizeMax, long pCount)
                            throws IOException {
                        FileUploadException ex = new SizeLimitExceededException(
                                format("the request was rejected because its size (%s) exceeds the configured maximum (%s)",
                                        pCount, pSizeMax),
                                pCount, pSizeMax);
                        throw new FileUploadIOException(ex);
                    }
                };
            }


            boundary = getBoundary(contentType);
            if (boundary == null) {
                // TODO
                throw new FileUploadException("the request was rejected because no multipart boundary was found");
            }

            notifier = new ProgressNotifier(listener, requestSize);
            multi = new MultipartStream(input, boundary, notifier);
            multi.setHeaderEncoding(request.getCharacterEncoding());

            skipPreamble = true;
            findNextItem();
        }

        /**
         * Called for finding the next item, if any.
         *
         * @return True, if an next item was found, otherwise false.
         * @throws IOException An I/O error occurred.
         */
        private boolean findNextItem() throws IOException {
            if (eof) {
                return false;
            }
            if (currentItem != null) {
                currentItem.close();
                currentItem = null;
            }
            for (; ; ) {
                boolean nextPart;
                if (skipPreamble) {
                    nextPart = multi.skipPreamble();
                } else {
                    nextPart = multi.readBoundary();
                }
                if (!nextPart) {
                    if (currentFieldName == null) {
                        // Outer multipart terminated -> No more data
                        eof = true;
                        return false;
                    }
                    // Inner multipart terminated -> Return to parsing the outer
                    multi.setBoundary(boundary);
                    currentFieldName = null;
                    continue;
                }
                FileItemHeaders headers = getParsedHeaders(multi.readHeaders());
                if (currentFieldName == null) {
                    // We're parsing the outer multipart
                    String fieldName = getFieldName(headers);
                    if (fieldName != null) {
                        String subContentType = headers.getHeader(CONTENT_TYPE);
                        if (subContentType != null
                                && subContentType.toLowerCase(Locale.ENGLISH)
                                .startsWith(MULTIPART_MIXED)) {
                            currentFieldName = fieldName;
                            // Multiple files associated with this field name
                            byte[] subBoundary = getBoundary(subContentType);
                            multi.setBoundary(subBoundary);
                            skipPreamble = true;
                            continue;
                        }
                        String fileName = getFileName(headers);
                        currentItem = new FileItemStreamImpl(fileName,
                                fieldName, headers.getHeader(CONTENT_TYPE),
                                fileName == null, getContentLength(headers));
                        currentItem.setHeaders(headers);
                        notifier.noteItem();
                        itemValid = true;
                        return true;
                    }
                } else {
                    String fileName = getFileName(headers);
                    if (fileName != null) {
                        currentItem = new FileItemStreamImpl(fileName,
                                currentFieldName,
                                headers.getHeader(CONTENT_TYPE),
                                false, getContentLength(headers));
                        currentItem.setHeaders(headers);
                        notifier.noteItem();
                        itemValid = true;
                        return true;
                    }
                }
                multi.discardBodyData();
            }
        }

        private long getContentLength(FileItemHeaders pHeaders) {
            try {
                return Long.parseLong(pHeaders.getHeader(CONTENT_LENGTH));
            } catch (Exception e) {
                return -1;
            }
        }

        /**
         * Returns, whether another instance of {@link FileItemStream}
         * is available.
         *
         * @return True, if one or more additional file items
         * are available, otherwise false.
         * @throws FileUploadException Parsing or processing the
         *                             file item failed.
         * @throws IOException         Reading the file item failed.
         */
        public boolean hasNext() throws IOException, FileUploadException {
            if (eof) {
                return false;
            }
            if (itemValid) {
                return true;
            }
            try {
                return findNextItem();
            } catch (FileUploadIOException e) {
                // unwrap encapsulated SizeException
                throw (FileUploadException) e.getCause();
            }
        }

        /**
         * Returns the next available {@link FileItemStream}.
         *
         * @return FileItemStream instance, which provides
         * access to the next file item.
         * @throws NoSuchElementException No more items are
         *                                available. Use {@link #hasNext()} to prevent this exception.
         * @throws FileUploadException    Parsing or processing the
         *                                file item failed.
         * @throws IOException            Reading the file item failed.
         */
        public FileItemStream next() throws FileUploadException, IOException {
            if (eof || (!itemValid && !hasNext())) {
                throw new NoSuchElementException();
            }
            itemValid = false;
            return currentItem;
        }


        /**
         * Default implementation of {@link FileItemStream}.
         */
        class FileItemStreamImpl implements FileItemStream {

            private final String contentType;   // 文件 Content-Type
            private final String fieldName;     // 文件字段名（input标签的 name属性）
            private final String name;          // 文件名

            private final boolean formField;    // 是否是以表单上传

            private final InputStream stream;   // 文件输入流

            private boolean opened;             // 文件是否已经打开

            private FileItemHeaders headers;    // request payload 中各个文件的描述头

            /**
             * Creates a new instance.
             *
             * @param pName          The items file name, or null.
             * @param pFieldName     The items field name.
             * @param pContentType   The items content type, or null.
             * @param pFormField     Whether the item is a form field.
             * @param pContentLength The items content length, if known, or -1
             * @throws IOException Creating the file item failed.
             */
            FileItemStreamImpl(String pName, String pFieldName,
                               String pContentType, boolean pFormField,
                               long pContentLength) throws IOException {
                name = pName;
                fieldName = pFieldName;
                contentType = pContentType;
                formField = pFormField;
                final MultipartStream.ItemInputStream itemStream = multi.newInputStream();
                InputStream istream = itemStream;
                if (fileSizeMax != -1) {
                    if (pContentLength != -1
                            && pContentLength > fileSizeMax) {
                        FileSizeLimitExceededException e =
                                new FileSizeLimitExceededException(
                                        format("The field %s exceeds its maximum permitted size of %s bytes.",
                                                fieldName, fileSizeMax),
                                        pContentLength, fileSizeMax);
                        e.setFileName(pName);
                        e.setFieldName(pFieldName);
                        throw new FileUploadIOException(e);
                    }
                    istream = new LimitedInputStream(istream, fileSizeMax) {
                        @Override
                        protected void raiseError(long pSizeMax, long pCount)
                                throws IOException {
                            itemStream.close(true);
                            FileSizeLimitExceededException e =
                                    new FileSizeLimitExceededException(
                                            format("The field %s exceeds its maximum permitted size of %s bytes.",
                                                    fieldName, pSizeMax),
                                            pCount, pSizeMax);
                            e.setFieldName(fieldName);
                            e.setFileName(name);
                            throw new FileUploadIOException(e);
                        }
                    };
                }
                stream = istream;
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

            /**
             * Returns the items file name.
             *
             * @return File name, if known, or null.
             * @throws InvalidFileNameException The file name contains a NUL character,
             *                                  which might be an indicator of a security attack. If you intend to
             *                                  use the file name anyways, catch the exception and use
             *                                  InvalidFileNameException#getName().
             */
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
                if (opened) {
                    throw new IllegalStateException(
                            "The stream was already opened.");
                }
                if (((Closeable) stream).isClosed()) {
                    throw new ItemSkippedException();
                }
                return stream;
            }

            public void close() throws IOException {
                stream.close();
            }
        }
    }
}
