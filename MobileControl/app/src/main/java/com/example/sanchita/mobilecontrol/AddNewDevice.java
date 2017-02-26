package com.example.sanchita.mobilecontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import interfaces.IAppManager;
import services.MsgService;


public class AddNewDevice extends Activity implements View.OnClickListener {

    private EditText editDevice;
    private Button dAdd;
    private Button dCancel;

    private static IAppManager appManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_device);

        editDevice = (EditText) findViewById(R.id.editDevice);
        dAdd = (Button) findViewById(R.id.buttonAdd);
        dCancel = (Button) findViewById(R.id.buttonDCancel);

        if (dAdd != null) {
            dAdd.setOnClickListener(this);
        } else {
            throw new NullPointerException("onCreate: mAddFriendButton is null");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, MsgService.class);
        if (mConnection != null) {
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == dCancel) {
            finish();
        } else if (view == dAdd) {
            addNewDevice();
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            appManager = ((MsgService.MsgBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            if (appManager != null) {
                appManager = null;
            }

            Toast.makeText(AddNewDevice.this, "App service stopped", Toast.LENGTH_LONG).show();
        }
    };

    private void addNewDevice() {
        if (editDevice.length() > 0) {
            Thread thread1 = new Thread() {
                @Override
                public void run() {
                    appManager.addNewDeviceRequest(editDevice.getText().toString());
                    super.run();
                }
            };
            thread1.start();
            Toast.makeText(AddNewDevice.this, "Your request has been sent", Toast.LENGTH_LONG).show();

            finish();
        } else {
            Toast.makeText(AddNewDevice.this, "Type Device UserName", Toast.LENGTH_LONG).show();
        }
    }
}