package com.test.chajianhua_asm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hookAMS();
        hookHandler();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.test.chajinaapp", "com.test.chajinaapp.MainActivity"));
//                intent.setComponent(new ComponentName("com.wangyz.plugin", "com.wangyz.plugin.MainActivity"));
                startActivity(intent);
            }
        });
    }

    private void hookHandler() {
        try {
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField(
                    "sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object sCurrentActivityThreadObj = sCurrentActivityThreadField.get(null);

            Field mHField = activityThreadClazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(sCurrentActivityThreadObj);
            Field callBackField = Handler.class.getDeclaredField("mCallback");
            callBackField.setAccessible(true);
            callBackField.set(mH, new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    switch (msg.what) {
                        case 159:
                            // EXECUTE_TRANSACTION android9.0及以上
                            Log.i("AMSHookUtil", "AMSHookInvocationHandler:" + msg);
                            try {
                                Object obj = msg.obj;
                                Field mActivityCallbacksField = obj.getClass().getDeclaredField("mActivityCallbacks");
                                mActivityCallbacksField.setAccessible(true);
                                List mActivityCallbacks = (List) mActivityCallbacksField.get(msg.obj);
                                for (Object mActivityCallback : mActivityCallbacks) {
                                    if (mActivityCallback.getClass().getName().equals("android.app.servertransaction.LaunchActivityItem")) {
                                        Log.e("AMSHookUtil", "handleLaunchActivity:" + mActivityCallback);
                                        Field mIntentField = mActivityCallback.getClass().getDeclaredField("mIntent");
                                        mIntentField.setAccessible(true);
                                        Intent intent = (Intent) mIntentField.get(mActivityCallback);
                                        // 获取插件的
                                        Intent proxyIntent = intent.getParcelableExtra("ORIGINALLY_INTENT");
                                        //替换
                                        if (proxyIntent != null) {
                                            Log.e("AMSHookUtil", "handleLaunchActivity:" + proxyIntent);
                                            mIntentField.set(mActivityCallback, proxyIntent);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 100: {
                            // LAUNCH_ACTIVITY android9.0以下
                            try {
                                Field intentField = msg.obj.getClass().getDeclaredField("intent");
                                Intent proxyIntent = (Intent) intentField.get(msg.obj);
                                Intent targetIntent = proxyIntent.getParcelableExtra("ORIGINALLY_INTENT");
                                if (targetIntent != null) {
//                                    proxyIntent.setComponent(targetIntent.getComponent());
                                    intentField.set(msg.obj, targetIntent);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                        default:
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookAMS() {
        try {
            Class<?> atmClass = Class.forName("android.app.ActivityTaskManager");
            Field iatmSingleton = atmClass.getDeclaredField("IActivityTaskManagerSingleton");
            iatmSingleton.setAccessible(true);
            Object value = iatmSingleton.get(null);
            System.out.println(value);

            Class<?> singletonClz = Class.forName("android.util.Singleton");
            Field instanceField = singletonClz.getDeclaredField("mInstance");
            instanceField.setAccessible(true);
            Object iActivityManagerObject = instanceField.get(value);


            Class<?> iActivity = Class.forName("android.app.IActivityTaskManager");
            Object proxyObj = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{iActivity},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("startActivity".equals(method.getName())) {
                                System.out.println("ready to startActivity");
                                for (Object object : args) {
                                    System.out.println("invoke: object=" + object);
                                }

                                int index = -1;
                                //  找到我们启动时的intent
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        index = i;
                                        break;
                                    }
                                }
                                if (index >= 0) {
                                    // 取出在真实的Intent
                                    Intent originallyIntent = (Intent) args[index];
                                    Log.i("AMSHookUtil", "AMSHookInvocationHandler:" + originallyIntent.getComponent().getClassName());
                                    // 自己伪造一个配置文件已注册过的Activity Intent
                                    Intent proxyIntent = new Intent();
                                    //  因为我们调用的Activity没有注册，所以这里我们先偷偷换成已注册。使用一个假的Intent
                                    ComponentName componentName = new ComponentName(getPackageName(), "com.test.chajianhua_asm.MainActivity2");
                                    proxyIntent.setComponent(componentName);
                                    // 在这里把未注册的Intent先存起来 一会儿我们需要在Handle里取出来用
                                    proxyIntent.putExtra("ORIGINALLY_INTENT", originallyIntent);
                                    args[index] = proxyIntent;
                                }
                            }
                            return method.invoke(iActivityManagerObject, args);
                        }
                    }
            );
            instanceField.set(value, proxyObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}