package com.yieldnull.httpd;


import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class Streams {


    private Streams() {
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 把输入流内的内容写入到输出流。并关闭输入流
     *
     * @param in       最后会被关闭
     * @param out      可为null，表示输入流中的内容直接被丢弃
     * @param closeOut 最后是否关闭输出流
     * @return 从输入流复制的数据量，按byte记
     */
    public static long copy(InputStream in, OutputStream out,
                            boolean closeOut, ProgressNotifier notifier)

            throws IOException {

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            long total = 0;
            while (true) {
                int bytes = in.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (bytes > 0) {
                    total += bytes;
                    if (out != null) {
                        out.write(buffer, 0, bytes);
                        if (notifier != null) {
                            notifier.noteBytesRead(bytes); // notify bytes read
                        }
                    }
                }
            }
            if (out != null) {
                if (closeOut) {
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
            if (closeOut) {
                safeClose(out);
            }
        }
    }

    /**
     * 安全地关闭流
     *
     * @param closable 可关闭对象
     */
    public static void safeClose(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 安全地关闭Socket
     *
     * @param socket socket
     */
    public static void safeClose(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * 处理文件名，替换其中的非法字符
     *
     * @param fileName 文件名
     * @return 处理后的文件名
     */
    public static String verifyFileName(String fileName) {
        return fileName.replaceAll("[/\\|:?*<>\"]", "_");
    }


    /**
     * 若遇到附件文件名相同的情况，在文件名后面加序号
     * <p/>
     * 如 filename(1) filename(2)
     *
     * @param fileName 要添加序号的文件名
     * @return 更改后的文件名
     */
    public static String genVersionedFileName(File repository, String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        int insertIndex = dotIndex >= 0 ? dotIndex : fileName.length();

        int version = 1;
        StringBuilder builder = new StringBuilder(fileName);
        builder.insert(insertIndex, String.format("(%s)", String.valueOf(version)));

        while (new File(repository, builder.toString()).exists()) {
            builder.replace(insertIndex + 1, insertIndex + 2, String.valueOf(++version));
        }

        return builder.toString();
    }
}
