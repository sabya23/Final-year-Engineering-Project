package services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.sanchita.mobilecontrol.CommandLog;
import com.example.sanchita.mobilecontrol.MobileControl;
import com.example.sanchita.mobilecontrol.R;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import communication.SocketOperator;
import interfaces.IAppManager;
import interfaces.ISocketOperator;
import interfaces.IUpdateData;
import tools.DeviceController;
import tools.LocalStorageHandler;
import tools.MessageController;
import tools.XMLHandler;
import types.DeviceInfo;
import types.MessageInfo;

public class MsgService extends Service implements IAppManager, IUpdateData {

    public static String USERNAME;
    public static final String TAKE_MESSAGE = "Take_Message";
    public static final String DEVICE_LIST_UPDATED = "Take Device List";
    public static final String MESSAGE_LIST_UPDATED = "Take Message List";
    public ConnectivityManager connectivityManager = null;
    private final int UPDATE_TIME_PERIOD = 15000;
    private String rawDeviceList = new String();
    private String rawMessageList = new String();

    ISocketOperator socketOperator = new SocketOperator(this);

    private final IBinder binder = new MsgBinder();

    private String username;
    private String password;
    private boolean authenticatedUser = false;
    private Timer timer;

    private LocalStorageHandler localStorageHandler;

    private NotificationManager nM;

    public MsgService(){}

    public class MsgBinder extends Binder {
        public IAppManager getService() {
            return MsgService.this;
        }
    }

    @Override
    public void onCreate() {
        nM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        localStorageHandler = new LocalStorageHandler(this);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        timer = new Timer();

        Thread thread5 = new Thread() {
            @Override
            public void run() {
                Random random = new Random();
                int tryCount = 0;
                while(socketOperator.startListening(10000 + random.nextInt(20000)) != 1) {
                    tryCount++;
                    if(tryCount > 10) {
                        break;
                    }
                }
            }
        };
        thread5.start();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void showNotification(String username, String msg)
    {
        String title = "You got a new Message! (" + username + ")";

        String text = username + ": " + ((msg.length() < 5) ? msg : msg.substring(0, 5)+ "...");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)
                .setContentTitle(title)
                .setContentText(text);

        Intent i = new Intent(this, CommandLog.class);
        i.putExtra(DeviceInfo.USERNAME, username);
        i.putExtra(MessageInfo.MESSAGE_TEXT, msg);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setContentText("New message from " + username + ": " + msg);

        nM.notify(R.string.new_message, mBuilder.build());
    }

    @Override
    public String getUserName() {
        return this.username;
    }

    @Override
    public String sendMessage(String username, String toUsername, String message) throws UnsupportedEncodingException {
        String params = "username=" + URLEncoder.encode(this.username, "UTF-8") + "&password=" + URLEncoder.encode(this.password, "UTF-8") + "&to=" + URLEncoder.encode(toUsername, "UTF-8") + "&message=" + URLEncoder.encode(message, "UTF-8") + "&action=" + URLEncoder.encode("sendMessage", "UTF-8") + "&";
        return socketOperator.sendHttpRequest(params);
    }

