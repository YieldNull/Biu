package com.bbbbiu.biu.gui.choose;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.bbbbiu.biu.gui.adapters.ApkAdapter;
import com.bbbbiu.biu.gui.adapters.DividerItemDecoration;
import com.bbbbiu.biu.gui.adapters.PanelBaseAdapter;
import com.bbbbiu.biu.util.Apk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkChooseActivity extends ChooseBaseActivity {
    PackageManager pm ;
    private ArrayList<Apk> apk_lists;
    private ArrayList<String> pknm_list;
    Apk adapter_apk;


    public static String TAG = ApkChooseActivity.class.getSimpleName();
    private ArrayList<String> mDatas;
    private List<PackageInfo> apkInfos;
    private ArrayList<String> apk_names;
    private ArrayList<Drawable> apk_icons;

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new DividerItemDecoration(this,DividerItemDecoration.VERTICAL_LIST);
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new GridContentLayoutManager(this,5);
    }

    @Override
    protected RecyclerView.Adapter onCreateContentAdapter() {
        initData();
        return new ApkAdapter(ApkChooseActivity.this,apk_lists);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    public void onFileChosen(File file) {

    }

    @Override
    public void onFileDismissed(File file) {


    }
    private void initData()
    {
        pm = getPackageManager();
        apk_lists = new ArrayList<Apk>();
        apkInfos = pm.getInstalledPackages(0);
        apk_names = new ArrayList<String>();
        apk_icons = new ArrayList<Drawable>();
        pknm_list = new ArrayList<String>();

        String name = " ";
        Drawable icon;
        PackageInfo apkinfo;
        String pknm;
        ApplicationInfo appInfo;
        for (int i = 0; i < apkInfos.size(); )
        {

            apkinfo = apkInfos.get(i);
            appInfo = apkinfo.applicationInfo;
            String s = appInfo.dataDir;

            i++;
            //判断是否为系统应用
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM)<=0) {
                pknm = appInfo.packageName;

                name = (String) pm.getApplicationLabel(apkinfo.applicationInfo);
                icon = pm.getApplicationIcon(apkinfo.applicationInfo);

                apk_icons.add(icon);

                apk_names.add(name);
                adapter_apk = new Apk(name, convertDrawableToBitmap(icon));
                apk_lists.add(adapter_apk);
                pknm_list.add(pknm);
            }


        }
        String prefix = "/data/app";
        String installed_apk_path=null;
        String apkname;
        //获取apk安装路径
        for (int i =0;i<pknm_list.size();i++)
        {
            apkname = apk_names.get(i);
            pknm = pknm_list.get(i);
            try{
                installed_apk_path = pm.getApplicationInfo(
                        pknm, PackageManager.GET_UNINSTALLED_PACKAGES).sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG,apkname+" : "+installed_apk_path);

        }


        mDatas = new ArrayList<String>();
        for (int i = 'A'; i < 'z'; i++)
        {
            mDatas.add("" + (char) i);
        }
    }
    public Bitmap convertDrawableToBitmap(Drawable drawable)
    {
        BitmapDrawable bd = (BitmapDrawable) drawable;
        return bd.getBitmap();
    }
}
