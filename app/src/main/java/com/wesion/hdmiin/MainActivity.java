package com.wesion.hdmiin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button btnCamera2API;
    private Button btnTvAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        btnCamera2API = (Button) findViewById(R.id.btn_camera2API);
        btnCamera2API.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Camera2Activity.class);
                startActivity(intent);
            }
        });

        btnTvAPI = (Button) findViewById(R.id.btn_TvAPI);
        btnTvAPI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TvActivity.class);
                startActivity(intent);
            }
        });
    }

}