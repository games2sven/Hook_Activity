package com.highgreat.sven.hook;

import android.app.Application;

import com.highgreat.sven.hook.os.HGAMSCheckEngine;
import com.highgreat.sven.hook.os.HGActivityThread;

import java.lang.reflect.InvocationTargetException;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            HGAMSCheckEngine.mHookAMS(this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        try {
            HGActivityThread dnActivityThread = new HGActivityThread(this);
            dnActivityThread.mActivityThreadmHAction(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
