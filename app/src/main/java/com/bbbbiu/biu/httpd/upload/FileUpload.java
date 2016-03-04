package com.bbbbiu.biu.httpd.upload;

import com.bbbbiu.biu.httpd.HttpRequest;
import com.bbbbiu.biu.httpd.util.Closeable;
import com.bbbbiu.biu.httpd.util.FileItemHeadersImpl;
import com.bbbbiu.biu.httpd.util.LimitedInputStream;
import com.bbbbiu.biu.httpd.util.Streams;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * <p>High level API for processing file uploads.</p>
 * <p/>
 * <p>This class handles multiple files per single HTML widget, sent using
 * <code>multipart/mixed</code> encoding type, as specified by
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>.  Use {@link
 * #parseRequest(HttpRequest)} to acquire a list of {@link
 * //FileItem}s associated with a given HTML
 * widget.</p>
 * <p/>
 * <p>How the data for individual parts is stored is determined by the factory
 * used to create them; a given part may be in memory, on disk, or somewhere
 * else.</p>
 *
 * @version $Id: FileUpload.java 1565194 2014-02-06 12:16:30Z markt $
 */
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


    /**
     * The progress listener.
     */
    private ProgressListener listener;


    /**
     * The factory to use to create new form items.
     */
    private FileItemFactory fileItemFactory;


    /**
     * Returns the factory class used when creating file items.
     *
     * @return The factory class for new file items.
     */
    public FileItemFactory getFileItemFactory() {
        return fileItemFactory;
    }


    public FileUpload(FileItemFactory fileItemFactory) {
        this.fileItemFactory = fileItemFactory;
    }


    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.
     *
     * @param ctx The context for the request to be parsed.
     * @return A list of <code>FileItem</code> instances parsed from the
     * request, in the order that they were transmitted.
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     */
    public List<FileItem> parseRequest(HttpRequest ctx)
            throws FileUploadException {
        List<FileItem> items = new ArrayList<FileItem>();
        boolean successful = false;
        try {
            FileItemIterator iter = getItemIterator(ctx);
            FileItemFactory fac = getFileItemFactory();
            if (fac == null) {
                throw new NullPointerException("No FileItemFactory has been set.");
            }
            while (iter.hasNext()) {
                final FileItemStream item = iter.next();
                // Don't use getName() here to prevent an InvalidFileNameException.
                final String fileName = ((FileItemIteratorImpl.FileItemStreamImpl) item).name;
                FileItem fileItem = fac.createItem(item.getFieldName(), item.getContentType(),
                        item.isFormField(), fileName);
                items.add(fileItem);
                try {
                    Streams.copy(item.openStream(), fileItem.getOutputStream(), true);
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
     * @param ctx The context for the request to be parsed.
     * @return An iterator to instances of <code>FileItemStream</code>
     * parsed from the request, in the order that they were
     * transmitted.
     * @throws FileUploadException if there are problems reading/parsing
     *                             the request or storing files.
     * @throws IOException         An I/O error occurred. This may be a network
     *                             error while communicating with the client or a problem while
     *                             storing the uploaded content.
     */
    public FileItemIterator getItemIterator(HttpRequest ctx)
            throws FileUploadException, IOException {
        try {
            return new FileItemIteratorImpl(ctx);
        } catch (FileUploadIOException e) {
            // unwrap encapsulated SizeException
            throw (FileUploadException) e.getCause();
        }
    }


    /**
     * Retrieves the boundary from the <code>Content-type</code> header.
     *
     * @param contentType The value of the content type header from which to
     *                    extract the boundary value.
     * @return The boundary, as a byte array.
     */
    protected byte[] getBoundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        // Parameter parser can handleRequest null input
        Map<String, String> params = parser.parse(contentType, new char[]{';', ','});
        String boundaryStr = params.get("boundary");

        if (boundaryStr == null) {
            return null;
        }
        byte[] boundary;
        try {
            boundary = boundaryStr.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            boundary = boundaryStr.getBytes(); // Intentionally falls back to default charset
        }
        return boundary;
    }


    /**
     * Retrieves the file name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers The HTTP headers object.
     * @return The file name for the current <code>encapsulation</code>.
     */
    protected String getFileName(FileItemHeaders headers) {
        return getFileName(headers.getHeader(CONTENT_DISPOSITION));
    }

    /**
     * Returns the given content-disposition headers file name.
     *
     * @param pContentDisposition The content-disposition headers value.
     * @return The file name
     */
    private String getFileName(String pContentDisposition) {
        String fileName = null;
        if (pContentDisposition != null) {
            String cdl = pContentDisposition.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith(FORM_DATA) || cdl.startsWith(ATTACHMENT)) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                // Parameter parser can handleRequest null input
                Map<String, String> params = parser.parse(pContentDisposition, ';');
                if (params.containsKey("filename")) {
                    fileName = params.get("filename");
                    if (fileName != null) {
                        fileName = fileName.trim();
                    } else {
                        // Even if there is no value, the parameter is present,
                        // so we return an empty file name rather than no file
                        // name.
                        fileName = "";
                    }
                }
            }
        }
        return fileName;
    }

    /**
     * Retrieves the field name from the <code>Content-disposition</code>
     * header.
     *
     * @param headers A <code>Map</code> containing the HTTP request headers.
     * @return The field name for the current <code>encapsulation</code>.
     */
    protected String getFieldName(FileItemHeaders headers) {
        return getFieldName(headers.getHeader(CONTENT_DISPOSITION));
    }

    /**
     * Returns the field name, which is given by the content-disposition
     * header.
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
            // Parameter parser can handleRequest null input
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
        FileItemHeadersImpl headers = newFileItemHeaders();
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
    protected FileItemHeadersImpl newFileItemHeaders() {
        return new FileItemHeadersImpl();
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
    private void parseHeaderLine(FileItemHeadersImpl headers, String header) {
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
         * Default implementation of {@link FileItemStream}.
         */
        class FileItemStreamImpl implements FileItemStream {

            /**
             * The file items content type.
             */
            private final String contentType;

            /**
             * The file items field name.
             */
            private final String fieldName;

            /**
             * The file items file name.
             */
            private final String name;

            /**
             * Whether the file item is a form field.
             */
            private final boolean formField;

            /**
             * The file items input stream.
             */
            private final InputStream stream;

            /**
             * Whether the file item was already opened.
             */
            private boolean opened;

            /**
             * The headers, if any.
             */
            private FileItemHeaders headers;

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

            /**
             * Returns the items content type, or null.
             *
             * @return Content type, if known, or null.
             */
            public String getContentType() {
                return contentType;
            }

            /**
             * Returns the items field name.
             *
             * @return Field name.
             */
            public String getFieldName() {
                return fieldName;
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

            /**
             * Returns, whether this is a form field.
             *
             * @return True, if the item is a form field,
             * otherwise false.
             */
            public boolean isFormField() {
                return formField;
            }

            /**
             * Returns an input stream, which may be used to
             * read the items contents.
             *
             * @return Opened input stream.
             * @throws IOException An I/O error occurred.
             */
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

            /**
             * Closes the file item.
             *
             * @throws IOException An I/O error occurred.
             */
            void close() throws IOException {
                stream.close();
            }

            /**
             * Returns the file item headers.
             *
             * @return The items header object
             */
            public FileItemHeaders getHeaders() {
                return headers;
            }

            /**
             * Sets the file item headers.
             *
             * @param pHeaders The items header object
             */
            public void setHeaders(FileItemHeaders pHeaders) {
                headers = pHeaders;
            }

        }

        /**
         * The multi part stream to process.
         */
        private final MultipartStream multi;

        /**
         * The notifier, which used for triggering the
         * {@link ProgressListener}.
         */
        private final MultipartStream.ProgressNotifier notifier;

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
         * @param ctx The request context.
         * @throws FileUploadException An error occurred while
         *                             parsing the request.
         * @throws IOException         An I/O error occurred.
         */
        FileItemIteratorImpl(HttpRequest ctx)
                throws FileUploadException, IOException {
            if (ctx == null) {
                throw new NullPointerException("ctx parameter");
            }

            String contentType = ctx.getContentType();
            if ((null == contentType)
                    || (!contentType.toLowerCase(Locale.ENGLISH).startsWith(MULTIPART))) {
                throw new InvalidContentTypeException(
                        format("the request doesn't contain a %s or %s stream, content type header is %s",
                                MULTIPART_FORM_DATA, MULTIPART_MIXED, contentType));
            }

            InputStream input = ctx.getInputStream();

            @SuppressWarnings("deprecation") // still has to be backward compatible
            final int contentLengthInt = ctx.getContentLength();

            final long requestSize = HttpRequest.class.isAssignableFrom(ctx.getClass())
                    // Inline conditional is OK here CHECKSTYLE:OFF
                    ? ((HttpRequest) ctx).contentLength()
                    : contentLengthInt;
            // CHECKSTYLE:ON

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

            String charEncoding = ctx.getCharacterEncoding();

            boundary = getBoundary(contentType);
            if (boundary == null) {
                throw new FileUploadException("the request was rejected because no multipart boundary was found");
            }

            notifier = new MultipartStream.ProgressNotifier(listener, requestSize);
            try {
                multi = new MultipartStream(input, boundary, notifier);
            } catch (IllegalArgumentException iae) {
                throw new InvalidContentTypeException(
                        format("The boundary specified in the %s header is too long", CONTENT_TYPE), iae);
            }
            multi.setHeaderEncoding(charEncoding);

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
        public boolean hasNext() throws FileUploadException, IOException {
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

    }

    /**
     * This exception is thrown for hiding an inner
     * {@link FileUploadException} in an {@link IOException}.
     */
    public static class FileUploadIOException extends IOException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -7047616958165584154L;

        /**
         * The exceptions cause; we overwrite the parent
         * classes field, which is available since Java
         * 1.4 only.
         */
        private final FileUploadException cause;

        /**
         * Creates a <code>FileUploadIOException</code> with the
         * given cause.
         *
         * @param pCause The exceptions cause, if any, or null.
         */
        public FileUploadIOException(FileUploadException pCause) {
            // We're not doing super(pCause) cause of 1.3 compatibility.
            cause = pCause;
        }

        /**
         * Returns the exceptions cause.
         *
         * @return The exceptions cause, if any, or null.
         */
        @Override
        public Throwable getCause() {
            return cause;
        }

    }

    /**
     * Thrown to indicate that the request is not a multipart request.
     */
    public static class InvalidContentTypeException
            extends FileUploadException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -9073026332015646668L;

        /**
         * Constructs a <code>InvalidContentTypeException</code> with no
         * detail message.
         */
        public InvalidContentTypeException() {
            super();
        }

        /**
         * Constructs an <code>InvalidContentTypeException</code> with
         * the specified detail message.
         *
         * @param message The detail message.
         */
        public InvalidContentTypeException(String message) {
            super(message);
        }

        /**
         * Constructs an <code>InvalidContentTypeException</code> with
         * the specified detail message and cause.
         *
         * @param msg   The detail message.
         * @param cause the original cause
         * @since 1.3.1
         */
        public InvalidContentTypeException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Thrown to indicate an IOException.
     */
    public static class IOFileUploadException extends FileUploadException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = 1749796615868477269L;

        /**
         * The exceptions cause; we overwrite the parent
         * classes field, which is available since Java
         * 1.4 only.
         */
        private final IOException cause;

        /**
         * Creates a new instance with the given cause.
         *
         * @param pMsg       The detail message.
         * @param pException The exceptions cause.
         */
        public IOFileUploadException(String pMsg, IOException pException) {
            super(pMsg);
            cause = pException;
        }

        /**
         * Returns the exceptions cause.
         *
         * @return The exceptions cause, if any, or null.
         */
        @Override
        public Throwable getCause() {
            return cause;
        }

    }

    /**
     * This exception is thrown, if a requests permitted size
     * is exceeded.
     */
    protected abstract static class SizeException extends FileUploadException {

        /**
         * Serial version UID, being used, if serialized.
         */
        private static final long serialVersionUID = -8776225574705254126L;

        /**
         * The actual size of the request.
         */
        private final long actual;

        /**
         * The maximum permitted size of the request.
         */
        private final long permitted;

        /**
         * Creates a new instance.
         *
         * @param message   The detail message.
         * @param actual    The actual number of bytes in the request.
         * @param permitted The requests size limit, in bytes.
         */
        protected SizeException(String message, long actual, long permitted) {
            super(message);
            this.actual = actual;
            this.permitted = permitted;
        }

        /**
         * Retrieves the actual size of the request.
         *
         * @return The actual size of the request.
         * @since 1.3
         */
        public long getActualSize() {
            return actual;
        }

        /**
         * Retrieves the permitted size of the request.
         *
         * @return The permitted size of the request.
         * @since 1.3
         */
        public long getPermittedSize() {
            return permitted;
        }

    }


    /**
     * Thrown to indicate that the request size exceeds the configured maximum.
     */
    public static class SizeLimitExceededException
            extends SizeException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = -2474893167098052828L;


        /**
         * Constructs a <code>SizeExceededException</code> with
         * the specified detail message, and actual and permitted sizes.
         *
         * @param message   The detail message.
         * @param actual    The actual request size.
         * @param permitted The maximum permitted request size.
         */
        public SizeLimitExceededException(String message, long actual,
                                          long permitted) {
            super(message, actual, permitted);
        }

    }

    /**
     * Thrown to indicate that A files size exceeds the configured maximum.
     */
    public static class FileSizeLimitExceededException
            extends SizeException {

        /**
         * The exceptions UID, for serializing an instance.
         */
        private static final long serialVersionUID = 8150776562029630058L;

        /**
         * File name of the item, which caused the exception.
         */
        private String fileName;

        /**
         * Field name of the item, which caused the exception.
         */
        private String fieldName;

        /**
         * Constructs a <code>SizeExceededException</code> with
         * the specified detail message, and actual and permitted sizes.
         *
         * @param message   The detail message.
         * @param actual    The actual request size.
         * @param permitted The maximum permitted request size.
         */
        public FileSizeLimitExceededException(String message, long actual,
                                              long permitted) {
            super(message, actual, permitted);
        }

        /**
         * Returns the file name of the item, which caused the
         * exception.
         *
         * @return File name, if known, or null.
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Sets the file name of the item, which caused the
         * exception.
         *
         * @param pFileName the file name of the item, which caused the exception.
         */
        public void setFileName(String pFileName) {
            fileName = pFileName;
        }

        /**
         * Returns the field name of the item, which caused the
         * exception.
         *
         * @return Field name, if known, or null.
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Sets the field name of the item, which caused the
         * exception.
         *
         * @param pFieldName the field name of the item,
         *                   which caused the exception.
         */
        public void setFieldName(String pFieldName) {
            fieldName = pFieldName;
        }

    }

    /**
     * Returns the progress listener.
     *
     * @return The progress listener, if any, or null.
     */
    public ProgressListener getProgressListener() {
        return listener;
    }

    /**
     * Sets the progress listener.
     *
     * @param pListener The progress listener, if any. Defaults to null.
     */
    public void setProgressListener(ProgressListener pListener) {
        listener = pListener;
    }


    /**
     * Exception for errors encountered while processing the request.
     *
     * @version $Id: FileUploadException.java 1454690 2013-03-09 12:08:48Z simonetripodi $
     */
    public static class FileUploadException extends Exception {

        /**
         * Serial version UID, being used, if the exception
         * is serialized.
         */
        private static final long serialVersionUID = 8881893724388807504L;

        /**
         * The exceptions cause. We overwrite the cause of
         * the super class, which isn't available in Java 1.3.
         */
        private final Throwable cause;

        /**
         * Constructs a new <code>FileUploadException</code> without message.
         */
        public FileUploadException() {
            this(null, null);
        }

        /**
         * Constructs a new <code>FileUploadException</code> with specified detail
         * message.
         *
         * @param msg the error message.
         */
        public FileUploadException(final String msg) {
            this(msg, null);
        }

        /**
         * Creates a new <code>FileUploadException</code> with the given
         * detail message and cause.
         *
         * @param msg   The exceptions detail message.
         * @param cause The exceptions cause.
         */
        public FileUploadException(String msg, Throwable cause) {
            super(msg);
            this.cause = cause;
        }

        /**
         * Prints this throwable and its backtrace to the specified print stream.
         *
         * @param stream <code>PrintStream</code> to use for output
         */
        @Override
        public void printStackTrace(PrintStream stream) {
            super.printStackTrace(stream);
            if (cause != null) {
                stream.println("Caused by:");
                cause.printStackTrace(stream);
            }
        }

        /**
         * Prints this throwable and its backtrace to the specified
         * print writer.
         *
         * @param writer <code>PrintWriter</code> to use for output
         */
        @Override
        public void printStackTrace(PrintWriter writer) {
            super.printStackTrace(writer);
            if (cause != null) {
                writer.println("Caused by:");
                cause.printStackTrace(writer);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Throwable getCause() {
            return cause;
        }

    }
}
