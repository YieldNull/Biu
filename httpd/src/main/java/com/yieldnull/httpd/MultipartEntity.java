package com.yieldnull.httpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class MultipartEntity {

    /**
     * Content Type
     */
    private final String contentType;


    /**
     * 文件字段名（input标签的 name属性）
     */
    private final String fieldName;


    /**
     * 文件名
     */
    private final String fileName;


    /**
     * 是否是文件
     */
    private final boolean isFormData;


    /**
     * form field value
     */
    private final String fieldValue;


    /**
     * 输入流
     */
    private final InputStream stream;


    /**
     * headers
     */
    private MultipartHeader headers;


    /**
     * 构造函数
     *
     * @param fieldName   fieldName in form
     * @param contentType content type
     * @param fileName    文件名，可为空，表示非文件
     * @param inputStream Item body的输入流
     * @throws IOException
     */
    MultipartEntity(String fieldName, String contentType, String fileName,
                    InputStream inputStream) throws IOException {

        this.fileName = fileName;
        this.fieldName = fieldName;
        this.contentType = contentType;
        stream = inputStream;

        isFormData = fileName == null && contentType == null;

        if (isFormData) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            fieldValue = reader.readLine();

            stream.close();
        } else {
            fieldValue = null;
        }
    }


    /**
     * 表单name
     *
     * @return fieldName
     */
    public String fieldName() {
        return fieldName;
    }


    /**
     * 表单值
     *
     * @return 若是文件，则返回null
     */
    public String fieldValue() {
        return isFormData ? fieldValue : null;
    }


    /**
     * 是否为文件
     *
     * @return 是否是文件
     */
    public boolean isFile() {
        return !isFormData;
    }


    /**
     * 获取文件名
     *
     * @return 文件名
     */
    public String fileName() {
        return fileName;
    }


    /**
     * headers
     *
     * @return headers
     */
    public MultipartHeader headers() {
        return headers;
    }


    /**
     * content type
     *
     * @return content type
     */
    public String contentType() {
        return contentType;
    }


    /**
     * 获取大小
     *
     * @return 获取不到则为-1
     */
    public long contentLength() {
        long size;
        try {
            String cl1 = headers().getHeader("content-length");
            size = Long.parseLong(cl1);
        } catch (NumberFormatException var4) {
            size = -1L;
        }

        return size;
    }


    /**
     * 设置headers
     *
     * @param headers headers
     */
    public void setHeaders(MultipartHeader headers) {
        this.headers = headers;
    }


    /**
     * 获取数据流
     *
     * @return 数据流若不是文件，则返回null
     * @see #fieldValue()
     */
    public InputStream stream() {
        return isFormData ? null : stream;
    }


    /**
     * 关闭输入流
     */
    public void close() {
        Streams.safeClose(stream);
    }

}
