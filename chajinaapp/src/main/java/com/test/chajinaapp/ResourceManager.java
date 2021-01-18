package com.test.chajinaapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.lang.reflect.Method;

public class ResourceManager {
    private static final String PATH = "/storage/emulated/0/Android/data/com.test.chajianhua_asm/files/chajianapp/chajinaapp-debug.apk";

    private static Resources mResources;

    public static Resources getResources(Context context) {
        if (mResources == null) {
            mResources = loadResource(context);
        }
        return mResources;
    }

    private static Resources loadResource(Context context) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPathField = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathField.setAccessible(true);
            addAssetPathField.invoke(assetManager, PATH);
            Resources resources = context.getResources();
            return new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
