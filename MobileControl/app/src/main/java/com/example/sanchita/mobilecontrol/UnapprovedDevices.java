package com.example.sanchita.mobilecontrol;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import interfaces.IAppManager;
import services.MsgService;
import types.DeviceInfo;


public class UnapprovedDevices extends ListActivity {

    private static final int APPROVE_SELECTED_DEVICE_ID = 0;
    private String[] deviceUsername;
    private static IAppManager msgService;
    String approvedDeviceNames = new String();
    String discardedDeviceNames = new String();
    Button buttonAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unapproved_devices);
        buttonAccept = (Button) findViewById(R.id.buttonAccept);

        Bundle extras = getIntent().getExtras();

        String names = extras.getString(DeviceInfo.DEVICE_LIST);

        deviceUsername = names.split(",");

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, deviceUsername));

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        NotificationManager NM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NM.cancel(deviceUsername.length);
        buttonAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int reqLength = getListAdapter().getCount();

                for (int i = 0; i < reqLength ; i++)
                {
                    if (getListView().isItemChecked(i)) {
                        approvedDeviceNames = approvedDeviceNames.concat(deviceUsername[i]).concat(",");
                    }
                    else {
                        discardedDeviceNames = discardedDeviceNames.concat(deviceUsername[i]).concat(",");
                    }
                }
                Thread thread4 = new Thread(){
                    @Override
                    public void run() {
                        if ( approvedDeviceNames.length() > 0 || discardedDeviceNames.length() > 0 )
                        {
                            msgService.sendDeviceRqResponse(approvedDeviceNames, discardedDeviceNames);
                        }
                        super.run();
                    }
                };
                thread4.start();

                Toast.makeText(UnapprovedDevices.this, "Your request has been sent", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, APPROVE_SELECTED_DEVICE_ID, 0, "Approve Selected and Discard Others");

        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        switch(item.getItemId())
        {
            case APPROVE_SELECTED_DEVICE_ID:
            {
                int reqLength = getListAdapter().getCount();

                for (int i = 0; i < reqLength ; i++)
                {
                    if (getListView().isItemChecked(i)) {
                        approvedDeviceNames = approvedDeviceNames.concat(deviceUsername[i]).concat(",");
                    }
                    else {
                        discardedDeviceNames = discardedDeviceNames.concat(deviceUsername[i]).concat(",");
                    }
                }
                Thread thread4 = new Thread(){
                    @Override
                    public void run() {
                        if ( approvedDeviceNames.length() > 0 || discardedDeviceNames.length() > 0 )
                        {
                            msgService.sendDeviceRqResponse(approvedDeviceNames, discardedDeviceNames);
                        }
                        super.run();
                    }
                };
                thread4.start();

                Toast.makeText(UnapprovedDevices.this, "Your request has been sent", Toast.LENGTH_LONG).show();
                finish();
                return true;
            }

        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onPause()
    {
        unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        bindService(new Intent(UnapprovedDevices.this, MsgService.class), mConnection , Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            msgService = ((MsgService.MsgBinder)service).getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            msgService = null;
            Toast.makeText(UnapprovedDevices.this, "App service stopped", Toast.LENGTH_SHORT).show();
        }
    };
}