    private String getDeviceList() throws UnsupportedEncodingException {
        rawDeviceList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));

        if (rawDeviceList != null) {
            this.parseDeviceInfo(rawDeviceList);
        }
        return rawDeviceList;
    }

    private String getMessageList() throws UnsupportedEncodingException 	{
        rawMessageList = socketOperator.sendHttpRequest(getAuthenticateUserParams(username, password));
        if (rawMessageList != null) {
            this.parseMessageInfo(rawMessageList);
        }
        return rawMessageList;
    }

    private String getAuthenticateUserParams(String usernameText, String passwordText) throws UnsupportedEncodingException
    {
        String params = "username=" + URLEncoder.encode(usernameText,"UTF-8") + "&password="+ URLEncoder.encode(passwordText,"UTF-8") + "&action=" + URLEncoder.encode("authenticateUser","UTF-8")+ "&port=" + URLEncoder.encode(Integer.toString(socketOperator.getListeningPort()),"UTF-8") + "&";

        return params;
    }

    private void parseDeviceInfo(String xml)
    {
        try
        {
            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
            sp.parse(new ByteArrayInputStream(xml.getBytes()), new XMLHandler(MsgService.this));
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void parseMessageInfo(String xml)
    {
        try
        {
            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
            sp.parse(new ByteArrayInputStream(xml.getBytes()), new XMLHandler(MsgService.this));
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String authenticateUser(String usernameText, String passwordText) throws UnsupportedEncodingException {
        this.username = usernameText;
        this.password = passwordText;

        this.authenticatedUser = false;
        String result = this.getDeviceList();

        if(result != null && !result.equals(MobileControl.AUTHENTICATION_FAILED)) {
            this.authenticatedUser = true;

            USERNAME = this.username;
            Intent i = new Intent(DEVICE_LIST_UPDATED);
            i.putExtra(DeviceInfo.DEVICE_LIST, rawDeviceList);
            sendBroadcast(i);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Intent i = new Intent(DEVICE_LIST_UPDATED);
                        Intent i2 = new Intent(MESSAGE_LIST_UPDATED);
                        String tmp = MsgService.this.getDeviceList();
                        String tmp2 = MsgService.this.getMessageList();
                        if (tmp != null) {
                            i.putExtra(DeviceInfo.DEVICE_LIST, tmp);
                            sendBroadcast(i);
                            Log.i("friend list broadcast ", "");

                            if (tmp2 != null) {
                                i2.putExtra(MessageInfo.MESSAGE_LIST, tmp2);
                                sendBroadcast(i2);
                                Log.i("friend list broadcast", "");
                            }
                        }
                        else {
                            Log.i("friend list null", "");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, UPDATE_TIME_PERIOD, UPDATE_TIME_PERIOD);
        }
        return result;
    }

    @Override
    public void messageReceived(String username, String message) {
        MessageInfo msg = MessageController.checkMessage(username, message);
        if(msg != null) {
            Intent i = new Intent(TAKE_MESSAGE);
            i.putExtra(MessageInfo.USER_ID, msg.userId);
            i.putExtra(MessageInfo.MESSAGE_TEXT, msg.messageText);
            sendBroadcast(i);
            String activeFriend = DeviceController.getActiveDevice();
            if (activeFriend == null || !activeFriend.equals(username))
            {
                localStorageHandler.insert(username,this.getUserName(), message);
                showNotification(username, message);
            }
        }
    }

    @Override
    public boolean isNetworkConnected() {
        return connectivityManager.getActiveNetworkInfo().isConnected();
    }

    @Override
    public boolean isUserAuthenticated() {
        return authenticatedUser;
    }

    @Override
    public String getLastRawDeviceList() {
        return this.rawDeviceList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setUserKey(String value)
    {
    }

    @Override
    public void exit() {
        timer.cancel();
        socketOperator.exit();
        socketOperator = null;
        this.stopSelf();
    }

    @Override
    public String signUpUser(String usernameText, String passwordText, String email) {
        String params = "username=" + usernameText + "&password=" + passwordText + "&action=" + "signUpUser"+ "&email=" + email + "&";

        String result = socketOperator.sendHttpRequest(params);

        return result;
    }

    @Override
    public String addNewDeviceRequest(String deviceUserName) {
        String params = "username=" + this.username + "&password=" + this.password + "&action=" + "addNewDevice" + "&deviceUserName=" + deviceUserName + "&";

        String result = socketOperator.sendHttpRequest(params);

        return result;
    }

    @Override
    public String sendDeviceRqResponse(String approvedDeviceList, String discardedDeviceList) {
        String params = "username=" + this.username + "&password=" + this.password + "&action=" + "responseOfDeviceReqs"+ "&approvedDevices=" + approvedDeviceList + "&discardedDevices=" + discardedDeviceList + "&";

        String result = socketOperator.sendHttpRequest(params);

        return result;
    }

    @Override
    public void updateData(MessageInfo[] messages, DeviceInfo[] devices, DeviceInfo[] unapprovedDevices, String userKey) {
        this.setUserKey(userKey);
        MessageController.setMessageInfo(messages);
        int i = 0;
        while (i < messages.length) {
            messageReceived(messages[i].userId, messages[i].messageText);
            i++;
        }
        DeviceController.setDeviceInfo(devices);
        DeviceController.setUnapprovedDevices(unapprovedDevices);
    }
}