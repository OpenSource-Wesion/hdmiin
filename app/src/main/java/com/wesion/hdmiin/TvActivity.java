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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class TvActivity extends AppCompatActivity {

    private static final String TAG = TvActivity.class.getSimpleName();
    private static final String INPUT_ID = "com.droidlogic.tvinput/.services.Hdmi2InputService/HW6";
    private TvInputManager mTvInputManager;
    private TvView mTvView;
    private Button mBtnFullScreen;
    public static final String SIG_INFO_ARGS  = "sig_info_args";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity);

        mBtnFullScreen = (Button)findViewById(R.id.btn_fullScreen);
        mBtnFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent intent = new Intent(TvInputManager.ACTION_SETUP_INPUTS);
                intent.putExtra("from_tv_source", true);
                intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, INPUT_ID);
                startActivity(intent);
                */
                Intent intent = new Intent(TvActivity.this, TvFullScreenActivity.class);
                intent.putExtra("input_id", INPUT_ID);
                startActivity(intent);
            }
        });

        mTvView = (TvView) findViewById(R.id.source_view);
        mTvView.setCallback(new TvViewInputCallback());
        mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);

        showAllInputInfo();
        playHdmi(INPUT_ID);
    }

    public class TvViewInputCallback extends TvView.TvInputCallback {
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Log.d(TAG, "onEvent inputId " + inputId + " eventType " + eventType + " eventArgs " + eventArgs.getString(SIG_INFO_ARGS));
            Toast.makeText(TvActivity.this, eventArgs.getString(SIG_INFO_ARGS), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onVideoAvailable(String inputId) {
            Log.d(TAG, "onVideoAvailable inputId = " + inputId);
        }

        @Override
        public void onConnectionFailed(String inputId) {
            Log.d(TAG, "onConnectionFailed inputId = " + inputId);
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Log.d(TAG, "onVideoUnavailable inputId = " + inputId + " reason " + reason);
        }

        @Override
        public void onContentBlocked(String inputId, TvContentRating rating) {
            Log.d(TAG, "onContentBlocked " + inputId + " rating " + rating);
        }

        @Override
        public void onContentAllowed(String inputId) {
            Log.d(TAG, "onContentAllowed " + inputId);
        }

        @Override
        public void onTracksChanged(String inputId, List<TvTrackInfo> tracks) {
            Log.d(TAG, "onTracksChanged inputId = " + inputId);
        }

        @Override
        public void onTrackSelected(String inputId, int type, String trackId) {
            Log.d(TAG, "onTrackSelected inputId = " + inputId + " type = " + type + " trackId = " + trackId);
        }
    }

    public static Uri buildChannelUriForPassthroughInput(String inputId) {
        return TvContract.buildChannelUriForPassthroughInput(inputId);
    }

    private void playHdmi(String inputId) {
        mTvView.reset();
        Uri uri = buildChannelUriForPassthroughInput(inputId);
        mTvView.tune(inputId, uri);
    }

    private void showAllInputInfo() {
        TvInputManager tvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tvInputManager == null) {
            Log.d(TAG, "tvInputManager is null");
            return;
        }
        tvInputManager.getTvInputList();
        List<TvInputInfo> list = tvInputManager.getTvInputList();
        for (TvInputInfo info : list) {
            Log.d(TAG, "tv id:" + info.getId());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}