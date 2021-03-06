package com.yieldnull.httpd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ContentType {

    public static final String CONTENT_TYPE = "Content-type";
    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String CONTENT_LENGTH = "Content-length";
    public static final String FORM_DATA = "form-data";
    public static final String ATTACHMENT = "attachment";
    public static final String MULTIPART = "multipart/";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String MULTIPART_MIXED = "multipart/mixed";
    private static final String TAG = ContentType.class.getSimpleName();

    public static final String MIME_PLAINTEXT = "text/plain; charset=utf-8";
    public static final String MIME_HTML = "text/html; charset=utf-8";
    public static final String MIME_CSS = "text/css; charset=utf-8";
    public static final String MIME_JAVASCRIPT = "text/javascript; charset=UTF-8";
    public static final String MIME_STREAM = "application/octet-stream";
    public static final String MIME_PNG = "image/png";
    public static final String TEXT = "text/";
    public static final String JSON = "application/json";

    private static final String ASCII_ENCODING = "US-ASCII";
    private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";

    private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";
    private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";
    private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

    private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

    private final String contentTypeHeader;
    private final String contentType;
    private final String encoding;
    private final String boundary;


    public ContentType(String contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
        if (contentTypeHeader != null) {
            contentType = getDetailFromContentHeader(contentTypeHeader, MIME_PATTERN, "", 1);
            encoding = getDetailFromContentHeader(contentTypeHeader, CHARSET_PATTERN, null, 2);
        } else {
            contentType = "";
            encoding = "UTF-8";
        }
        if (MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(contentType)) {
            boundary = getDetailFromContentHeader(contentTypeHeader, BOUNDARY_PATTERN, null, 2);
        } else {
            boundary = null;
        }
    }

    private String getDetailFromContentHeader(String contentTypeHeader, Pattern pattern, String defaultValue, int group) {
        Matcher matcher = pattern.matcher(contentTypeHeader);
        return matcher.find() ? matcher.group(group) : defaultValue;
    }

    public String getContentTypeHeader() {
        return contentTypeHeader;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEncoding() {
        return encoding == null ? ASCII_ENCODING : encoding;
    }

    public String getBoundary() {
        return boundary;
    }

    public boolean isMultipart() {
        return MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(contentType);
    }

    public ContentType tryUTF8() {
        if (encoding == null) {
            return new ContentType(this.contentTypeHeader + "; charset=UTF-8");
        }
        return this;
    }
}