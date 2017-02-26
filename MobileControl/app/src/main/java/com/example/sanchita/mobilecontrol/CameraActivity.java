package com.example.sanchita.mobilecontrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import tools.MultipartEntity;


public class CameraActivity extends ActionBarActivity {
    private static final String TAG = "CamTestActivity";
    Preview preview;
    Camera camera;
    Activity act;
    Context ctx;
    private File outFile;
    String fileName;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        act = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.camera_preview)).addView(preview);
        preview.setKeepScreenOn(true);
        new CountDownTimer(5000,1000) {
            @Override
            public void onFinish() {
                try {
                    camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                } catch (RuntimeException ex){
                    Log.e(TAG, ex.getMessage());
                }
            }
            @Override
            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(getFrontCameraId());
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Log.e(TAG, ex.getMessage());
            }
        }
    }
    private int getFrontCameraId(){
        int camId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo ci = new Camera.CameraInfo();

        for(int i = 0;i < numberOfCameras;i++){
            Camera.getCameraInfo(i,ci);
            if(ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                camId = i;
            }
            else {
                camId = 0;
            }
        }
        return camId;
    }
    @Override
    protected void onPause() {
        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        super.onPause();
    }
    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
// Log.d(TAG, "onShutter'd");
        }
    };
    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
// Log.d(TAG, "onPictureTaken - raw");
        }
    };
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if(data != null) {
                Bitmap oldBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                if(oldBmp != null) {
                    ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                    oldBmp.compress(Bitmap.CompressFormat.JPEG, 100, baoStream);
                    byte[] bytesImage = baoStream.toByteArray();
                    new SaveImageTask().execute(bytesImage);
                    resetCam();
                    try {
                        sendPhoto(oldBmp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    new CountDownTimer(5000,1000){
                        @Override
                        public void onFinish() {
                            releaseCamera();
                        }
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }
                    }.start();
                }
                else {
                    Log.d(TAG, "Decoding failed");
                }
            } else {
                Log.d(TAG, "Picture empty");
            }
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };
    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;
// Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();
                fileName = String.format("%d.jpg", System.currentTimeMillis());
                outFile = new File(dir, fileName);
                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();
                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }
    }
    public void releaseCamera(){
        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        if(fileName == null) {
            Log.d(TAG, "fileName is null");
        }
        Intent i = new Intent();
        i.putExtra("outFile", outFile);
        i.putExtra("fileName", fileName);
        setResult(RESULT_OK, i);
        finish();
    }

    private void sendPhoto(Bitmap bitmap) throws Exception {
        new UploadTask().execute(bitmap);
    }
    private class UploadTask extends AsyncTask<Bitmap, Void, Void> {
        protected Void doInBackground(Bitmap... bitmaps) {
            if (bitmaps[0] == null)
                return null;
            setProgress(0);
            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream); // convert Bitmap to ByteArrayOutputStream
            InputStream in = new ByteArrayInputStream(stream.toByteArray()); // convert ByteArrayOutputStream to ByteArrayInputStream
            DefaultHttpClient httpclient = new DefaultHttpClient();
            try {
                HttpPost httppost = new HttpPost(
                        "http://mymobilecontrol.orgfree.com/savetofile.php"); // server
                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("myFile",fileName, in);
                httppost.setEntity(reqEntity);
                Log.i(TAG, "request " + httppost.getRequestLine());
                HttpResponse response = null;
                try {
                    response = httpclient.execute(httppost);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (response != null)
                        Log.i(TAG, "response " + response.getStatusLine().toString());
                } catch (Exception e) {Log.e(TAG, e.getMessage());} finally {
                }
            } catch (Exception e) {Log.e(TAG, e.getMessage());} finally {
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(CameraActivity.this, "Photo uploaded", Toast.LENGTH_LONG).show();
            super.onPostExecute(result);
        }
    }
}