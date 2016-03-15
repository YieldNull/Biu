package com.bbbbiu.biu.httpd.util;

import android.util.Log;

import com.bbbbiu.biu.httpd.HttpDaemon;
import com.bbbbiu.biu.httpd.upload.exceptions.InvalidFileNameException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public final class Streams {

    private Streams() {
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 把输入流内的内容写入到输出流。并关闭输入流
     *
     * @param inputStream       最后会被关闭
     * @param outputStream      可为null，表示输入流中的内容直接被丢弃
     * @param closeOutputStream 最后是否关闭输出流
     * @return 从输入流复制的数据量，按byte记
     */
    public static long copy(InputStream inputStream, OutputStream outputStream, boolean closeOutputStream)
            throws IOException {

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        OutputStream out = outputStream;
        InputStream in = inputStream;
        try {
            long total = 0;
            while (true) {
                int res = in.read(buffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    total += res;
                    if (out != null) {
                        out.write(buffer, 0, res);
                    }
                }
            }
            if (out != null) {
                if (closeOutputStream) {
                    out.close();
                } else {
                    out.flush();
                }
                out = null;
            }
            in.close();
            in = null;
            return total;
        } finally {
            safeClose(in);
            if (closeOutputStream) {
                safeClose(out);
            }
        }
    }


    /**
     * Checks, whether the given file name is valid in the sense,
     * that it doesn't contain any NUL characters. If the file name
     * is valid, it will be returned without any modifications. Otherwise,
     * an {@link InvalidFileNameException} is raised.
     *
     * @param fileName The file name to check
     * @return Unmodified file name, if valid.
     * @throws InvalidFileNameException The file name was found to be invalid.
     */
    public static String checkFileName(String fileName) {
        if (fileName != null && fileName.indexOf('\u0000') != -1) {
            // pFileName.replace("\u0000", "\\0")
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fileName.length(); i++) {
                char c = fileName.charAt(i);
                switch (c) {
                    case 0:
                        sb.append("\\0");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            throw new InvalidFileNameException(fileName,
                    "Invalid file name: " + sb);
        }
        return fileName;
    }

    /**
     * 安全地关闭流
     *
     * @param closable 可关闭对象
     */
    public static void safeClose(Object closable) {
        try {
            if (closable != null) {
                if (closable instanceof Closeable) {
                    ((Closeable) closable).close();
                } else {
                    throw new IllegalArgumentException("Unknown object to close");
                }
            }
        } catch (IOException e) {
            Log.e(HttpDaemon.TAG, "Could not close", e);
        }
    }
}
