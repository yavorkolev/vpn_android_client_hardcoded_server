package com.free.vpn.utils;

import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import com.free.vpn.R;

public class Utils {


    public void taskBarChangeColor(Window window, int color){
        if (Build.VERSION.SDK_INT >= 21) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);
        }
    }
}
