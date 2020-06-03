package com.highgreat.sven.hook.os;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import com.highgreat.sven.hook.LoginActivity;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;

public class HGActivityThread {

    private Context context;

    public HGActivityThread(Context context) {
        this.context = context;
    }

    public void mActivityThreadmHAction(Context mContext) throws Exception {
        context = mContext;
        if (AndroidSdkVersion.isAndroidOS_26_27_28()) {
            do_26_27_28_mHRestore();
        } else if (AndroidSdkVersion.isAndroidOS_21_22_23_24_25()) {
            do_21_22_23_24_25_mHRestore();
        } else {
            throw new IllegalStateException("实在是没有检测到这种系统，需要对这种系统单独处理...");
        }
    }

    /**
     * TODO 给 26_27_28 系统版本 做【还原操作】的
     */
    private final void do_26_27_28_mHRestore() throws Exception {
        Class mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Object mActivityThread = mActivityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Object mH = mHField.get(mActivityThread);
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        // 把系统中的Handler.Callback实现 替换成 我们自己写的Custom_26_27_28_Callback
        mCallbackField.set(mH, new Custom_26_27_28_Callback());
    }

    private class Custom_26_27_28_Callback implements Handler.Callback {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            if (Parameter.LAUNCH_ACTIVITY == msg.what) {
                try{
                    Object mClientTransaction = msg.obj;
                    Field mIntentField = mClientTransaction.getClass().getDeclaredField("intent");
                    mIntentField.setAccessible(true);
                    // 需要拿到真实的Intent
                    Intent proxyIntent = (Intent) mIntentField.get(mClientTransaction);
                    Intent targetIntent = proxyIntent.getParcelableExtra(Parameter.TARGET_INTENT);
                    if (targetIntent != null) {
                        //集中式登录
                        SharedPreferences share = context.getSharedPreferences("sven",
                                Context.MODE_PRIVATE);
                        if (share.getBoolean("login", false)) {
                            // 登录  还原  把原有的意图
                            proxyIntent.setComponent(targetIntent.getComponent());
                        } else {

                            ComponentName componentName = new ComponentName(context, LoginActivity.class);
                            proxyIntent.putExtra("extraIntent", targetIntent.getComponent().getClassName());
                            proxyIntent.setComponent(componentName);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            return false;
        }
    }


    /**
     * TODO 给 21_22_23_24_25 系统版本 做【还原操作】的
     */
    private void do_21_22_23_24_25_mHRestore() throws Exception {
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        Field msCurrentActivityThreadField = mActivityThreadClass.getDeclaredField("sCurrentActivityThread");
        msCurrentActivityThreadField.setAccessible(true);
        Object mActivityThread = msCurrentActivityThreadField.get(null);

        Field mHField = mActivityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mH = (Handler) mHField.get(mActivityThread);
        Field mCallbackFile = Handler.class.getDeclaredField("mCallback");
        mCallbackFile.setAccessible(true);

        mCallbackFile.set(mH, new Custom_21_22_23_24_25_Callback());
    }

    private class Custom_21_22_23_24_25_Callback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            if (Parameter.LAUNCH_ACTIVITY == msg.what) {
                Object mActivityClientRecord = msg.obj;
                try {
                    Field intentField = mActivityClientRecord.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);
                    Intent proxyIntent = (Intent) intentField.get(mActivityClientRecord);
                    // TODO 还原操作，要把之前的LoginActivity给换回来
                    Intent targetIntent = proxyIntent.getParcelableExtra(Parameter.TARGET_INTENT);
                    if (targetIntent != null) {
                        //集中式登录
                        SharedPreferences share = context.getSharedPreferences("sven",
                                Context.MODE_PRIVATE);
                        if (share.getBoolean("login", false)) {
                            // 登录  还原  把原有的意图    放到realyIntent
                            targetIntent.setComponent(targetIntent.getComponent());
                        } else {

                            String className = targetIntent.getComponent().getClassName();
                            ComponentName componentName = new ComponentName(context, LoginActivity.class);
                            targetIntent.putExtra("extraIntent", className);
                            targetIntent.setComponent(componentName);
                        }
                        // 反射的方式
                        intentField.set(mActivityClientRecord, targetIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }
}
