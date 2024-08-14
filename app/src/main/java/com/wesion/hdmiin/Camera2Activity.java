package com.wesion.hdmiin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Camera2Activity extends AppCompatActivity {
    private static final String TAG = Camera2Activity.class.getSimpleName();
    private Button mBtnStart;
    private Button mBtnStop;
    private Button mBtnSnapshotc;
    private TextureView mTextureView;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice.StateCallback mCameraDeviceStateCallback;
    private CameraCaptureSession.StateCallback mSessionStateCallback;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;
    private CaptureRequest.Builder mPreviewCaptureRequest;
    private CaptureRequest.Builder mRecorderCaptureRequest;
    private MediaRecorder mMediaRecorder;
    private String mCurrentSelectCamera;
    private Handler mChildHandler;
    private ImageView mIvShowSnap;
    private ImageReader mImageReader;
    private boolean isStartRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera2_activity);
        mTextureView = findViewById(R.id.textureview);
        mBtnStart = findViewById(R.id.btn_start);
        mBtnStop = findViewById(R.id.btn_finish);
        mBtnSnapshotc = findViewById(R.id.btn_snapshot);
        mIvShowSnap = (ImageView) findViewById(R.id.iv_showSnap);

        initClickListener();
        initChildHandler();
        initTextureViewStateListener();
        initMediaRecorder();
        initCameraDeviceStateCallback();
        initSessionStateCallback();
        initSessionCaptureCallback();
    }

    private void initClickListener(){
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecorder();
            }
        });
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecorder();
            }
        });

        mBtnSnapshotc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snapshot();
            }
        });
    }

    private void initTextureViewStateListener(){
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                initCameraManager();
                selectCamera();
                openCamera();
                initSnapImageReader();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    private String getNowDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("YYYYMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        String nowDate = formatter.format(curDate);
        return nowDate;
    }

    private void saveImage(byte[] bytes) {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), getNowDate() + ".jpg");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.close();
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), null);
            Log.e(TAG, "saveImage file" + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
    }

    private void initChildHandler(){
        HandlerThread handlerThread = new HandlerThread("Camera2API");
        handlerThread.start();
        mChildHandler = new Handler(handlerThread.getLooper());
    }

    private void initSnapImageReader() {
        Size cameraSize = getMatchingSize();
        mImageReader = ImageReader.newInstance(cameraSize.getWidth(), cameraSize.getHeight(), ImageFormat.JPEG,2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "onImageAvailable");
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap != null) {
                                mIvShowSnap.setImageBitmap(bitmap);
                            }
                        }
                    });
                    saveImage(bytes);
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    //reader.close();
                }
            }

        }, mChildHandler);
    }

    private void snapshot() {
        if (mCameraDevice == null) return;
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Log.d(TAG, "onCaptureCompleted");
                }
            };
            mCameraCaptureSession.capture(mCaptureRequest, CaptureCallback, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configMediaRecorder(){
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),getNowDate() + ".mp4");
        if (file.exists()){
            file.delete();
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setVideoEncodingBitRate(8*1024*1920);
        mMediaRecorder.setVideoFrameRate(30);
        Size size = getMatchingSize();
        mMediaRecorder.setVideoSize(size.getWidth(),size.getHeight());
        Surface surface = new Surface(mTextureView.getSurfaceTexture());
        mMediaRecorder.setPreviewDisplay(surface);
        mMediaRecorder.setOutputFile(file.getAbsolutePath());
        Log.e(TAG, "configMediaRecorder outputFile " + file.getAbsolutePath());
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void config(){
        try {
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        configMediaRecorder();
        Size cameraSize = getMatchingSize();
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(),cameraSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        Surface recorderSurface = mMediaRecorder.getSurface();

        try {
            mPreviewCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewCaptureRequest.addTarget(previewSurface);
            mPreviewCaptureRequest.addTarget(recorderSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,recorderSurface, mImageReader.getSurface()),mSessionStateCallback,mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startRecorder(){
        if (!isStartRecord) {
            config();
            mMediaRecorder.start();
            isStartRecord = true;
        }
    }

    private void stopRecorder(){
        if (isStartRecord) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            isStartRecord = false;

            try {
                if(mCameraDevice != null) {
                    Size cameraSize = getMatchingSize();
                    SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(), cameraSize.getHeight());
                    Surface previewSurface = new Surface(surfaceTexture);
                    mPreviewCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mPreviewCaptureRequest.addTarget(previewSurface);
                    mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), mSessionStateCallback, mChildHandler);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void initCameraManager(){
        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
    }

    private void selectCamera(){
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList.length == 0){
                Log.e(TAG, "selectCamera: cameraIdList length is 0");
                return;
            }

            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.d(TAG, "cameraId " + cameraId + " facing " + facing);
            }

            mCurrentSelectCamera = cameraIdList[cameraIdList.length - 1];
            Log.d(TAG, "mCurrentSelectCamera " + mCurrentSelectCamera);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initCameraDeviceStateCallback(){
        mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                try {
                    mCameraDevice = camera;
                    Size cameraSize = getMatchingSize();
                    SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(cameraSize.getWidth(),cameraSize.getHeight());
                    Surface previewSurface = new Surface(surfaceTexture);
                    mPreviewCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mPreviewCaptureRequest.addTarget(previewSurface);
                    mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),mSessionStateCallback,mChildHandler);//创建数据捕获会话，用于摄像头画面预览，这里需要等待mSessionStateCallback回调
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
            }
        };
    }

    private void initSessionStateCallback(){
        mSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                try {
                    mCameraCaptureSession.setRepeatingRequest(mPreviewCaptureRequest.build(), mSessionCaptureCallback, mChildHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }
        };
    }

    private void initSessionCaptureCallback(){
        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void openCamera(){
        try {
            mCameraManager.openCamera(mCurrentSelectCamera, mCameraDeviceStateCallback, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getMatchingSize(){
        Size selectSize = null;
        if(mCameraManager == null) {
            return null;
        }
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentSelectCamera);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            for (int i = 0; i < sizes.length; i++) {
                Size itemSize = sizes[i];
                Log.e(TAG,"itemSize Width = " + itemSize.getWidth() + " Height = "+itemSize.getHeight());
            }
            //select outputSize, default index 0.
            selectSize = sizes[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "selectSize Width = "+selectSize.getWidth());
        Log.e(TAG, "selectSize Height = "+selectSize.getHeight());
        return selectSize;
    }

}