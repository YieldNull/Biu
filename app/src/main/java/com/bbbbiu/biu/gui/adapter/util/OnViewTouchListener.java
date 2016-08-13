package com.bbbbiu.biu.gui.adapter.util;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import com.bbbbiu.biu.R;

/**
 * Created by YieldNull at 5/16/16
 */
public class OnViewTouchListener implements View.OnTouchListener {
    private Context context;
    private int pressedResId;


    public OnViewTouchListener(Context context) {
        this(context, R.color.option_panel_pressed);
    }

    public OnViewTouchListener(Context context, int pressedResId) {
        this.context = context;
        this.pressedResId = pressedResId;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            v.setBackgroundColor(context.getResources().getColor(pressedResId));
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            v.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }
        return false;
    }
}
