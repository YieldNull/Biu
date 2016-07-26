package com.bbbbiu.biu.lib.util;


import com.yieldnull.httpd.HttpDaemon;

import java.net.InetAddress;
import java.util.HashMap;

/**
 * Created by YieldNull at 4/23/16
 */
public class HttpConstants {
    public static final String FILE_FORM_NAME = "files";
    public static final String FILE_URI = "fileUri";

    public static class Computer {
        public static final String HOST = "http://www.bbbbiu.com";//"http://192.168.1.102";//

        public static final String URL_BIND = HOST + "/bind";
        public static final String URL_UPLOAD = HOST + "/api/upload";
        public static final String URL_FILE_LIST = HOST + "/api/filelist";

        private static final String BIND_WHAT_UPLOAD = "upload";
        private static final String BIND_WHAT_DOWNLOAD = "download";

        public static String getBindUploadUrl(String uid) {
            return URL_BIND + "?uid=" + uid + "&what=" + BIND_WHAT_UPLOAD;
        }

        public static String getBindDownloadUrl(String uid) {
            return URL_BIND + "?uid=" + uid + "&what=" + BIND_WHAT_DOWNLOAD;
        }

        public static String getUploadUrl(String uid) {
            return URL_UPLOAD;
        }

        public static HashMap<String, String> getUploadFormData(String uid) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("uid", uid);
            return hashMap;
        }

        public static String getDownloadUrl(String uid, String fileUri) {
            return HOST + fileUri + "?uid=" + uid;
        }

        public static String getManifestUrl(String uid) {
            return URL_FILE_LIST + "?uid=" + uid;
        }
    }


    public static class Android {
        public static final String URL_MANIFEST = "/manifest";
        public static final String URL_UPLOAD = "/upload";

        public static String getManifestUrl(InetAddress serverAddress) {
            return "http://" + serverAddress.getHostAddress() + ":" + HttpDaemon.getPort() + URL_MANIFEST;
        }

        public static String getSendUrl(InetAddress serverAddress) {
            return "http://" + serverAddress.getHostAddress() + ":" + HttpDaemon.getPort() + URL_UPLOAD;
        }
    }

    public static class Apple {
        public static final String URL_UPLOAD = "/upload";
        public static final String URL_MANIFEST = "/manifest";
        public static final String URL_DOWNLOAD = "/download";
    }

}
