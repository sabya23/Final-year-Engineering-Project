package com.example.sanchita.mobilecontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import interfaces.IAppManager;
import services.MsgService;


public class SignUp extends ActionBarActivity {

    private static final String SERVER_RES_RES_SIGN_UP_SUCCESFULL = "1";
    private static final String SERVER_RES_SIGN_UP_USERNAME_CRASHED = "2";

    private IAppManager appManager;
    private Handler handler = new Handler();

    private EditText sUsername;
    private EditText sPassword;
    private EditText sConfirm;
    private EditText sEmail;
    private Button signUp;
    private Button sCancel;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            appManager = ((MsgService.MsgBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            appManager = null;
            Toast.makeText(SignUp.this, "App service stopped", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        sUsername = (EditText) findViewById(R.id.editSUser);
        sPassword = (EditText) findViewById(R.id.editSPassword);
        sConfirm = (EditText) findViewById(R.id.editSConfirm);
        sEmail = (EditText) findViewById(R.id.editEmail);
        signUp = (Button) findViewById(R.id.buttonSignUp);
        sCancel = (Button) findViewById(R.id.buttonSCancel);

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sUsername.length()>0 && sPassword.length()>0 && sConfirm.length()>0 && sEmail.length()>0) {
                    if(sPassword.getText().toString().equals(sConfirm.getText().toString())) {
                        if(sUsername.length()>=5 && sPassword.length()>=5) {
                            final Thread thread3 = new Thread() {
                                String result = null;
                                @Override
                                public void run() {
                                    result = appManager.signUpUser(sUsername.getText().toString(), sPassword.getText().toString(), sEmail.getText().toString());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(result.equals(SERVER_RES_RES_SIGN_UP_SUCCESFULL)) {
                                                Log.i("Registration complete", "");
                                                Toast.makeText(getApplicationContext(),"Your registration is completed successfully, you can login now", Toast.LENGTH_LONG).show();
                                            }
                                            else if(result.equals(SERVER_RES_SIGN_UP_USERNAME_CRASHED)) {
                                                Log.i("Username exists", "");
                                                Toast.makeText(getApplicationContext(),"Your username have been already taken, please try another username", Toast.LENGTH_LONG).show();
                                            }
                                            else {
                                                Log.i("SignUp failed", "");
                                                Toast.makeText(getApplicationContext(),"Sign up failed because of unknown reason", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                    super.run();
                                }
                            };
                            thread3.start();
                        }
                        else {
                            Toast.makeText(getApplicationContext(),"Username and password length must be at least 5", Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Type same password in password fields", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(),"Please fill all fields", Toast.LENGTH_LONG).show();
                }
            }
        });

        sCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
        bindService(new Intent(SignUp.this, MsgService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_up, menu);
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
}