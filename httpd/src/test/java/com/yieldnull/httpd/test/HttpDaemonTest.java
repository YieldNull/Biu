package com.yieldnull.httpd.test;

import com.google.common.io.Files;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by YieldNull at 7/25/16
 */
public class HttpDaemonTest {

    private final String url = "http://127.0.0.1:5050";

    @Before
    public void setUp() throws Exception {
        HttpDaemon.getSingleton().start();
    }


    @After
    public void tearDown() throws Exception {
        HttpDaemon.getSingleton().stop();
    }

    @Test
    public void form() throws Exception {
        HttpDaemon.registerServlet("/", new HttpServlet() {
            @Override
            public HttpResponse doGet(HttpRequest request) {
                return null;
            }

            @Override
            public HttpResponse doPost(HttpRequest request) {

                File repository = new File(System.getProperty("user.home"));

                request.parseMultipartBody(repository, null);

                String title = request.form().get("title");

                return HttpResponse.newResponse(title);
            }
        });


        final String fileName = "httpd.jar";
        final File file = new File("build/libs/httpd.jar");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", "test")
                .addFormDataPart("files", fileName,
                        RequestBody.create(MediaType.parse("image/png"), file))
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = new OkHttpClient().newCall(request).execute();

        String title = response.body().string();

        boolean same = Files.equal(new File(System.getProperty("user.home"), fileName), file);

        assertEquals(true, title.equals("test") && same);
    }


    @Test
    public void raw() throws Exception {
        final String jsonPayload = "{\"name\":\"yieldnull\"}";

        HttpDaemon.registerServlet("/", new HttpServlet() {
            @Override
            public HttpResponse doGet(HttpRequest request) {
                return null;
            }

            @Override
            public HttpResponse doPost(HttpRequest request) {

                return HttpResponse.newResponse(request.text());
            }
        });

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonPayload);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Response response = new OkHttpClient().newCall(request).execute();

        String json = response.body().string();

        assertEquals(jsonPayload, json);
    }
}