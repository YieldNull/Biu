package com.yieldnull.httpd;

import com.google.common.io.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * Created by YieldNull at 7/25/16
 */
public class HttpDaemonTest {

    private final String url = "http://127.0.0.1:5050";

    final String fileName = "httpd.jar";
    final File file = new File("build/libs/httpd.jar");

    final File repository = new File(System.getProperty("user.home"));
    final File fileCopy = new File(repository, fileName);

    final String FILE_URI = "fileUri";

    @Before
    public void setUp() throws Exception {
        HttpDaemon.getSingleton().start();
    }


    @After
    public void tearDown() throws Exception {
        HttpDaemon.getSingleton().stop();

        //noinspection ResultOfMethodCallIgnored
        fileCopy.delete();
    }


    @Test
    public void testGet() throws Exception {
        final String greet = "Hello World";

        HttpDaemon.registerServlet("/", new HttpServlet() {
            @Override
            public HttpResponse doGet(HttpRequest request) {
                return HttpResponse.newResponse(greet);
            }

            @Override
            public HttpResponse doPost(HttpRequest request) {
                return null;
            }
        });


        Request request = new Request.Builder().url(url).build();

        Response response = new OkHttpClient().newCall(request).execute();

        assertThat(response.body().string(), is(greet));
    }

    @Test
    public void testFormFileProgress() throws Exception {

        final List<Integer> progressList = new ArrayList<>();

        HttpDaemon.registerServlet("/", new HttpServlet() {
            @Override
            public HttpResponse doGet(HttpRequest request) {
                return null;
            }

            @Override
            public HttpResponse doPost(HttpRequest request) {

                ProgressListener listener = new ProgressListener() {

                    private int mProgress;

                    @Override
                    public void update(String fileUri, long pBytesRead, long pContentLength) {
                        int progress = (int) (pBytesRead * 100.0 / pContentLength);

                        if (progress > mProgress) {
                            mProgress = progress;
                            progressList.add(mProgress);
                        }
                    }
                };

                request.parseMultipartBody(repository, listener);


                return HttpResponse.newResponse(request.form().get(FILE_URI));
            }
        });

        Request request = formWithFileAndUriRequest();
        Response response = new OkHttpClient().newCall(request).execute();


        System.out.println(progressList); // 进度输出

        assertThat(progressList, hasItem(100));
    }

    @Test
    public void testFormWithFileAndUri() throws Exception {
        HttpDaemon.registerServlet("/", new HttpServlet() {
            @Override
            public HttpResponse doGet(HttpRequest request) {
                return null;
            }

            @Override
            public HttpResponse doPost(HttpRequest request) {

                request.parseMultipartBody(repository, null);

                String title = request.form().get("fileUri");

                return HttpResponse.newResponse(title);
            }
        });

        Request request = formWithFileAndUriRequest();
        Response response = new OkHttpClient().newCall(request).execute();

        String uri = response.body().string();

        // two files contain same data
        boolean same = Files.equal(fileCopy, file);

        assertThat(same, is(true));
        assertThat(uri, is(file.getAbsolutePath()));
    }


    @Test
    public void testRawData() throws Exception {
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
                .addHeader("Connection", "close")
                .build();

        System.setProperty("http.keepAlive", "false");
        Response response = new OkHttpClient().newCall(request).execute();

        String json = response.body().string();

        assertThat(json, is(jsonPayload));
    }


    private Request formWithFileAndUriRequest() {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(FILE_URI, file.getAbsolutePath())
                .addFormDataPart("files", fileName,
                        RequestBody.create(MediaType.parse("application/java-archive"), file))
                .build();


        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }
}