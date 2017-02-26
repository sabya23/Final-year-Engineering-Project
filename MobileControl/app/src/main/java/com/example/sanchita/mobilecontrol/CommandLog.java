package com.example.sanchita.mobilecontrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import interfaces.IAppManager;
import services.MsgService;
import tools.DeviceController;
import tools.HardwareControl;
import tools.LocalStorageHandler;
import types.DeviceInfo;
import types.MessageInfo;


public class CommandLog extends Activity {

    private EditText editLogHistory;
    private EditText editLog;
    private Button send;
    private Button takePicture;
    private final static int CAMERA_PIC_REQUEST1 = 0;

    public String username;
    private IAppManager appManager;
    private DeviceInfo deviceInfo = new DeviceInfo();
    private LocalStorageHandler localstoragehandler;
    private Cursor dbCursor;
    private HardwareControl hardwareControl = null;
    private File picFile;
    private String fileName;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            appManager = ((MsgService.MsgBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            appManager = null;
            Toast.makeText(CommandLog.this, "App service stopped", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_log);

        editLogHistory = (EditText) findViewById(R.id.txtLogHistory);
        editLog = (EditText) findViewById(R.id.editLog);
        send = (Button) findViewById(R.id.buttonSend);
        takePicture = (Button) findViewById(R.id.buttonPicture);
        editLog.requestFocus();

        Bundle extras = this.getIntent().getExtras();

        deviceInfo.username = extras.getString(DeviceInfo.USERNAME);
        deviceInfo.ip = extras.getString(DeviceInfo.IP);
        deviceInfo.port = extras.getString(DeviceInfo.PORT);
        String msg = extras.getString(MessageInfo.MESSAGE_TEXT);
        hardwareControl = new HardwareControl(this);

        setTitle("Messaging with " + deviceInfo.username);

        localstoragehandler = new LocalStorageHandler(this);
        dbCursor = localstoragehandler.get(deviceInfo.username, MsgService.USERNAME );

        if (dbCursor.getCount() > 0){
            int noOfScorer = 0;
            dbCursor.moveToFirst();
            while ((!dbCursor.isAfterLast())&&noOfScorer<dbCursor.getCount())
            {
                noOfScorer++;
                this.appendToMessageHistory(dbCursor.getString(2) , dbCursor.getString(3));
                dbCursor.moveToNext();
            }
        }
        localstoragehandler.close();

        if (msg != null) {
            this.appendToMessageHistory(deviceInfo.username, msg);
        }

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(send.getText().toString().equalsIgnoreCase("Turn Flashlight On")) {
                    editLog.setText("TurnFlashOn");
                    send.setText("Turn Flashlight Off");
                }
                else {
                    editLog.setText("TurnFlashOff");
                    send.setText("Turn Flashlight On");
                }
                sendMsg();
            }
        });
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLog.setText("TakePicture");
                sendMsg();
            }
        });
    }

    private void sendMsg() {
        final CharSequence message;
        final Handler handler = new Handler();
        message = editLog.getText();
        if (message.length()>0)
        {
            appendToMessageHistory(appManager.getUserName(), message.toString());

            localstoragehandler.insert(appManager.getUserName(), deviceInfo.username, message.toString());

            editLog.setText("");
            Thread thread2 = new Thread(){
                public void run() {
                    try {
                        if (appManager.sendMessage(appManager.getUserName(), deviceInfo.username, message.toString()) == null)
                        {
                            handler.post(new Runnable(){
                                public void run() {
                                    Toast.makeText(getApplicationContext(),"Message cannot be sent", Toast.LENGTH_LONG).show();
                                }

                            });
                        }
                    } catch (UnsupportedEncodingException e) {
                        Toast.makeText(getApplicationContext(),"Message cannot be sent", Toast.LENGTH_LONG).show();

                        e.printStackTrace();
                    }
                    super.run();
                }
            };
            thread2.start();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(messageReceiver);
        unbindService(mConnection);
        DeviceController.setActiveDevice(null);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        bindService(new Intent(CommandLog.this, MsgService.class), mConnection , Context.BIND_AUTO_CREATE);
        IntentFilter i = new IntentFilter();
        i.addAction(MsgService.TAKE_MESSAGE);
        registerReceiver(messageReceiver, i);
        DeviceController.setActiveDevice(deviceInfo.username);
        super.onResume();
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Bundle extra = intent.getExtras();
            if(action.equals(MsgService.MESSAGE_LIST_UPDATED) || action.equals(MsgService.TAKE_MESSAGE)) {

                String username = extra.getString(MessageInfo.USER_ID);
                String message = extra.getString(MessageInfo.MESSAGE_TEXT);

                if (username != null && message != null) {
                    if (deviceInfo.username.equals(username)) {
                        appendToMessageHistory(username, message);
                        if (message.contains("TurnFlashOn")) {
                            hardwareControl.turnFlashOn();
                        } else if (message.contains("TurnFlashOff")) {
                            hardwareControl.turnFlashOff();
                        } else if (message.contains("TakePicture")) {
                            Intent cameraIntent = new Intent();
                            cameraIntent.setClass(CommandLog.this, CameraActivity.class);
                            startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST1);
                        } else if (message.contains(".jpg")) {
                            new BitmapGet().execute("http://mymobilecontrol.orgfree.com/uploads/" + message);
                        }
                        localstoragehandler.insert(username, appManager.getUserName(), message);
                    } else {
                        if (message.length() > 15) {
                            message = message.substring(0, 15);
                        }
                        Toast.makeText(CommandLog.this, username + " says '" + message + "'", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private MessageReceiver messageReceiver = new MessageReceiver();

    public  void appendToMessageHistory(String username, String message) {
        if (username != null && message != null) {
            editLogHistory.append(username + ":\n");
            editLogHistory.append(message + "\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localstoragehandler != null) {
            localstoragehandler.close();
        }
        if (dbCursor != null) {
            dbCursor.close();
        }
        hardwareControl.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_command_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_PIC_REQUEST1) {
            if(resultCode == RESULT_OK) {
                fileName = data.getStringExtra("fileName");
                editLog.setText(fileName);
                sendMsg();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class BitmapGet extends AsyncTask<String, Void, Void> {
        byte[] bytesImage;
        @Override
        protected Void doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                if (myBitmap != null) {
                    ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baoStream);
                    bytesImage = baoStream.toByteArray();
                }
                input.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("getBmpFromUrl error: ", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(bytesImage != null) {
                new SaveImageTask().execute(bytesImage);
            }
            super.onPostExecute(aVoid);
        }
    }

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest2");
                dir.mkdirs();
                String fileName1 = String.format("%d.jpg", System.currentTimeMillis());
                File outFile1 = new File(dir, fileName1);
                outStream = new FileOutputStream(outFile1);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();
                refreshGallery(outFile1);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(CommandLog.this, "Photo saved to gallery", Toast.LENGTH_LONG).show();
            super.onPostExecute(aVoid);
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }
}