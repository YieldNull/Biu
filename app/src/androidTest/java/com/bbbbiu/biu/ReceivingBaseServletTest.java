package com.bbbbiu.biu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.LocalBroadcastManager;

import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.ProgressListenerImpl;
import com.bbbbiu.biu.lib.servlet.ReceivingBaseServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.bbbbiu.biu.util.StorageUtil;
import com.google.common.io.Files;
import com.yieldnull.httpd.HttpDaemon;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static org.junit.Assert.assertTrue;

/**
 * 测试接收文件
 * <p/>
 * Created by YieldNull at 7/26/16
 */
@RunWith(AndroidJUnit4.class)
public class ReceivingBaseServletTest {
    Context context = InstrumentationRegistry.getTargetContext();

    private final String url = "http://127.0.0.1:5050";

    private String fileName;
    private File file;

    private File repository;
    private File fileCopy;

    private final String FILE_URI = "fileUri";

    @Before
    public void setUp() throws Exception {
        HttpdService.startService(context);

        file = getApk();
        assert file != null;

        fileName = file.getName();

        repository = StorageUtil.getDownloadDir(context);
        fileCopy = new File(repository, fileName);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() throws Exception {
        HttpdService.stopService(context);

        fileCopy.delete();
    }

    @Test
    public void testReceiveFile() throws Exception {
        HttpDaemon.registerServlet("/", new ReceivingBaseServlet(context));


        Receiver receiver = new Receiver();
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                new IntentFilter(TransferBaseActivity.ACTION_UPDATE_PROGRESS));

        while (!HttpDaemon.getSingleton().isStarted()) {
            Thread.sleep(1000);
        }

        Request request = formWithFileAndUriRequest();
        Response response = new OkHttpClient().newCall(request).execute();

        while (receiver.fileUri == null) {
            Thread.sleep(1000);
        }

        System.out.println(receiver.progressList);

        assertThat(receiver.fileUri, is(file.getAbsolutePath()));
        assertThat(receiver.progressList, hasItem(100));
        assertThat(response.code(), is(200));
        assertTrue(Files.equal(file, fileCopy));
    }


    /**
     * 监听进度广播
     */
    static class Receiver extends BroadcastReceiver {
        String fileUri;
        List<Integer> progressList = new ArrayList<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra(TransferBaseActivity.EXTRA_RESULT_CODE, -1);
            Bundle bundle = intent.getBundleExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE);

            if (resultCode == ProgressListenerImpl.RESULT_PROGRESS) {
                int progress = bundle.getInt(ProgressListenerImpl.RESULT_EXTRA_PROGRESS);
                fileUri = bundle.getString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI);

                progressList.add(progress);

                if (progress == 100) {
                    System.out.println(fileUri);
                }
            }
        }
    }

    /**
     * 构造okhttp请求
     * <p/>
     * 将APK文件以及其在文件系统中的URI以 multipart/form-data的形式发送
     *
     * @return okhttp request
     */
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

    /**
     * 获取本应用的APK文件
     *
     * @return 文件
     */
    private File getApk() {
        PackageManager manager = context.getPackageManager();

        try {
            ApplicationInfo ai = manager.getApplicationInfo("com.bbbbiu.biu", 0);
            String sourceApk = ai.publicSourceDir;
            return new File(sourceApk);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return null;
    }
}