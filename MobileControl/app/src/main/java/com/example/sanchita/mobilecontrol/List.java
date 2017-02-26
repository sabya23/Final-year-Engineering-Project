package com.example.sanchita.mobilecontrol;

import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import interfaces.IAppManager;
import services.MsgService;
import tools.DeviceController;
import types.DeviceInfo;
import types.STATUS;


public class List extends ListActivity {

    private Button buttonAddDevice;
    private static final int ADD_NEW_FRIEND_ID = Menu.FIRST;
    private static final int EXIT_APP_ID = Menu.FIRST + 1;

    private IAppManager appManager = null;
    private DeviceListAdapter deviceListAdapter;
    public String ownUsername;

    private class DeviceListAdapter extends BaseAdapter {

        class ViewHolder {
            TextView text;
            ImageView icon;
        }

        private LayoutInflater layoutInflater;
        private Bitmap onlineIcon;
        private Bitmap offlineIcon;

        private DeviceInfo[] devices = null;

        public DeviceListAdapter(Context context) {
            super();
            layoutInflater = LayoutInflater.from(context);

            onlineIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.greenstar);
            offlineIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.redstar);
        }

        public void setDeviceList(DeviceInfo[] deviceList) {
            this.devices = deviceList;
        }

        public int getCount() {
            return devices.length;
        }

        public DeviceInfo getItem(int position) {
            return devices[position];
        }

        public long getItemId(int position) {return 0;}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if(convertView == null) {
                convertView = layoutInflater.inflate(R.layout.device_list, null);

                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) convertView.findViewById(R.id.text);
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);

                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.text.setText(devices[position].username);
            viewHolder.icon.setImageBitmap(devices[position].status == STATUS.ONLINE ? onlineIcon : offlineIcon);

            return convertView;
        }
    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            if(extra != null) {
                String action = intent.getAction();
                if(action.equals(MsgService.DEVICE_LIST_UPDATED)) {
                    List.this.updateData(DeviceController.getDevices(), DeviceController.getUnapprovedDevices());
                }
            }
        }
    }

    public MessageReceiver messageReceiver = new MessageReceiver();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            appManager = ((MsgService.MsgBinder) service).getService();

            DeviceInfo[] device = DeviceController.getDevices();

            if(device != null) {
                List.this.updateData(device, null);
            }

            setTitle(appManager.getUserName() + "'s device list");
            ownUsername = appManager.getUserName();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            appManager = null;
            Toast.makeText(List.this, "App service stopped", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        buttonAddDevice = (Button) findViewById(R.id.buttonAddDevice);

        deviceListAdapter = new DeviceListAdapter(this);

        buttonAddDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(List.this, AddNewDevice.class);
                startActivity(i);
            }
        });

    }

    public void updateData(DeviceInfo[] deviceInfo, DeviceInfo[] unapprovedDevices) {

        if(deviceInfo != null) {
            deviceListAdapter.setDeviceList(deviceInfo);
            setListAdapter(deviceListAdapter);
        }

        if(unapprovedDevices != null) {

            NotificationManager nM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if(unapprovedDevices.length > 0) {
                String tmp = new String();
                for(int j = 0; j < unapprovedDevices.length; j++) {
                    tmp = tmp.concat(unapprovedDevices[j].username).concat(",");
                }
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.stat_sample).setContentTitle("New device request");
                Intent i = new Intent(this, UnapprovedDevices.class);
                i.putExtra(DeviceInfo.DEVICE_LIST, tmp);
                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);

                mBuilder.setContentText("You have new device request(s)");
                mBuilder.setContentIntent(contentIntent);
                nM.notify(R.string.new_friend_request_exist, mBuilder.build());

            }
            else {
                nM.cancel(R.string.new_friend_request_exist);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent i =  new Intent(this, CommandLog.class);
        DeviceInfo device = deviceListAdapter.getItem(position);
        i.putExtra(DeviceInfo.USERNAME, device.username);
        i.putExtra(DeviceInfo.PORT, device.port);
        i.putExtra(DeviceInfo.IP, device.ip);
        startActivity(i);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(messageReceiver);
        unbindService(serviceConnection);
        super.onPause();
    }

    @Override
    protected void onResume() {
        bindService(new Intent(List.this, MsgService.class), serviceConnection , Context.BIND_AUTO_CREATE);

        IntentFilter i = new IntentFilter();
        i.addAction(MsgService.DEVICE_LIST_UPDATED);
        registerReceiver(messageReceiver, i);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, ADD_NEW_FRIEND_ID, 0, "Add New Device");

        menu.add(0, EXIT_APP_ID, 0, "Exit");

        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {

        switch(item.getItemId())
        {
            case ADD_NEW_FRIEND_ID:
            {
                Intent i = new Intent(List.this, AddNewDevice.class);
                startActivity(i);
                return true;
            }
            case EXIT_APP_ID:
            {
                appManager.exit();
                finish();
                return true;
            }
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}