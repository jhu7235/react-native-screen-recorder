package com.screenrecorder;

import com.facebook.react.ReactActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.IOException;

public class MainActivity extends ReactActivity implements ActivityCompat.OnRequestPermissionsResultCallback  {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private String videoPath;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission granted");
                // Permission has been granted. Start camera preview Activity.
                _startRecording();
            } else {
                Log.d(TAG, "permission denied");
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecorderManager.updateActivity(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = null;

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            mMediaRecorder = null;
            mMediaProjection = null;
            return;
        }

        try {
            mMediaProjectionCallback = new MediaProjectionCallback();
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            mMediaProjection.registerCallback(mMediaProjectionCallback, null);
            mVirtualDisplay = createVirtualDisplay();
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(TAG, "******* verifying permission");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "******* no permission, asking");

            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }
        return true;
    }


    public void startRecording() {
        if(verifyStoragePermissions()) {
            _startRecording();
        }
    }

    private void _startRecording() {
        try {
            initRecorder();
            shareScreen();
            Log.d(TAG, "******* started Recording");
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder = null;
            mMediaProjection = null;
        }
    }

    public void stopRecording() {
        Log.d(TAG, "******* stopRecording");
        if (mMediaRecorder == null) {
            Log.d(TAG, "******* no media recorder");
            return;
        }
        try {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            stopScreenSharing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getVideoPath() {
        return videoPath;
    }

    private void shareScreen() {
        Log.d(TAG, "******* shareScreen");
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        Log.d(TAG, "******* on mMediaRecorder.start");
        mMediaRecorder.start();
        Log.d(TAG, "******* mMediaRecorder.started");
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {
            Log.d(TAG, "1");
            mMediaRecorder = new MediaRecorder();
            Log.d(TAG, "2");
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            Log.d(TAG, "3");
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            Log.d(TAG, "4");
            videoPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video.mp4";
            Log.d(TAG, "5");
            mMediaRecorder.setOutputFile(videoPath);
            Log.d(TAG, "6");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            Log.d(TAG, "7");
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            Log.d(TAG, "8");
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            Log.d(TAG, "9");
            mMediaRecorder.setVideoFrameRate(30);
            Log.d(TAG, "10");
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            Log.d(TAG, "11");
            mMediaRecorder.prepare();
            Log.d(TAG, "12");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            stopRecording();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mMediaRecorder.release();
        mMediaRecorder = null;
        destroyMediaProjection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "screenrecorder";
    }
}
