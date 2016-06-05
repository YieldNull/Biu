package com.bbbbiu.biu.lib.servlet.android;

import android.content.Context;
import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.servlet.ReceivingBaseServlet;
import com.bbbbiu.biu.lib.util.HttpConstants;

/**
 * Created by YieldNull at 4/22/16
 */
public class ReceivingServlet extends ReceivingBaseServlet {
    private static final String TAG = ReceivingServlet.class.getSimpleName();

    public static void register(Context context) {
        HttpDaemon.registerServlet(HttpConstants.Android.URL_UPLOAD, new ReceivingServlet(context));
    }

    private ReceivingServlet(Context context) {
        super(context);
    }
}
