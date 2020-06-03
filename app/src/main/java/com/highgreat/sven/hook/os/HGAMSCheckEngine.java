package com.highgreat.sven.hook.os;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.highgreat.sven.hook.ProxyActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 用来绕过ActivityManagerService的检测，
 * hook AMS
 */
public class HGAMSCheckEngine {

    public static void mHookAMS(final Context mContext) throws ClassNotFoundException,
            NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        // 公共区域
        Object mIActivityManagerSingleton = null; // TODO 公共区域 适用于 21以下的版本 以及 21_22_23_24_25  26_27_28 等系统版本
        Object mIActivityManager = null; // TODO 公共区域 适用于 21以下的版本 以及 21_22_23_24_25  26_27_28 等系统版本

        if (AndroidSdkVersion.isAndroidOS_26_27_28()) {
            // 获取系统的 IActivityManager.aidl
            Class mActivityManagerClass = Class.forName("android.app.ActivityManager");
            mIActivityManager = mActivityManagerClass.getMethod("getService").invoke(null);//静态方法可省略对象，直接用null替代

            // 获取IActivityManagerSingleton
            Field mIActivityManagerSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
            mIActivityManagerSingletonField.setAccessible(true);
            mIActivityManagerSingleton = mIActivityManagerSingletonField.get(null);
        }else if(AndroidSdkVersion.isAndroidOS_21_22_23_24_25()){
            Class mActivityManagerClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = mActivityManagerClass.getDeclaredMethod("getDefault");
            getDefaultMethod.setAccessible(true);
            mIActivityManager = getDefaultMethod.invoke(null);

            //gDefault
            Field gDefaultField = mActivityManagerClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            mIActivityManagerSingleton = gDefaultField.get(null);
        }

        //获取动态代理
        Class mIActivityManagerClass = Class.forName("android.app.IActivityManager");
        final Object finalMIActivityManager = mIActivityManager;
        Object mIActivityManagerProxy = Proxy.newProxyInstance(mContext.getClassLoader(), new Class[]{mIActivityManagerClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("startActivity".equals(method.getName())) {
                            // TODO 把不能经过检测的LoginActivity 替换 成能够经过检测的ProxyActivity
                            Intent proxyIntent = new Intent(mContext, ProxyActivity.class);
                            // 把目标的LoginActivity 取出来 携带过去
                            Intent target = (Intent) args[2];
                            if(target != null){
                                for (int i = 0; i < args.length; i++) {
                                    if(null != args[i] ){
                                        Log.e("hook","args:"+args[i].getClass().getName() + " i =" +i);
                                    }
                                }
                            }
                            proxyIntent.putExtra(Parameter.TARGET_INTENT, target);
                            args[2] = proxyIntent;
                        }
                        return method.invoke(finalMIActivityManager,args);
                    }
                });
        if (mIActivityManagerSingleton == null || mIActivityManagerProxy == null) {
            throw new IllegalStateException("实在是没有检测到这种系统，需要对这种系统单独处理...");
        }

        Class mSingletonClass = Class.forName("android.util.Singleton");

        Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);

        // 把系统里面的 IActivityManager 换成 我们自己写的动态代理
        mInstanceField.set(mIActivityManagerSingleton, mIActivityManagerProxy);

    }
}
