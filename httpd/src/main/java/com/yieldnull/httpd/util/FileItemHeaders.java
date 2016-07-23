package com.yieldnull.httpd.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FileItemHeaders implements Serializable {

    private static final long serialVersionUID = -4455695752627032559L;

    private final Map<String, List<String>> headerNameToValueListMap = new LinkedHashMap<String, List<String>>();

    public String getHeader(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (null == headerValueList) {
            return null;
        }
        return headerValueList.get(0);
    }

    public synchronized void addHeader(String name, String value) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (null == headerValueList) {
            headerValueList = new ArrayList<>();
            headerNameToValueListMap.put(nameLower, headerValueList);
        }
        headerValueList.add(value);
    }
}
