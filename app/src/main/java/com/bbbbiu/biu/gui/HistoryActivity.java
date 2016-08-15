package com.bbbbiu.biu.gui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.TransferRecord;
import com.bbbbiu.biu.gui.adapter.HistoryAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.gui.transfer.android.SendingActivity;
import com.bbbbiu.biu.gui.transfer.computer.ConnectingActivity;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HistoryActivity extends AppCompatActivity implements OnChoosingListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();

    @Bind(R.id.container)
    protected ViewPager mViewPager;

    @Bind(R.id.tabs)
    protected TabLayout mTabLayout;


    private static final String ACTION_SEND_ANDROID = "Android";
    private static final String ACTION_SEND_COMPUTER = "Computer";
    private static final String ACTION_SEND_APPLE = "Apple";

    @Bind(R.id.float_action_menu)
    protected FloatingActionMenu mFloatingActionMenu;

    @OnClick(R.id.fbtn_send_android)
    protected void clickSendAndroid() {
        sendFile(ACTION_SEND_ANDROID);
    }

    @OnClick(R.id.fbtn_send_apple)
    protected void clickSendApple() {
        sendFile(ACTION_SEND_APPLE);
    }

    @OnClick(R.id.fbtn_send_computer)
    protected void clickSendComputer() {
        sendFile(ACTION_SEND_COMPUTER);
    }


    private HistoryAdapter mCurrentHistoryAdapter;
    private TransferPageAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ButterKnife.bind(this);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        // floating action menu show && hide动画
        mFloatingActionMenu.setMenuButtonShowAnimation(AnimationUtils.loadAnimation(this, R.anim.show_from_bottom));
        mFloatingActionMenu.setMenuButtonHideAnimation(AnimationUtils.loadAnimation(this, R.anim.hide_to_bottom));

        // floating action menu
        mFloatingActionMenu.setIconAnimated(false);
        mFloatingActionMenu.setClosedOnTouchOutside(true);
        mFloatingActionMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryAdapter adapter = getCurrentHistoryAdapter();
                if (adapter.getChosenFiles().size() == 0) {

                    Log.i(TAG, "No file chosen, abort sending");
                    showToastInUI(R.string.hint_choose_file_required);

                    if (!adapter.isOnChoosing()) {
                        adapter.setOnChoosing();
                    }
                } else {
                    mFloatingActionMenu.toggle(true);
                }
            }
        });


        mPagerAdapter = new TransferPageAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentHistoryAdapter = mPagerAdapter.getFragment(position).adapter;
                HistoryActivity.this.invalidateOptionsMenu();
                refreshTitle();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (getCurrentHistoryAdapter().getChosenCount() == 0) {
            getMenuInflater().inflate(R.menu.common_normal, menu);
        } else {
            getMenuInflater().inflate(R.menu.history_chosen, menu);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_choose_all:
                getCurrentHistoryAdapter().setFileAllChosen();
                invalidateOptionsMenu();
                refreshTitle();
                break;
            case R.id.action_choosing_dismiss:
                getCurrentHistoryAdapter().setFileAllDismissed();
                invalidateOptionsMenu();
                refreshTitle();
                break;
            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.hint_file_delete_confirm))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                showToastInUI(R.string.file_background_running);

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getCurrentHistoryAdapter().deleteChosenFiles();

                                        HistoryActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                invalidateOptionsMenu();
                                                refreshTitle();

                                                PlaceholderFragment fragment = mPagerAdapter.getFragment(mViewPager.getCurrentItem());
                                                fragment.checkDataSetAmount();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToastInUI(R.string.hint_file_delete_dismissed);
                            }
                        }).show();
                break;
            case R.id.action_share:
                shareFile();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onFileChosen(String filePath) {
        if (getCurrentHistoryAdapter().getChosenCount() == 1) {
            invalidateOptionsMenu();
        }
        refreshTitle();

        Log.i(TAG, "File chosen: " + filePath);
    }

    @Override
    public void onFileDismissed(String filePath) {
        if (getCurrentHistoryAdapter().getChosenCount() == 0) {
            invalidateOptionsMenu();
        }
        refreshTitle();

        Log.i(TAG, "File dismissed: " + filePath);
    }


    /**
     * 更新title，显示选中数量
     */
    @SuppressWarnings("ConstantConditions")
    protected void refreshTitle() {
        if (getCurrentHistoryAdapter().getChosenCount() == 0) {
            getSupportActionBar().setTitle(R.string.title_activity_history);
        } else {
            getSupportActionBar().setTitle(getString(R.string.title_chosen_file_count,
                    mCurrentHistoryAdapter.getChosenCount()));
        }
    }


    /**
     * 获取当前 {@link HistoryAdapter}，不要直接使用{@link #mCurrentHistoryAdapter}
     *
     * @return {@link HistoryAdapter}
     */
    private HistoryAdapter getCurrentHistoryAdapter() {
        if (mCurrentHistoryAdapter == null) {
            mCurrentHistoryAdapter = mPagerAdapter.getFragment(mViewPager.getCurrentItem()).adapter;
        }

        return mCurrentHistoryAdapter;
    }


    /**
     * 点击发送按钮
     *
     * @param action 发送给谁
     */
    private void sendFile(String action) {
        mFloatingActionMenu.toggle(false);

        Set<String> files = mCurrentHistoryAdapter.getChosenFiles();
        PreferenceUtil.storeFilesToSend(this, files);

        switch (action) {
            case ACTION_SEND_ANDROID:
                SendingActivity.startConnection(this);
                break;
            case ACTION_SEND_COMPUTER:
                ConnectingActivity.connectForSending(this);
                break;
            default:
                com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForSending(this);
                break;
        }

        Log.i(TAG, String.format("Sending files to %s. File Amount: %d", action, files.size()));
    }


    /**
     * 分享选中文件
     */
    private void shareFile() {
        Intent shareIntent = new Intent();

        if (getCurrentHistoryAdapter().getChosenCount() == 1) {
            TransferRecord record = getCurrentHistoryAdapter().getChosenRecords().iterator().next();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, record.getUri());

            String mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(StorageUtil.getFileExtension(record.name));
            mime = mime == null ? "*/*" : mime;
            shareIntent.setType(mime);

            Log.i(TAG, "Ready to share a single file. MIME:" + mime + " Uri:" + record.uri);

        } else {
            ArrayList<Uri> uris = new ArrayList<>();
            Log.i(TAG, "Ready to share multi files");

            for (TransferRecord record : getCurrentHistoryAdapter().getChosenRecords()) {
                uris.add(record.getUri());

                Log.i(TAG, "Ready to share file. Uri:" + record.uri);
            }

            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.setType("*/*");
        }

        Log.i(TAG, "Show chooser list");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.title_share_file_to)));
    }


    /**
     * 在UI线程显示Toast
     *
     * @param stringId string resource id
     */
    private void showToastInUI(final int stringId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HistoryActivity.this, stringId, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * PageAdapter
     */
    public class TransferPageAdapter extends FragmentPagerAdapter {
        private List<PlaceholderFragment> fragmentList;


        public TransferPageAdapter(FragmentManager fm) {
            super(fm);
            fragmentList = new ArrayList<>();
            fragmentList.add(PlaceholderFragment.newInstance(0));
            fragmentList.add(PlaceholderFragment.newInstance(1));

        }

        /**
         * 获取指定位置的Fragment
         *
         * @param position 位置
         * @return Fragment
         */
        public PlaceholderFragment getFragment(int position) {
            return fragmentList.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_received);
                case 1:
                    return getString(R.string.tab_sent);
            }
            return null;
        }
    }


    /**
     * Placeholder Fragment
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "TAB_NUMBER";


        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Fragment 对应的Adapter
         */
        private HistoryAdapter adapter;

        private TextView emptyText;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {


            View rootView = inflater.inflate(R.layout.fragment_history, container, false);

            RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

            emptyText = (TextView) rootView.findViewById(R.id.textView_empty);

            adapter = new HistoryAdapter(getActivity(), this,
                    getArguments().getInt(ARG_SECTION_NUMBER) == 0);

            recyclerView.setAdapter(adapter);

            checkDataSetAmount();

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.addItemDecoration(new HorizontalDividerItemDecoration
                    .Builder(getContext()).build());

            return rootView;
        }

        public void checkDataSetAmount() {
            if (adapter.getItemCount() == 0) {
                emptyText.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.GONE);
            }
        }
    }
}
