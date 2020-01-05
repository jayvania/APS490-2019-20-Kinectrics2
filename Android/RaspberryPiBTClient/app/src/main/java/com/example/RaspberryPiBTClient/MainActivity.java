package com.example.RaspberryPiBTClient;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;




public class MainActivity extends AppCompatActivity {

    // Message types sent from the RPiBluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_TOAST = 4;

    // Key names received from the RPiBluetoothService Handler
    public static final String TOAST = "toast";

    // Intent request codes
    private final static int REQUEST_ENABLE_BT = 1;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private RPiBluetoothService mBluetoothService = null;

    LinearLayout myLayout;
    Button button;

    BluetoothDevice serverDevice = null; //stores the RPi server

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myLayout = findViewById(R.id.my_layout);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        //ask user to turn Bluetooth on
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) {
                setup();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == mBluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    private void setup() {
        //TODO: Set up initial UI

        // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothService = new RPiBluetoothService(this, mHandler);
        addToUI("Started; connecting");
        connect(null);

    }

    //a method for debugging purposes - prints a message to the UI
    private void addToUI(String content) {
        TextView display = new TextView(this);
        display.setText(content);
        myLayout.addView(display);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth service
        if (mBluetoothService != null) mBluetoothService.stop();
    }

    /*
        JSONObject req = new JSONObject();

        try {
            req.put("request", "DUMP");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        TextView reqDisplay = new TextView(this);
        reqDisplay.setText(req.toString());
        myLayout.addView(reqDisplay);
    */

    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != mBluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "No connected device", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    /**
     * Send a request to dump all data that the server has
     */
    public void dumpDatabase() {
        mBluetoothService.dump();
    }

    /**
     * Send a query to dump all data that the server has, no older than the specified date
     */
    public void query(int year, int month, int day) {
        mBluetoothService.query(year, month, day);
    }

    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch(msg.arg1) {
                        case RPiBluetoothService.STATE_NONE:
                            addToUI("STATE_NONE");
                            break;
                        case RPiBluetoothService.STATE_CONNECTING:
                            addToUI("Connecting");
                            break;
                        case RPiBluetoothService.STATE_CONNECTED:
                            addToUI("Connected");
                            query(2019, 12, 15);
                            break;
                        default:
                            addToUI(Integer.toString(msg.arg1));
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    //TODO: handle a message write here
                    addToUI("Sending message: \n" + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    //TODO: handle a message read here
                    addToUI("Received message: \n" + readMessage);

                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setup();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, "Error: BT not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    public void connect(View v) {

        BluetoothDevice serverDevice = null;

        /* connect to the RPi, which is already paired */
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceMacAddress = device.getAddress();
                String displayText = "Paired device: " + deviceName + " - " + deviceMacAddress;
                if (deviceName.equals("elderberry")) {
                    serverDevice = device;
                    displayText += " (!)";
                }

                TextView deviceDisplay = new TextView(this);
                deviceDisplay.setText(displayText);
                myLayout.addView(deviceDisplay);
            }
        }

        if (serverDevice != null) {
            mBluetoothService.connect(serverDevice);
        }
    }
}