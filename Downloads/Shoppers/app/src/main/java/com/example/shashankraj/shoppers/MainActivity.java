package com.example.shashankraj.shoppers;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mobstac.beaconstac.core.Beaconstac;
import com.mobstac.beaconstac.core.BeaconstacReceiver;
import com.mobstac.beaconstac.core.MSConstants;
import com.mobstac.beaconstac.core.MSPlace;
import com.mobstac.beaconstac.models.MSAction;
import com.mobstac.beaconstac.models.MSBeacon;
import com.mobstac.beaconstac.models.MSCard;
import com.mobstac.beaconstac.models.MSMedia;
import com.mobstac.beaconstac.utils.MSException;
import com.mobstac.beaconstac.utils.MSLogger;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private ArrayList<MSBeacon> beacons = new ArrayList<>();

    private TextView bCount;
    private TextView camp;
    private TextView time;
    int major = 0 ,minor = 0;
    private int flag=0;
    long entryTime = 0;
    static  int count=15;
    private boolean appInForeground = true;
    public  static boolean isPopupVisible=false;

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;


    private Beaconstac bstac;
    boolean entry=false;

    private boolean registered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        checkPermission();


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            Toast.makeText(this, "Unable to obtain a BluetoothAdapter", Toast.LENGTH_LONG).show();

        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        bstac = Beaconstac.getInstance(this);
        bstac.setRegionParams("F94DBB23-2266-7822-3782-57BEAC0952AC", "com.example.shashankraj.beaconcheck");
        bstac.setUserFacts("bt", count);
        bstac.syncRules();


        try {
            bstac.startRangingBeacons();
        } catch (MSException e) {
            e.printStackTrace();
        }
        init();
    }



    private void init() {
        bCount = (TextView) findViewById(R.id.RangedBeacons);
        camp = (TextView)findViewById(R.id.CampedB) ;
        time = (TextView)findViewById(R.id.timeStamp) ;


        registerBroadcast();
    }

    private void registerBroadcast() {
        if (!registered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_RANGED_BEACON);
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_CAMPED_BEACON);
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_RULE_TRIGGERED);
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_EXITED_BEACON);
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_ENTERED_REGION);
            intentFilter.addAction(MSConstants.BEACONSTAC_INTENT_EXITED_REGION);
            registerReceiver(myBroadcastReceiver, intentFilter);
            registered = true;

        }
    }




    BeaconstacReceiver myBroadcastReceiver = new BeaconstacReceiver() {
        @Override
        public void rangedBeacons(Context context, ArrayList<MSBeacon> rangedBeacons) {

            bCount.setText("RangedBeacons " + rangedBeacons.size());


        }



        @Override
        public void campedOnBeacon(Context context, MSBeacon msBeacon) {

            if (major != msBeacon.getMajor() || minor != msBeacon.getMinor()) {
                camp.setText("Major = " + msBeacon.getMajor() + " Minor = " + msBeacon.getMinor());
                major=msBeacon.getMajor();
                minor=msBeacon.getMinor();
                long currentTime=(System.currentTimeMillis() / 1000) - entryTime;
                time.setText("" + currentTime);
                entryTime=System.currentTimeMillis()/1000;

            }
        }
        @Override
        public void exitedBeacon(Context context, MSBeacon msBeacon) {
            //camp.setText("Camped on None");
        }

        @Override
        public void triggeredRule(final Context context, String ruleName, ArrayList<MSAction> actions) {
            HashMap<String, Object> messageMap;
            if (appInForeground) {
                for (MSAction action : actions) {
                    messageMap = action.getMessage();
                    Log.v(TAG,"BTMessage" +action.getMessage().toString());

                    switch (action.getType()) {
                        case MSActionTypePopup:
                            if (!isPopupVisible) {
                                isPopupVisible = true;
                                Log.v(TAG,"Popup");
                            }
                            break;

                        case MSActionTypeCard:
                            if (!isPopupVisible) {
                                isPopupVisible = true;
                                MSCard card = (MSCard) messageMap.get("card");

                                String title =card.getTitle();
                                Log.v(TAG,"Tile Card : "+ title);


                                switch (card.getType()) {
                                    case MSCardTypePhoto:
                                        Log.v(TAG,"BTPhoto");
                                        break;

                                    case MSCardTypeSummary:
                                        Log.v(TAG,"BTSummary");
                                        break;

                                    case MSCardTypeMedia:
                                        Log.v(TAG,"BTYoutube");
                                        break;
                                }
                            }
                            break;

                        case MSActionTypeWebpage:
                            if (!isPopupVisible) {
                                Log.v(TAG,"BTWebPage");
                                isPopupVisible = true;
                            }
                            break;

                        case MSActionTypeCustom:
                            MSLogger.log("Card id: " + action.getActionID());
                            break;
                    }
                }
            }
        }


        @Override
        public void enteredRegion(Context context, String s) {

        }

        @Override
        public void exitedRegion(Context context, String s) {
            camp.setText("None Camped");
            major=0;
            minor=0;
        }

        @Override
        public void enteredGeofence(Context context, ArrayList<MSPlace> arrayList) {

        }

        @Override
        public void exitedGeofence(Context context, ArrayList<MSPlace> arrayList) {

        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();


    }
    @Override
    protected void onPause(){
        super.onPause();

    }

    @Override
    protected void onResume(){
        super.onResume();

    }
    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
    }

}




