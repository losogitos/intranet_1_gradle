package com.stxnext.management.android;

import android.app.Application;
import android.webkit.CookieManager;

public class AppIntranet extends Application{

    private static AppIntranet _instance;
    
    public static AppIntranet getApp(){
        return _instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _instance = this;
    }
}
