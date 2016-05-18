package com.bbbbiu.biu.gui.adapter.choose.content;

import android.util.Log;

import com.bbbbiu.biu.gui.choose.BaseChooseActivity;

/**
 * 实现了如何读取，更新数据的CommonAdapter
 * <p/>
 * 如何获取数据：
 * <p/>
 * - 先从数据库读数据 {@link CommonContentAdapter#readDataFromDB()}，要是没有则
 * <p/>
 * - 从系统中 {@link CommonContentAdapter#readDataFromSys()} 读取数据
 * <p/>
 * - 将数据显示（没有则显示空）
 * <p/>
 * - 要数据是从数据库中读取的，那么就后台再扫描一遍MediaStore，存入数据库 {@link CommonContentAdapter#updateDatabase()}
 * <p/>
 * - 然后更新数据集并显示，{@link CommonContentAdapter#updateDataSet()}
 * <p/>
 * <p/>
 * 如何显示 TODO
 * <p/>
 * Created by YieldNull at 3/26/16
 */
public abstract class CommonContentAdapter extends BaseContentAdapter {
    private static final String TAG = CommonContentAdapter.class.getSimpleName();

    public CommonContentAdapter(final BaseChooseActivity context) {
        super(context);

        notifyStartLoadingData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasData = readDataFromDB();
                boolean loadFromDb = true;

                if (!hasData) {
                    hasData = readDataFromSys();
                    loadFromDb = false;
                }

                final boolean finalHasData = hasData;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyFinishLoadingData(); // 取消加载动画

                        if (finalHasData) {
                            notifyDataSetChanged();
                        } else {
                            // TODO 显示空界面
                        }

                    }
                });

                if (loadFromDb) {
                    Log.i(TAG, "Updating database");
                    updateDatabase();
                    Log.i(TAG, "Finish updating");

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "updating dataSet");
                            updateDataSet();
                            Log.i(TAG, "notify dataSet updated");
                        }
                    });
                }

            }
        }).start();
    }

    /***********************************获取数据***************************************/

    /**
     * 从数据库读取数据，并设置为Adapter的数据集
     *
     * @return 是否有数据
     */
    protected abstract boolean readDataFromDB();

    /**
     * 直接从MediaStore读取数据，并设置为Adapter的数据集
     *
     * @return 是否有数据
     */
    protected abstract boolean readDataFromSys();

    /**
     * 更新数据库
     */
    protected abstract void updateDatabase();

    /***********************************************************************************/

}
