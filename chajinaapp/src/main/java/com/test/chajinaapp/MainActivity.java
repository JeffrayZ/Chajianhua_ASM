package com.test.chajinaapp;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

//    @Override
//    public Resources getResources() {
//        Resources resources = ResourceManager.getResources(getApplicationContext());
//        return resources == null ? super.getResources() : resources;
//    }
}