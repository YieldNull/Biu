package com.yieldnull.httpd.upload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entity Header
 */
public class MultipartHeader {

    private final Map<String, List<String>> headerNameToValueListMap = new LinkedHashMap<>();

    public String getHeader(String name) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (headerValueList == null) {
            return null;
        }
        return headerValueList.get(0);
    }

    public synchronized void addHeader(String name, String value) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);

        if (headerValueList == null) {
            headerValueList = new ArrayList<>();
            headerNameToValueListMap.put(nameLower, headerValueList);
        }

        headerValueList.add(value);
    }
}
