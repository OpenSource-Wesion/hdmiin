package com.wesion.hdmiin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvTrackInfo;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.util.List;

public class TvFullScreenActivity extends Activity {
    private static final String TAG = TvFullScreenActivity.class.getSimpleName();
    private static String INPUT_ID = "com.droidlogic.tvinput/.services.Hdmi2InputService/HW6";
    private TvView mTvView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.tv_full_screen_activity);
        mTvView = (TvView) findViewById(R.id.main_tunable_tv_view);
        Intent intent = getIntent();
        String id = intent.getStringExtra("input_id");
        if (!TextUtils.isEmpty(id)) {
            INPUT_ID = id;
        }
        Log.d(TAG, "INPUT_ID " + id);
        playHdmi(INPUT_ID);
    }

    public static Uri buildChannelUriForPassthroughInput(String inputId) {
        return TvContract.buildChannelUriForPassthroughInput(inputId);
    }

    private void playHdmi(String inputId) {
        mTvView.reset();
        Uri uri = buildChannelUriForPassthroughInput(inputId);
        mTvView.tune(inputId, uri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}