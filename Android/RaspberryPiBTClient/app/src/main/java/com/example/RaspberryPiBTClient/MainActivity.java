package com.example.RaspberryPiBTClient;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
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

    // Member object for the Bluetooth service
    private RPiBluetoothService mBluetoothService = null;

    //UI elements
    LinearLayout myLayout = null;
    TextView statusDisplay = null;
    Button connectButton = null; //button used to initiate the connection
    Button dumpButton = null;
    Button queryButton = null;
    DatePickerDialog datePicker = null; //selecting a date for the query
    Button endButton = null;

    /**
     * Check if the device supports bluetooth, quit if not
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myLayout = findViewById(R.id.my_layout);

        statusDisplay = new TextView(this);
        statusDisplay.setText("Initializing");
        myLayout.addView(statusDisplay);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    /**
     * Check if Bluetooth is enabled, ask the user to turn on if not
     */
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

    /**
     * Restart Bluetooth service if previously quit
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == mBluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    /**
     * Set up the application - initializes the Bluetooth service and draws a decent UI
     */
    private void setup() {
        initializeUI();

        // Initialize the BluetoothChatService to perform bluetooth connections
        mBluetoothService = new RPiBluetoothService(this, mHandler);
        updateStatus("Started");
    }

    /**
     * A method for debugging purposes - prints a message to the App UI
     * @param content The message that will be printed to the screen
     */
    private void addToUI(String content) {
        TextView display = new TextView(this);
        display.setText(content);
        myLayout.addView(display);
    }

    /**
     * Update the status message TextView at the top of the IU
     */
    private void updateStatus(String message) {
        statusDisplay.setText(message);
    }

    /**
     * Draws the initial user interface
     */
    private void initializeUI() {
        connectButton = new Button(this);
        connectButton.setText("Connect");
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connect(v);
            }
        });
        myLayout.addView(connectButton);
    }

    /**
     * Draw UI elements once the app has connected to the server
     *
     * Remove initialized UI elements
     * Add elements to send commands to the server
     */
    private void connectedUI() {
        if (connectButton != null) {
            myLayout.removeView(connectButton);
            connectButton = null;
        }

        //Dump button
        dumpButton = new Button(this);
        dumpButton.setText("Dump all data");
        dumpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dumpDatabase();
            }
        });
        myLayout.addView(dumpButton);

        //Query button
        queryButton = new Button(this);
        queryButton.setText("Get data from X date");
        final Context thisContext = this;
        queryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                datePicker = new DatePickerDialog(thisContext);
                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        query(year, month+1 /* DatePicker month is 0 indexed */, day);
                    }
                });
                datePicker.show();
            }
        });

        myLayout.addView(queryButton);

        //Disconnect button
        endButton = new Button(this);
        endButton.setText("Close connection");
        endButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                end();
            }
        });
        myLayout.addView(endButton);
    }

    /**
     * Stop the Bluetooth service when the app loses focus
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth service
        if (mBluetoothService != null) mBluetoothService.stop();
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

    /**
     * Send a request to the server to gracefully close the connection
     */
    public void end() {
        mBluetoothService.end();
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
                            updateStatus("Disconnected");
                            break;
                        case RPiBluetoothService.STATE_CONNECTING:
                            updateStatus("Connecting");
                            break;
                        case RPiBluetoothService.STATE_CONNECTED:
                            updateStatus("Connected");
                            connectedUI();
                            //query(2019, 12, 15);
                            break;
                        default:
                            updateStatus(Integer.toString(msg.arg1));
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

    /**
     * Handles when the user responds to the request to enable Bluetooth
     * @param requestCode
     * @param resultCode
     * @param data
     */
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

    /**
     * Connect to the Raspberry Pi.
     * Hardcoded in.
     * @param v
     */
    public void connect(View v) {

        BluetoothDevice serverDevice = null;

        /* connect to the RPi, which is already paired */
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceMacAddress = device.getAddress();
//                String displayText = "Paired device: " + deviceName + " - " + deviceMacAddress;
                if (deviceName.equals("elderberry")) {
                    serverDevice = device;
//                    displayText += " (!)";
                }

//                addToUI(displayText);
            }
        }

        if (serverDevice != null) {
            mBluetoothService.connect(serverDevice);
        }
    }
}