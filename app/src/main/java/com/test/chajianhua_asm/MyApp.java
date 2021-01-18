package com.test.chajianhua_asm;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        savePlugin(this, getExternalFilesDir("chajianapp").getAbsolutePath());
        load2Plugin(this, getExternalFilesDir("chajianapp").getAbsolutePath() + File.separator +
                "chajinaapp-debug.apk");


    }

    private void savePlugin(Context context, String pluginDir) {
        try {
            InputStream inputStream = getAssets().open("chajinaapp-debug.apk");
            System.out.println(inputStream);
            File file = new File(pluginDir, "chajinaapp-debug.apk");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteCount);
            }
            fileOutputStream.flush();
            inputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load1Plugin(Context context, String pluginPath) {
        try {
            // 获取自己的dexElements
            Class<?> classLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");

            Field pathListField = classLoaderClass.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathListObject = pathListField.get(context.getClassLoader());
            System.out.println(pathListObject);

            Field dexElementsField = pathListObject.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object[] dexElementsObject = (Object[]) dexElementsField.get(pathListObject);
            System.out.println(dexElementsObject);

            //获取dex中的dexElements
            File odex = context.getDir("odex", Context.MODE_PRIVATE);

            BaseDexClassLoader dexClassLoader = new BaseDexClassLoader(pluginPath, odex
                    , null, context.getClassLoader());
            System.out.println("dexClassLoader >>>>> " + dexClassLoader);
            System.out.println("dexClassLoader.getClass() >>>>> " + dexClassLoader.getClass());

            Field pluginPathListField = dexClassLoader.getClass().getDeclaredField("pathList");
            pluginPathListField.setAccessible(true);
            Object pluginPathListObject = pluginPathListField.get(dexClassLoader);
            System.out.println("插件的 pathList >>>" + pluginPathListObject);

            Field pluginDexElementsField = pluginPathListObject.getClass().getDeclaredField(
                    "dexElements");
            pluginDexElementsField.setAccessible(true);
            Object[] pluginDexElementsObject =
                    (Object[]) pluginDexElementsField.get(pluginPathListObject);
            System.out.println("插件的 dexElements >>>" + pluginDexElementsObject);

            Class<?> elementClazz = dexElementsObject.getClass().getComponentType();
            System.out.println(elementClazz);
            Object newDexElements = Array.newInstance(elementClazz,
                    pluginDexElementsObject.length + dexElementsObject.length);
            System.arraycopy(pluginDexElementsObject, 0, newDexElements, 0,
                    pluginDexElementsObject.length);
            System.arraycopy(dexElementsObject, 0, newDexElements, pluginDexElementsObject.length
                    , dexElementsObject.length);

            // 设置
            dexElementsField.set(pathListObject, newDexElements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load2Plugin(Context context, String dexPath) {
        System.out.println("开始");
        //判断dex是否存在
        File dex = new File(dexPath);
        if (!dex.exists()) {
            return;
        }

        try {
            //获取自己的dexElements
            PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();

            Field pathListField = getField(pathClassLoader.getClass(), "pathList");
            Object pathListObject = pathListField.get(pathClassLoader);

            Field dexElementsField = getField(pathListObject.getClass(), "dexElements");
            Object[] dexElementsObject = (Object[]) dexElementsField.get(pathListObject);

            //获取dex中的dexElements
            File odex = context.getDir("odex", Context.MODE_PRIVATE);
            DexClassLoader dexClassLoader = new DexClassLoader(dexPath, odex.getAbsolutePath(), null, pathClassLoader);

            Field pluginPathListField = getField(dexClassLoader.getClass(), "pathList");
            Object pluginPathListObject = pluginPathListField.get(dexClassLoader);

            Field pluginDexElementsField = getField(pluginPathListObject.getClass(), "dexElements");
            Object[] pluginDexElementsObject = (Object[]) pluginDexElementsField.get(pluginPathListObject);

            Class<?> elementClazz = dexElementsObject.getClass().getComponentType();
            Object newDexElements = Array.newInstance(elementClazz, pluginDexElementsObject.length + dexElementsObject.length);
            System.arraycopy(pluginDexElementsObject, 0, newDexElements, 0, pluginDexElementsObject.length);
            System.arraycopy(dexElementsObject, 0, newDexElements, pluginDexElementsObject.length, dexElementsObject.length);

            //设置
            dexElementsField.set(pathListObject, newDexElements);
            System.out.println("结束");
        } catch (Exception e) {
            System.out.println("出错了");
            e.printStackTrace();
        }
    }

    /**
     * 获取Field
     *
     * @param clazz
     * @param fieldName
     * @return
     */
    private Field getField(Class clazz, String fieldName) {
        Field field;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取Method
     *
     * @param clazz
     * @param methodName
     * @return
     */
    private Method getMethod(Class clazz, String methodName) {
        Method method;
        while (clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
