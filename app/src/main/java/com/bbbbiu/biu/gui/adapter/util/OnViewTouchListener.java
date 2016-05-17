package com.bbbbiu.biu.gui.adapter.util;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

import com.bbbbiu.biu.R;

/**
 * Created by YieldNull at 5/16/16
 */
public class OnViewTouchListener implements View.OnTouchListener {
    private Context context;
    private static OnViewTouchListener sOnOptionTouchListener;

    public static OnViewTouchListener getSingleton(Context context) {
        if (sOnOptionTouchListener == null) {
            sOnOptionTouchListener = new OnViewTouchListener(context);
        }

        return sOnOptionTouchListener;
    }

    private OnViewTouchListener(Context context) {
        this.context = context;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(context.getResources().getColor(R.color.option_panel_pressed));
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(Color.WHITE);
        }
        return false;
    }
}
