package com.example.sanchita.mobilecontrol;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import interfaces.IAppManager;
import services.MsgService;


public class MobileControl extends Activity {

    private EditText username;
    private EditText password;
    private Button login;
    private Button cancel;
    private TextView registration;

    private IAppManager appManager;

    public static final String AUTHENTICATION_FAILED = "0";
    public static final int SIGN_UP_ID = Menu.FIRST;
    public static final int EXIT_APP_ID = Menu.FIRST + 1;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            appManager = ((MsgService.MsgBinder) service).getService();

            if(appManager.isUserAuthenticated()) {
                Intent i = new Intent(MobileControl.this, List.class);
                startActivity(i);
                MobileControl.this.finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            appManager = null;
            Toast.makeText(MobileControl.this, "App service stopped", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_control);
        Intent i1 = new Intent(MobileControl.this, MsgService.class);
        startService(i1);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.buttonLogin);
        cancel = (Button) findViewById(R.id.buttonCancel);
        registration = (TextView) findViewById(R.id.txtRegistration);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(appManager == null) {
                    Toast.makeText(getApplicationContext(),"Not connected to the service", Toast.LENGTH_LONG).show();
                }
                else if(!appManager.isNetworkConnected()) {
                    Toast.makeText(getApplicationContext(),"There is no available connected network", Toast.LENGTH_LONG).show();
                }
                else if(username.length() > 0 && password.length() > 0) {
                    Thread loginThread = new Thread() {
                        private Handler handler = new Handler();
                        @Override
                        public void run() {
                            String result = null;
                            try {
                                result = appManager.authenticateUser(username.getText().toString(), password.getText().toString());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            if (result == null || result.equals(AUTHENTICATION_FAILED)) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Please make sure that your username and password are correct", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent i = new Intent(MobileControl.this, List.class);
                                        startActivity(i);
                                    }
                                });
                            }
                        }
                    };
                    loginThread.start();
                }
                else {
                    Toast.makeText(getApplicationContext(),"Please fill both username and password", Toast.LENGTH_LONG).show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appManager.exit();
                finish();
            }
        });

        registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSignUp = new Intent(MobileControl.this, SignUp.class);
                startActivity(intentSignUp);
            }
        });
    }

    @Override
    protected void onPause() {
        unbindService(serviceConnection);
        super.onPause();
    }

    @Override
    protected void onResume() {
        bindService(new Intent(MobileControl.this, MsgService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        menu.add(0, SIGN_UP_ID, 0, "Sign Up");
        menu.add(0, EXIT_APP_ID, 0, "Exit");

        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        switch(item.getItemId())
        {
            case SIGN_UP_ID:
                Intent i = new Intent(MobileControl.this, SignUp.class);
                startActivity(i);
                return true;
            case EXIT_APP_ID:
                cancel.performClick();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
}